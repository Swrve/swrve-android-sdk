package com.swrve.sdk;

import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.service.notification.StatusBarNotification;

import androidx.annotation.RequiresApi;

import com.swrve.sdk.rest.SwrveFilterInputStream;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

class NotificationMediaManager {

    public static final String EXTRA_GIF_URI = "com.swrve.sdk.extra.GIF_URI";
    public static final int NATIVE_GIF_SUPPORT_MIN_API = Build.VERSION_CODES.UPSIDE_DOWN_CAKE;
    private final Context context;
    private final ISwrveCommon swrveCommon;

    NotificationMediaManager(Context context) {
        this.context = context;
        this.swrveCommon = SwrveCommon.getInstance();
    }

    static class BigPictureFetchResult {
        public android.graphics.Bitmap bitmap;
        public Uri mediaUri;
    }

    Bitmap downloadBitmap(final String urlString) {
        Bitmap bitmap = null;
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        try {
            URL url = null;
            if (SwrveHelper.isNotNullOrEmpty(urlString)) {
                url = new URL(urlString);
                url.toURI();
                SwrveLogger.i("Downloading notification image from: %s", urlString);
            }

            if (url != null) {
                connection = openConnection(urlString);
                connection.connect();

                String encoding = connection.getContentEncoding();
                if (encoding != null && encoding.toLowerCase(Locale.ENGLISH).contains("gzip")) {
                    inputStream = new GZIPInputStream(connection.getInputStream());
                } else {
                    inputStream = new SwrveFilterInputStream(connection.getInputStream());
                }
                bitmap = downloadBigPictureBitmap(urlString, inputStream, swrveCommon.getCacheDir(context));
            }
        } catch (Exception e) {
            SwrveLogger.e("Exception downloading notification image:%s", e, urlString);
            QaUser.assetFailedToDownload("notification_image", urlString, getShortExceptionMessage(e));
        } finally {
            try { if (inputStream != null) inputStream.close(); } catch (Exception ignored) {}
            if (connection != null) connection.disconnect();
        }
        return bitmap;
    }

    BigPictureFetchResult downloadBigPictureImage(String urlString, int notificationId) {
        if (SwrveHelper.isNullOrEmpty(urlString) || notificationId <= 0) {
            return null;
        }
        BigPictureFetchResult fetchResult = new BigPictureFetchResult();

        HttpURLConnection connection = null;
        InputStream inputStream = null;
        try {
            connection = openConnection(urlString);
            connection.connect();
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                SwrveLogger.e("Failed to download big picture image:%s (response=%d)", urlString, connection.getResponseCode());
                QaUser.assetFailedToDownload("notification_id:" + notificationId, urlString, "HTTP response: " + connection.getResponseCode());
                return null;
            }

            InputStream filteredInputStream = new SwrveFilterInputStream(connection.getInputStream());
            String encoding = connection.getContentEncoding();
            if (encoding != null && encoding.toLowerCase(java.util.Locale.ENGLISH).contains("gzip")) {
                filteredInputStream = new GZIPInputStream(filteredInputStream);
            }

            inputStream = new BufferedInputStream(filteredInputStream);
            if (Build.VERSION.SDK_INT < NATIVE_GIF_SUPPORT_MIN_API) {
                // device doesn't support animated icons -> decode bitmap directly from stream
                fetchResult.bitmap = downloadBigPictureBitmap(urlString, inputStream, swrveCommon.getCacheDir(context));
            } else {
                boolean isGif = isGif(connection.getContentType(), inputStream);
                if (isGif) {
                    fetchResult.mediaUri = downloadBigPictureUri(inputStream, notificationId);
                } else {
                    fetchResult.bitmap = downloadBigPictureBitmap(urlString, inputStream, swrveCommon.getCacheDir(context));
                }
            }
        } catch (Exception e) {
            SwrveLogger.e("Exception downloading media for notification:%s", e, urlString);
            QaUser.assetFailedToDownload("notification_id:" + notificationId, urlString, getShortExceptionMessage(e));
        } finally {
            try { if (inputStream != null) inputStream.close(); } catch (Exception ignored) {}
            if (connection != null) connection.disconnect();
        }
        return fetchResult;
    }

    private HttpURLConnection openConnection(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
        if (httpConnection instanceof HttpsURLConnection) {
            if (swrveCommon.getSSLSocketFactoryConfig() != null) {
                SSLSocketFactory socketFactory = swrveCommon.getSSLSocketFactoryConfig().getFactory(url.getHost());
                if (socketFactory != null) {
                    ((HttpsURLConnection) httpConnection).setSSLSocketFactory(socketFactory);
                }
            }
        }
        httpConnection.setRequestMethod("GET");
        httpConnection.setRequestProperty("Accept-Encoding", "gzip");
        httpConnection.setConnectTimeout(swrveCommon.getHttpTimeout());
        return httpConnection;
    }

    private Bitmap downloadBigPictureBitmap(String urlString, InputStream inputStream, File cacheDir) {
        int dW = SwrveHelper.getDisplayWidth(context);
        int dH = SwrveHelper.getDisplayHeight(context);
        int minSample = 1;
        return SwrveImageScaler.decodeSampledBitmapFromStream(inputStream, dW, dH, minSample, urlString, cacheDir);
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    Uri downloadBigPictureUri(InputStream input, int notificationId) {
        ContentResolver resolver = context.getContentResolver();
        Uri gifUri = createGifUri(resolver, notificationId);
        if (gifUri == null) {
            return null;
        }

        boolean success = false;
        try (OutputStream os = resolver.openOutputStream(gifUri)) {
            if (os == null) {
                return null;
            }
            byte[] buf = new byte[4096];
            int n;
            while ((n = input.read(buf)) != -1) {
                os.write(buf, 0, n);
            }
            os.flush();
            publishGifUri(resolver, gifUri);
            success = true;
        } catch (Exception e) {
            SwrveLogger.e("Exception downloading GIF for notification:%s", e, notificationId);
        } finally {
            if (!success) {
                deleteGifUri(gifUri);
                gifUri = null;
            }
        }
        return gifUri;
    }

    private boolean isGif(String contentType, InputStream inputStream) throws Exception {
        boolean isGif = false;
        if (contentType != null && contentType.toLowerCase(java.util.Locale.ENGLISH).contains("image/gif")) {
            isGif = true;
        } else if (inputStream.markSupported()) {
            inputStream.mark(16);
            byte[] header = new byte[6];
            int headerRead = inputStream.read(header);
            inputStream.reset();
            if (headerRead >= 6) {
                String magic = new String(header, 0, 6, StandardCharsets.US_ASCII);
                if (magic.startsWith("GIF87a") || magic.startsWith("GIF89a")) {
                    isGif = true;
                }
            }
        }
        return isGif;
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private Uri createGifUri(ContentResolver resolver, int notificationId) {
        Uri contentUri = null;
        try {
            // Do not set MIME type so MediaStore so other apps cannot easily recognise the file type
            ContentValues contentValues = new ContentValues();
            String filename = String.valueOf(notificationId); // Use notificationId as filename but no not use .gif extension so file is slightly hidden
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
            String relativePath = getGifRelativePath();
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath);
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 1);
            contentUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);
        } catch (Exception e) {
            SwrveLogger.e("Failed to insert MediaStore record for GIF", e);
        }
        return contentUri;
    }

    private String getGifRelativePath() {
        String pkgDir = context.getPackageName() + "/animated_gifs";
        return Environment.DIRECTORY_DOWNLOADS + "/" + pkgDir + "/";
    }

    private void publishGifUri(ContentResolver resolver, Uri contentUri) {
        if (context == null || resolver == null || contentUri == null) {
            return;
        }
        try {
            ContentValues done = new ContentValues();
            done.put(MediaStore.MediaColumns.IS_PENDING, 0); // clear pending so system can access the file
            resolver.update(contentUri, done, null, null);
        } catch (Exception e) {
            SwrveLogger.e("Failed to publish GIF MediaStore URI:%s", e, contentUri);
        }
    }

    protected int deleteGifUri(Intent intent) {
        if (context == null || intent == null || SwrveHelper.isNullOrEmpty(intent.getStringExtra(EXTRA_GIF_URI))) {
            return 0;
        }
        int rowsDeleted = 0;
        String uriStr = intent.getStringExtra(EXTRA_GIF_URI);
        try {
            Uri uri = Uri.parse(uriStr);
            rowsDeleted = deleteGifUri(uri);
        } catch (Exception e) {
            SwrveLogger.e("Exception deleting MediaStore URI:%s", e, uriStr);
        }
        return rowsDeleted;
    }

    private int deleteGifUri(Uri uri) {
        if (context == null || uri == null) {
            return 0;
        }
        int rowsDeleted = 0;
        try {
            ContentResolver resolver = context.getContentResolver();
            rowsDeleted = resolver.delete(uri, null, null);
        } catch (Exception e) {
            SwrveLogger.e("Exception deleting MediaStore URI:%s", e, uri);
        }
        return rowsDeleted;
    }

    @RequiresApi(api = NotificationMediaManager.NATIVE_GIF_SUPPORT_MIN_API)
    protected int cleanupOrphanedGifs() {
        if (context == null) {
            return 0;
        }
        int rowsDeleted = 0;
        Set<String> activeNotificationIds = new HashSet<>();
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            StatusBarNotification[] activeNotifications = notificationManager.getActiveNotifications();
            if (activeNotifications != null) {
                for (StatusBarNotification sbn : activeNotifications) {
                    activeNotificationIds.add(String.valueOf(sbn.getId()));
                }
            }
        }

        if (activeNotificationIds.isEmpty()) {
            // Fallback cleanup if the deletePendingIntent wasn't delivered (e.g., low battery or app force-stop).
            // Because orphaned GIFs can only be identified reliably when there are no active notifications, perform a full cleanup only in that case.
            rowsDeleted = deleteAllOrphanedGifs();
        }
        return rowsDeleted;
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private int deleteAllOrphanedGifs() {
        int rowsDeleted = 0;
        ContentResolver resolver = context.getContentResolver();
        String relativePath = getGifRelativePath();
        String selection = MediaStore.MediaColumns.RELATIVE_PATH + " = ?";
        String[] selectionArgs = {relativePath};
        try {
            rowsDeleted = resolver.delete(MediaStore.Downloads.EXTERNAL_CONTENT_URI, selection, selectionArgs);
        } catch (Exception e) {
            SwrveLogger.e("Orphaned GIF cleanup: Exception while deleting all GIFs.", e);
        }
        return rowsDeleted;
    }

    private static String getShortExceptionMessage(Exception e) {
        if (e == null) return "UnknownException";
        String msg = e.getMessage();
        if (msg != null && msg.length() > 100) { // 100 is arbitrary, just need a glimpse of the exception
            msg = msg.substring(0, 100) + "...[truncated]";
        }
        return e.getClass().getSimpleName() + (msg != null ? ": " + msg : "");
    }
}

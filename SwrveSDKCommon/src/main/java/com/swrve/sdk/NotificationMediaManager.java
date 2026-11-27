package com.swrve.sdk;

import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.service.notification.StatusBarNotification;

import androidx.annotation.RequiresApi;

import com.swrve.sdk.rest.SwrveFilterInputStream;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

class NotificationMediaManager {

    public static final String EXTRA_GIF_URI = "com.swrve.sdk.extra.GIF_URI";
    public static final String EXTRA_GIF_URIS = "com.swrve.sdk.extra.GIF_URIS"; // one per image, null where it is a static image
    public static final int NATIVE_GIF_SUPPORT_MIN_API = Build.VERSION_CODES.UPSIDE_DOWN_CAKE;
    private static final String IMAGE_CACHE_DIR = "swrve_notification_images";
    private static final long ORPHANED_GIF_MIN_AGE_MILLIS = 10 * 60 * 1000;
    private static final long DOWNLOAD_IMAGES_TIMEOUT_MILLIS = 60000; // runs in the push worker, which allows ~10 minutes
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

    // Downloads a static image to the cache, if not already cached
    void downloadImage(String url) {
        if (SwrveHelper.isNullOrEmpty(url) || !ensureImageCacheDir() || getCachedImageFile(url).exists()) {
            return;
        }
        Bitmap bitmap = downloadBitmap(url);
        if (bitmap != null) {
            saveCachedImage(bitmap, getCachedImageFile(url));
        }
    }

    // Downloads images that may be GIFs. Static images are cached; GIFs are published to MediaStore.
    // Returns the published GIF uri for each url, in the same order, or null where it is a static image
    ArrayList<String> downloadImages(List<String> urls, int notificationId) {
        ArrayList<String> gifUris = new ArrayList<>(Collections.nCopies(urls.size(), (String) null));
        if (!ensureImageCacheDir()) {
            return gifUris;
        }
        Map<String, String> publishedGifs = new ConcurrentHashMap<>();
        List<Callable<Void>> downloads = new ArrayList<>();
        for (String url : new LinkedHashSet<>(urls)) {
            if (SwrveHelper.isNullOrEmpty(url) || getCachedImageFile(url).exists()) {
                continue;
            }
            downloads.add(() -> {
                BigPictureFetchResult result = downloadBigPictureImage(url, notificationId);
                if (result != null && result.mediaUri != null) {
                    publishedGifs.put(url, result.mediaUri.toString());
                } else if (result != null && result.bitmap != null) {
                    saveCachedImage(result.bitmap, getCachedImageFile(url));
                }
                return null;
            });
        }
        if (downloads.isEmpty()) {
            return gifUris;
        }
        // One download at a time, so only one decoded bitmap is in memory, with an overall deadline so a very slow
        // network can't hold the push worker indefinitely. Images not ready by then show a placeholder. A GIF that
        // lands after the deadline isn't referenced, so it's left for cleanupOrphanedGifs.
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            executor.invokeAll(downloads, getDownloadImagesTimeoutMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            executor.shutdownNow();
        }
        for (int i = 0; i < urls.size(); i++) {
            String url = urls.get(i);
            if (url != null) {
                gifUris.set(i, publishedGifs.get(url));
            }
        }
        return gifUris;
    }

    long getDownloadImagesTimeoutMillis() {
        return DOWNLOAD_IMAGES_TIMEOUT_MILLIS;
    }

    private boolean ensureImageCacheDir() {
        File cacheDir = getImageCacheDir();
        if (!cacheDir.mkdirs() && !cacheDir.isDirectory()) { // another push worker may have just created it
            SwrveLogger.e("Could not create image cache dir: %s", cacheDir);
            return false;
        }
        return true;
    }

    Bitmap getCachedImage(String url) {
        if (SwrveHelper.isNullOrEmpty(url)) {
            return null;
        }
        File file = getCachedImageFile(url);
        if (!file.exists()) {
            return null;
        }
        // already scaled to the screen when downloaded, so no scaling here: display size isn't reliable from a broadcast receiver
        try {
            return BitmapFactory.decodeFile(file.getAbsolutePath());
        } catch (OutOfMemoryError e) {
            SwrveLogger.e("Out of memory decoding cached image:%s", e, file);
            return null;
        }
    }

    // Reads only the file's header, so nothing is decoded. 0 when there's no file. Cached images are stored as they're
    // shown, so this is the shape shown.
    float getImageHeightRatio(String url, String gifUri) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        try {
            if (SwrveHelper.isNotNullOrEmpty(gifUri)) {
                try (InputStream inputStream = context.getContentResolver().openInputStream(Uri.parse(gifUri))) {
                    BitmapFactory.decodeStream(inputStream, null, options);
                }
            } else if (SwrveHelper.isNotNullOrEmpty(url)) {
                File cachedFile = getCachedImageFile(url);
                if (cachedFile.exists()) {
                    BitmapFactory.decodeFile(cachedFile.getAbsolutePath(), options);
                }
            }
        } catch (Exception e) {
            SwrveLogger.e("Exception reading the size of notification image:%s", e, url);
            return 0;
        }
        if (options.outWidth <= 0 || options.outHeight <= 0) {
            return 0;
        }
        return (float) options.outHeight / options.outWidth;
    }

    private void saveCachedImage(Bitmap bitmap, File file) {
        File tempFile = null;
        boolean saved = false;
        try {
            tempFile = File.createTempFile(file.getName(), ".tmp", file.getParentFile()); // unique, as another push may be saving the same image
            try (OutputStream outputStream = new FileOutputStream(tempFile)) {
                saved = bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            }
        } catch (Exception e) {
            saved = false;
            SwrveLogger.e("Exception saving cached image:%s", e, file);
        }
        if (!saved || !tempFile.renameTo(file)) {
            SwrveLogger.e("Could not save cached image:%s", file);
            if (tempFile != null) {
                tempFile.delete();
            }
        }
    }

    File getCachedImageFile(String url) {
        return new File(getImageCacheDir(), SwrveHelper.sha1(url.getBytes(StandardCharsets.UTF_8)));
    }

    private File getImageCacheDir() {
        return new File(swrveCommon.getCacheDir(context), IMAGE_CACHE_DIR);
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
        // without a read timeout a stalled response blocks forever
        httpConnection.setReadTimeout(swrveCommon.getHttpTimeout());
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
        if (context == null || intent == null) {
            return 0;
        }
        Set<String> uris = new LinkedHashSet<>();
        if (SwrveHelper.isNotNullOrEmpty(intent.getStringExtra(EXTRA_GIF_URI))) {
            uris.add(intent.getStringExtra(EXTRA_GIF_URI));
        }
        ArrayList<String> gifUris = intent.getStringArrayListExtra(EXTRA_GIF_URIS);
        if (gifUris != null) {
            for (String uri : gifUris) {
                if (SwrveHelper.isNotNullOrEmpty(uri)) {
                    uris.add(uri);
                }
            }
        }
        int rowsDeleted = 0;
        for (String uriStr : uris) {
            try {
                rowsDeleted += deleteGifUri(Uri.parse(uriStr));
            } catch (Exception e) {
                SwrveLogger.e("Exception deleting MediaStore URI:%s", e, uriStr);
            }
        }
        return rowsDeleted;
    }

    int deleteGifUri(Uri uri) {
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
        // Skip recent GIFs: a push worker may have published them for a notification it hasn't posted yet
        String selection = MediaStore.MediaColumns.RELATIVE_PATH + " = ? AND " + MediaStore.MediaColumns.DATE_ADDED + " < ?";
        String[] selectionArgs = {relativePath, String.valueOf(getOrphanedGifCutoffSeconds())};
        try {
            rowsDeleted = resolver.delete(MediaStore.Downloads.EXTERNAL_CONTENT_URI, selection, selectionArgs);
        } catch (Exception e) {
            SwrveLogger.e("Orphaned GIF cleanup: Exception while deleting all GIFs.", e);
        }
        return rowsDeleted;
    }

    long getOrphanedGifCutoffSeconds() {
        return (System.currentTimeMillis() - ORPHANED_GIF_MIN_AGE_MILLIS) / 1000; // DATE_ADDED is in seconds
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

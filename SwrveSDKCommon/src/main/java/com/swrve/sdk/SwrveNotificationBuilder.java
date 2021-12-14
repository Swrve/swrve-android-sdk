package com.swrve.sdk;

import static com.swrve.sdk.SwrveNotificationConstants.SOUND_DEFAULT;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.core.app.NotificationCompat;

import com.swrve.sdk.notifications.model.SwrveNotification;
import com.swrve.sdk.notifications.model.SwrveNotificationButton;
import com.swrve.sdk.notifications.model.SwrveNotificationChannel;
import com.swrve.sdk.notifications.model.SwrveNotificationExpanded;
import com.swrve.sdk.notifications.model.SwrveNotificationMedia;
import com.swrve.sdk.rest.SwrveFilterInputStream;

import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

public class SwrveNotificationBuilder {

    private final Context context;
    private int iconDrawableId;
    private int iconMaterialDrawableId;
    private NotificationChannel defaultNotificationChannel;
    private int largeIconDrawableId;
    private String accentColorHex;
    private String notificationTitle;
    private final int minSampleSize = 1;

    private boolean usingFallbackDeeplink = false;
    private SwrveNotification swrveNotification;
    private Bundle msg;
    private String msgText;
    protected String campaignType; // accessed in unity subclass
    private Bundle eventPayload;
    private int notificationId;
    protected int requestCode;
    private SwrveNotificationDetails notificationDetails = new SwrveNotificationDetails();

    private static int deviceWidth = 0;
    private static int deviceHeight = 0;

    public SwrveNotificationBuilder(Context context, SwrveNotificationConfig config) {
        this.context = context;
        this.iconDrawableId = config.getIconDrawableId();
        this.iconMaterialDrawableId = config.getIconMaterialDrawableId();
        this.defaultNotificationChannel = config.getDefaultNotificationChannel();
        this.largeIconDrawableId = config.getLargeIconDrawableId();
        this.accentColorHex = config.getAccentColorHex();
        this.notificationId = new Random().nextInt();
        this.requestCode = new Random().nextInt();
    }

    public NotificationCompat.Builder build(String msgText, Bundle msg, String campaignType, Bundle eventPayload) {
        SwrveNotification swrveNotification = parseBundle(msg);
        return build(msgText, msg, swrveNotification, campaignType, eventPayload);
    }

    public NotificationCompat.Builder build(String msgText, Bundle msg, SwrveNotification swrveNotification, String campaignType, Bundle eventPayload) {

        this.msgText = msgText;
        this.msg = msg;
        this.swrveNotification = swrveNotification;
        this.campaignType = campaignType;
        this.eventPayload = eventPayload;

        if (deviceWidth == 0 && deviceHeight == 0) {
            deviceWidth = SwrveHelper.getDisplayWidth(context);
            deviceHeight = SwrveHelper.getDisplayHeight(context);
            if (deviceWidth > deviceHeight) {
                int tmp = this.deviceWidth;
                deviceWidth = deviceHeight;
                deviceHeight = tmp;
            }
        }

        boolean materialDesignIcon = (getSDKVersion() >= android.os.Build.VERSION_CODES.LOLLIPOP);
        int iconResource = (materialDesignIcon && iconMaterialDrawableId >= 0) ? iconMaterialDrawableId : iconDrawableId;

        String notificationChannelId = getNotificationChannelId();

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, notificationChannelId)
                .setSmallIcon(iconResource)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(this.msgText))
                .setTicker(this.msgText)
                .setContentText(this.msgText)
                .setAutoCancel(true);
        notificationDetails.setBody(this.msgText);

        if (largeIconDrawableId >= 0) {
            Bitmap largeIconBitmap = BitmapFactory.decodeResource(context.getResources(), largeIconDrawableId);
            mBuilder.setLargeIcon(largeIconBitmap);
        }

        applyAccentColor(mBuilder);

        String msgSound = msg.getString(SwrveNotificationConstants.SOUND_KEY);
        if (!SwrveHelper.isNullOrEmpty(msgSound)) {
            Uri soundUri;
            if (msgSound.equalsIgnoreCase(SOUND_DEFAULT)) {
                soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            } else {
                String packageName = context.getApplicationContext().getPackageName();
                soundUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + packageName + "/raw/" + msgSound);
            }
            mBuilder.setSound(soundUri);
        }

        if (swrveNotification != null) {
            mBuilder = getNotificationBuilderFromSwrvePayload(mBuilder);
        }

        List<NotificationCompat.Action> actions = getNotificationActions();
        if (actions != null && actions.size() > 0) {
            for (NotificationCompat.Action item : actions) {
                mBuilder.addAction(item);
            }
        }

        if (SwrveHelper.isNullOrEmpty(notificationTitle)) {
            String fallback = getFallbackNotificationTitle();
            SwrveLogger.d("No notification title in configured from server payload so using app name:%s", fallback);
            mBuilder.setContentTitle(fallback);
            notificationDetails.setTitle(fallback);
        }

        PendingIntent pendingIntent = createPendingIntent(msg, campaignType, eventPayload);
        mBuilder.setContentIntent(pendingIntent);

        return mBuilder;
    }

    private SwrveNotification parseBundle(Bundle msg) {
        SwrveNotification swrveNotification = null;
        String swrvePushPayload = msg.getString(SwrveNotificationConstants.SWRVE_PAYLOAD_KEY);
        if (SwrveHelper.isNotNullOrEmpty(swrvePushPayload)) {
            swrveNotification = SwrveNotification.fromJson(swrvePushPayload);
            if (swrveNotification != null && swrveNotification.getNotificationId() > 0) {
                notificationId = swrveNotification.getNotificationId();
            }
        }
        return swrveNotification;
    }

    @TargetApi(value = 26)
    private String getNotificationChannelId() {

        if (getSDKVersion() < Build.VERSION_CODES.O) {
            return null;
        }

        String notificationChannelId = null;
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Check if the channelId came down in payload and use that if its valid.
        if (swrveNotification != null) {
            if (SwrveHelper.isNotNullOrEmpty(swrveNotification.getChannelId())) {
                String payloadChannelId = swrveNotification.getChannelId();
                NotificationChannel payloadChannel = mNotificationManager.getNotificationChannel(payloadChannelId);
                if (payloadChannel == null) {
                    SwrveLogger.w("Notification channel %s from push payload does not exist, using params from payload or the default from config.", payloadChannelId);
                } else {
                    SwrveLogger.i("Notification channel %s from push payload will be used instead of config.", payloadChannelId);
                    notificationChannelId = payloadChannelId;
                }
            }

            // If no channel has been selected and one was provided in the payload, create it from the payload attributes
            SwrveNotificationChannel channelInfo = swrveNotification.getChannel();
            if (notificationChannelId == null && channelInfo != null) {
                NotificationChannel payloadChannel = mNotificationManager.getNotificationChannel(channelInfo.getId());
                notificationChannelId = channelInfo.getId();
                if (payloadChannel != null) {
                    SwrveLogger.i("Notification channel %s from push payload already exists.", notificationChannelId);
                } else {
                    NotificationChannel newChannel = new NotificationChannel(channelInfo.getId(), channelInfo.getName(), channelInfo.getAndroidImportance());
                    mNotificationManager.createNotificationChannel(newChannel);
                }
            }
        }

        // Use the default from config if none available from payload
        SwrveCommon.checkInstanceCreated(); // throws RuntimeException
        ISwrveCommon swrveCommon = SwrveCommon.getInstance();
        if (notificationChannelId == null && swrveCommon != null) {
            if (defaultNotificationChannel != null) {
                NotificationChannel existingChannel = mNotificationManager.getNotificationChannel(defaultNotificationChannel.getId());
                if (existingChannel == null) {
                    SwrveLogger.i("Notification channel from default config[%s] does not exist, creating it", defaultNotificationChannel.getId());
                    mNotificationManager.createNotificationChannel(defaultNotificationChannel);
                }
                notificationChannelId = defaultNotificationChannel.getId();
            }
        }

        if (notificationChannelId == null) {
            SwrveLogger.e("Notification channel could not be found, the swrve notification cannot be shown.");
        }

        return notificationChannelId;
    }

    private void applyAccentColor(NotificationCompat.Builder builder) {
        if (SwrveHelper.isNullOrEmpty(accentColorHex)) {
            return;
        }
        try {
            int color = Color.parseColor(accentColorHex);
            builder.setColor(color); // if is an invalid HexColor, we do not set it.
        } catch (Exception e) {
            SwrveLogger.e("Exception getting accent color for notification.");
        }
    }

    private NotificationCompat.Builder getNotificationBuilderFromSwrvePayload(NotificationCompat.Builder builder) {

        if (swrveNotification.getVersion() > SwrveNotificationConstants.SWRVE_PUSH_VERSION) {
            SwrveLogger.i("Notification version is greater than version that this sdk can show. Showing default");
            return builder;
        }

        // Base Title
        if (SwrveHelper.isNotNullOrEmpty(swrveNotification.getTitle())) {
            notificationTitle = swrveNotification.getTitle();
            builder.setContentTitle(swrveNotification.getTitle());
            notificationDetails.setTitle(swrveNotification.getTitle());
        }

        // Base Subtitle
        if (SwrveHelper.isNotNullOrEmpty(swrveNotification.getSubtitle())) {
            builder.setSubText(swrveNotification.getSubtitle());
        }

        // Accent Color
        if (SwrveHelper.isNotNullOrEmpty(swrveNotification.getAccent())) {
            builder.setColor(Color.parseColor(swrveNotification.getAccent()));
        }

        // Icon
        if (SwrveHelper.isNotNullOrEmpty(swrveNotification.getIconUrl())) {
            Bitmap icon = getImageFromUrl(swrveNotification.getIconUrl());
            if (icon != null) {
                builder.setLargeIcon(icon);
            }
        }

        // Visibility
        buildVisibility(builder);

        // Base Ticker
        if (SwrveHelper.isNotNullOrEmpty(swrveNotification.getTicker())) {
            builder.setTicker(swrveNotification.getTicker());
        }

        // Notification Priority (checks if it's not zero since default doesn't need to be set)
        if (swrveNotification.getPriority() != 0) {
            builder.setPriority(swrveNotification.getPriority());
        }

        // set Default style for expanded content
        NotificationCompat.Style defaultStyle = buildDefaultStyle(swrveNotification);
        if (defaultStyle != null) {
            builder.setStyle(defaultStyle);
        }

        // if media is present apply a different template based on type
        buildMediaText(builder);

        buildLockScreen(builder, swrveNotification);

        return builder;
    }

    private void buildVisibility(NotificationCompat.Builder builder) {
        if (swrveNotification.getVisibility() != null) {
            switch (swrveNotification.getVisibility()) {
                case PUBLIC:
                    builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
                    break;
                case PRIVATE:
                    builder.setVisibility(NotificationCompat.VISIBILITY_PRIVATE);
                    break;
                case SECRET:
                    builder.setVisibility(NotificationCompat.VISIBILITY_SECRET);
                    break;
                default:
                    builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
                    break;
            }
        }
    }

    private void buildMediaText(NotificationCompat.Builder builder) {
        SwrveNotificationMedia media = swrveNotification.getMedia();
        if (media != null && media.getType() != null) {
            // Media is present so apply a different template based on type
            NotificationCompat.Style mediaStyle = buildNotificationStyle(media.getType(), false, swrveNotification);
            if (mediaStyle != null) {
                builder.setStyle(mediaStyle);
                setMediaText(builder);
                if (usingFallbackDeeplink) {
                    msg.putString(SwrveNotificationConstants.DEEPLINK_KEY, media.getFallbackSd());
                }
            }
        }
    }

    private void setMediaText(NotificationCompat.Builder mBuilder) {

        SwrveNotificationMedia media = swrveNotification.getMedia();
        if (media != null) {
            if (SwrveHelper.isNotNullOrEmpty(media.getTitle())) {
                notificationTitle = media.getTitle();
                mBuilder.setContentTitle(media.getTitle());
                notificationDetails.setTitle(media.getTitle());
            }

            if (SwrveHelper.isNotNullOrEmpty(media.getSubtitle())) {
                mBuilder.setSubText(media.getSubtitle());
            }

            if (SwrveHelper.isNotNullOrEmpty(media.getBody())) {
                mBuilder.setContentText(media.getBody());
                notificationDetails.setBody(media.getBody());
                // If ticker is not set from earlier, set the body to it
                if (SwrveHelper.isNullOrEmpty(swrveNotification.getTicker())) {
                    mBuilder.setTicker(media.getBody());
                }
            }
        }
    }

    @SuppressLint("WrongConstant")
    private void buildLockScreen(NotificationCompat.Builder builder, SwrveNotification pushPayload) {
        if (getSDKVersion() >= Build.VERSION_CODES.LOLLIPOP) {
            if (SwrveHelper.isNotNullOrEmpty(pushPayload.getLockScreenMsg())) {
                // Use the notification builder to build a copy of the private notification with different lock screen message text

                // Create a public visible notification version
                builder.setTicker(pushPayload.getLockScreenMsg());
                builder.setContentText(pushPayload.getLockScreenMsg());
                Notification lockScreenNotification = builder.build();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    lockScreenNotification.visibility = NotificationCompat.VISIBILITY_PUBLIC;
                }
                builder.setPublicVersion(lockScreenNotification);

                // Reset changed values
                builder.setTicker(msgText);
                if (SwrveHelper.isNotNullOrEmpty(pushPayload.getTicker())) {
                    builder.setTicker(pushPayload.getTicker());
                }
                setMediaText(builder);
            }
        }
    }

    private NotificationCompat.Style buildNotificationStyle(SwrveNotificationMedia.MediaType type, Boolean fallback, SwrveNotification payload) {
        NotificationCompat.Style responseStyle;
        SwrveNotificationMedia media = payload.getMedia();
        if (type == null) {
            // Enters here in the case fallback is null. if there's no fallback type then media failed
            return null;
        }

        switch (type) {
            case IMAGE:
                NotificationCompat.BigPictureStyle bigPictureStyle = new NotificationCompat.BigPictureStyle();

                if (!fallback) {
                    // Main Big Picture
                    if (SwrveHelper.isNotNullOrEmpty(media.getUrl())) {

                        Bitmap bigImage = getImageFromUrl(media.getUrl());
                        if (bigImage != null) {
                            bigPictureStyle.bigPicture(bigImage);
                            notificationDetails.setMediaUrl(media.getUrl());
                            notificationDetails.setMediaBitmap(bigImage);
                        } else {
                            // If m_url failed to download, traverse the same switch with fallback type
                            return buildNotificationStyle(media.getFallbackType(), true, payload);
                        }
                    } else {
                        // Both have failed, return null.
                        return null;
                    }
                } else {
                    Bitmap fallbackImage = getImageFromUrl(media.getFallbackUrl());
                    if (fallbackImage != null) {
                        bigPictureStyle.bigPicture(fallbackImage);
                    } else {
                        // If m_fallback_url failed to download, revert to bigText
                        return null;
                    }

                    if (media.getFallbackSd() != null) {
                        // If there's a fallback deep link available and image bitmap is not null
                        usingFallbackDeeplink = true;
                    }
                }

                SwrveNotificationExpanded expanded = payload.getExpanded();
                if (expanded != null) {
                    // Expanded Icon
                    if (SwrveHelper.isNotNullOrEmpty(expanded.getIconUrl())) {
                        bigPictureStyle.bigLargeIcon(getImageFromUrl(expanded.getIconUrl()));
                    }
                    // Expanded Title
                    if (SwrveHelper.isNotNullOrEmpty(expanded.getTitle())) {
                        bigPictureStyle.setBigContentTitle(expanded.getTitle());
                        notificationDetails.setExpandedTitle(expanded.getTitle());
                    }
                    // Expanded Body
                    if (SwrveHelper.isNotNullOrEmpty(expanded.getBody())) {
                        // Summary Text in bigPicture places text in the same place as bigText
                        // so it keeps the format consistent by placing expanded body here.
                        bigPictureStyle.setSummaryText(expanded.getBody());
                        notificationDetails.setExpandedBody(expanded.getBody());
                    }
                }
                responseStyle = bigPictureStyle;
                break;
            default:
                responseStyle = buildDefaultStyle(payload);
                break;
        }
        return responseStyle;
    }

    private NotificationCompat.Style buildDefaultStyle(SwrveNotification payload) {
        NotificationCompat.Style responseStyle = null;
        NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
        SwrveNotificationExpanded expanded = payload.getExpanded();
        if (expanded != null) {
            if (SwrveHelper.isNotNullOrEmpty(expanded.getTitle())) {
                bigTextStyle.setBigContentTitle(expanded.getTitle()); // Expanded Title
                notificationDetails.setExpandedTitle(expanded.getTitle());
                responseStyle = bigTextStyle;
            }
            if (SwrveHelper.isNotNullOrEmpty(expanded.getBody())) {
                bigTextStyle.bigText(expanded.getBody()); // Expanded Body
                notificationDetails.setExpandedBody(expanded.getBody());
                responseStyle = bigTextStyle;
            }
        }
        return responseStyle;
    }

    private List<NotificationCompat.Action> getNotificationActions() {
        String swrvePayloadKey = msg.getString(SwrveNotificationConstants.SWRVE_PAYLOAD_KEY);

        if(SwrveHelper.isNullOrEmpty(swrvePayloadKey)){
            // there's no payload available in the Bundle
            return null;
        }

        SwrveNotification payload = SwrveNotification.fromJson(swrvePayloadKey);
        if (payload == null) {
            //payload cannot be parsed
            return null;
        }

        if(payload.getVersion() > SwrveNotificationConstants.SWRVE_PUSH_VERSION) {
            // push version isn't correct, don't render anything
            return null;
        }

        List<NotificationCompat.Action> actions = new ArrayList<>();
        List<SwrveNotificationButton> buttons = payload.getButtons();
        if(buttons != null && !buttons.isEmpty()){
            for(int i = 0; i < buttons.size(); i++){
                SwrveNotificationButton button = buttons.get(i);
                String actionKey = String.valueOf(i);
                NotificationCompat.Action action = createNotificationAction(button.getTitle(), SwrveNotificationConstants.NO_ACTION_ICON, actionKey, button.getActionType(), button.getAction());
                actions.add(action);
            }
        }
        return actions;
    }

    private NotificationCompat.Action createNotificationAction(String buttonText, int icon, String actionKey, SwrveNotificationButton.ActionType actionType, String actionUrl) {
        boolean isDismissAction = actionType == SwrveNotificationButton.ActionType.DISMISS;
        Intent intent = createButtonIntent(context, msg, actionType, isDismissAction);
        intent.putExtra(SwrveNotificationConstants.CONTEXT_ID_KEY, actionKey);
        intent.putExtra(SwrveNotificationConstants.PUSH_ACTION_TYPE_KEY, actionType);
        intent.putExtra(SwrveNotificationConstants.PUSH_ACTION_URL_KEY, actionUrl);
        intent.putExtra(SwrveNotificationConstants.BUTTON_TEXT_KEY, buttonText);
        intent.putExtra(SwrveNotificationConstants.EVENT_PAYLOAD, eventPayload);
        int flags = PendingIntent.FLAG_CANCEL_CURRENT;
        if (getSDKVersion() >= Build.VERSION_CODES.M) {
            flags = PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntentButton = getPendingIntent(getSDKVersion(), intent, flags, isDismissAction);
        return new NotificationCompat.Action.Builder(icon, buttonText, pendingIntentButton).build();
    }

    // Called by Unity - overridden in Unity version of SwrveNotificationBuilder
    public Intent createButtonIntent(Context context, Bundle msg, SwrveNotificationButton.ActionType actionType, boolean isDismissAction) {
        Class clazz = getIntentClass(getSDKVersion(), isDismissAction);
        Intent intent = new Intent(context, clazz);
        intent.putExtra(SwrveNotificationConstants.PUSH_BUNDLE, msg);
        intent.putExtra(SwrveNotificationConstants.PUSH_NOTIFICATION_ID, notificationId);
        intent.putExtra(SwrveNotificationConstants.CAMPAIGN_TYPE, campaignType);
        return intent;
    }

    protected PendingIntent createPendingIntent(Bundle msg, String campaignType, Bundle eventPayload) {
        Intent intent = createIntent(msg, campaignType, eventPayload);
        int flags = PendingIntent.FLAG_CANCEL_CURRENT;
        if (getSDKVersion() >= Build.VERSION_CODES.M) {
            flags = PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = getPendingIntent(getSDKVersion(), intent, flags, false);
        return pendingIntent;
    }

    private Intent createIntent(Bundle msg, String campaignType, Bundle eventPayload) {
        Class clazz = getIntentClass(getSDKVersion(), false);
        Intent intent = new Intent(context, clazz);
        intent.putExtra(SwrveNotificationConstants.PUSH_BUNDLE, msg);
        intent.putExtra(SwrveNotificationConstants.PUSH_NOTIFICATION_ID, notificationId);
        intent.putExtra(SwrveNotificationConstants.CAMPAIGN_TYPE, campaignType);
        intent.putExtra(SwrveNotificationConstants.EVENT_PAYLOAD, eventPayload);
        return intent;
    }

    // Called by Unity - overridden in Unity version of SwrveNotificationBuilder
    public Class getIntentClass(int sdkVersion, boolean isDismissAction) {
        Class clazz;
        // A dismiss action should dismiss the notification without opening the app
        if (sdkVersion >= 31 && !isDismissAction) { // Unity compile level is 30 has does not contain Build.VERSION_CODES.S constant
            clazz = SwrveNotificationEngageActivity.class;
        } else {
            clazz = SwrveNotificationEngageReceiver.class;
        }
        return clazz;
    }

    protected PendingIntent getPendingIntent(int sdkVersion, Intent intent, int flags, boolean isDismissAction) {
        PendingIntent pendingIntent;
        if (sdkVersion >= 31 && !isDismissAction) { // Unity compile level is 30 has does not contain Build.VERSION_CODES.S constant
            pendingIntent = PendingIntent.getActivity(context, requestCode++, intent, flags);
        } else {
            pendingIntent = PendingIntent.getBroadcast(context, requestCode++, intent, flags);
        }
        return pendingIntent;
    }

    protected Date getNow() {
        return new Date();
    }

    private String getFallbackNotificationTitle() {
        String pushTitle = "";
        try {
            PackageManager packageManager = context.getPackageManager();
            ApplicationInfo app = packageManager.getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            CharSequence appTitle = app.loadLabel(packageManager);
            if (appTitle != null) {
                pushTitle = appTitle.toString();
            }

        } catch (Exception e) {
            SwrveLogger.e("Exception getting fallback notification title.", e);
        }
        return pushTitle;
    }

    protected Bitmap getImageFromUrl(final String url) {
        Bitmap bitmap = getImageFromCache(url);
        if (bitmap == null) {
            bitmap = downloadBitmapImageFromUrl(url);
        }
        return bitmap;
    }

    protected Bitmap downloadBitmapImageFromUrl(final String mediaUrl) {
        Bitmap bitmap = null;
        InputStream inputStream = null;
        try {
            URL url = null;
            if (SwrveHelper.isNotNullOrEmpty(mediaUrl)) {
                url = new URL(mediaUrl);
                url.toURI();
                SwrveLogger.i("Downloading notification image from: %s", mediaUrl);
            }

            if (url != null) {
                HttpsURLConnection httpsConnection = (HttpsURLConnection) url.openConnection();
                if (SwrveCommon.getInstance().getSSLSocketFactoryConfig() != null) {
                    SSLSocketFactory socketFactory = SwrveCommon.getInstance().getSSLSocketFactoryConfig().getFactory(url.getHost());
                    if (socketFactory != null) {
                        httpsConnection.setSSLSocketFactory(socketFactory);
                    }
                }
                httpsConnection.setDoInput(true);
                httpsConnection.setConnectTimeout(SwrveCommon.getInstance().getHttpTimeout());
                httpsConnection.setRequestProperty("Accept-Encoding", "gzip");
                httpsConnection.connect();

                inputStream = new SwrveFilterInputStream(httpsConnection.getInputStream());

                // Support gzip if possible
                String encoding = httpsConnection.getContentEncoding();
                if (encoding != null && encoding.toLowerCase(Locale.ENGLISH).contains("gzip")) {
                    inputStream = new GZIPInputStream(inputStream);
                }
                bitmap = SwrveImageScaler.decodeSampledBitmapFromStream(inputStream, deviceWidth, deviceHeight, minSampleSize, mediaUrl, context.getCacheDir());
            }
        } catch (Exception e) {
            SwrveLogger.e("Exception downloading notification image:%s", e, mediaUrl);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception e) {
                    SwrveLogger.e("Exception closing stream for downloading notification image.", e);
                }
            }
        }
        return bitmap;
    }

    protected Bitmap getImageFromCache(final String url) {
        Bitmap bitmap = null;
        try {
            ISwrveCommon swrveCommon = SwrveCommon.getInstance();
            File cacheDir = swrveCommon.getCacheDir(context);
            String hashedUrl = SwrveHelper.md5(url.toLowerCase(Locale.ENGLISH));
            File file = new File(cacheDir, hashedUrl);
            if (file.exists() && file.canRead()) {
                bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                SwrveLogger.v("Using cached notification image:%s", url);
            }
        } catch (Exception e) {
            SwrveLogger.e("Exception trying to get notification image from cache.", e);
        }
        return bitmap;
    }

    protected int getSDKVersion() {
        return Build.VERSION.SDK_INT;
    }

    public int getNotificationId() {
        return notificationId;
    }

    public SwrveNotificationDetails getNotificationDetails() {
        return notificationDetails;
    }
}

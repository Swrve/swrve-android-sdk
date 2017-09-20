package com.swrve.sdk;

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
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;

import com.swrve.sdk.model.PushPayloadButton;
import com.swrve.sdk.model.PushPayloadChannel;
import com.swrve.sdk.model.PushPayloadExpanded;
import com.swrve.sdk.model.PushPayloadMedia;
import com.swrve.sdk.model.PushPayload;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Used internally to build a local notification from a push notification.
 */
public class SwrvePushNotificationConfig {

    private static final String TAG = "SwrvePush";

    private static final String SWRVE_PUSH_ICON_METADATA = "SWRVE_PUSH_ICON";
    private static final String SWRVE_PUSH_ICON_MATERIAL_METADATA = "SWRVE_PUSH_ICON_MATERIAL";
    private static final String SWRVE_PUSH_ICON_LARGE_METADATA = "SWRVE_PUSH_ICON_LARGE";
    private static final String SWRVE_PUSH_ACCENT_COLOR_METADATA = "SWRVE_PUSH_ACCENT_COLOR";
    private static final String SWRVE_PUSH_ACTIVITY_METADATA = "SWRVE_PUSH_ACTIVITY";
    private static final String SWRVE_PUSH_TITLE_METADATA = "SWRVE_PUSH_TITLE";

    private static SwrvePushNotificationConfig instance;

    private final Class<?> activityClass;
    private final int iconDrawableId;
    private final int iconMaterialDrawableId;
    private final Bitmap largeIconDrawable;
    private final Integer accentColorObject;
    private final String notificationTitle;
    private boolean usingFallbackDeeplink;
    private PushPayload pushPayload;
    protected SwrvePushMediaHelper mediaHelper;

    // Called by Unity Swrve SDK
    public SwrvePushNotificationConfig(Class<?> activityClass, int iconDrawableId, int iconMaterialDrawableId, Bitmap largeIconDrawable, Integer accentColorObject, String notificationTitle) {
        this.activityClass = activityClass;
        this.iconDrawableId = iconDrawableId;
        this.iconMaterialDrawableId = iconMaterialDrawableId;
        this.largeIconDrawable = largeIconDrawable;
        this.accentColorObject = accentColorObject;
        this.notificationTitle = notificationTitle;
        this.usingFallbackDeeplink = false;
        this.mediaHelper = new SwrvePushMediaHelper();
    }

    // Called by Swrve SDK Native
    public static SwrvePushNotificationConfig getInstance(Context context) {
        if (instance == null) {
            instance = createNotificationFromMetaData(context);
        }
        return instance;
    }

    private static SwrvePushNotificationConfig createNotificationFromMetaData(Context context) {
        SwrvePushNotificationConfig swrvePushNotificationConfig = null;
        try {
            PackageManager packageManager = context.getPackageManager();
            ApplicationInfo app = packageManager.getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            Bundle metaData = app.metaData;
            if (metaData == null) {
                throw new RuntimeException("No SWRVE metadata specified in AndroidManifest.xml");
            }

            int iconId = metaData.getInt(SWRVE_PUSH_ICON_METADATA, -1);
            if (iconId < 0) {
                // Default to the application icon
                iconId = app.icon;
            }

            int iconMaterialId = metaData.getInt(SWRVE_PUSH_ICON_MATERIAL_METADATA, -1);
            if (iconMaterialId < 0) {
                // No material (Android L+) icon specified in the metadata
                SwrveLogger.w(TAG, "No " + SWRVE_PUSH_ICON_MATERIAL_METADATA + " specified. We recommend setting a special material icon for Android L+");
            }

            Bitmap largeIconBitmap = null;
            int largeIconBitmapId = metaData.getInt(SWRVE_PUSH_ICON_LARGE_METADATA, -1);
            if (largeIconBitmapId < 0) {
                // No large icon specified in the metadata
                SwrveLogger.w(TAG, "No " + SWRVE_PUSH_ICON_LARGE_METADATA + " specified. We recommend setting a large image for your notifications");
            } else {
                largeIconBitmap = BitmapFactory.decodeResource(context.getResources(), largeIconBitmapId);
            }

            int accentColorResourceId = metaData.getInt(SWRVE_PUSH_ACCENT_COLOR_METADATA, -1);
            Integer accentColorObject = null;
            if (accentColorResourceId < 0) {
                // No accent color specified in the metadata
                SwrveLogger.w(TAG, "No " + SWRVE_PUSH_ACCENT_COLOR_METADATA + " specified. We recommend setting an accent color for your notifications");
            } else {
                accentColorObject = ContextCompat.getColor(context, accentColorResourceId);
            }

            Class<?> pushActivityClass = null;
            String pushActivity = metaData.getString(SWRVE_PUSH_ACTIVITY_METADATA);
            if (SwrveHelper.isNullOrEmpty(pushActivity)) {
                ResolveInfo resolveInfo = packageManager.resolveActivity(
                        packageManager.getLaunchIntentForPackage(context.getPackageName()),
                        PackageManager.MATCH_DEFAULT_ONLY);

                if (resolveInfo != null) {
                    pushActivity = resolveInfo.activityInfo.name;
                } else {
                    // No activity specified in the metadata
                    throw new RuntimeException("No " + SWRVE_PUSH_ACTIVITY_METADATA + " specified in AndroidManifest.xml");
                }
            }

            if (pushActivity != null) {
                // Check that the Activity exists and can be found
                pushActivityClass = getClassForActivityClassName(context, pushActivity);
                if (pushActivityClass == null) {
                    throw new RuntimeException("The Activity with name " + pushActivity + " could not be found");
                }
            }

            String pushTitle = metaData.getString(SWRVE_PUSH_TITLE_METADATA);
            if (SwrveHelper.isNullOrEmpty(pushTitle)) {
                CharSequence appTitle = app.loadLabel(packageManager);
                if (appTitle != null) {
                    pushTitle = appTitle.toString();
                }
                if (SwrveHelper.isNullOrEmpty(pushTitle)) {
                    // No title specified in the metadata and could not find default
                    throw new RuntimeException("No " + SWRVE_PUSH_TITLE_METADATA + " specified in AndroidManifest.xml");
                }
            }

            swrvePushNotificationConfig = new SwrvePushNotificationConfig(pushActivityClass, iconId, iconMaterialId, largeIconBitmap, accentColorObject, pushTitle);
        } catch (Exception ex) {
            SwrveLogger.e(TAG, "Error creating push notification from metadata", ex);
        }
        return swrvePushNotificationConfig;
    }

    private static Class<?> getClassForActivityClassName(Context context, String className) throws ClassNotFoundException {
        if (!SwrveHelper.isNullOrEmpty(className)) {
            if (className.startsWith(".")) {
                className = context.getPackageName() + className; // Append application package as it starts with .
            }
            return Class.forName(className);
        }
        return null;
    }

    Class<?> getActivityClass() {
        return activityClass;
    }

    public NotificationCompat.Builder createNotificationBuilder(Context context, String msgText, Bundle msg, int notificationId) {

        parsePushPayload(msg);

        boolean materialDesignIcon = (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP);
        int iconResource = (materialDesignIcon && iconMaterialDrawableId >= 0) ? iconMaterialDrawableId : iconDrawableId;

        NotificationChannel defaultNotificationChannel = SwrvePushSDK.getInstance().getDefaultNotificationChannel();
        String notificationChannelId = getNotificationChannelId(context, defaultNotificationChannel);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, notificationChannelId)
                .setSmallIcon(iconResource)
                .setContentTitle(notificationTitle)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(msgText))
                .setTicker(msgText)
                .setContentText(msgText)
                .setAutoCancel(true);

        if (largeIconDrawable != null) {
            mBuilder.setLargeIcon(largeIconDrawable);
        }

        if (accentColorObject != null) {
            mBuilder.setColor(accentColorObject);
        }

        String msgSound = msg.getString("sound");
        if (!SwrveHelper.isNullOrEmpty(msgSound)) {
            Uri soundUri;
            if (msgSound.equalsIgnoreCase("default")) {
                soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            } else {
                String packageName = context.getApplicationContext().getPackageName();
                soundUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + packageName + "/raw/" + msgSound);
            }
            mBuilder.setSound(soundUri);
        }

        if (pushPayload != null) {
            mBuilder = getNotificationBuilderFromSwrvePayload(mBuilder, msg);
        }


        List<NotificationCompat.Action> actions = getNotificationActions(context, msg, notificationId);
        if (actions != null && actions.size() > 0) {
            for (NotificationCompat.Action item : actions) {
                mBuilder.addAction(item);
            }
        }

        return mBuilder;
    }

    private void parsePushPayload(Bundle msg){
        String swrvePushPayload = msg.getString(SwrvePushConstants.SWRVE_PAYLOAD_KEY);
        if (SwrveHelper.isNotNullOrEmpty(swrvePushPayload)) {
            pushPayload = PushPayload.fromJson(swrvePushPayload);
        }
    }

    @TargetApi(value = 26)
    private String getNotificationChannelId(Context context, NotificationChannel defaultChannel) {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return null;
        }

        String notificationChannelId = null;
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Check if the channelId came down in payload and use that if its valid.
        if (pushPayload != null) {
            if (SwrveHelper.isNotNullOrEmpty(pushPayload.getChannelId())) {
                String payloadChannelId = pushPayload.getChannelId();
                NotificationChannel payloadChannel = mNotificationManager.getNotificationChannel(payloadChannelId);
                if (payloadChannel == null) {
                    SwrveLogger.w("Notification channel " + payloadChannelId + " from push payload does not exist, using params from payload or the default from config.");
                } else {
                    SwrveLogger.i("Notification channel " + payloadChannelId + " from push payload will be used instead of config.");
                    notificationChannelId = payloadChannelId;
                }
            }

            // If no channel has been selected and one was provided in the payload, create it from the payload attributes
            PushPayloadChannel channelInfo = pushPayload.getChannel();
            if (notificationChannelId == null && channelInfo != null) {
                NotificationChannel payloadChannel = mNotificationManager.getNotificationChannel(channelInfo.getId());
                notificationChannelId = channelInfo.getId();
                if (payloadChannel != null) {
                    SwrveLogger.i("Notification channel " + notificationChannelId + " from push payload already exists.");
                } else {
                    NotificationChannel newChannel = new NotificationChannel(channelInfo.getId(), channelInfo.getName(), channelInfo.getAndroidImportance());
                    mNotificationManager.createNotificationChannel(newChannel);
                }
            }
        }

        // Use the default from config if none available from payload
        if (notificationChannelId == null && defaultChannel != null) {
            NotificationChannel existingChannel = mNotificationManager.getNotificationChannel(defaultChannel.getId());
            if (existingChannel == null) {
                SwrveLogger.i("Notification channel " + defaultChannel.getId() + " does not exist, creating it");
                mNotificationManager.createNotificationChannel(defaultChannel);
            }
            notificationChannelId = defaultChannel.getId();
        }

        if(notificationChannelId == null) {
            SwrveLogger.e("Notification channel could not be found, the swrve notification cannot be shown.");
        }

        return notificationChannelId;
    }

    private NotificationCompat.Builder getNotificationBuilderFromSwrvePayload(NotificationCompat.Builder mBuilder, Bundle msg) {

        if (pushPayload.getVersion() > SwrvePushConstants.SWRVE_PUSH_VERSION) {
            // version number is beyond this SDK. return to default
            return mBuilder;
        }

        // Base Title
        if (SwrveHelper.isNotNullOrEmpty(pushPayload.getTitle())) {
            mBuilder.setContentTitle(pushPayload.getTitle());
        }

        // Base Subtitle
        if (SwrveHelper.isNotNullOrEmpty(pushPayload.getSubtitle())) {
            mBuilder.setSubText(pushPayload.getSubtitle());
        }

        // Accent Color
        if (SwrveHelper.isNotNullOrEmpty(pushPayload.getAccent())) {
            mBuilder.setColor(Color.parseColor(pushPayload.getAccent()));
        }

        // Icon
        if (SwrveHelper.isNotNullOrEmpty(pushPayload.getIconUrl())) {
            Bitmap icon = mediaHelper.downloadBitmapImageFromUrl(pushPayload.getIconUrl());
            if (icon != null) {
                mBuilder.setLargeIcon(icon);
            }
        }

        // Visibility
        if (pushPayload.getVisibility() != null) {
            switch (pushPayload.getVisibility()) {
                case PUBLIC:
                    mBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
                    break;
                case PRIVATE:
                    mBuilder.setVisibility(NotificationCompat.VISIBILITY_PRIVATE);
                    break;
                case SECRET:
                    mBuilder.setVisibility(NotificationCompat.VISIBILITY_SECRET);
                    break;
                default:
                    mBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
                    break;
            }
        }

        // Base Ticker
        String ticker = pushPayload.getTicker();
        if (SwrveHelper.isNotNullOrEmpty(ticker)) {
            mBuilder.setTicker(ticker);
        }

        // Notification Priority
        if (pushPayload.getPriority() != 0) {
            // Checks if it's not zero since default doesn't need to be set
            mBuilder.setPriority(pushPayload.getPriority());
        }

        // set Default style for extended content
        mBuilder.setStyle(buildDefaultStyle(pushPayload));

        // Media is present so apply a different template based on type
        PushPayloadMedia media = pushPayload.getMedia();
        if (media != null) {
            if (media.getType() != null) {
                // Notification Style Template
                NotificationCompat.Style mediaStyle = buildNotificationStyle(media.getType(), false, pushPayload);
                if (mediaStyle != null) {
                    mBuilder.setStyle(mediaStyle);

                    // Set media Text since style is set
                    setMediaText(mBuilder, pushPayload);
                    // If the image download failed and the fallback is a video

                    if (usingFallbackDeeplink) {
                        msg.putString(SwrvePushConstants.DEEPLINK_KEY, media.getFallbackSd());
                    }
                }

            }
        }

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Lock Screen Message
            if (SwrveHelper.isNotNullOrEmpty(pushPayload.getLockScreenMsg())) {
                // Use the notification builder to build a copy of the private notification
                // with different lock screen message text
                String contentText = mBuilder.mContentText.toString();

                // Create a public visible notification version
                mBuilder.setTicker(pushPayload.getLockScreenMsg());
                mBuilder.setContentText(pushPayload.getLockScreenMsg());
                Notification lockScreenNotification = mBuilder.build();
                lockScreenNotification.visibility = NotificationCompat.VISIBILITY_PUBLIC;
                mBuilder.setPublicVersion(lockScreenNotification);

                // Reset changed values
                mBuilder.setContentText(contentText);
                if (SwrveHelper.isNotNullOrEmpty(ticker)) {
                    mBuilder.setTicker(ticker);
                } else {
                    mBuilder.setTicker(contentText);
                }
            }
        }

        return mBuilder;
    }

    private void setMediaText(NotificationCompat.Builder mBuilder, PushPayload payload) {

        PushPayloadMedia media = payload.getMedia();
        if(media != null){
            // Media Title
            if (SwrveHelper.isNotNullOrEmpty(media.getTitle())) {
                mBuilder.setContentTitle(media.getTitle());
            }

            // Media Subtitle
            if (SwrveHelper.isNotNullOrEmpty(media.getSubtitle())) {
                mBuilder.setSubText(media.getSubtitle());
            }

            // Media Body
            if (SwrveHelper.isNotNullOrEmpty(media.getBody())) {
                mBuilder.setContentText(media.getBody());

                // If ticker is not set from earlier, set the body to it
                if (SwrveHelper.isNullOrEmpty(payload.getTicker())) {
                    mBuilder.setTicker(media.getBody());
                }
            }
        }
    }

    private NotificationCompat.Style buildNotificationStyle(PushPayloadMedia.MediaType type, Boolean fallback, PushPayload payload) {
        NotificationCompat.Style responseStyle;
        PushPayloadMedia media = payload.getMedia();
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

                        Bitmap bigImage = mediaHelper.downloadBitmapImageFromUrl(media.getUrl());
                        if (bigImage != null) {
                            bigPictureStyle.bigPicture(bigImage);
                        } else {
                            // If m_url failed to download, traverse the same switch with fallback type
                            return buildNotificationStyle(media.getFallbackType(), true, payload);
                        }
                    } else {
                        // Both have failed, return null.
                        return null;
                    }
                } else {
                    Bitmap fallbackImage = mediaHelper.downloadBitmapImageFromUrl(media.getFallbackUrl());
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

                PushPayloadExpanded expanded = payload.getExpanded();
                if (expanded != null) {
                    // Expanded Icon
                    if (SwrveHelper.isNotNullOrEmpty(expanded.getIconUrl())) {
                        bigPictureStyle.bigLargeIcon(mediaHelper.downloadBitmapImageFromUrl(expanded.getIconUrl()));
                    }
                    // Expanded Title
                    if (SwrveHelper.isNotNullOrEmpty(expanded.getTitle())) {
                        bigPictureStyle.setBigContentTitle(expanded.getTitle());
                    }
                    // Expanded Body
                    if (SwrveHelper.isNotNullOrEmpty(expanded.getBody())) {
                        // Summary Text in bigPicture places text in the same place as bigText
                        // so it keeps the format consistent by placing expanded body here.
                        bigPictureStyle.setSummaryText(expanded.getBody());
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

    private NotificationCompat.Style buildDefaultStyle(PushPayload payload) {
        NotificationCompat.Style responseStyle;
        NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
        PushPayloadExpanded exp = payload.getExpanded();
        if (exp != null) {
            // Expanded Title
            if (SwrveHelper.isNotNullOrEmpty(exp.getTitle())) {
                bigTextStyle.setBigContentTitle(exp.getTitle());
            }

            // Expanded Body
            if (SwrveHelper.isNotNullOrEmpty(exp.getBody())) {
                bigTextStyle.bigText(exp.getBody());
            }
        }
        responseStyle = bigTextStyle;
        return responseStyle;
    }

    public List<NotificationCompat.Action> getNotificationActions(Context context, Bundle msg, int notificationId) {
        String swrvePayloadKey = msg.getString(SwrvePushConstants.SWRVE_PAYLOAD_KEY);

        if(SwrveHelper.isNullOrEmpty(swrvePayloadKey)){
            // there's no payload available in the Bundle
            return null;
        }

        PushPayload payload = PushPayload.fromJson(swrvePayloadKey);
        if (payload == null) {
            //payload cannot be parsed
            return null;
        }

        if(payload.getVersion() > SwrvePushConstants.SWRVE_PUSH_VERSION) {
            // push version isn't correct, don't render anything
            return null;
        }

        List<NotificationCompat.Action> actions = new ArrayList<NotificationCompat.Action>();
        List<PushPayloadButton> buttons = payload.getButtons();
        if(buttons != null && !buttons.isEmpty()){
            for(int index = 0; index < buttons.size(); index++){
                PushPayloadButton button = buttons.get(index);
                actions.add(createNotificationAction(context, msg, notificationId, button.getTitle(), SwrvePushConstants.NO_ACTION_ICON, "" + index, button.getActionType(), button.getAction()));
            }
        }
        return actions;
    }

    private NotificationCompat.Action createNotificationAction(Context context, Bundle msg, int notificationId, String buttonText, int icon, String actionKey, PushPayloadButton.ActionType actionType, String actionUrl) {
        Intent intent = createButtonIntent(context, msg, notificationId);
        intent.putExtra(SwrvePushConstants.PUSH_ACTION_KEY, actionKey);
        intent.putExtra(SwrvePushConstants.PUSH_ACTION_TYPE_KEY, actionType);
        intent.putExtra(SwrvePushConstants.PUSH_ACTION_URL_KEY, actionUrl);
        intent.putExtra(SwrvePushConstants.PUSH_ACTION_TEXT, buttonText);
        PendingIntent pIntendButton = PendingIntent.getBroadcast(context, generateTimestampId(), intent, PendingIntent.FLAG_CANCEL_CURRENT);
        return new NotificationCompat.Action.Builder(icon, buttonText, pIntendButton).build();
    }

    public Intent createButtonIntent(Context context, Bundle msg, int notificationId) {
        Intent intent = new Intent(context, SwrvePushEngageReceiver.class);
        intent.putExtra(SwrvePushConstants.PUSH_BUNDLE, msg);
        intent.putExtra(SwrvePushConstants.PUSH_NOTIFICATION_ID, notificationId);
        return intent;
    }

    protected Date getNow() {
        return new Date();
    }

    private int generateTimestampId() {
        return (int) (getNow().getTime() % Integer.MAX_VALUE);
    }
}

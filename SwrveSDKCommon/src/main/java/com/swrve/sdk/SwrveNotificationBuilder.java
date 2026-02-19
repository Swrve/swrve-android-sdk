package com.swrve.sdk;

import static com.swrve.sdk.NotificationMediaManager.EXTRA_GIF_URI;
import static com.swrve.sdk.NotificationMediaManager.NATIVE_GIF_SUPPORT_MIN_API;
import static com.swrve.sdk.SwrveNotificationConstants.SOUND_DEFAULT;

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
import android.graphics.drawable.Icon;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.swrve.sdk.notifications.model.SwrveNotification;
import com.swrve.sdk.notifications.model.SwrveNotificationButton;
import com.swrve.sdk.notifications.model.SwrveNotificationChannel;
import com.swrve.sdk.notifications.model.SwrveNotificationExpanded;
import com.swrve.sdk.notifications.model.SwrveNotificationMedia;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class SwrveNotificationBuilder {

    private final Context context;
    private final int iconMaterialDrawableId;
    private final NotificationChannel defaultNotificationChannel;
    private final int largeIconDrawableId;
    private final String accentColorHex;
    private final SwrveNotificationDetails notificationDetails = new SwrveNotificationDetails();

    private String notificationTitle;
    private boolean usingFallbackDeeplink = false;
    private SwrveNotification swrveNotification;
    private Bundle msg;
    private String msgText;
    protected String campaignType; // accessed in unity subclass
    private Bundle eventPayload;
    private int notificationId;
    private String iamCampaignId; // a campaign to show (eg: IAM) after engaging with push
    protected int requestCode;
    protected NotificationMediaManager mediaManager;

    private static int deviceWidth = 0;
    private static int deviceHeight = 0;

    public SwrveNotificationBuilder(Context context, SwrveNotificationConfig config) {
        this.context = context;
        this.iconMaterialDrawableId = config.getIconMaterialDrawableId();
        this.defaultNotificationChannel = config.getDefaultNotificationChannel();
        this.largeIconDrawableId = config.getLargeIconDrawableId();
        this.accentColorHex = config.getAccentColorHex();
        this.notificationId = Math.abs(new Random().nextInt());
        this.requestCode = Math.abs(new Random().nextInt());
        this.mediaManager = new NotificationMediaManager(context);
    }

    public NotificationCompat.Builder build(String msgText, Bundle msg, String campaignType, Bundle eventPayload) {
        setMessage(msg);
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

        String notificationChannelId = getNotificationChannelId();

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, notificationChannelId)
                .setSmallIcon(iconMaterialDrawableId)
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

        addDeletePendingIntent(mBuilder);

        return mBuilder;
    }

    protected void setMessage(Bundle message) {
        this.msg = message;
        this.swrveNotification = parseBundle(message);
    }

    private SwrveNotification parseBundle(Bundle msg) {
        SwrveNotification swrveNotification = null;
        String swrvePushPayload = msg.getString(SwrveNotificationConstants.SWRVE_PAYLOAD_KEY);
        if (SwrveHelper.isNotNullOrEmpty(swrvePushPayload)) {
            swrveNotification = SwrveNotification.fromJson(swrvePushPayload);
            if (swrveNotification != null && swrveNotification.getNotificationId() > 0) {
                notificationId = swrveNotification.getNotificationId();
            }
            if (swrveNotification != null && swrveNotification.getCampaign() != null) {
                iamCampaignId = swrveNotification.getCampaign().getId();
            }
        }
        return swrveNotification;
    }

    @TargetApi(value = 26)
    protected String getNotificationChannelId() {

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
        if (getSDKVersion() < Build.VERSION_CODES.O && swrveNotification.getPriority() != 0) {
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
            NotificationCompat.Style mediaStyle = buildNotificationStyle(media.getType(), swrveNotification);
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

    private void buildLockScreen(NotificationCompat.Builder builder, SwrveNotification pushPayload) {
        if (SwrveHelper.isNotNullOrEmpty(pushPayload.getLockScreenMsg())) {
            // Use the notification builder to build a copy of the private notification with different lock screen message text

            // Create a public visible notification version
            builder.setTicker(pushPayload.getLockScreenMsg());
            builder.setContentText(pushPayload.getLockScreenMsg());
            Notification lockScreenNotification = builder.build();
            lockScreenNotification.visibility = Notification.VISIBILITY_PUBLIC;
            builder.setPublicVersion(lockScreenNotification);

            // Reset changed values
            builder.setTicker(msgText);
            if (SwrveHelper.isNotNullOrEmpty(pushPayload.getTicker())) {
                builder.setTicker(pushPayload.getTicker());
            }
            setMediaText(builder);
        } else {
            // Ensure a push marked public will show the title,body,etc. on the lock screen,
            // regardless of the user's channel lock screen setting
            // (otherwise it could be treated as sensitive content and not shown)
            if (pushPayload.getVisibility() == SwrveNotification.VisibilityType.PUBLIC) {
                builder.setPublicVersion(builder.build());
            }
        }
    }

    private NotificationCompat.Style buildNotificationStyle(SwrveNotificationMedia.MediaType type, SwrveNotification payload) {
        NotificationCompat.Style responseStyle;
        SwrveNotificationMedia media = payload.getMedia();
        if (type == null) {
            // Enters here in the case fallback is null. if there's no fallback type then media failed
            return null;
        }

        switch (type) {
            case IMAGE:
                NotificationCompat.BigPictureStyle bigPictureStyle = buildBigPictureStyle(media);
                if (bigPictureStyle == null) {
                    return null; // Pictures have failed, return null so default style is used
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

    private NotificationCompat.BigPictureStyle buildBigPictureStyle(SwrveNotificationMedia media) {
        if (!SwrveHelper.isNotNullOrEmpty(media.getUrl())) {
            return null;
        }

        NotificationCompat.BigPictureStyle bigPictureStyle = null;
        NotificationMediaManager.BigPictureFetchResult fetchResult = mediaManager.downloadBigPictureImage(media.getUrl(), notificationId);
        bigPictureStyle = buildBigPictureStyle(fetchResult, media.getUrl());
        if (bigPictureStyle == null) {
            // Last resort - try fallback if available
            fetchResult = mediaManager.downloadBigPictureImage(media.getFallbackUrl(), notificationId);
            bigPictureStyle = buildBigPictureStyle(fetchResult, media.getFallbackUrl());
            if (bigPictureStyle != null) {
                usingFallbackDeeplink = true;
            }
        }
        return bigPictureStyle;
    }

    private NotificationCompat.BigPictureStyle buildBigPictureStyle(NotificationMediaManager.BigPictureFetchResult fetchResult, String mediaUrl) {
        NotificationCompat.BigPictureStyle bigPictureStyle = null;
        if (fetchResult != null && fetchResult.mediaUri != null && Build.VERSION.SDK_INT >= NATIVE_GIF_SUPPORT_MIN_API) {
            bigPictureStyle = buildBigPictureStyleGifIcon(mediaUrl, fetchResult.mediaUri);
        } else if (fetchResult != null && fetchResult.bitmap != null) {
            // Not a GIF, or device doesn't support animated icons, or some other issue -> try bitmap path
            bigPictureStyle = buildBigPictureStyleBitmap(mediaUrl, fetchResult.bitmap);
        }
        return bigPictureStyle;
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    private NotificationCompat.BigPictureStyle buildBigPictureStyleGifIcon(String url, Uri gifUri) {
        if (gifUri == null) {
            return null;
        }
        NotificationCompat.BigPictureStyle bigPictureStyle = null;
        try {
            Icon icon = Icon.createWithContentUri(gifUri.toString());
            // At the time of implementation the app compat library in unity-sdk was version 1.2.0
            // which doesn't support api bigPictureStyle.bigPicture(Icon). As a temp workaround, call
            // the api using reflection. Remove reflection later when the app compat library is updated.
            Method bigPictureMethod = NotificationCompat.BigPictureStyle.class.getMethod("bigPicture", Icon.class);
            bigPictureStyle = new NotificationCompat.BigPictureStyle();
            bigPictureMethod.invoke(bigPictureStyle, icon);
            notificationDetails.setMediaUrl(url);
            notificationDetails.setMediaContentUri(gifUri.toString());
        } catch (Exception e) {
            SwrveLogger.e("Exception creating GIF Icon for: %s", e, url);
            bigPictureStyle = null;
        }
        return bigPictureStyle;
    }

    private NotificationCompat.BigPictureStyle buildBigPictureStyleBitmap(String url, Bitmap bitmap) {
        if (SwrveHelper.isNullOrEmpty(url)) {
            return null;
        }
        NotificationCompat.BigPictureStyle bigPictureStyle = null;
        if (bitmap != null) {
            bigPictureStyle = new NotificationCompat.BigPictureStyle();
            bigPictureStyle.bigPicture(bitmap);
            notificationDetails.setMediaUrl(url);
            notificationDetails.setMediaBitmap(bitmap);
        }
        return bigPictureStyle;
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

    protected NotificationCompat.Action createNotificationAction(String buttonText, int icon, String actionKey, SwrveNotificationButton.ActionType actionType, String actionUrl) {
        boolean isDismissAction = actionType == SwrveNotificationButton.ActionType.DISMISS;
        Intent intent = createButtonIntent(context, msg, actionType, actionUrl, isDismissAction);
        intent.putExtra(SwrveNotificationConstants.PUSH_BUNDLE, msg);
        intent.putExtra(SwrveNotificationConstants.PUSH_NOTIFICATION_ID, notificationId);
        intent.putExtra(SwrveNotificationConstants.CAMPAIGN_TYPE, campaignType);
        intent.putExtra(SwrveNotificationConstants.EVENT_PAYLOAD, eventPayload);
        intent.putExtra(SwrveNotificationConstants.SWRVE_UNIQUE_MESSAGE_ID_KEY, msg.getString(SwrveNotificationConstants.SWRVE_UNIQUE_MESSAGE_ID_KEY));
        intent.putExtra(SwrveNotificationConstants.CONTEXT_ID_KEY, actionKey);
        intent.putExtra(SwrveNotificationConstants.PUSH_ACTION_TYPE_KEY, actionType); // if actionType==OPEN_CAMPAIGN, then actionUrl will be the campaign id. Not actually supported yet.
        intent.putExtra(SwrveNotificationConstants.PUSH_ACTION_URL_KEY, actionUrl);
        intent.putExtra(SwrveNotificationConstants.BUTTON_TEXT_KEY, buttonText);
        if (SwrveHelper.isNotNullOrEmpty(notificationDetails.getMediaContentUri())) { // only add if we have a mediaContentUri to delete
            intent.putExtra(EXTRA_GIF_URI, notificationDetails.getMediaContentUri());
        }

        int flags = PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pendingIntentButton = getPendingIntent(intent, flags, isDismissAction);
        return new NotificationCompat.Action.Builder(icon, buttonText, pendingIntentButton).build();
    }

    // Called by Unity - overridden in Unity version of SwrveNotificationBuilder
    public Intent createButtonIntent(Context context, Bundle msg, SwrveNotificationButton.ActionType actionType, String actionUrl, boolean isDismissAction) {
        Intent intent;
        SwrveNotificationConfig notificationConfig = SwrveCommon.getInstance().getNotificationConfig();
        if (notificationConfig == null || notificationConfig.useEngagementProxy()) {
            intent = getSwrveNotificationEngageIntent(isDismissAction);
        } else if (actionType == SwrveNotificationButton.ActionType.OPEN_URL && SwrveHelper.isNotNullOrEmpty(actionUrl)) {
            intent = SwrveNotificationEngage.getDeeplinkIntent(msg, actionUrl);
        } else if (isDismissAction) {
            Class clazz = SwrveNotificationEngageReceiver.class; // use broadcast receiver for dismiss action because no UI required
            intent = new Intent(context, clazz);
        } else {
            intent = SwrveNotificationEngage.getActivityIntent(context, msg);
        }
        return ensureIntentEngagementCanBeHandled(intent, notificationConfig);
    }

    // Called by Unity
    public PendingIntent createPendingIntent(Bundle msg, String campaignType, Bundle eventPayload) {
        Intent intent = createIntent(msg);
        intent.putExtra(SwrveNotificationConstants.PUSH_BUNDLE, msg);
        intent.putExtra(SwrveNotificationConstants.PUSH_NOTIFICATION_ID, notificationId);
        intent.putExtra(SwrveNotificationConstants.CAMPAIGN_TYPE, campaignType);
        intent.putExtra(SwrveNotificationConstants.EVENT_PAYLOAD, eventPayload);
        intent.putExtra(SwrveNotificationConstants.SWRVE_UNIQUE_MESSAGE_ID_KEY, msg.getString(SwrveNotificationConstants.SWRVE_UNIQUE_MESSAGE_ID_KEY));
        if (SwrveHelper.isNotNullOrEmpty(iamCampaignId)) {
            intent.putExtra(SwrveNotificationConstants.SWRVE_CAMPAIGN_KEY, iamCampaignId);
        }
        if (SwrveHelper.isNotNullOrEmpty(notificationDetails.getMediaContentUri())) { // only add if we have a mediaContentUri to delete
            intent.putExtra(EXTRA_GIF_URI, notificationDetails.getMediaContentUri());
        }

        int flags = PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pendingIntent = getPendingIntent(intent, flags, false);
        return pendingIntent;
    }

    // createIntent is the main intent that will be launched when the notification is engaged with.
    // It is not the intent that will be launched when a button is clicked. It is never a dismiss
    // action hence false is always passed to getSwrveNotificationEngageIntent.
    private Intent createIntent(Bundle msg) {
        Intent intent;
        SwrveNotificationConfig notificationConfig = SwrveCommon.getInstance().getNotificationConfig();
        if (notificationConfig == null || notificationConfig.useEngagementProxy()) {
            intent = getSwrveNotificationEngageIntent(false);
        } else if (msg.containsKey(SwrveNotificationConstants.DEEPLINK_KEY)) {
            String deeplink = msg.getString(SwrveNotificationConstants.DEEPLINK_KEY);
            intent = SwrveNotificationEngage.getDeeplinkIntent(msg, deeplink);
        } else {
            intent = SwrveNotificationEngage.getActivityIntent(context, msg);
        }

        return ensureIntentEngagementCanBeHandled(intent, notificationConfig);
    }

    private Intent getSwrveNotificationEngageIntent(boolean isDismissAction) {
        Class clazz = getIntentClass(isDismissAction);
        Intent intent = new Intent(context, clazz);
        intent.addFlags(SwrveIntentHelper.getDefaultIntentFlags());
        return intent;
    }

    // Called by Unity - overridden in Unity version of SwrveNotificationBuilder
    public Class getIntentClass(boolean isDismissAction) {
        Class clazz;
        // A dismiss action should dismiss the notification without opening the app
        if (isDismissAction) {
            clazz = SwrveNotificationEngageReceiver.class; // use broadcast receiver for dismiss action because no UI required
        } else {
            clazz = SwrveNotificationEngageActivity.class; // everything else should use proxy activity
        }
        return clazz;
    }

    // After resolving the intent to show, verify it can be opened internally by the app when useEngagementProxy is false. If it can't be opened,
    // then override the useEngagementProxy and fallback to opening via the Engagement Proxy. Otherwise, the engagement event may be lost.
    private Intent ensureIntentEngagementCanBeHandled(Intent intent, SwrveNotificationConfig notificationConfig) {
        if (notificationConfig != null && !notificationConfig.useEngagementProxy()) {
            if (!SwrveIntentHelper.canOpenIntentInternally(context, intent)) {
                SwrveLogger.w("Swrve: useEngagementProxy is false, but the intent cannot be handled by app. Falling back to using engagement proxy.");
                intent = getSwrveNotificationEngageIntent(false);
            } else {
                // The intent can be opened internally by the app
                intent.putExtra(SwrveNotificationEngage.DO_NOT_OPEN_INTENT, true); // flag to prevent opening the intent
            }
        }
        return intent;
    }

    protected PendingIntent getPendingIntent(Intent intent, int flags, boolean isDismissAction) {
        PendingIntent pendingIntent;
        if (isDismissAction) {
            pendingIntent = PendingIntent.getBroadcast(context, requestCode++, intent, flags); // use broadcast PI for dismiss action to avoid UI
        } else {
            pendingIntent = PendingIntent.getActivity(context, requestCode++, intent, flags); // everything else should use activity PI
        }
        return pendingIntent;
    }

    private void addDeletePendingIntent(NotificationCompat.Builder mBuilder) {
        if (SwrveHelper.isNotNullOrEmpty(notificationDetails.getMediaContentUri())) { // only add if we have a mediaContentUri to delete
            Intent deleteIntent = new Intent(context, SwrveNotificationDeleteReceiver.class);
            deleteIntent.putExtra(EXTRA_GIF_URI, notificationDetails.getMediaContentUri());
            int flags = PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE;
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, notificationId, deleteIntent, flags);
            mBuilder.setDeleteIntent(pendingIntent);
        }
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
        return mediaManager.downloadBitmap(url);
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

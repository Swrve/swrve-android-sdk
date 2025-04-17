package com.swrve.sdk;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.robolectric.Robolectric;

import java.util.List;

public class SwrveNotificationEngageActivityTest extends SwrveBaseTest {

    private Context context = ApplicationProvider.getApplicationContext();

    @Test
    public void testReceiverInManifest() {
        Intent intent = new Intent(context, SwrveNotificationEngageActivity.class);
        List<ResolveInfo> receiverDataList = context.getPackageManager().queryIntentActivities(intent, 0);
        boolean inManifest = false;
        for (ResolveInfo receiverData : receiverDataList) {
            if (receiverData.activityInfo.name.equals("com.swrve.sdk.SwrveNotificationEngageActivity")) {
                inManifest = true;
                break;
            }
        }
        assertTrue(inManifest);
    }

    @Test
    public void testEngage() {
        ISwrveCommon swrveCommon = mock(ISwrveCommon.class);
        SwrveCommon.setSwrveCommon(swrveCommon);
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), SwrveNotificationEngageActivity.class);
        Robolectric.buildActivity(SwrveNotificationEngageActivity.class, intent).create().visible().get();
        verify(swrveCommon, times(1)).handlePushEngagement(intent);
    }
}

package com.swrve.sdk;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.util.List;

public class SwrveNotificationEngageReceiverTest extends SwrveBaseTest {

    private Context context = ApplicationProvider.getApplicationContext();

    @Test
    public void testReceiverInManifest() {
        Intent intent = new Intent(context, SwrveNotificationEngageReceiver.class);
        List<ResolveInfo> receiverDataList = context.getPackageManager().queryBroadcastReceivers(intent, 0);
        boolean inManifest = false;
        for (ResolveInfo receiverData : receiverDataList) {
            if (receiverData.activityInfo.name.equals("com.swrve.sdk.SwrveNotificationEngageReceiver")) {
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
        Intent intent = new Intent();
        SwrveNotificationEngageReceiver receiver = new SwrveNotificationEngageReceiver();
        receiver.onReceive(context, intent);
        verify(swrveCommon, times(1)).handlePushEngagement(intent);
    }
}

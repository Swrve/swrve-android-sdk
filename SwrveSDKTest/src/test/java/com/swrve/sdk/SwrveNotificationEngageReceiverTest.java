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
import org.robolectric.RobolectricTestRunner;

import java.util.List;

public class SwrveNotificationEngageReceiverTest extends SwrveBaseTest {

    Context context = ApplicationProvider.getApplicationContext();

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
        SwrveNotificationEngageReceiver receiverSpy = spy(new SwrveNotificationEngageReceiver());
        SwrveNotificationEngage notificationEngageMock = mock(SwrveNotificationEngage.class);
        doReturn(notificationEngageMock).when(receiverSpy).getSwrveNotificationEngage(context);
        Intent intent = new Intent();
        receiverSpy.onReceive(context, intent);
        verify(notificationEngageMock, times(1)).processIntent(intent);
    }
}

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

import java.util.List;

public class SwrveNotificationDeleteReceiverTest extends SwrveBaseTest {

    private Context context = ApplicationProvider.getApplicationContext();

    @Test
    public void testReceiverInManifest() {
        Intent intent = new Intent(context, SwrveNotificationDeleteReceiver.class);
        List<ResolveInfo> receiverDataList = context.getPackageManager().queryBroadcastReceivers(intent, 0);
        boolean inManifest = false;
        for (ResolveInfo receiverData : receiverDataList) {
            if (receiverData.activityInfo.name.equals("com.swrve.sdk.SwrveNotificationDeleteReceiver")) {
                inManifest = true;
                break;
            }
        }
        assertTrue(inManifest);
    }

    @Test
    public void testEngage() {
        SwrveNotificationDeleteReceiver receiverSpy = spy(new SwrveNotificationDeleteReceiver());
        NotificationMediaManager notificationMediaManagerMock = mock(NotificationMediaManager.class);
        doReturn(notificationMediaManagerMock).when(receiverSpy).getNotificationMediaManager(context);

        Intent intent = new Intent();
        receiverSpy.onReceive(context, intent);

        verify(notificationMediaManagerMock, times(1)).deleteGifUri(intent);
    }
}

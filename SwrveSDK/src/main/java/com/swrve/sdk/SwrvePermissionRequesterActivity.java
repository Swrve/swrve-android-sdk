package com.swrve.sdk;

import static android.Manifest.permission.POST_NOTIFICATIONS;
import static android.content.pm.PackageManager.PERMISSION_DENIED;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

public class SwrvePermissionRequesterActivity extends Activity {

    private static final int PERMISSION_REQUEST_CODE = 101;
    private static final int SETTINGS_REQUEST_CODE = 102;
    private static final String EXTRAS_KEY_PERMISSION = "PERMISSION";

    private boolean shouldShowRequestPermissionRationaleStart = false;

    public static void requestPermission(Context context, String permission) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            SwrveLogger.v("SwrveSDK: Permissions requests are not required below API level 23.");
            return;
        }
        Intent intent = new Intent(context, SwrvePermissionRequesterActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(EXTRAS_KEY_PERMISSION, permission);
        context.startActivity(intent);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setFinishOnTouchOutside(false); // do not finish when touched outside its window's bounds

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(0);
        }

        Bundle extras = getIntent().getExtras();
        String permission = null;
        if (extras != null && extras.containsKey(EXTRAS_KEY_PERMISSION)) {
            permission = extras.getString(EXTRAS_KEY_PERMISSION);
        }

        shouldShowRequestPermissionRationaleStart = shouldShowRequestPermissionRationale(permission);

        if (SwrveHelper.isNotNullOrEmpty(permission) && ContextCompat.checkSelfPermission(this, permission) == PERMISSION_DENIED) {
            final Swrve swrve = (Swrve) SwrveSDK.getInstance();
            int permissionAnsweredTime = swrve.getPermissionAnsweredTime(permission);
            if (permissionAnsweredTime >= 2) {
                showSettings(permission); // infer that we cannot request the permission directly so show settings instead
            } else {
                String[] permissions = new String[]{permission};
                requestPermissions(permissions, PERMISSION_REQUEST_CODE);
            }
        } else {
            SwrveLogger.v("SwrveSDK: %s permission is already GRANTED", permission);
            finish();
        }
    }

    private void showSettings(String permission) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && POST_NOTIFICATIONS.equalsIgnoreCase(permission)) {
            Intent intent = SwrveHelper.getNotificationPermissionSettingsIntent(this);
            startActivityForResult(intent, SETTINGS_REQUEST_CODE);
        } else {
            Intent intent = SwrveHelper.getAppSettingsIntent(this);
            startActivityForResult(intent, SETTINGS_REQUEST_CODE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SETTINGS_REQUEST_CODE) {
            queueDeviceUpdate(); // It could be any permission (not one tracked by swrve) and it might not have changed, but queue device update just in case.
        }
        finish();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        try {
            if (requestCode != PERMISSION_REQUEST_CODE && permissions.length != 1 || grantResults.length != 1) {
                return; // this shouldn't happen because this activity currently only requests one permission at a time
            }

            String permission = permissions[0];
            int permissionResult = grantResults[0];
            if (permissionResult == PERMISSION_GRANTED) {
                SwrveLogger.v("SwrveSDK: %s permission is GRANTED", permission);
            } else if (permissionResult == PERMISSION_DENIED) {
                SwrveLogger.v("SwrveSDK: %s permission is DENIED", permission);
            }

            // check if shouldShowRequestPermissionRationale value changes
            boolean shouldShowRequestPermissionRationaleNow = shouldShowRequestPermissionRationale(permission);
            if (shouldShowRequestPermissionRationaleStart != shouldShowRequestPermissionRationaleNow) {
                // if changed then the prompt was answered (back button or outside prompt wasn't pressed)
                incrementNotificationPermissionAnsweredTime(permission);
            }
            queueDeviceUpdate();
        } finally {
            finish(); // always call finish because this activity is a blank UI
        }
    }

    private void incrementNotificationPermissionAnsweredTime(String permission) {
        final Swrve swrve = (Swrve) SwrveSDK.getInstance();
        int permissionAnsweredTimes = swrve.getPermissionAnsweredTime(permission);
        permissionAnsweredTimes++;
        String cacheKey = SwrveHelper.getPermissionAnsweredCacheKey(permission);
        swrve.multiLayerLocalStorage.setCacheEntry("", cacheKey, String.valueOf(permissionAnsweredTimes)); // note this without userId
    }

    private void queueDeviceUpdate() {
        final Swrve swrve = (Swrve) SwrveSDK.getInstance();
        swrve.deviceUpdate(SwrveSDK.getUserId(), swrve.getDeviceInfo());
    }
}

package com.swrve.sdk.sample.multiplepushproviders

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.swrve.sdk.SwrveSDK

class MainActivity : ComponentActivity() {

    private val permissionGranted = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MultiplePushProvidersSampleTheme {
                Scaffold { innerPadding ->
                    MultiplePushProvidersScreen(
                        permissionGranted = permissionGranted.value,
                        onRequestPermission = {
                            // Swrve triggers the OS permission prompt when it sees an event listed in pushNotificationPermissionEvents. See SampleApplication.
                            SwrveSDK.event(SampleApplication.EVENT_NOTIFICATION_PERMISSION_REQUEST)
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        permissionGranted.value = hasNotificationPermission()
    }

    // Android 13 (TIRAMISU) introduced the runtime notification permission. Below that there is nothing to request.
    private fun hasNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
}

@Composable
fun MultiplePushProvidersScreen(
    permissionGranted: Boolean,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
    sdkVersion: String = SwrveSDK.getSdkVersion(),
    userId: String = SwrveSDK.getUserId()
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Multiple Push Providers Sample",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Image(
            // A drawable-night variant is supplied, so this follows the system theme.
            painter = painterResource(id = R.drawable.app_logo),
            contentDescription = "MessageGears logo",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .padding(top = 8.dp)
                .fillMaxWidth(0.6f)
        )

        Text(
            text = "Swrve SDK $sdkVersion",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Text(
            text = "User ID: $userId",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "This app owns the FirebaseMessagingService, so every push — " +
                "Swrve's included — arrives in MyFirebaseMessagingService and is routed there.\n\n" +
                "Have a look at the code!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        if (permissionGranted) {
            Text(
                text = "✓ Notification permission granted",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        } else {
            Button(
                onClick = onRequestPermission,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Request notification permission")
            }

            // The OS stops showing the prompt after two declines. After that the button does
            // nothing, so say so rather than leaving it looking broken.
            Text(
                text = "If nothing happens, notifications were declined previously — enable them "
                    + "in Settings.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true, name = "Light — permission not yet granted")
@Composable
private fun MultiplePushProvidersScreenPreviewLight() {
    MultiplePushProvidersSampleTheme(useDarkTheme = false) {
        MultiplePushProvidersScreen(
            permissionGranted = false,
            onRequestPermission = {},
            sdkVersion = "12.2.1",
            userId = "a1b2c3d4-preview"
        )
    }
}

@Preview(showBackground = true, name = "Dark — permission granted")
@Composable
private fun MultiplePushProvidersScreenPreviewDark() {
    MultiplePushProvidersSampleTheme(useDarkTheme = true) {
        MultiplePushProvidersScreen(
            permissionGranted = true,
            onRequestPermission = {},
            sdkVersion = "12.2.1",
            userId = "a1b2c3d4-preview"
        )
    }
}

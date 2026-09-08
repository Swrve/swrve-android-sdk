package com.swrve.sdk.sample.identity

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swrve.sdk.SwrveIdentityResponse
import com.swrve.sdk.SwrveSDK

/** What the screen shows: either the login form, or the signed-in view. */
data class IdentityUiState(
    val sdkTracking: Boolean,
    val identified: Boolean, // This app's own flag — did an identify() actually succeed. Not the same question.
    val externalUserId: String, // The ID you passed to identify(), for contrast with the Swrve one.
    val swrveUserId: String,
    val status: String
)

class MainActivity : ComponentActivity() {

    /**
     * Sign-in is the app's own state, not something to infer from the SDK after the fact — see [readState]. `isStarted()` is only used for the initial value, 
     * to skip the login screen for a user who identified on a previous launch.
     */
    private val uiState = mutableStateOf(
        IdentityUiState(
            sdkTracking = false,
            identified = false,
            externalUserId = "",
            swrveUserId = "",
            status = ""
        )
    )

    /** Stands in for your app's own session store — see [initialState]. */
    private val prefs by lazy { getSharedPreferences("identity_sample", MODE_PRIVATE) }

    private val busy = mutableStateOf(false)

    /** The ID comes from this app's own storage — your app knows who is signed in, Swrve does not. */
    private fun initialState(): IdentityUiState {
        val previous = prefs.getString(KEY_EXTERNAL_USER_ID, "").orEmpty()
        // Started is not the same as identified: with autoStartLastUser = true the SDK is started
        // from launch even before anyone has identified.
        val identified = previous.isNotEmpty() && SwrveSDK.isStarted()
        return IdentityUiState(
            sdkTracking = SwrveSDK.isStarted(),
            identified = identified,
            externalUserId = previous,
            swrveUserId = SwrveSDK.getUserId(),
            status = when {
                identified -> "Identified on a previous launch"
                previous.isNotEmpty() -> "Identified previously — identify again to resume tracking"
                else -> "Not identified yet"
            }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        uiState.value = initialState() // needs a context, so not a field initialiser
        setContent {
            IdentitySampleTheme {
                Scaffold { innerPadding ->
                    IdentityScreen(
                        state = uiState.value,
                        busy = busy.value,
                        onIdentify = ::identify,
                        onStopTracking = ::stopTracking,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    /**
     * Links this device to your own ID for the user. Called from a button rather than at startup, which is the point — call it when you actually learn who the user is.
     */
    private fun identify(externalUserId: String) {
        busy.value = true

        // Fired before identify so you can watch where it lands in Swrve. With autoStartLastUser = false the SDK is not tracking yet, so this one is dropped. With it
        // set to true the event lands on the anonymous user, and whether that user survives identify() depends on whether Swrve already knows the ID — see SampleApplication.
        SwrveSDK.event(SampleApplication.EVENT_BEFORE_IDENTIFY)

        SwrveSDK.identify(externalUserId, object : SwrveIdentityResponse {
            override fun onSuccess(status: String, swrveId: String) {
                Log.i(LOG_TAG, "identify success: status=$status swrveId=$swrveId")

                prefs.edit().putString(KEY_EXTERNAL_USER_ID, externalUserId).apply() // Remembered by the app, so the next launch can show it and re-identify.

                // The SDK is now tracking, so this event is attributed to the identified user.
                SwrveSDK.event(SampleApplication.EVENT_AFTER_IDENTIFY)

                // Callbacks arrive off the main thread, so the UI update needs to be posted to it.
                runOnUiThread {
                    busy.value = false
                    uiState.value = readState(
                        identified = true,
                        externalUserId = externalUserId,
                        status = status
                    )
                }
            }

            override fun onError(responseCode: Int, errorMessage: String) {
                Log.e(LOG_TAG, "identify failed: $responseCode $errorMessage")

                runOnUiThread {
                    busy.value = false
                    // Not signed in — but note the SDK is now tracking the unidentified user, so isStarted() would say true. Sign-in state has to be the app's own.
                    uiState.value = readState(
                        identified = false,
                        externalUserId = externalUserId,
                        status = "Error $responseCode: $errorMessage"
                    )
                }
            }
        })
    }

    /** The SDK keeps the current user but sends nothing until start() or identify() is called. */
    private fun stopTracking() {
        SwrveSDK.stopTracking()
        uiState.value = readState(
            identified = false,
            externalUserId = uiState.value.externalUserId,
            status = "Stopped tracking"
        )
    }

    private fun readState(identified: Boolean, externalUserId: String, status: String) =
        IdentityUiState(
            sdkTracking = SwrveSDK.isStarted(),
            identified = identified,
            externalUserId = externalUserId,
            swrveUserId = SwrveSDK.getUserId(), // UserId is always present, and kept after stopTracking() — the SDK retains the last user.
            status = status
        )

    private companion object {
        const val LOG_TAG = "IdentitySample"
        const val KEY_EXTERNAL_USER_ID = "external_user_id"
    }
}

@Composable
fun IdentityScreen(
    state: IdentityUiState,
    busy: Boolean,
    onIdentify: (String) -> Unit,
    onStopTracking: () -> Unit,
    modifier: Modifier = Modifier,
    autoStartLastUser: Boolean = SwrveSDK.getConfig().isAutoStartLastUser,
    sdkVersion: String = SwrveSDK.getSdkVersion()
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Identity Sample",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Image(
            painter = painterResource(id = R.drawable.app_logo),
            contentDescription = "MessageGears logo",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth(0.4f)
        )

        Text(
            text = "Swrve SDK $sdkVersion",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        val intro = when {
            !autoStartLastUser ->
                "autoStartLastUser is false, so nothing is tracked until you identify."

            !state.sdkTracking ->
                "autoStartLastUser is true, but tracking is stopped until you identify or start again."

            state.identified ->
                "autoStartLastUser is true, so the SDK resumed tracking as the user identified " +
                    "previously — not anonymously."

            else ->
                "autoStartLastUser is true, so the SDK is tracking anonymously until you identify."
        }

        Text(
            text = "$intro Identify again with a different ID to switch user — no need to stop "
                + "tracking first.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        var userId by remember { mutableStateOf("") } // Stays available after identifying, so you can identify again as someone else.

        OutlinedTextField(
            value = userId,
            onValueChange = { userId = it },
            label = { Text("External user ID") },
            supportingText = { Text("Your own ID for this user — what you pass to identify()") },
            singleLine = true,
            enabled = !busy,
            modifier = Modifier.fillMaxWidth()
        )

        // Paired: the two things you can do to the current identity.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { onIdentify(userId.trim()) },
                enabled = !busy && userId.isNotBlank(),
                modifier = Modifier.weight(1f)
            ) {
                Text(if (busy) "Identifying…" else "Identify")
            }

            OutlinedButton(
                onClick = onStopTracking,
                enabled = state.sdkTracking,
                modifier = Modifier.weight(1f)
            ) {
                Text("Stop tracking")
            }
        }

        StatusPanel(state)
    }
}

/** `sdkTracking` and `identified` are shown together because they can legitimately disagree. */
@Composable
private fun StatusPanel(state: IdentityUiState) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LabelledRow("SDK tracking", if (state.sdkTracking) "yes" else "no")
            LabelledRow("Identified", if (state.identified) "yes" else "no")
            LabelledRow("External user ID", state.externalUserId.ifEmpty { "—" })
            // Swrve's own ID for the user it resolved to — not the ID you passed in.
            LabelledRow("Swrve user ID", state.swrveUserId, valueSize = 11.sp)

            HorizontalDivider()

            // Wraps rather than truncating: some status values are full sentences.
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Identify status",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = state.status,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun LabelledRow(label: String, value: String, valueSize: TextUnit = TextUnit.Unspecified) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = valueSize
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Preview(showBackground = true, name = "Light — not identified")
@Composable
private fun IdentityScreenPreviewNotIdentified() {
    IdentitySampleTheme(useDarkTheme = false) {
        IdentityScreen(
            state = IdentityUiState(
                sdkTracking = false,
                identified = false,
                externalUserId = "",
                swrveUserId = "a1b2c3d4-anon",
                status = "Not identified yet"
            ),
            busy = false,
            onIdentify = {},
            onStopTracking = {},
            autoStartLastUser = false,
            sdkVersion = "1.2.3"
        )
    }
}

@Preview(showBackground = true, name = "Dark — identified")
@Composable
private fun IdentityScreenPreviewIdentified() {
    IdentitySampleTheme(useDarkTheme = true) {
        IdentityScreen(
            state = IdentityUiState(
                sdkTracking = true,
                identified = true,
                externalUserId = "dom1",
                swrveUserId = "a1b2c3d4-preview",
                status = "Loaded from cache"
            ),
            busy = false,
            onIdentify = {},
            onStopTracking = {},
            autoStartLastUser = false,
            sdkVersion = "1.2.3"
        )
    }
}

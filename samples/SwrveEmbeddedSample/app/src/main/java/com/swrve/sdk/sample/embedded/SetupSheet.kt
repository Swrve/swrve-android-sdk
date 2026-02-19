package com.swrve.sdk.sample.embedded

import android.content.ClipData
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.swrve.sdk.SwrveIdentityResponse
import com.swrve.sdk.SwrveRefreshContentListenerResult
import com.swrve.sdk.SwrveSDK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SetupSheetContent() {
    var isRefreshing by remember { mutableStateOf(false) }
    var embeddedCampaignCount by remember { mutableIntStateOf(0) }
    var userPropKey by remember { mutableStateOf(TextFieldValue("")) }
    var userPropValue by remember { mutableStateOf(TextFieldValue("")) }
    var uiFeedback by remember { mutableStateOf<String?>(null) }
    var userId by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboard.current
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    val prefs = remember(context) { SwrvePrefs.prefs(context) }
    var offlineMode by remember { mutableStateOf(SwrvePrefs.isOffline(prefs)) }
    var appIdText by remember { mutableStateOf(TextFieldValue("")) }
    var apiKeyText by remember { mutableStateOf(TextFieldValue("")) }
    var stack by remember { mutableStateOf("us") }

    LaunchedEffect(Unit) {
        embeddedCampaignCount = withContext(Dispatchers.IO) { SwrveSDK.getEmbeddedMessageCenterCampaigns().size }
        userId = SwrveSDK.getUserId().orEmpty()
        val savedAppId = prefs.getString(SwrvePrefs.KEY_APP_ID, "") ?: ""
        val savedApiKey = prefs.getString(SwrvePrefs.KEY_API_KEY, "") ?: ""
        val savedStack = SwrvePrefs.loadStack(prefs)
        appIdText = TextFieldValue(savedAppId)
        apiKeyText = TextFieldValue(savedApiKey)
        stack = savedStack
    }

    // Helper to refresh content and provide common UI feedback handling.
    val refreshWithUi: (String?, ((SwrveRefreshContentListenerResult) -> Unit)?) -> Unit = { prefix, resultHandler ->
        scope.launch {
            val pre = prefix?.let { "$it\n" } ?: ""
            uiFeedback = pre + "Refreshing content..."
            isRefreshing = true
            SwrveSDK.refreshContent(object : com.swrve.sdk.SwrveRefreshContentListener {
                override fun onComplete(result: SwrveRefreshContentListenerResult) {
                    scope.launch {
                        isRefreshing = false
                        if (resultHandler != null) {
                            try {
                                resultHandler(result)
                            } catch (_: Throwable) {
                            }
                        } else {
                            when (result.resultCode) {
                                SwrveRefreshContentListenerResult.ResultCode.SUCCESS -> {
                                    uiFeedback = (uiFeedback ?: "") + "\nRefresh succeeded (HTTP ${result.httpResponseCode})"
                                }

                                SwrveRefreshContentListenerResult.ResultCode.ERROR,
                                SwrveRefreshContentListenerResult.ResultCode.ERROR_UNKNOWN -> {
                                    val codeDesc = result.resultCode.toString()
                                    val parts = mutableListOf<String>("Refresh failed (")
                                    parts.add(codeDesc)
                                    if (!result.errorMessage.isNullOrEmpty()) parts.add(": ${result.errorMessage}")
                                    parts.add(") HTTP ${result.httpResponseCode}")
                                    uiFeedback = (uiFeedback ?: "") + parts.joinToString("")
                                }
                            }
                        }

                        embeddedCampaignCount = withContext(Dispatchers.IO) { SwrveSDK.getEmbeddedMessageCenterCampaigns().size }
                        delay(4000)
                        if (!isRefreshing) uiFeedback = null
                    }
                }
            })
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(8.dp)
            .pointerInput(Unit) {
                // Dismiss keyboard when tapping anywhere in the sheet outside text fields
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                })
            },
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Setup",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "User Id: $userId",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clickable {
                            scope.launch {
                                val clipEntry = ClipEntry(
                                    ClipData("User Id", arrayOf("text/plain"), ClipData.Item(userId))
                                )
                                clipboardManager.setClipEntry(clipEntry)
                                Toast.makeText(context, "Copied User Id", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = "Copy User Id",
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Copy",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Content Mode (Live / Offline)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 2.dp
            ) {
                Column(modifier = Modifier
                    .padding(12.dp)
                    .animateContentSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Content Mode",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        SingleChoiceSegmentedButtonRow {
                            SegmentedButton(
                                selected = !offlineMode,
                                onClick = {
                                    offlineMode = false
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                colors = SegmentedButtonDefaults.colors(
                                    activeContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    activeContentColor = MaterialTheme.colorScheme.onSurface,
                                    inactiveContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    activeBorderColor = MaterialTheme.colorScheme.outline,
                                    inactiveBorderColor = MaterialTheme.colorScheme.outline
                                )
                            ) {
                                Text("Live", style = MaterialTheme.typography.labelSmall)
                            }
                            SegmentedButton(
                                selected = offlineMode,
                                onClick = {
                                    offlineMode = true
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                colors = SegmentedButtonDefaults.colors(
                                    activeContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    activeContentColor = MaterialTheme.colorScheme.onSurface,
                                    inactiveContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    activeBorderColor = MaterialTheme.colorScheme.outline,
                                    inactiveBorderColor = MaterialTheme.colorScheme.outline
                                )
                            ) {
                                Text("Offline", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    // Credentials row (App ID, API Key, Stack) — collapse and animate when hidden
                    if (!offlineMode) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // App ID (numeric)
                            OutlinedTextField(
                                value = appIdText,
                                onValueChange = { appIdText = it },
                                label = { Text("App ID", style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.width(88.dp),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )

                            // API Key (text)
                            OutlinedTextField(
                                value = apiKeyText,
                                onValueChange = { apiKeyText = it },
                                label = { Text("API Key", style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface)
                            )

                            // Stack (US / EU / FS) as compact exposed dropdown
                            val stacks = listOf("us", "eu", "fs")
                            var stackExpanded by remember { mutableStateOf(false) }
                            androidx.compose.foundation.layout.Box {
                                OutlinedTextField(
                                    value = stack.uppercase(),
                                    onValueChange = {},
                                    label = { Text("Stack", style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.width(104.dp),
                                    singleLine = true,
                                    readOnly = true,
                                    textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
                                    trailingIcon = {
                                        Icon(
                                            imageVector = Icons.Filled.ArrowDropDown,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.clickable { stackExpanded = !stackExpanded }
                                        )
                                    }
                                )
                                DropdownMenu(
                                    expanded = stackExpanded,
                                    onDismissRequest = { stackExpanded = false }
                                ) {
                                    stacks.forEach { code ->
                                        DropdownMenuItem(
                                            text = { Text(code.uppercase()) },
                                            onClick = {
                                                stack = code
                                                stackExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            SwrvePrefs.saveAll(prefs, offlineMode, appIdText.text, apiKeyText.text, stack)
                            uiFeedback = "Saved. Restart app to fully apply."
                            scope.launch {
                                delay(3000)
                                if (uiFeedback?.startsWith("Saved.") == true) uiFeedback = null
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Save - requires restart",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }
        }

        // Identify Section (text input + identify button)
        var identifyInput by remember { mutableStateOf(TextFieldValue("")) }
        var isIdentifying by remember { mutableStateOf(false) }
        // Identify Section — reserve constant space
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (!offlineMode) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = identifyInput,
                        onValueChange = { identifyInput = it },
                        label = { Text("External User ID", style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface)
                    )

                    OutlinedButton(
                        onClick = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            val text = identifyInput.text.trim()
                            if (text.isEmpty()) {
                                uiFeedback = "External user id required"
                                scope.launch {
                                    delay(2000)
                                    uiFeedback = null
                                }
                                return@OutlinedButton
                            }
                            isIdentifying = true
                            scope.launch {
                                SwrveSDK.identify(text, object : SwrveIdentityResponse {
                                    override fun onSuccess(status: String, swrveId: String) {
                                        scope.launch {
                                            uiFeedback = "Identify succeeded: $status (id=$swrveId)"
                                            // update visible user id immediately
                                            userId = SwrveSDK.getUserId().orEmpty()

                                            refreshWithUi("Identify result") { result ->
                                                isIdentifying = false
                                            }
                                        }
                                    }

                                    override fun onError(responseCode: Int, errorMessage: String) {
                                        scope.launch {
                                            isIdentifying = false
                                            uiFeedback = "Identify failed: $responseCode - ${errorMessage ?: ""}"
                                            delay(3000)
                                            uiFeedback = null
                                        }
                                    }
                                })
                            }
                        },
                        enabled = !isIdentifying,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            if (isIdentifying) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onSurface)
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(text = "Identify", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                        }
                    }
                }
            }
        }

        // Refresh Content Section
        // Refresh Section — reserve constant space and hide when Offline
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (!offlineMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            val inUseAppId = prefs.getString(SwrvePrefs.KEY_APP_ID, "TODO") ?: "TODO"
                            val inUseApiKey = prefs.getString(SwrvePrefs.KEY_API_KEY, "TODO") ?: "TODO"
                            refreshWithUi("Refreshing content using creds $inUseAppId/$inUseApiKey", null)
                        },
                        enabled = !isRefreshing,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            if (isRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = "Refresh Content",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Embedded Campaigns: $embeddedCampaignCount",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // User Property Section
        // User Property Section — reserve constant space and hide when Offline
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(212.dp),
            contentAlignment = Alignment.TopStart
        ) {
            if (!offlineMode) {
                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant, tonalElevation = 2.dp) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "User Property",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedButton(
                                onClick = {
                                    userPropKey = TextFieldValue("carousel_layout")
                                    userPropValue = TextFieldValue("tall")
                                },
                                modifier = Modifier.height(28.dp),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "carousel_layout=tall",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    userPropKey = TextFieldValue("carousel_layout")
                                    userPropValue = TextFieldValue("image")
                                },
                                modifier = Modifier.height(28.dp),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "carousel_layout=image",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedButton(
                                onClick = {
                                    userPropKey = TextFieldValue("banner_location")
                                    userPropValue = TextFieldValue("top")
                                },
                                modifier = Modifier.height(28.dp),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "banner_location=top",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    userPropKey = TextFieldValue("banner_location")
                                    userPropValue = TextFieldValue("bottom")
                                },
                                modifier = Modifier.height(28.dp),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "banner_location=bottom",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = userPropKey,
                                onValueChange = { userPropKey = it },
                                label = { Text("Key", style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface)
                            )

                            OutlinedTextField(
                                value = userPropValue,
                                onValueChange = { userPropValue = it },
                                label = { Text("Value", style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface)
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                val key = userPropKey.text.trim()
                                val value = userPropValue.text.trim()

                                if (key.isNotEmpty() && value.isNotEmpty()) {
                                    isRefreshing = true
                                    val attributes = mapOf(key to value)
                                    SwrveSDK.userUpdate(attributes)
                                    SwrveSDK.sendQueuedEvents()

                                    uiFeedback = "Sent user property $key=$value to Swrve\n"

                                    scope.launch {
                                        delay(2000) // allow time for the event to be processed
                                        refreshWithUi("Post-userUpdate refresh", null)
                                    }
                                } else {
                                    uiFeedback = "Key and value required"
                                    scope.launch {
                                        delay(2000)
                                        uiFeedback = null
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isRefreshing,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            if (isRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = "Send User Property and Refresh",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                }
            }
        }

        // Transient UI feedback area (fixed height to avoid bottom sheet resize)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = uiFeedback ?: "",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun SetupSheetPreview() {
    SwrveSampleTheme {
        SetupSheetContent()
    }
}

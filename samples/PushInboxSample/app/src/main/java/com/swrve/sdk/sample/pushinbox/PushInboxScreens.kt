package com.swrve.sdk.sample.pushinbox

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import com.swrve.sdk.SwrveSDK
import kotlinx.coroutines.delay
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class InboxTab(val label: String) { Home("Home"), Inbox("Inbox") }

@Composable
fun PushInboxApp(
    messages: List<InboxMessage>,
    externalUserId: String,
    swrveUserId: String,
    busy: Boolean,
    onIdentify: (String) -> Unit,
    onOpenMessage: (InboxMessage) -> Unit,
    onDelete: (InboxMessage) -> Unit,
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    modifier: Modifier = Modifier,
    sdkVersion: String = SwrveSDK.getSdkVersion()
) {
    val unreadCount = messages.count { it.unread }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar {
                InboxTab.entries.forEachIndexed { index, entry ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { onSelectTab(index) },
                        label = { Text(entry.label) },
                        icon = {
                            val icon = when (entry) {
                                InboxTab.Home -> Icons.Default.Home
                                InboxTab.Inbox -> Icons.Default.Email
                            }
                            // The badge is the only visible sign that the update listener fired, and
                            // that marking a message read actually took effect.
                            if (entry == InboxTab.Inbox && unreadCount > 0) {
                                BadgedBox(badge = { Badge { Text("$unreadCount") } }) {
                                    Icon(icon, contentDescription = null)
                                }
                            } else {
                                Icon(icon, contentDescription = null)
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)) {
            when (InboxTab.entries[selectedTab]) {
                InboxTab.Home -> HomeScreen(
                    externalUserId = externalUserId,
                    swrveUserId = swrveUserId,
                    busy = busy,
                    sdkVersion = sdkVersion,
                    onIdentify = onIdentify
                )

                InboxTab.Inbox -> InboxScreen(
                    messages = messages,
                    onOpenMessage = onOpenMessage,
                    onDelete = onDelete
                )
            }
        }
    }
}

@Composable
private fun HomeScreen(
    externalUserId: String,
    swrveUserId: String,
    busy: Boolean,
    sdkVersion: String,
    onIdentify: (String) -> Unit
) {
    var userId by remember { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Push Inbox Sample",
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

        // The Swrve user ID, which is not the external one typed in below — identifying swaps it for the ID Swrve holds against that external ID,
        // so watching it change is the clearest confirmation that identify did something.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "User ID: $swrveUserId",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val context = LocalContext.current
            var copied by remember { mutableStateOf(false) }
            LaunchedEffect(copied) {
                if (copied) {
                    delay(1500)
                    copied = false
                }
            }

            TextButton(
                onClick = {
                    val clipboard = context.getSystemService(ClipboardManager::class.java)
                    clipboard?.setPrimaryClip(ClipData.newPlainText("User ID", swrveUserId))
                    copied = true
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Text(
                    text = if (copied) "Copied" else "Copy",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        Text(
            text = "Identified as: ${externalUserId.ifEmpty { "—" }}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "This sample has no push integration — see PushNotificationSample for that. "
                    + "It does not need one: inbox messages arrive with the regular content fetch, "
                    + "not with the notification.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Identify as the user your campaign was sent to, then open the Inbox tab.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )

        OutlinedTextField(
            value = userId,
            onValueChange = { userId = it },
            label = { Text("External user ID") },
            singleLine = true,
            enabled = !busy,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                keyboard?.hide()
                onIdentify(userId.trim())
            }),
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                keyboard?.hide()
                onIdentify(userId.trim())
            },
            enabled = !busy && userId.isNotBlank()
        ) {
            Text(if (busy) "Identifying…" else "Identify")
        }
    }
}

@Composable
private fun InboxScreen(
    messages: List<InboxMessage>,
    onOpenMessage: (InboxMessage) -> Unit,
    onDelete: (InboxMessage) -> Unit
) {
    if (messages.isEmpty()) {
        Text(
            text = "No messages for this user.\n\nSend a push with inbox content to them and " +
                    "reopen the app, or identify on the Home tab as the user your campaign targeted.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
        )
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(messages, key = { it.messageId }) { message ->
            MessageRow(
                message = message,
                onOpen = { onOpenMessage(message) },
                onDelete = { onDelete(message) }
            )
        }
    }
}

@Composable
private fun MessageRow(
    message: InboxMessage,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            // Unread is shown three ways: an accent stripe, a bold subject, and a stateDescription.
            // Colour alone would be invisible to a screen reader and to colour-blind users.
            .semantics { stateDescription = if (message.unread) "Unread" else "Read" }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpen)
                .height(IntrinsicSize.Min)
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(
                        if (message.unread) MaterialTheme.colorScheme.primary else Color.Transparent
                    )
            )

            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // thumbnail is optional in the schema, so the row has to look right without one.
                message.thumbnailUrl?.let { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(6.dp))
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // subject is optional too; the body is the only field guaranteed to be there.
                    Text(
                        text = message.subject ?: message.body,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (message.unread) FontWeight.Bold else FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (message.subject != null) {
                        Text(
                            text = message.body,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        text = formatSentDate(message.sentDate),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                TextButton(
                    onClick = onDelete,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text("Delete", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageDetailScreen(
    message: InboxMessage,
    onBack: () -> Unit,
    onFollowAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Inbox") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to inbox")
                    }
                }
            )
        }
    ) { innerPadding ->
        MessageDetail(
            message = message,
            onFollowAction = onFollowAction,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
private fun MessageDetail(
    message: InboxMessage,
    onFollowAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        message.thumbnailUrl?.let { url ->
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
        }

        message.subject?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Text(
            text = formatSentDate(message.sentDate),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = message.body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Engaging is the user acting on the message, which is why it lives on this button rather than on opening the screen.
        // Keyed on action_value, not action_type: the type varies with how the campaign was composed, while the value is a URL either way.
        if (!message.actionValue.isNullOrBlank()) {
            Button(onClick = onFollowAction) { Text("Open") }
        }

        RawPayloadPanel(message.rawCustomerJson)
    }
}

/**
 * Debug aid for this sample only — a real inbox renders the payload, it does not display it.
 */
@Composable
private fun RawPayloadPanel(rawCustomerJson: String) {
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        TextButton(onClick = { expanded = !expanded }, contentPadding = PaddingValues(0.dp)) {
            Text(
                text = if (expanded) "Hide customer_json" else "Show customer_json",
                style = MaterialTheme.typography.labelMedium
            )
        }

        if (expanded) {
            Text(
                text = rawCustomerJson,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(6.dp)
                    )
                    .padding(10.dp)
            )
        }
    }
}

private fun formatSentDate(epochMillis: Long): String =
    SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()).format(Date(epochMillis))

// ---------------------------------------------------------------------------
// Previews.

private val previewMessages = listOf(
    InboxMessage(
        messageId = 1,
        subject = "Your order has shipped",
        body = "Track your package and estimated delivery date.",
        thumbnailUrl = null,
        actionValue = "myapp://orders/48213",
        sentDate = 1_714_842_566_000,
        unread = true,
        rawCustomerJson = "{\n  \"subject\": \"Your order has shipped\",\n  \"action_type\": \"Open web page\"\n}"
    ),
    // The documented minimum: message only.
    InboxMessage(
        messageId = 2,
        subject = null,
        body = "Enjoy 20% off all hoodies this weekend only.",
        thumbnailUrl = null,
        actionValue = null,
        sentDate = 1_714_756_166_000,
        unread = false,
        rawCustomerJson = "{\n  \"message\": \"Enjoy 20% off all hoodies this weekend only.\"\n}"
    )
)

@Preview(showBackground = true, name = "Home")
@Composable
private fun HomePreview() {
    PushInboxSampleTheme(useDarkTheme = false) {
        PushInboxApp(
            messages = previewMessages,
            externalUserId = "",
            swrveUserId = "a1b2c3d4-preview",
            busy = false,
            onIdentify = {}, onOpenMessage = {}, onDelete = {},
            selectedTab = 0,
            onSelectTab = {},
            sdkVersion = "1.2.3"
        )
    }
}

@Preview(showBackground = true, name = "Inbox — dark")
@Composable
private fun InboxPreviewDark() {
    PushInboxSampleTheme(useDarkTheme = true) {
        InboxScreen(
            messages = previewMessages,
            onOpenMessage = {}, onDelete = {}
        )
    }
}

@Preview(showBackground = true, name = "Inbox — not identified")
@Composable
private fun InboxPreviewNotIdentified() {
    PushInboxSampleTheme(useDarkTheme = false) {
        InboxScreen(messages = emptyList(), onOpenMessage = {}, onDelete = {})
    }
}

@Preview(showBackground = true, name = "Detail")
@Composable
private fun DetailPreview() {
    PushInboxSampleTheme(useDarkTheme = false) {
        MessageDetailScreen(message = previewMessages[0], onBack = {}, onFollowAction = {})
    }
}

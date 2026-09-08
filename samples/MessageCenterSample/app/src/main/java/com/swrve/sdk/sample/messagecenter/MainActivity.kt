package com.swrve.sdk.sample.messagecenter

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.swrve.sdk.SwrveCampaignsUpdateListener
import com.swrve.sdk.SwrveSDK
import com.swrve.sdk.messaging.SwrveCampaignState
import com.swrve.sdk.messaging.SwrveOrientation

/**
 * What one row shows, copied out of the SDK's campaign object. Immutable, so Compose can compare two lists and see that a campaign became Seen.
 */
data class MessageCenterItem(
    val id: Int,
    val title: String,
    val description: String?,
    val unseen: Boolean
)

class MainActivity : ComponentActivity() {

    private val campaigns = mutableStateOf<List<MessageCenterItem>>(emptyList())

    private val refreshing = mutableStateOf(false)

    // The SDK does not guarantee which thread this arrives on, and reloadCampaigns writes Compose state.
    private val campaignsUpdateListener = SwrveCampaignsUpdateListener { runOnUiThread { reloadCampaigns() } }

    private var orientation = SwrveOrientation.Portrait

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Fires after the initial load, and again when fetched content may have changed the campaigns —
        // including once the SDK has attempted their asset downloads, which is what makes the list populate
        // on a fresh install without polling. Registered here rather than from a screen: the first
        // notification is never replayed, so a listener installed later may miss it.
        // Held weakly by the SDK, so the strong reference in campaignsUpdateListener is what keeps it alive.
        SwrveSDK.setCampaignsUpdateListener(campaignsUpdateListener)

        setContent {
            // Orientation filters the campaign list, so it has to be re-read whenever it changes.
            // Reading it from LocalConfiguration rather than onResume means this keeps working
            // even if the activity later declares android:configChanges and is not recreated.
            val isLandscape =
                LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
            LaunchedEffect(isLandscape) {
                orientation =
                    if (isLandscape) SwrveOrientation.Landscape else SwrveOrientation.Portrait
                reloadCampaigns()
            }

            MessageCenterSampleTheme {
                Scaffold { innerPadding ->
                    MessageCenterScreen(
                        campaigns = campaigns.value,
                        refreshing = refreshing.value,
                        onRefresh = ::refreshContent,
                        onOpen = { id ->
                            // Displays the in-app message, and marks the campaign Seen as a side effect.
                            SwrveSDK.getMessageCenterCampaign(id, null)?.let {
                                SwrveSDK.showMessageCenterCampaign(it)
                            }
                            reloadCampaigns()
                        },
                        onMarkSeen = { id ->
                            SwrveSDK.markMessageCenterCampaignAsSeen(id)
                            reloadCampaigns()
                        },
                        onRemove = { id ->
                            SwrveSDK.removeMessageCenterCampaign(id)
                            reloadCampaigns()
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        reloadCampaigns()
    }

    /**
     * Asks the server for new content — campaigns, resources, push inbox, real-time user properties — then re-reads the list once it completes.
     * The SDK already refreshes on its own schedule, so an app does not have to call this. It is here because a sample needs a way to pull new campaigns on demand rather than waiting.
     */
    private fun refreshContent() {
        refreshing.value = true
        SwrveSDK.refreshContent { result ->
            runOnUiThread {
                refreshing.value = false
                Log.i(LOG_TAG, "refreshContent finished: ${result.resultCode} ${result.errorMessage}")
                reloadCampaigns()
            }
        }
    }

    /**
     * Re-reads the list from the SDK's local state — no network. Two reasons this has to run again rather than once: the campaigns returned are a snapshot, so marking one seen or
     * removing it changes what the *next* call returns; and the current [orientation] filters which campaigns come back at all.
     *
     * Each campaign is copied into an immutable [MessageCenterItem]. That is deliberate: campaign objects are mutable and the SDK hands back the same instances every time, 
     * so holding them as Compose state means a status change is invisible — the list looks unchanged. Copying the values out gives Compose something it can actually compare.
     */
    private fun reloadCampaigns() {
        // Orientation filters out campaigns with no format for the way the device is currently held. Passing null for properties means no extra personalization values are supplied.
        campaigns.value = SwrveSDK.getInAppMessageCenterCampaigns(orientation, null).map { campaign ->
            // Subject and description are optional dashboard fields, so fall back to the name.
            val details = campaign.messageCenterDetails
            MessageCenterItem(
                id = campaign.id,
                title = details?.subject ?: campaign.name,
                description = details?.description,
                unseen = campaign.status != SwrveCampaignState.Status.Seen
            )
        }
    }

    private companion object {
        const val LOG_TAG = "MessageCenterSample"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageCenterScreen(
    campaigns: List<MessageCenterItem>,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    onOpen: (Int) -> Unit,
    onMarkSeen: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    modifier: Modifier = Modifier,
    sdkVersion: String = SwrveSDK.getSdkVersion(),
    userId: String = SwrveSDK.getUserId()
) {
    // PullToRefreshBox wraps the whole screen and the header is the first list item, so the header scrolls away and gives the list the full height.
    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Header(
                    sdkVersion = sdkVersion,
                    userId = userId,
                    campaigns = campaigns
                )
            }

            if (campaigns.isEmpty()) {
                item {
                    // Inside the list so pull-to-refresh still has a scrollable child when empty.
                    Text(
                        text = "No Message Center campaigns.\n\nCreate an in-app campaign in "
                            + "Swrve, tick \"Message Center\", and make yourself a QA user. "
                            + "Pull down to refresh.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
                    )
                }
            } else {
                items(campaigns, key = { it.id }) { item ->
                    CampaignRow(
                        item = item,
                        onOpen = { onOpen(item.id) },
                        onMarkSeen = { onMarkSeen(item.id) },
                        onRemove = { onRemove(item.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun Header(
    sdkVersion: String,
    userId: String,
    campaigns: List<MessageCenterItem>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Title gets its own row; logo and metadata share the next one.
        Text(
            text = "Message Center Sample",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            // Centred as a group, so the logo and its values sit together rather than being pushed to opposite edges.
            horizontalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = "MessageGears logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier.height(32.dp)
            )

            // Left-aligned and hugging the logo. fill = false lets the column take only the width it needs — so the pair stays centred — while still capping it,
            // so a long user ID ellipsises instead of pushing the logo off screen.
            Column(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .weight(1f, fill = false),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Swrve SDK $sdkVersion",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "User ID: $userId",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Always rendered, including as "0 IAMs · 0 unseen", so the line reserves its space — appearing only when campaigns load would shift the centred row.
                // Both counts come from the list already fetched, so there is no extra SDK call. Removed campaigns are excluded by the SDK, so there is no deleted count.
                val unseen = campaigns.count { it.unseen }
                Text(
                    text = "${campaigns.size} IAM${if (campaigns.size == 1) "" else "s"} · $unseen unseen",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun CampaignRow(
    item: MessageCenterItem,
    onOpen: () -> Unit,
    onMarkSeen: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            // Unseen is shown three ways: an accent stripe down the leading edge, a bold title,
            // and a stateDescription. Colour alone would be invisible to a screen reader and to
            // colour-blind users, and weight still reads in greyscale. stateDescription rather
            // than contentDescription because this is a state, not a replacement label.
            .semantics { stateDescription = if (item.unseen) "Unseen" else "Seen" }
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
                        if (item.unseen) MaterialTheme.colorScheme.primary
                        else Color.Transparent
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 6.dp, top = 8.dp, bottom = 4.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (item.unseen) FontWeight.Bold else FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (!item.description.isNullOrEmpty()) {
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "ID ${item.id}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row {
                        val dense = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                        TextButton(onClick = onMarkSeen, contentPadding = dense) {
                            Text("Mark seen", style = MaterialTheme.typography.labelMedium)
                        }
                        TextButton(onClick = onRemove, contentPadding = dense) {
                            Text("Remove", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

// The UI model is a plain data class, so previews can show populated rows — which was not possible while the screen took SDK campaign objects directly.
private val previewItems = listOf(
    MessageCenterItem(101, "Summer Sale", "20% off everything this weekend", unseen = true),
    MessageCenterItem(102, "Autumn Promo", null, unseen = false)
)

@Preview(showBackground = true, name = "Light")
@Composable
private fun MessageCenterScreenPreviewLight() {
    MessageCenterSampleTheme(useDarkTheme = false) {
        MessageCenterScreen(
            campaigns = previewItems,
            refreshing = false,
            onRefresh = {},
            onOpen = {}, onMarkSeen = {}, onRemove = {},
            sdkVersion = "1.2.3",
            userId = "a1b2c3d4-preview"
        )
    }
}

@Preview(showBackground = true, name = "Dark")
@Composable
private fun MessageCenterScreenPreviewDark() {
    MessageCenterSampleTheme(useDarkTheme = true) {
        MessageCenterScreen(
            campaigns = previewItems,
            refreshing = false,
            onRefresh = {},
            onOpen = {}, onMarkSeen = {}, onRemove = {},
            sdkVersion = "1.2.3",
            userId = "a1b2c3d4-preview"
        )
    }
}

@Preview(showBackground = true, name = "Light — no campaigns")
@Composable
private fun MessageCenterScreenPreviewEmpty() {
    MessageCenterSampleTheme(useDarkTheme = false) {
        MessageCenterScreen(
            campaigns = emptyList(),
            refreshing = false,
            onRefresh = {},
            onOpen = {}, onMarkSeen = {}, onRemove = {},
            sdkVersion = "1.2.3",
            userId = "a1b2c3d4-preview"
        )
    }
}

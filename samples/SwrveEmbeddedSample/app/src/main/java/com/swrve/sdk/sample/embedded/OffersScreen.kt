package com.swrve.sdk.sample.embedded

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.swrve.sdk.SwrveSDK


@Composable
fun OffersScreen(embeddedViewModel: EmbeddedViewModel = viewModel()) {
    LaunchedEffect(Unit) {
        embeddedViewModel.loadOffers()
    }
    val entries = embeddedViewModel.offers.collectAsState(initial = emptyList()).value
    val reportedIds = remember { mutableSetOf<Int>() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 16.dp)),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Offers",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        if (entries.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "No offers available",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Check back later for personalized offers.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(entries, key = { it.message.id }) { entry ->
                    // Report impression once per message ID as item appears
                    LaunchedEffect(entry.message.id) {
                        if (reportedIds.add(entry.message.id)) {
                            SwrveSDK.embeddedMessageWasShownToUser(entry.message)
                        }
                    }
                    OfferRow(entry)
                }
            }
        }
    }
}

@Composable
private fun OfferRow(entry: EmbeddedEntry) {
    val layout = entry.data.layout?.lowercase() ?: "tall_card"
    when (layout) {
        "compact_card" -> CompactOfferCard(entry)
        else -> TallOfferCard(entry)
    }
}

@Composable
private fun TallOfferCard(entry: EmbeddedEntry) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val width = maxWidth
        val imageHeight = width / 2
        val corner = 6.dp
        val item = entry.data
        val ctx = LocalContext.current
        val onCtaClick: () -> Unit = {
            item.cta?.url?.let { url ->
                SwrveSDK.embeddedMessageButtonWasPressed(entry.message, item.cta?.text)
                val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                ctx.startActivity(intent)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(width = 1.dp, color = Color(0x20000000), shape = RoundedCornerShape(corner))
                .clip(RoundedCornerShape(corner))
                .background(hexToColor(item.backgroundColor) ?: MaterialTheme.colorScheme.surface)
                .clickable(enabled = !item.cta?.url.isNullOrBlank()) { onCtaClick() }
        ) {
            // Image area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(imageHeight)
                    .clip(RoundedCornerShape(topStart = corner, topEnd = corner))
            ) {
                val imgReqBuilder = ImageRequest.Builder(ctx).crossfade(true)
                imgReqBuilder.data(item.image)
                AsyncImage(
                    model = imgReqBuilder.build(),
                    contentDescription = item.title?.text,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(imageHeight),
                    contentScale = ContentScale.Crop
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 80.dp)
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    val titleColor = hexToColor(item.title?.color) ?: MaterialTheme.colorScheme.onSurface
                    item.title?.text?.let { titleText ->
                        Text(
                            text = titleText,
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                            color = titleColor
                        )
                    }
                    item.body?.text?.let { bodyText ->
                        val bodyColor = hexToColor(item.body?.color) ?: MaterialTheme.colorScheme.onSurfaceVariant
                        Text(
                            text = bodyText,
                            style = MaterialTheme.typography.bodySmall,
                            color = bodyColor,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                OfferCTA(item = item, onClick = onCtaClick)
            }
        }
    }
}

@Composable
private fun CompactOfferCard(entry: EmbeddedEntry) {
    val item = entry.data
    val corner = 6.dp
    val ctx = LocalContext.current
    val onCtaClick: () -> Unit = {
        item.cta?.url?.let { url ->
            SwrveSDK.embeddedMessageButtonWasPressed(entry.message, item.cta?.text)
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            ctx.startActivity(intent)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = Color(0x20000000), shape = RoundedCornerShape(corner))
            .clip(RoundedCornerShape(corner))
            .background(hexToColor(item.backgroundColor) ?: MaterialTheme.colorScheme.surface)
            .clickable(enabled = !item.cta?.url.isNullOrBlank()) { onCtaClick() }
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Square thumbnail on the left
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(6.dp))
        ) {
            val imgReqBuilder = ImageRequest.Builder(ctx).crossfade(true)
            imgReqBuilder.data(item.image)
            AsyncImage(
                model = imgReqBuilder.build(),
                contentDescription = item.title?.text,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item.title?.text?.let { titleText ->
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                    color = hexToColor(item.title?.color) ?: MaterialTheme.colorScheme.onSurface
                )
            }
            item.body?.text?.let { bodyText ->
                Text(
                    text = bodyText,
                    style = MaterialTheme.typography.bodySmall,
                    color = hexToColor(item.body?.color) ?: MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        OfferCTA(item = item, onClick = onCtaClick)
    }
}

@Composable
private fun OfferCTA(item: EmbeddedData, onClick: () -> Unit) {
    val cta = item.cta
    val showButton = cta?.let {
        !it.text.isNullOrBlank() && !it.backgroundColor.isNullOrBlank() && !it.color.isNullOrBlank()
    } ?: false
    if (!showButton) return

    Button(
        onClick = onClick,
        modifier = Modifier
            .widthIn(min = 80.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = hexToColor(cta?.backgroundColor) ?: MaterialTheme.colorScheme.primary,
            contentColor = hexToColor(cta?.color) ?: MaterialTheme.colorScheme.onPrimary
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = cta?.text.orEmpty(),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

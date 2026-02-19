package com.swrve.sdk.sample.embedded

import android.annotation.SuppressLint
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.swrve.sdk.SwrveSDK
import kotlinx.coroutines.flow.distinctUntilChanged
import timber.log.Timber

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun Carousel(viewModel: EmbeddedViewModel) {
    // Each card ~88% of available width, spacing 12dp. Left edge aligns with parent (outer LazyColumn already applies horizontal padding).
    LaunchedEffect(Unit) {
        viewModel.loadCarousel()
    }
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val cardWidth = maxWidth * 0.88f
        val cardSpacing = 12.dp
        val items = viewModel.embeddedData?.collectAsState(initial = emptyList())?.value ?: emptyList()
        val listState = rememberLazyListState()
        val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
        val reportedIds = remember { mutableSetOf<Int>() }

        LazyRow(
            state = listState,
            flingBehavior = flingBehavior,
            contentPadding = PaddingValues(horizontal = 0.dp),
            horizontalArrangement = Arrangement.spacedBy(cardSpacing)
        ) {
            items(items, key = { it.message.id }) { entry ->
                CarouselCard(entry = entry, width = cardWidth)
            }
        }

        // Report impression when an item first becomes visible
        LaunchedEffect(listState, items) {
            // Handle initial visible items immediately to avoid missing first impressions
            val initialVisible = listState.layoutInfo.visibleItemsInfo.map { it.index }
            initialVisible.forEach { idx ->
                if (idx in items.indices) {
                    val entry = items[idx]
                    val uniqueId = entry.message.id
                    if (reportedIds.add(uniqueId)) {
                        Timber.tag("EmbeddedSample").d("Reporting initial impression for embedded message: ${entry.message.id}")
                        SwrveSDK.embeddedMessageWasShownToUser(entry.message)
                    }
                }
            }
            snapshotFlow { listState.layoutInfo.visibleItemsInfo.map { it.index }.toSet() }
                .distinctUntilChanged()
                .collect { visibleIndices ->
                    visibleIndices.forEach { idx ->
                        if (idx in items.indices) {
                            val entry = items[idx]
                            val uniqueId = entry.message.id
                            if (reportedIds.add(uniqueId)) {
                                Timber.tag("EmbeddedSample").d("Reporting impression for embedded message: ${entry.message.id}")
                                SwrveSDK.embeddedMessageWasShownToUser(entry.message)
                            }
                        }
                    }
                }
        }
    }
    Spacer(modifier = Modifier.height(12.dp))
}

@Composable
fun CarouselCard(entry: EmbeddedEntry, width: Dp) {
    val item = entry.data
    // Layout variants: tall_card shows image + text area; image_only_card shows only image
    // image (2:1 ratio)
    val isImageOnly = item.layout.equals("image_only_card", ignoreCase = true)
    val textAreaHeight = if (isImageOnly) 0.dp else 80.dp
    val imageHeight = width / 2
    val corner = 6.dp
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
            .width(width)
            .height(imageHeight + textAreaHeight)
            .border(width = 0.5.dp, color = Color(0x14000000), shape = RoundedCornerShape(corner))
            .clip(RoundedCornerShape(corner))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(enabled = !item.cta?.url.isNullOrBlank()) { onCtaClick() }
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(imageHeight)
                .clip(if (isImageOnly) RoundedCornerShape(corner) else RoundedCornerShape(topStart = corner, topEnd = corner))
                .let { base ->
                    if (item.image == null) {
                        base.background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF0A66C2),
                                    Color(0xFF004B91)
                                )
                            )
                        )
                    } else base
                },
            contentAlignment = Alignment.Center
        ) {
            if (item.image != null) {
                val imgReqBuilder = ImageRequest.Builder(ctx)
                    .crossfade(true)
                imgReqBuilder.data(item.image)
                AsyncImage(
                    model = imgReqBuilder.build(),
                    contentDescription = item.title?.text,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(imageHeight),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = "IMG",
                    color = Color.White.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
        // Text/info area centered vertically (tall_card only)
        if (!isImageOnly) {
            val textAreaBg = hexToColor(item.backgroundColor) ?: MaterialTheme.colorScheme.surface
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(textAreaHeight)
                    .background(textAreaBg)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        val titleColor = hexToColor(item.title?.color) ?: Color.Black
                        Text(
                            text = item.title?.text ?: "",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            color = titleColor
                        )
                        item.body?.text?.let { bodyText ->
                            val bodyColor = hexToColor(item.body.color) ?: MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            Text(
                                text = bodyText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = bodyColor,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    val showButton = item.cta?.let { cta ->
                        // Show only when all visual properties required are present
                        !cta.text.isNullOrBlank() && !cta.backgroundColor.isNullOrBlank() && !cta.color.isNullOrBlank()
                    } ?: false
                    if (showButton) {
                        val ctaText = item.cta.text!!
                        Button(
                            onClick = onCtaClick,
                            modifier = Modifier
                                .height(34.dp)
                                .widthIn(min = 96.dp),
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = hexToColor(item.cta.backgroundColor) ?: MaterialTheme.colorScheme.primary,
                                contentColor = hexToColor(item.cta.color) ?: MaterialTheme.colorScheme.onPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
                        ) {
                            Text(text = ctaText, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
        }
    }
}

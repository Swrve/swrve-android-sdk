package com.swrve.sdk.sample.embedded

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.swrve.sdk.SwrveSDK
import kotlinx.coroutines.delay

// Timing
private const val FLOATING_BANNER_LOAD_DELAY_MS = 1000L
private const val IMAGE_ANIMATION_DURATION_MS = 300
private const val CARD_ANIMATION_DURATION_MS = 300

// Sizing
private val COLLAPSED_IMAGE_SIZE = 48.dp
private val EXPANDED_IMAGE_SIZE = 96.dp

// Spacing
private val IMAGE_START_PADDING = 12.dp
private val IMAGE_TOP_PADDING_COLLAPSED = 6.dp
private val EXPANDED_IMAGE_TOP_EXTRA_PADDING = 8.dp
private val FLOATING_BANNER_VERTICAL_MARGIN = 20.dp
private val FLOATING_BANNER_CORNER_RADIUS = 14.dp
private val NO_IMAGE_COLLAPSED_LEADING_SPACER = 12.dp
private val BOTTOM_BANNER_COLLAPSED_IMAGE_OFFSET = 8.dp

/**
 * Displays a floating banner that can be positioned at the top or bottom of the screen.
 * The banner automatically loads after a delay, supports expand/collapse interactions,
 * and reports impressions to the Swrve SDK.
 *
 * @param viewModel The ViewModel managing the floating banner data and state
 * @param modifier Optional modifier for the root container
 */
@Composable
fun FloatingBanner(viewModel: EmbeddedViewModel, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current

    LaunchedEffect(Unit) {
        delay(FLOATING_BANNER_LOAD_DELAY_MS)
        viewModel.loadFloatingBanner()
    }

    val entry = viewModel.floatingBanner.collectAsState(initial = null).value
    var isVisible by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(entry) {
        isVisible = entry != null
    }

    // Report impression once per message when it becomes visible
    LaunchedEffect(entry?.message?.id) {
        if (entry != null && isVisible) {
            SwrveSDK.embeddedMessageWasShownToUser(entry.message)
        }
    }

    // Determine banner position: "top" (default) or "bottom"
    val isTop = (entry?.data?.location ?: "top").lowercase() != "bottom"

    AnimatedVisibility(
        visible = isVisible && entry != null,
        enter = slideInVertically(initialOffsetY = { fullHeight -> if (isTop) -fullHeight else fullHeight }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { fullHeight -> if (isTop) -fullHeight else fullHeight }) + fadeOut()
    ) {
        val data = entry!!.data

        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = if (isTop) Alignment.TopCenter else Alignment.BottomCenter
        ) {
            FloatingBannerCard(
                context = ctx,
                data = data,
                isExpanded = isExpanded,
                onExpandedChange = { isExpanded = it },
                onClose = { isVisible = false },
                onCtaClick = {
                    val cta = data.cta
                    val msg = entry?.message
                    if (cta != null && !cta.url.isNullOrBlank() && msg != null) {
                        SwrveSDK.embeddedMessageButtonWasPressed(msg, cta.text ?: "Unknown")
                        val intent = Intent(Intent.ACTION_VIEW, cta.url.toUri())
                        ctx.startActivity(intent)
                        isVisible = false
                    }
                }
            )
        }
    }
}

@Composable
private fun FloatingBannerCard(
    context: android.content.Context,
    data: EmbeddedData,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onClose: () -> Unit,
    onCtaClick: () -> Unit
) {
    val hasImage = !data.image.isNullOrEmpty()
    var showCollapsedText by remember { mutableStateOf(true) }
    var showCloseButton by remember { mutableStateOf(true) }
    var showChevronButton by remember { mutableStateOf(false) }

    // Synchronize button visibility with image animation to create smooth state transitions.
    LaunchedEffect(isExpanded, hasImage) {
        if (!isExpanded) {
            if (hasImage) {
                delay(IMAGE_ANIMATION_DURATION_MS.toLong())
            }
            showCollapsedText = true
            showCloseButton = true
        } else {
            if (hasImage) {
                delay(IMAGE_ANIMATION_DURATION_MS.toLong())
            }
            showChevronButton = true
        }
    }
    val closeIconColor = if (isExpanded) {
        hexToColor(data.expandedTitle?.color) ?: MaterialTheme.colorScheme.onSurface
    } else {
        hexToColor(data.title?.color) ?: MaterialTheme.colorScheme.onSurface
    }

    // Shared image animation (single image used for both collapsed and expanded states)
    val imageSize by animateDpAsState(
        targetValue = if (isExpanded) EXPANDED_IMAGE_SIZE else COLLAPSED_IMAGE_SIZE,
        animationSpec = tween(
            durationMillis = IMAGE_ANIMATION_DURATION_MS,
            easing = LinearEasing
        ),
        label = "bannerImageSize"
    )

    BoxWithConstraints(
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = FLOATING_BANNER_VERTICAL_MARGIN)
            .fillMaxWidth()
            .clip(RoundedCornerShape(FLOATING_BANNER_CORNER_RADIUS))
            .background(hexToColor(data.backgroundColor) ?: MaterialTheme.colorScheme.surface)
            .animateContentSize(
                animationSpec = tween(
                    durationMillis = CARD_ANIMATION_DURATION_MS,
                    easing = LinearEasing
                )
            )
    ) {
        // Animate the image horizontally from its collapsed left position to centered when expanded,
        // using the final expanded image size so size and position stay in sync.
        val centerOffset = (maxWidth - EXPANDED_IMAGE_SIZE) / 2 - IMAGE_START_PADDING
        val imageHorizontalOffset by animateDpAsState(
            targetValue = if (isExpanded) centerOffset else 0.dp,
            animationSpec = tween(
                durationMillis = IMAGE_ANIMATION_DURATION_MS,
                easing = LinearEasing
            ),
            label = "bannerImageHorizontalOffset"
        )

        // For bottom-positioned banners, add a small vertical offset when collapsed to keep the image visually aligned with the collapsed card height.
        val isBottomLocation = (data.location ?: "top").lowercase() == "bottom"
        val imageVerticalOffset by animateDpAsState(
            targetValue = if (isBottomLocation && !isExpanded) {
                BOTTOM_BANNER_COLLAPSED_IMAGE_OFFSET
            } else {
                0.dp
            },
            animationSpec = tween(
                durationMillis = IMAGE_ANIMATION_DURATION_MS,
                easing = LinearEasing
            ),
            label = "bannerImageVerticalOffset"
        )

        if (hasImage) {
            val req = ImageRequest.Builder(context)
                .data(data.image)
                .crossfade(true)
                .build()
            AsyncImage(
                model = req,
                contentDescription = data.title?.text ?: data.expandedTitle?.text,
                modifier = Modifier
                    .padding(start = IMAGE_START_PADDING, top = IMAGE_TOP_PADDING_COLLAPSED)
                    .offset(x = imageHorizontalOffset, y = imageVerticalOffset)
                    .width(imageSize)
                    .height(imageSize)
                    .clip(RoundedCornerShape(if (isExpanded) 12.dp else 8.dp)),
                contentScale = ContentScale.Crop
            )
        }

        Column(
            modifier = Modifier
                .clickable(
                    // Disable ripple/pressed color to avoid visual artifacts during collapse
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    if (!isExpanded) {
                        // Expand: when we have an image, hide collapsed text and close button during animation
                        if (hasImage) {
                            showCloseButton = false
                            showCollapsedText = false
                        } else {
                            showCloseButton = false
                        }
                        onExpandedChange(true)
                    } else {
                        // Collapse: when we have an image, hide chevron and text during animation
                        showChevronButton = false
                        showCloseButton = false
                        if (hasImage) {
                            showCollapsedText = false
                        }
                        onExpandedChange(false)
                    }
                }
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            if (!isExpanded) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val leadingSpacerWidth = if (hasImage) {
                        imageSize + 4.dp
                    } else {
                        NO_IMAGE_COLLAPSED_LEADING_SPACER
                    }
                    Spacer(modifier = Modifier.width(leadingSpacerWidth))

                    if (showCollapsedText) {
                        CollapsedBannerContent(
                            data = data
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Close button inline in collapsed state
                    if (showCloseButton) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = closeIconColor,
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .clickable(onClick = onClose)
                        )
                    }
                }
            } else {
                ExpandedBannerContent(
                    data = data,
                    hasImage = hasImage,
                    onCtaClick = onCtaClick
                )
            }
        }

        // Collapse button (chevron) overlaid at the top-right when expanded
        if (isExpanded && showChevronButton) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = "Collapse",
                tint = closeIconColor,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 18.dp)
                    .clickable(
                        onClick = {
                            // Hide chevron during animation, LaunchedEffect will show close button after
                            showChevronButton = false
                            showCloseButton = false
                            showCollapsedText = false
                            onExpandedChange(false)
                        }
                    )
            )
        }
    }
}

@Composable
private fun CollapsedBannerContent(
    data: EmbeddedData
) {
    Column(
        verticalArrangement = Arrangement.Center
    ) {
        data.title?.text?.let {
            val color = hexToColor(data.title.color) ?: MaterialTheme.colorScheme.onSurface
            Text(
                text = it,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        data.body?.text?.let {
            val color = hexToColor(data.body.color) ?: MaterialTheme.colorScheme.onSurfaceVariant
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = color,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ExpandedBannerContent(
    data: EmbeddedData,
    hasImage: Boolean,
    onCtaClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val topSpacer = if (hasImage) {
            EXPANDED_IMAGE_SIZE + EXPANDED_IMAGE_TOP_EXTRA_PADDING
        } else {
            16.dp
        }
        Spacer(modifier = Modifier.height(topSpacer))

        data.expandedTitle?.text?.let {
            val color = hexToColor(data.expandedTitle.color) ?: MaterialTheme.colorScheme.onSurface
            Text(
                text = it,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = color,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            )
        }

        data.expandedBody?.text?.let {
            val color = hexToColor(data.expandedBody.color) ?: MaterialTheme.colorScheme.onSurfaceVariant
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = color,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
        }

        data.helperText?.text?.let {
            val color = hexToColor(data.helperText.color) ?: MaterialTheme.colorScheme.onSurfaceVariant
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = color,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, start = 24.dp, end = 24.dp)
            )
        }

        data.cta?.let { cta ->
            if (!cta.text.isNullOrBlank() && !cta.backgroundColor.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onCtaClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = hexToColor(cta.backgroundColor) ?: MaterialTheme.colorScheme.primary,
                        contentColor = hexToColor(cta.color) ?: MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = cta.text,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

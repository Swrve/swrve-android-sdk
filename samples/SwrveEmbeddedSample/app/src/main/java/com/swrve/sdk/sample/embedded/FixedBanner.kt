package com.swrve.sdk.sample.embedded

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.swrve.sdk.SwrveSDK
import com.swrve.sdk.messaging.SwrveEmbeddedMessage
import kotlinx.coroutines.delay

@Composable
fun FixedBanner(viewModel: EmbeddedViewModel) {
    val ctx = LocalContext.current

    val entry = viewModel.fixedBanner.collectAsState(initial = null).value
    LaunchedEffect(entry == null) {
        if (entry == null) {
            delay(100)
            viewModel.requestBanner()
        }
    }

    // Only animate the first time the banner appears
    var hasAnimated by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(entry != null) {
        if (entry != null && !hasAnimated) {
            hasAnimated = true
        }
    }

    // Track impression when banner is shown
    LaunchedEffect(entry?.message) {
        entry?.message?.let { message ->
            SwrveSDK.embeddedMessageWasShownToUser(message)
        }
    }

    AnimatedVisibility(
        visible = entry != null,
        enter = if (!hasAnimated) {
            slideInVertically(initialOffsetY = { -it }) + fadeIn()
        } else {
            EnterTransition.None
        }
    ) {
        // Card UI with image background with overlay text and optional CTA
        val p = entry?.data
        if (p != null && !p.image.isNullOrEmpty()) {
            val layout = p.layout?.lowercase()
            val isLeft = layout?.contains("left") == true
            val isRight = layout?.contains("right") == true
            val hAlign: Alignment.Horizontal = when {
                isLeft -> Alignment.Start
                isRight -> Alignment.End
                else -> Alignment.CenterHorizontally
            }
            val textAlign: TextAlign = when {
                isLeft -> TextAlign.Start
                isRight -> TextAlign.End
                else -> TextAlign.Center
            }
            val startPad = if (isLeft) 20.dp else 16.dp
            val endPad = if (isRight) 20.dp else 16.dp
            val imgReq = ImageRequest.Builder(ctx)
                .data(p.image)
                .crossfade(hasAnimated)
                .build()
            Column {
                androidx.compose.material3.Card(shape = RoundedCornerShape(6.dp)) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        AsyncImage(
                            model = imgReq,
                            contentDescription = p.title?.text,
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.Crop
                        )
                        Column(
                            modifier = Modifier
                                .matchParentSize()
                                .padding(start = startPad, end = endPad, top = 12.dp, bottom = 12.dp),
                            horizontalAlignment = hAlign
                        ) {
                            p.title?.text?.let { title ->
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                                    color = hexToColor(p.title?.color) ?: androidx.compose.ui.graphics.Color.White,
                                    textAlign = textAlign
                                )
                            }
                            p.body?.text?.let { body ->
                                Text(
                                    text = body,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = hexToColor(p.body?.color) ?: androidx.compose.ui.graphics.Color.White,
                                    textAlign = textAlign
                                )
                            }
                            p.cta?.text?.let { ctaText ->
                                val ctaBg = hexToColor(p.cta.backgroundColor) ?: MaterialTheme.colorScheme.primary
                                val ctaFg = hexToColor(p.cta.color) ?: MaterialTheme.colorScheme.onPrimary
                                Text(
                                    text = ctaText,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                                    color = ctaFg,
                                    modifier = Modifier
                                        .padding(top = 8.dp)
                                        .background(
                                            color = ctaBg,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }
                        // Full-card click handler
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable(enabled = !p.cta?.url.isNullOrBlank()) {
                                    val cta = p.cta
                                    val msg: SwrveEmbeddedMessage? = entry?.message
                                    if (cta != null && !cta.url.isNullOrBlank() && msg != null) {
                                        SwrveSDK.embeddedMessageButtonWasPressed(msg, cta.text ?: "Unknown")
                                        val intent = Intent(Intent.ACTION_VIEW, cta.url!!.toUri())
                                        ctx.startActivity(intent)
                                    }
                                }
                        ) {}
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        } else {
            // If no data, keep spacing minimal
            Spacer(modifier = Modifier.height(1.dp))
        }
    }
}

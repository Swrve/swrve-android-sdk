package com.swrve.sdk.sample.embedded

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel


@Composable
fun HomeScreen(embeddedViewModel: EmbeddedViewModel = viewModel(), display: String) {
    // When showing the floating banner experience, we still render the normal
    // home content but overlay the banner on top so the user can scroll
    // underneath it.
    val contentDisplay = if (display == "floating_banner") "none" else display

    Box(modifier = Modifier.fillMaxSize()) {
        HomeContent(embeddedViewModel = embeddedViewModel, display = contentDisplay)

        if (display == "floating_banner") {
            FloatingBanner(viewModel = embeddedViewModel, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun HomeContent(embeddedViewModel: EmbeddedViewModel, display: String) {
    val featured = listOf(1, 2, 3, 4)
    val featuredRows = featured.chunked(2)
    val recommended = List(6) { it + 1 }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Home",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }

        item(key = display) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
            ) {
                when (display) {
                    "fixed_banner" -> FixedBanner(viewModel = embeddedViewModel)
                    "carousel" -> Carousel(viewModel = embeddedViewModel)
                }
            }
        }

        item {
            Text(
                text = "Featured Products",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
            )
        }
        items(featuredRows) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FeaturedProductCard(embeddedViewModel, index = row[0], modifier = Modifier.weight(1f))
                if (row.size > 1) {
                    FeaturedProductCard(embeddedViewModel, index = row[1], modifier = Modifier.weight(1f))
                } else {
                    Box(modifier = Modifier.weight(1f)) {} // Keep spacing when odd count
                }
            }
        }

        item { HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp)) }

        item {
            Text(
                text = "Recommended For You",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
            )
        }
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(recommended) { idx ->
                    MiniProductCard(index = idx)
                }
            }
        }
    }
}

@Composable
private fun FeaturedProductCard(embeddedViewModel: EmbeddedViewModel, index: Int, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(6.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(
                        Brush.linearGradient(
                            colors = if (isSystemInDarkTheme()) {
                                listOf(Color(0x40213545), Color(0x400E3E7F))
                            } else {
                                listOf(Color(0x40387CFA), Color(0x400051BF))
                            }
                        ), shape = RoundedCornerShape(6.dp)
                    ), contentAlignment = Alignment.Center
            ) {
                Text(text = "IMG $index", color = Color.White.copy(alpha = 0.9f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
            }

            Text(
                text = "Product Title $index",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface),
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(text = "$${19 + (index - 1)}.99", style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)))
            Button(
                onClick = { embeddedViewModel.addToCart(index) },
                modifier = Modifier
                    .padding(top = 6.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
            ) {
                Text(text = "Add to Cart", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
            }
        }
    }
}

@Composable
private fun MiniProductCard(index: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(4.dp)) {
        Box(
            modifier = Modifier
                .size(width = 90.dp, height = 70.dp)
                .background(
                    if (isSystemInDarkTheme()) Color(0x40213545) else Color(0x40FFA500),
                    shape = RoundedCornerShape(6.dp)
                ), contentAlignment = Alignment.Center
        ) {
            Text(text = "IMG", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
        }
        Text(text = "Item $index", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
        Text(text = "$${9 + (index - 1)}.99", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
    }
}

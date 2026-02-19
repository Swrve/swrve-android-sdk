package com.swrve.sdk.sample.embedded

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private val LOGO_WIDTH_FRACTION = 0.55f
private val LOGO_TOP_PADDING = 24.dp
private val BUTTONS_TOP_PADDING = 24.dp
private val CONTENT_BOTTOM_PADDING = 80.dp
private val BUTTON_CORNER_RADIUS = 6.dp

class LauncherActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SwrveSampleTheme {
                LauncherScreen(
                    onOpenCarousel = {
                        startActivity(
                            Intent(this@LauncherActivity, MainActivity::class.java)
                                .putExtra("display", "carousel")
                        )
                    },
                    onOpenOffers = {
                        startActivity(
                            Intent(this@LauncherActivity, MainActivity::class.java)
                                .putExtra("display", "offers")
                        )
                    },
                    onOpenFixedBanner = {
                        startActivity(
                            Intent(this@LauncherActivity, MainActivity::class.java)
                                .putExtra("display", "fixed_banner")
                        )
                    },
                    onOpenFloatingBanner = {
                        startActivity(
                            Intent(this@LauncherActivity, MainActivity::class.java)
                                .putExtra("display", "floating_banner")
                        )
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LauncherScreen(
    onOpenCarousel: () -> Unit,
    onOpenOffers: () -> Unit,
    onOpenFixedBanner: () -> Unit,
    onOpenFloatingBanner: () -> Unit
) {
    var showSetup by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .statusBarsPadding()
                .padding(PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = CONTENT_BOTTOM_PADDING)),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Embedded Samples",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Image(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = "App Logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .padding(top = LOGO_TOP_PADDING)
                    .fillMaxWidth(LOGO_WIDTH_FRACTION)
            )

            // Navigation buttons for embedded campaign examples
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = BUTTONS_TOP_PADDING),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = onOpenCarousel,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(BUTTON_CORNER_RADIUS),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(text = "Carousel", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                }

                Button(
                    onClick = onOpenOffers,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(BUTTON_CORNER_RADIUS),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(text = "Offers", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                }

                Button(
                    onClick = onOpenFixedBanner,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(BUTTON_CORNER_RADIUS),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(text = "Fixed Banner", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                }

                Button(
                    onClick = onOpenFloatingBanner,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(BUTTON_CORNER_RADIUS),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(text = "Floating Banner", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                }
            }
        }

        // Setup button anchored at bottom right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 16.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = { showSetup = true }
            ) {
                Text(
                    text = "Setup",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }

    if (showSetup) {
        ModalBottomSheet(
            onDismissRequest = { showSetup = false },
            sheetState = sheetState
        ) {
            SetupSheetContent()
        }
    }
}

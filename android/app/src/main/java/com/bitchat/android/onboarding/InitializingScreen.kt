package com.bitchat.android.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.bitchat.android.core.ui.icon.LocalLinkIcon
import com.bitchat.android.ui.theme.BitchatFontFamily
import com.bitchat.android.R

/**
 * Loading screen shown during app initialization after permissions are granted
 */
@Composable
fun InitializingScreen(modifier: Modifier) {
    val colorScheme = MaterialTheme.colorScheme
    
    // Animated rotation for the loading indicator
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Animated dots for loading text
    val dotCount = 3
    val animationDelay = 300
    val dots = (0 until dotCount).map { index ->
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = animationDelay * dotCount),
                repeatMode = RepeatMode.Reverse,
                initialStartOffset = StartOffset(animationDelay * index)
            ),
            label = "dot_$index"
        )
        alpha
    }

    Box(
        modifier = modifier.padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Branded mesh mark: the LocalLink node graph sits inside a rotating "signal" ring
            // and a slow breathing pulse, so the splash reads as the app finding the mesh rather
            // than a generic spinner.
            BrandedMeshMark(
                rotationAngle = rotationAngle,
                color = colorScheme.primary
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // App title
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontFamily = BitchatFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.primary
                    ),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(R.string.about_tagline),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = BitchatFontFamily,
                        color = colorScheme.onSurface.copy(alpha = 0.55f)
                    ),
                    textAlign = TextAlign.Center
                )
            }

            // Loading text with animated dots
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.initializing_mesh_network),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = BitchatFontFamily,
                        color = colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                )
                
                // Animated dots
                dots.forEach { alpha ->
                    Text(
                        text = stringResource(R.string.dot),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = BitchatFontFamily,
                            color = colorScheme.onSurface.copy(alpha = alpha)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Status message
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.setting_up_bluetooth),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = BitchatFontFamily,
                            color = colorScheme.onSurface.copy(alpha = 0.8f)
                        ),
                        textAlign = TextAlign.Center
                    )
                    
                    Text(
                        text = stringResource(R.string.should_take_seconds),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = BitchatFontFamily,
                            color = colorScheme.onSurface.copy(alpha = 0.6f)
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * The LocalLink mesh mark centered inside a rotating, dashed "signal" ring with a slow breathing
 * pulse. Purely decorative brand chrome for the splash — no state, no interaction.
 */
@Composable
private fun BrandedMeshMark(
    rotationAngle: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "meshMark")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "meshPulse"
    )

    Box(
        modifier = modifier.size(112.dp),
        contentAlignment = Alignment.Center
    ) {
        // Rotating dashed ring — the "scanning for peers" halo.
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .rotate(rotationAngle)
        ) {
            val stroke = 3.dp.toPx()
            drawArc(
                color = color.copy(alpha = 0.20f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(stroke, stroke),
                size = androidx.compose.ui.geometry.Size(size.width - stroke * 2, size.height - stroke * 2),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
            )
            // A bright leading segment gives the ring a clear direction of travel.
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(stroke, stroke),
                size = androidx.compose.ui.geometry.Size(size.width - stroke * 2, size.height - stroke * 2),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = stroke,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            )
        }

        // Soft filled disc so the black-stroked mesh mark stays legible on any background.
        Box(
            modifier = Modifier
                .size(64.dp)
                .scale(pulse),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = androidx.compose.foundation.shape.CircleShape,
                color = color
            ) {}
            Icon(
                imageVector = LocalLinkIcon,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

/**
 * Error screen shown if initialization fails
 */
@Composable
fun InitializationErrorScreen(
    modifier: Modifier,
    errorMessage: String,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    OnboardingScaffold(
        modifier = modifier,
        title = stringResource(R.string.setup_not_complete),
        titleColor = colorScheme.error,
        illustration = {
            OnboardingHaloIcon(
                icon = Icons.Filled.ErrorOutline,
                contentDescription = stringResource(R.string.cd_error),
                accent = colorScheme.error,
                spinning = false
            )
        },
        description = errorMessage,
        actions = {
            OnboardingPrimaryButton(
                text = stringResource(R.string.try_again),
                onClick = onRetry
            )
            OnboardingOutlinedButton(
                text = stringResource(R.string.open_settings),
                onClick = onOpenSettings
            )
        }
    )
}

package com.bitchat.android.onboarding

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bitchat.android.core.ui.icon.LocalLinkIcon
import com.bitchat.android.ui.theme.BitchatFontFamily

/**
 * Shared building blocks for the onboarding / permission screens.
 *
 * Every check screen (Bluetooth, location, battery, background location, errors) is laid out
 * with the same three parts — a haloed brand illustration, a title + supporting copy, and a
 * pinned action stack — so the whole first-run flow reads as one modern, coherent experience
 * rather than a set of differently-styled dialogs.
 */

/** Corner radius shared by onboarding buttons and info cards. */
private val OnboardingButtonShape = RoundedCornerShape(16.dp)
private val OnboardingCardShape = RoundedCornerShape(16.dp)

/**
 * The signature onboarding illustration: a glyph on a filled accent disc, wrapped in a soft
 * radial glow and a rotating dashed "signal" ring.
 *
 * @param spinning drives the ring; set false for terminal states (success / error / not-supported)
 *   so the mark reads as settled.
 * @param loading spins the ring fast, as a branded progress indicator for "checking…" states.
 */
@Composable
fun OnboardingHalo(
    modifier: Modifier = Modifier,
    size: Dp = 176.dp,
    accent: Color = MaterialTheme.colorScheme.primary,
    spinning: Boolean = true,
    loading: Boolean = false,
    glyph: @Composable () -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "onboardingHalo")
    val pulse by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "haloPulse"
    )
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (loading) 1400 else 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "haloSpin"
    )

    val ringRotation = if (spinning || loading) spin else 0f
    val discSize = size * 0.52f
    val ringSize = size * 0.84f

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(pulse)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.22f),
                            accent.copy(alpha = 0.06f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
        Canvas(
            modifier = Modifier
                .size(ringSize)
                .rotate(ringRotation)
        ) {
            val stroke = 2.dp.toPx()
            val inset = stroke
            drawArc(
                color = accent.copy(alpha = 0.18f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(this.size.width - inset * 2, this.size.height - inset * 2),
                style = Stroke(width = stroke)
            )
            drawArc(
                color = accent,
                startAngle = -90f,
                sweepAngle = if (loading) 100f else 70f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(this.size.width - inset * 2, this.size.height - inset * 2),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        Box(
            modifier = Modifier
                .size(discSize)
                .background(accent, CircleShape),
            contentAlignment = Alignment.Center,
            content = { glyph() }
        )
    }
}

/** Convenience: a haloed [ImageVector] glyph tinted for contrast against the accent disc. */
@Composable
fun OnboardingHaloIcon(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    spinning: Boolean = true,
    loading: Boolean = false,
    glyphTint: Color = Color.Black,
) {
    OnboardingHalo(
        modifier = modifier,
        accent = accent,
        spinning = spinning,
        loading = loading,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = glyphTint,
            modifier = Modifier.size(56.dp)
        )
    }
}

/** Convenience: a haloed LocalLink brand mark, used for neutral / checking states. */
@Composable
fun OnboardingHaloBrand(
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    loading: Boolean = false,
) {
    OnboardingHalo(
        modifier = modifier,
        accent = accent,
        loading = loading,
    ) {
        Icon(
            imageVector = LocalLinkIcon,
            contentDescription = null,
            tint = Color.Black,
            modifier = Modifier.size(62.dp)
        )
    }
}

/**
 * Standard onboarding layout: a vertically-centered (scrollable) illustration + copy block,
 * with an action stack pinned to the bottom.
 */
@Composable
fun OnboardingScaffold(
    modifier: Modifier,
    title: String,
    illustration: @Composable () -> Unit,
    description: String? = null,
    titleColor: Color = MaterialTheme.colorScheme.primary,
    content: @Composable ColumnScope.() -> Unit = {},
    actions: @Composable ColumnScope.() -> Unit = {},
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp)
            .padding(top = 12.dp, bottom = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
        ) {
            illustration()

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = BitchatFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 25.sp
                ),
                color = titleColor,
                textAlign = TextAlign.Center
            )

            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = BitchatFontFamily,
                        color = colorScheme.onSurface.copy(alpha = 0.65f),
                        lineHeight = 23.sp
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 360.dp)
                )
            }

            content()
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = actions
        )
    }
}

/**
 * A grouped info row: leading icon, bold title, supporting body. Matches the About sheet's card
 * language so settings and onboarding feel like the same app.
 */
@Composable
fun OnboardingInfoCard(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = colorScheme.surfaceVariant.copy(alpha = 0.25f),
        shape = OnboardingCardShape
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(20.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = colorScheme.onBackground
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = BitchatFontFamily),
                    color = colorScheme.onBackground.copy(alpha = 0.8f)
                )
            }
        }
    }
}

/** Full-width filled primary action, with an inline spinner when [loading]. */
@Composable
fun OnboardingPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val colorScheme = MaterialTheme.colorScheme
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = OnboardingButtonShape,
        colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary)
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.width(10.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = BitchatFontFamily,
                fontWeight = FontWeight.Bold
            ),
            color = colorScheme.onPrimary
        )
    }
}

/** Full-width outlined secondary action. */
@Composable
fun OnboardingOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colorScheme = MaterialTheme.colorScheme
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = OnboardingButtonShape
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = BitchatFontFamily,
                fontWeight = FontWeight.Medium
            ),
            color = colorScheme.onSurface
        )
    }
}

/** Low-emphasis text action (skip / dismiss). */
@Composable
fun OnboardingTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colorScheme = MaterialTheme.colorScheme
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(
                color = colorScheme.onSurface.copy(alpha = 0.6f)
            )
        )
    }
}

package com.bitchat.android.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.bitchat.android.R
import com.bitchat.android.core.ui.icon.LocalLinkIcon
import com.bitchat.android.ui.theme.BitchatFontFamily
import kotlinx.coroutines.launch

/**
 * First-run welcome carousel.
 *
 * A modern, swipeable intro shown once before the permission flow. Each page pairs an animated
 * brand illustration with a short pitch, and the bottom control advances the pager (or finishes
 * the intro on the last page). Purely presentational — it owns no app state and simply calls
 * [onGetStarted] when the user is ready to continue into onboarding.
 */
private data class WelcomePage(
    val titleRes: Int,
    val descRes: Int,
    val illustration: WelcomeIllustration,
)

private enum class WelcomeIllustration { MeshMark, Relay, Privacy }

@Composable
fun WelcomeScreen(
    modifier: Modifier = Modifier,
    onGetStarted: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme

    val pages = remember {
        listOf(
            WelcomePage(R.string.welcome_p1_title, R.string.welcome_p1_desc, WelcomeIllustration.MeshMark),
            WelcomePage(R.string.welcome_p2_title, R.string.welcome_p2_desc, WelcomeIllustration.Relay),
            WelcomePage(R.string.welcome_p3_title, R.string.welcome_p3_desc, WelcomeIllustration.Privacy),
        )
    }
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == pages.lastIndex

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        // Top row: brand mark + Skip. Skip jumps straight past the intro.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 8.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = LocalLinkIcon,
                    contentDescription = null,
                    tint = colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = BitchatFontFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = onGetStarted) {
                Text(
                    text = stringResource(R.string.welcome_skip),
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { page ->
            WelcomePageContent(page = pages[page])
        }

        // Page indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(pages.size) { index ->
                val active = pagerState.currentPage == index
                val width by animateDpAsState(
                    targetValue = if (active) 26.dp else 8.dp,
                    animationSpec = tween(280, easing = FastOutSlowInEasing),
                    label = "dotWidth"
                )
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .height(8.dp)
                        .width(width)
                        .clip(CircleShape)
                        .background(
                            if (active) colorScheme.primary
                            else colorScheme.onSurface.copy(alpha = 0.20f)
                        )
                )
            }
        }

        // Primary control
        Button(
            onClick = {
                if (isLastPage) {
                    onGetStarted()
                } else {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp)
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary)
        ) {
            AnimatedContent(
                targetState = isLastPage,
                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(180)) },
                label = "welcomeButtonLabel"
            ) { last ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(
                            if (last) R.string.welcome_get_started else R.string.welcome_next
                        ),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = BitchatFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = colorScheme.onPrimary
                    )
                    Icon(
                        imageVector = Icons.Filled.ArrowForward,
                        contentDescription = null,
                        tint = colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun WelcomePageContent(page: WelcomePage) {
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        WelcomeIllustrationArt(kind = page.illustration)

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = stringResource(page.titleRes),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = BitchatFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp
            ),
            color = colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = stringResource(page.descRes),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = BitchatFontFamily,
                color = colorScheme.onSurface.copy(alpha = 0.65f),
                lineHeight = 24.sp
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 340.dp)
        )
    }
}

/**
 * A large circular "halo" backdrop shared by every welcome illustration, with a per-page glyph.
 * The halo pulses slowly so the intro never feels static.
 */
@Composable
private fun WelcomeIllustrationArt(kind: WelcomeIllustration) {
    val colorScheme = MaterialTheme.colorScheme
    val primary = colorScheme.primary

    val transition = rememberInfiniteTransition(label = "welcomeArt")
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
            animation = tween(9000), repeatMode = RepeatMode.Restart
        ),
        label = "haloSpin"
    )

    Box(
        modifier = Modifier.size(220.dp),
        contentAlignment = Alignment.Center
    ) {
        // Soft radial glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(pulse)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            primary.copy(alpha = 0.22f),
                            primary.copy(alpha = 0.06f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
        // Rotating dashed ring
        Canvas(
            modifier = Modifier
                .size(184.dp)
                .rotate(spin)
        ) {
            val stroke = 2.dp.toPx()
            val inset = stroke
            drawArc(
                color = primary.copy(alpha = 0.18f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2),
                style = Stroke(width = stroke)
            )
            drawArc(
                color = primary,
                startAngle = -90f,
                sweepAngle = 70f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        // Center glyph on a filled disc
        Box(
            modifier = Modifier
                .size(112.dp)
                .background(primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            when (kind) {
                WelcomeIllustration.MeshMark -> Icon(
                    imageVector = LocalLinkIcon,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(64.dp)
                )
                WelcomeIllustration.Relay -> Icon(
                    imageVector = Icons.Outlined.Hub,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(58.dp)
                )
                WelcomeIllustration.Privacy -> Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(56.dp)
                )
            }
        }
    }
}

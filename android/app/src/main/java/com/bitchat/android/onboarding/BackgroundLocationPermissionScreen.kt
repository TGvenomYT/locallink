package com.bitchat.android.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Security
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bitchat.android.R

/**
 * Explanation screen shown before requesting background location permission.
 * Rebuilt on the shared onboarding kit.
 */
@Composable
fun BackgroundLocationPermissionScreen(
    modifier: Modifier,
    onContinue: () -> Unit,
    onRetry: () -> Unit,
    onSkip: () -> Unit
) {
    val explanation = stringResource(R.string.background_location_explanation)
    val settingsTip = stringResource(R.string.background_location_settings_tip)

    OnboardingScaffold(
        modifier = modifier,
        title = stringResource(R.string.background_location_required_title),
        illustration = {
            OnboardingHaloIcon(
                icon = Icons.Filled.LocationOn,
                contentDescription = stringResource(R.string.cd_location_services)
            )
        },
        description = "$explanation\n\n$settingsTip",
        content = {
            OnboardingInfoCard(
                icon = Icons.Filled.LocationOn,
                title = stringResource(R.string.background_location_needs_for),
                body = stringResource(R.string.background_location_needs_bullets)
            )
            OnboardingInfoCard(
                icon = Icons.Filled.Security,
                title = stringResource(R.string.cd_privacy_protected),
                body = stringResource(R.string.background_location_privacy_note)
            )
        },
        actions = {
            OnboardingPrimaryButton(
                text = stringResource(R.string.grant_background_location),
                onClick = onContinue
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OnboardingOutlinedButton(
                    text = stringResource(R.string.check_again),
                    onClick = onRetry,
                    modifier = Modifier.weight(1f)
                )
                OnboardingTextButton(
                    text = stringResource(R.string.battery_optimization_skip),
                    onClick = onSkip,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    )
}

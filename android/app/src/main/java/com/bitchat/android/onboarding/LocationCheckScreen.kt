package com.bitchat.android.onboarding

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.bitchat.android.R

/**
 * Screen shown when checking location services status or requesting the user to enable them.
 * Rebuilt on the shared onboarding kit for a consistent, modern look.
 */
@Composable
fun LocationCheckScreen(
    modifier: Modifier,
    status: LocationStatus,
    onEnableLocation: () -> Unit,
    onRetry: () -> Unit,
    isLoading: Boolean = false
) {
    val colorScheme = MaterialTheme.colorScheme

    when (status) {
        LocationStatus.DISABLED -> {
            OnboardingScaffold(
                modifier = modifier,
                title = stringResource(R.string.location_services_required),
                illustration = {
                    OnboardingHaloIcon(
                        icon = Icons.Outlined.LocationOn,
                        contentDescription = stringResource(R.string.cd_location_services)
                    )
                },
                content = {
                    OnboardingInfoCard(
                        icon = Icons.Filled.Security,
                        title = stringResource(R.string.privacy_first),
                        body = stringResource(R.string.location_explanation)
                    )
                    OnboardingInfoCard(
                        icon = Icons.Outlined.LocationOn,
                        title = stringResource(R.string.location_needs_for),
                        body = stringResource(R.string.location_needs_bullets)
                    )
                },
                actions = {
                    OnboardingPrimaryButton(
                        text = stringResource(R.string.open_location_settings),
                        onClick = onEnableLocation,
                        loading = isLoading
                    )
                    if (!isLoading) {
                        OnboardingOutlinedButton(
                            text = stringResource(R.string.check_again),
                            onClick = onRetry
                        )
                    }
                }
            )
        }

        LocationStatus.NOT_AVAILABLE -> {
            OnboardingScaffold(
                modifier = modifier,
                title = stringResource(R.string.location_services_unavailable),
                titleColor = colorScheme.error,
                illustration = {
                    OnboardingHaloIcon(
                        icon = Icons.Filled.ErrorOutline,
                        contentDescription = stringResource(R.string.cd_error),
                        accent = colorScheme.error,
                        spinning = false
                    )
                },
                description = stringResource(R.string.location_unavailable_explanation)
            )
        }

        LocationStatus.ENABLED -> {
            OnboardingScaffold(
                modifier = modifier,
                title = stringResource(R.string.app_name),
                illustration = { OnboardingHaloBrand(loading = true) },
                description = stringResource(R.string.checking_location_services)
            )
        }
    }
}

package com.bitchat.android.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Power
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bitchat.android.R

/**
 * Screen shown when checking battery-optimization status or asking the user to disable it.
 * Rebuilt on the shared onboarding kit.
 */
@Composable
fun BatteryOptimizationScreen(
    modifier: Modifier,
    status: BatteryOptimizationStatus,
    onDisableBatteryOptimization: () -> Unit,
    onRetry: () -> Unit,
    onSkip: () -> Unit,
    isLoading: Boolean = false
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        BatteryOptimizationPreferenceManager.init(context)
    }

    when (status) {
        BatteryOptimizationStatus.ENABLED -> {
            OnboardingScaffold(
                modifier = modifier,
                title = stringResource(R.string.battery_optimization_detected_title),
                illustration = {
                    OnboardingHaloIcon(
                        icon = Icons.Filled.Power,
                        contentDescription = stringResource(R.string.cd_battery_optimization)
                    )
                },
                content = {
                    OnboardingInfoCard(
                        icon = Icons.Filled.Power,
                        title = stringResource(R.string.battery_optimization_enabled_title),
                        body = stringResource(R.string.battery_optimization_explanation_short)
                    )
                    OnboardingInfoCard(
                        icon = Icons.Filled.CheckCircle,
                        title = stringResource(R.string.benefits_of_disabling),
                        body = stringResource(R.string.battery_benefits_short)
                    )
                },
                actions = {
                    OnboardingPrimaryButton(
                        text = stringResource(R.string.disable_battery_optimization),
                        onClick = onDisableBatteryOptimization,
                        loading = isLoading
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OnboardingOutlinedButton(
                            text = stringResource(R.string.check_again),
                            onClick = onRetry,
                            modifier = Modifier.weight(1f),
                            enabled = !isLoading
                        )
                        OnboardingTextButton(
                            text = stringResource(R.string.battery_optimization_skip),
                            onClick = {
                                BatteryOptimizationPreferenceManager.setSkipped(context, true)
                                onSkip()
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isLoading
                        )
                    }
                }
            )
        }

        BatteryOptimizationStatus.DISABLED -> {
            OnboardingScaffold(
                modifier = modifier,
                title = stringResource(R.string.app_name),
                illustration = { OnboardingHaloBrand(loading = true) },
                description = stringResource(R.string.battery_optimization_success_message)
            )
        }

        BatteryOptimizationStatus.NOT_SUPPORTED -> {
            OnboardingScaffold(
                modifier = modifier,
                title = stringResource(R.string.battery_optimization_not_required),
                illustration = {
                    OnboardingHaloIcon(
                        icon = Icons.Filled.CheckCircle,
                        contentDescription = stringResource(R.string.cd_not_supported_battery_optimization),
                        spinning = false
                    )
                },
                description = stringResource(R.string.battery_optimization_not_supported_message),
                actions = {
                    OnboardingPrimaryButton(
                        text = stringResource(R.string.continue_btn),
                        onClick = onRetry
                    )
                }
            )
        }
    }
}

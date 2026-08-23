package com.bitchat.android.onboarding

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.bitchat.android.R

/**
 * Screen shown when checking Bluetooth status or requesting Bluetooth enable.
 * Uses the shared onboarding kit so it matches the welcome carousel and the other check screens.
 */
@Composable
fun BluetoothCheckScreen(
    modifier: Modifier,
    status: BluetoothStatus,
    onEnableBluetooth: () -> Unit,
    onRetry: () -> Unit,
    onSkip: () -> Unit,
    isLoading: Boolean = false
) {
    val colorScheme = MaterialTheme.colorScheme

    when (status) {
        BluetoothStatus.DISABLED -> {
            OnboardingScaffold(
                modifier = modifier,
                title = stringResource(R.string.bluetooth_recommended),
                illustration = {
                    OnboardingHaloIcon(
                        icon = Icons.Outlined.Bluetooth,
                        contentDescription = stringResource(R.string.cd_bluetooth)
                    )
                },
                content = {
                    OnboardingInfoCard(
                        icon = Icons.Outlined.Bluetooth,
                        title = stringResource(R.string.bluetooth_needs_for),
                        body = stringResource(R.string.bluetooth_needs_bullets)
                    )
                },
                actions = {
                    OnboardingPrimaryButton(
                        text = stringResource(R.string.enable_bluetooth),
                        onClick = onEnableBluetooth,
                        loading = isLoading
                    )
                    if (!isLoading) {
                        OnboardingTextButton(
                            text = stringResource(R.string.skip),
                            onClick = onSkip
                        )
                    }
                }
            )
        }

        BluetoothStatus.NOT_SUPPORTED -> {
            OnboardingScaffold(
                modifier = modifier,
                title = stringResource(R.string.bluetooth_not_supported),
                titleColor = colorScheme.error,
                illustration = {
                    OnboardingHaloIcon(
                        icon = Icons.Filled.ErrorOutline,
                        contentDescription = stringResource(R.string.cd_bluetooth),
                        accent = colorScheme.error,
                        spinning = false
                    )
                },
                description = stringResource(R.string.bluetooth_unsupported_explanation),
                actions = {
                    OnboardingPrimaryButton(
                        text = stringResource(R.string.continue_btn),
                        onClick = onSkip
                    )
                }
            )
        }

        BluetoothStatus.ENABLED -> {
            OnboardingScaffold(
                modifier = modifier,
                title = stringResource(R.string.app_name),
                illustration = { OnboardingHaloBrand(loading = true) },
                description = stringResource(R.string.checking_bluetooth_status)
            )
        }
    }
}

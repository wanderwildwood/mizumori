package com.wanderwildwood.mizumori.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.lazy.LazyColumnMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.top_app_bar.TopAppBarMMD
import com.wanderwildwood.mizumori.level.LevelState
import kotlinx.coroutines.delay

/**
 * Four things to set and one to undo.
 *
 * Upstream also offers a bubble "viscosity" — how sluggishly the bubble follows the
 * phone. That is a setting about an animation, and there is no animation here: on E Ink
 * a bubble that slides is a bubble smearing across the panel, so it steps to where it
 * belongs and stops. The setting went with it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: LevelState,
    onClose: () -> Unit,
    onDisplay: () -> Unit,
    onLock: () -> Unit,
    onSound: () -> Unit,
    onCalibrate: () -> Unit,
    onResetCalibration: () -> Unit,
) {
    var aboutOpen by remember { mutableStateOf(false) }
    var resetArmed by remember { mutableStateOf(false) }

    // Armed rows disarm themselves, so a stray tap leaves nothing live for whoever picks
    // the phone up next.
    LaunchedEffect(resetArmed) {
        if (resetArmed) {
            delay(4000)
            resetArmed = false
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBarMMD(
                title = { TextMMD(text = "Settings") },
                navigationIcon = { BarButton(Icons.Close, "Close", onClose) },
                actions = { BarButton(Icons.Info, "About", { aboutOpen = true }) },
            )
        },
    ) { contentPadding ->
        // MMD's list, not a scrolling Column: it steps four rows to a swipe and stops, and it
        // brings the chevron rail at both ends. A settings screen that coasts was the one
        // screen in the app that did not behave like the phone it is on.
        LazyColumnMMD(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 20.dp),
        ) {
            item {
                Spacer(Modifier.height(12.dp))
            }
            item {
                Setting(title = "Read as", value = state.display.label, onClick = onDisplay)
            }
            item {
                Setting(
                    title = "Lock orientation",
                    value = if (state.lockOrientation) "On" else "Off",
                    // No label can carry this one: what it locks is which way of holding the
                    // phone is being measured, not the screen rotation everyone else means.
                    note = "Keeps measuring the way it is being held now, even if it is turned.",
                    onClick = onLock,
                )
            }
            item {
                Setting(
                    title = "Sound when level",
                    value = if (state.soundWhenLevel) "On" else "Off",
                    note = "For a surface you cannot see the screen from.",
                    onClick = onSound,
                )
            }
            item {
                HorizontalDividerMMD()
            }
            item {
                Setting(
                    title = "Calibrate",
                    value = if (state.isCalibrated) "Set" else "Not set",
                    note = "Rest the phone on something you trust to be level, then press. " +
                        "Each way of holding it is calibrated on its own.",
                    onClick = onCalibrate,
                )
            }
            item {
                // Destructive last, after everything that only adjusts — and it asks in its
                // own face rather than in a dialog. A dialog is two full-panel repaints to ask
                // one question; the row is one, and it asks where the answer belongs.
                if (state.isCalibrated) {
                    Setting(
                        title = if (resetArmed) {
                            "Forget calibration — tap again"
                        } else {
                            "Forget calibration"
                        },
                        value = if (resetArmed) {
                            "Every orientation goes back to the factory reading, and it has " +
                                "to be done again with a surface you trust."
                        } else {
                            "All five orientations"
                        },
                        onClick = {
                            if (resetArmed) {
                                onResetCalibration()
                                resetArmed = false
                            } else {
                                resetArmed = true
                            }
                        },
                    )
                }
            }
            item {
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (aboutOpen) AboutDialog(onDismiss = { aboutOpen = false })
}

@Composable
private fun Setting(
    title: String,
    value: String,
    note: String? = null,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
    ) {
        TextMMD(text = title, style = MaterialTheme.typography.bodyMedium)
        TextMMD(text = value, style = MaterialTheme.typography.labelSmall)
        if (note != null) {
            Spacer(Modifier.height(2.dp))
            TextMMD(text = note, style = MaterialTheme.typography.labelSmall)
        }
    }
}

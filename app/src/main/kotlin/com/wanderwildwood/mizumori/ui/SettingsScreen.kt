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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.lazy.LazyColumnMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.top_app_bar.TopAppBarMMD
import com.wanderwildwood.mizumori.level.LevelState

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
    var resetOpen by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBarMMD(
                title = { TextMMD(text = "Settings", fontSize = 24.sp) },
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
                // Destructive last, after everything that only adjusts.
                if (state.isCalibrated) {
                    Setting(
                        title = "Forget calibration",
                        value = "All five orientations",
                        onClick = { resetOpen = true },
                    )
                }
            }
            item {
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (aboutOpen) AboutDialog(onDismiss = { aboutOpen = false })

    if (resetOpen) {
        EInkDialog(onDismiss = { resetOpen = false }) {
            TextMMD(text = "Forget calibration?", fontSize = 20.sp)
            Spacer(Modifier.height(10.dp))
            TextMMD(
                text = "Every orientation goes back to the factory reading. There is no " +
                    "undo, and it has to be done again with a surface you trust.",
                fontSize = 14.sp,
            )
            Spacer(Modifier.height(16.dp))
            OutlinedButtonMMD(
                onClick = {
                    onResetCalibration()
                    resetOpen = false
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) { TextMMD(text = "Forget it", fontSize = 15.sp) }
            Spacer(Modifier.height(8.dp))
            OutlinedButtonMMD(
                onClick = { resetOpen = false },
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) { TextMMD(text = "Keep it", fontSize = 15.sp) }
        }
    }
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
        TextMMD(text = title, fontSize = 18.sp)
        TextMMD(text = value, fontSize = 14.sp)
        if (note != null) {
            Spacer(Modifier.height(2.dp))
            TextMMD(text = note, fontSize = 13.sp)
        }
    }
}

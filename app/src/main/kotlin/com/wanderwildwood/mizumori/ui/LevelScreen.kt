package com.wanderwildwood.mizumori.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.top_app_bar.TopAppBarMMD
import com.wanderwildwood.mizumori.R
import com.wanderwildwood.mizumori.core.Angles
import com.wanderwildwood.mizumori.core.ScreenFrame
import com.wanderwildwood.mizumori.level.LevelState

/**
 * The whole instrument: a vial, the angle in words, and a way into settings.
 *
 * Which vial is decided by how the phone is being held, not by a mode the reader has to
 * choose. Lying on its back it is a bullseye with two axes; stood on an edge it is a
 * tube with one. A level you have to tell which kind of level it is being is a level
 * that has to be operated rather than used.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevelScreen(
    state: LevelState,
    rotation: Int,
    onSettings: () -> Unit,
    onToggleDisplay: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBarMMD(
                title = { TextMMD(text = stringResource(R.string.app_name)) },
                actions = { BarButton(Icons.Settings, stringResource(R.string.level_cd_settings), onSettings) },
            )
        },
    ) { contentPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 20.dp),
        ) {
            val reading = state.reading
            val availableHeight = maxHeight
            val landscape = maxWidth > availableHeight

            if (!state.hasSensor) {
                NoSensor()
            } else if (reading == null) {
                Column(Modifier.fillMaxWidth().padding(top = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    TextMMD(text = stringResource(R.string.level_finding), style = MaterialTheme.typography.titleSmall)
                }
            } else if (landscape) {
                // Turned on its side there is room beside the vial rather than under it, and
                // not enough under it for both.
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxHeight().padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) { Vial(state, rotation, Modifier.fillMaxWidth().height(availableHeight - 16.dp)) }
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) { Readout(state, onToggleDisplay) }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(20.dp))
                    Vial(state, rotation, Modifier.fillMaxWidth().height(if (reading.orientation.isFlat) 260.dp else 120.dp))
                    Spacer(Modifier.height(20.dp))
                    Readout(state, onToggleDisplay)
                }
            }
        }
    }
}

/** The vial for however the phone is lying, drawn for however the screen is turned. */
@Composable
private fun Vial(state: LevelState, rotation: Int, modifier: Modifier) {
    val reading = state.reading ?: return
    if (reading.orientation.isFlat) {
        val (right, down) = ScreenFrame.flat(reading, rotation)
        FlatVial(rightDegrees = right, downDegrees = down, isLevel = state.isLevel, modifier = modifier)
    } else {
        EdgeVial(degrees = ScreenFrame.edge(reading, rotation), isLevel = state.isLevel, modifier = modifier)
    }
}

/** The angle in words and a press on it changes what the number means. */
@Composable
private fun Readout(state: LevelState, onToggleDisplay: () -> Unit) {
    val reading = state.reading ?: return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleDisplay)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (reading.orientation.isFlat) {
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Axis(stringResource(R.string.level_axis_across), reading.acrossAngle, state)
                Axis(stringResource(R.string.level_axis_along), reading.alongAngle, state)
            }
        } else {
            TextMMD(
                text = Angles.format(reading.angle, state.display),
                fontSize = 56.sp,
                fontWeight = FontWeight.Medium,
            )
        }

        Spacer(Modifier.height(10.dp))

        // One word, and only when it is true. A label that reads "not level" the
        // rest of the time would be on screen permanently, saying nothing the
        // bubble has not already said.
        TextMMD(
            text = if (state.isLevel) stringResource(R.string.level_is_level) else " ",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
        )

        if (state.lockOrientation) {
            Spacer(Modifier.height(4.dp))
            TextMMD(text = stringResource(R.string.level_orientation_locked), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun Axis(label: String, degrees: Float, state: LevelState) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        TextMMD(
            text = Angles.format(degrees, state.display),
            fontSize = 34.sp,
            fontWeight = FontWeight.Medium,
        )
        TextMMD(text = label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun NoSensor() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TextMMD(
            text = stringResource(R.string.level_no_sensor),
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
internal fun BarButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier.size(48.dp).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(22.dp),
        )
    }
}

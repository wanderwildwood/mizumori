package com.wanderwildwood.mizumori.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.top_app_bar.TopAppBarMMD
import com.wanderwildwood.mizumori.core.Angles
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
    onSettings: () -> Unit,
    onToggleDisplay: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBarMMD(
                title = { TextMMD(text = "Level", fontSize = 24.sp) },
                actions = { BarButton(Icons.Settings, "Settings", onSettings) },
            )
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (!state.hasSensor) {
                NoSensor()
                return@Column
            }

            val reading = state.reading

            Spacer(Modifier.height(20.dp))

            if (reading == null) {
                TextMMD(text = "Finding level…", fontSize = 16.sp)
                return@Column
            }

            if (reading.orientation.isFlat) {
                FlatVial(
                    acrossDegrees = reading.acrossAngle,
                    alongDegrees = reading.alongAngle,
                    isLevel = state.isLevel,
                )
            } else {
                EdgeVial(degrees = reading.angle, isLevel = state.isLevel)
            }

            Spacer(Modifier.height(20.dp))

            // The number, and a press on it changes what the number means. It is the only
            // thing on this screen worth pressing, so it does not need to say so.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleDisplay)
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (reading.orientation.isFlat) {
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        Axis("across", reading.acrossAngle, state)
                        Axis("along", reading.alongAngle, state)
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
                    text = if (state.isLevel) "Level" else " ",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                )
            }

            if (state.lockOrientation) {
                Spacer(Modifier.height(4.dp))
                TextMMD(text = "Orientation locked", fontSize = 13.sp)
            }
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
        TextMMD(text = label, fontSize = 13.sp)
    }
}

@Composable
private fun NoSensor() {
    Column(
        modifier = Modifier.fillMaxSize().padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TextMMD(
            text = "This phone has no accelerometer, so it cannot be a level.",
            fontSize = 16.sp,
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

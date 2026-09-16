package com.wanderwildwood.mizumori

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mudita.mmd.ThemeMMD
import com.wanderwildwood.mizumori.level.LevelViewModel
import com.wanderwildwood.mizumori.ui.LevelScreen
import com.wanderwildwood.mizumori.ui.SettingsScreen
import com.wanderwildwood.mizumori.ui.monochrome

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // The screen stays on while this app is in front.
        //
        // A level is propped against the thing being levelled and read in glances with
        // both hands busy, which is the one situation where the ordinary screen timeout
        // is not a sensible default but a fault. Reaching over to wake the phone moves
        // the phone, and moving the phone is the one thing that ruins the reading.
        //
        // This is the window flag, not a WAKE_LOCK: it needs no permission, and Android
        // drops it by itself the moment the window loses focus, so it cannot be left on
        // by accident or outlive the app.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            ThemeMMD(colorScheme = monochrome) {
                Level()
            }
        }
    }
}

@Composable
private fun Level(viewModel: LevelViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var settingsOpen by remember { mutableStateOf(false) }

    // The accelerometer runs only while this app is in front. A level has no reason
    // whatsoever to be reading the sensors from the background.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.start(displayRotation(context))
                Lifecycle.Event.ON_PAUSE -> viewModel.stop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // A short tone the moment it comes level, and nothing while it stays level. Keyed on
    // the boolean so it fires on the edge rather than on every reading, which would be a
    // continuous beep for as long as the phone sat still.
    LaunchedEffect(state.isLevel, state.soundWhenLevel) {
        if (state.isLevel && state.soundWhenLevel) {
            runCatching {
                ToneGenerator(AudioManager.STREAM_NOTIFICATION, 70).apply {
                    startTone(ToneGenerator.TONE_PROP_BEEP, 120)
                    // Released on a delay rather than at once: releasing the generator
                    // while the tone is still sounding cuts it off silently on some
                    // devices, which looks exactly like the setting not working.
                    kotlinx.coroutines.delay(300)
                    release()
                }
            }
        }
    }

    if (settingsOpen) {
        SettingsScreen(
            state = state,
            onClose = { settingsOpen = false },
            onDisplay = viewModel::nextDisplay,
            onLock = viewModel::toggleLock,
            onSound = viewModel::toggleSound,
            onCalibrate = viewModel::calibrate,
            onResetCalibration = viewModel::resetCalibration,
        )
    } else {
        LevelScreen(
            state = state,
            onSettings = { settingsOpen = true },
            onToggleDisplay = viewModel::nextDisplay,
        )
    }
}

/**
 * Which way the display is turned, which the sensor maths has to undo.
 *
 * `Context.display` arrived in API 30 and `WindowManager.defaultDisplay` was deprecated
 * in the same release; this phone is API 31, so the new one is used and the old branch
 * is not carried for a platform this app does not support.
 */
private fun displayRotation(context: android.content.Context): Int =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        context.display?.rotation ?: android.view.Surface.ROTATION_0
    } else {
        android.view.Surface.ROTATION_0
    }

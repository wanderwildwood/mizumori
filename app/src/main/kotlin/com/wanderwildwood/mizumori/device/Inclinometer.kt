package com.wanderwildwood.mizumori.device

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import com.wanderwildwood.mizumori.core.Gravity
import com.wanderwildwood.mizumori.core.Reading
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** Whether this phone can be a level at all. */
fun hasAccelerometer(context: Context): Boolean {
    val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    return manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null
}

/**
 * The Kompakt's accelerometer is mounted turned: its x axis points out of the screen and its
 * z axis runs down the long edge. Android's own orientation code reads an upright Kompakt as
 * lying flat, so the readings are put back into the usual frame (x right, y up, z out of the
 * screen) before any angle is taken from them.
 */
private val MOUNTED_TURNED = Build.MANUFACTURER.equals("Mudita", ignoreCase = true)

internal fun phoneFrame(sx: Float, sy: Float, sz: Float, turned: Boolean = MOUNTED_TURNED): Triple<Float, Float, Float> =
    if (turned) Triple(-sy, -sz, sx) else Triple(sx, sy, sz)

/**
 * A stream of raw, uncalibrated readings from the accelerometer.
 *
 * A spirit level does not care which way north is, so only the gravity vector is used and
 * the angles come straight from it ([Gravity]). The readings are in the phone's own frame
 * whichever way the screen is turned; [com.wanderwildwood.mizumori.core.ScreenFrame] turns
 * them to the screen when they are drawn.
 */
fun readings(context: Context): Flow<Reading> = callbackFlow {
    val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    val sensor = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    if (sensor == null) {
        close()
        return@callbackFlow
    }

    val listener = object : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

        override fun onSensorChanged(event: SensorEvent?) {
            if (event == null) return
            val (x, y, z) = phoneFrame(event.values[0], event.values[1], event.values[2])
            Gravity.reading(x, y, z)?.let { trySend(it) }
        }
    }

    // SENSOR_DELAY_UI, not GAME or FASTEST. The reading is quantised to a tenth of a
    // degree before anything is drawn, so a faster stream would only produce more
    // samples that round to the number already on the screen.
    manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)

    awaitClose { manager.unregisterListener(listener) }
}

package com.wanderwildwood.mizumori.device

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import com.wanderwildwood.mizumori.core.Orientation
import com.wanderwildwood.mizumori.core.Reading
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.asin
import kotlin.math.sqrt

/** Whether this phone can be a level at all. */
fun hasAccelerometer(context: Context): Boolean {
    val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    return manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null
}

/**
 * A stream of raw, uncalibrated readings from the accelerometer.
 *
 * The maths is Antoine Vianey's, and the part worth keeping is why it works with no
 * compass: `getRotationMatrix` normally wants gravity *and* a magnetic field, but a
 * spirit level does not care which way north is. Passing a constant dummy field gives a
 * matrix whose orientation about the vertical is meaningless and whose tilt away from
 * vertical — the only thing being measured — is exact.
 *
 * [displayRotation] is needed because pitch and roll are reported against the device's
 * own axes. Without remapping, a phone turned on its side reads its roll as its pitch
 * and the level lies confidently.
 */
fun readings(context: Context, displayRotation: Int): Flow<Reading> = callbackFlow {
    val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    val sensor = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    if (sensor == null) {
        close()
        return@callbackFlow
    }

    val rotation = FloatArray(16)
    val inclination = FloatArray(16)
    val remapped = FloatArray(16)
    val values = FloatArray(3)
    // Any non-zero constant does; only the gravity vector carries information here.
    val dummyField = floatArrayOf(1f, 1f, 1f)

    val listener = object : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

        override fun onSensorChanged(event: SensorEvent?) {
            if (event == null) return
            if (!SensorManager.getRotationMatrix(rotation, inclination, event.values, dummyField)) {
                return
            }

            when (displayRotation) {
                Surface.ROTATION_270 -> SensorManager.remapCoordinateSystem(
                    rotation, SensorManager.AXIS_MINUS_Y, SensorManager.AXIS_X, remapped
                )

                Surface.ROTATION_180 -> SensorManager.remapCoordinateSystem(
                    rotation, SensorManager.AXIS_MINUS_X, SensorManager.AXIS_MINUS_Y, remapped
                )

                Surface.ROTATION_90 -> SensorManager.remapCoordinateSystem(
                    rotation, SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X, remapped
                )

                else -> SensorManager.remapCoordinateSystem(
                    rotation, SensorManager.AXIS_X, SensorManager.AXIS_Y, remapped
                )
            }

            SensorManager.getOrientation(remapped, values)

            // Balance is the tilt of the phone's own vertical axis, normalised against
            // the horizontal plane. It is what a phone standing on edge measures.
            val magnitude = sqrt(
                remapped[8] * remapped[8] + remapped[9] * remapped[9]
            )
            val normalised = if (magnitude == 0f) 0f else remapped[8] / magnitude

            val pitch = Math.toDegrees(values[1].toDouble()).toFloat()
            val roll = -Math.toDegrees(values[2].toDouble()).toFloat()
            val balance = Math.toDegrees(asin(normalised.toDouble())).toFloat()

            trySend(Reading(Orientation.of(pitch, roll), pitch, roll, balance))
        }
    }

    // SENSOR_DELAY_UI, not GAME or FASTEST. The reading is quantised to a tenth of a
    // degree before anything is drawn, so a faster stream would only produce more
    // samples that round to the number already on the screen.
    manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)

    awaitClose { manager.unregisterListener(listener) }
}

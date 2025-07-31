package com.astralplayer.nextplayer.immersive

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

/**
 * Gyroscope Controller for device orientation tracking
 */
@Singleton
class GyroscopeController @Inject constructor(
    private val context: Context
) {
    
    private var sensorManager: SensorManager? = null
    private var gyroscope: Sensor? = null
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null
    
    private var headTrackingCallback: ((Quaternion) -> Unit)? = null
    private var sphericalNavigationCallback: ((Quaternion) -> Unit)? = null
    private var immersiveGestureCallback: ((GestureType, Float) -> Unit)? = null
    
    private val sensorEventListener = object : SensorEventListener {
        private val rotationMatrix = FloatArray(9)
        private val orientationAngles = FloatArray(3)
        private val quaternion = FloatArray(4)
        
        override fun onSensorChanged(event: SensorEvent) {
            when (event.sensor.type) {
                Sensor.TYPE_GYROSCOPE -> handleGyroscopeData(event)
                Sensor.TYPE_ACCELEROMETER -> handleAccelerometerData(event)
                Sensor.TYPE_ROTATION_VECTOR -> handleRotationVector(event)
            }
        }
        
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            // Handle accuracy changes if needed
        }
        
        private fun handleGyroscopeData(event: SensorEvent) {
            // Process gyroscope data for smooth rotation tracking
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            
            // Convert to quaternion
            val quaternion = calculateQuaternionFromGyro(x, y, z)
            
            // Notify appropriate callback
            headTrackingCallback?.invoke(quaternion)
            sphericalNavigationCallback?.invoke(quaternion)
        }
        
        private fun handleAccelerometerData(event: SensorEvent) {
            // Use for gravity compensation
        }
        
        private fun handleRotationVector(event: SensorEvent) {
            // Direct rotation vector to quaternion conversion
            SensorManager.getQuaternionFromVector(quaternion, event.values)
            
            val q = Quaternion(
                x = quaternion[1],
                y = quaternion[2],
                z = quaternion[3],
                w = quaternion[0]
            )
            
            // Send to appropriate callbacks
            headTrackingCallback?.invoke(q)
            sphericalNavigationCallback?.invoke(q)
        }
    }
    
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            gyroscope = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
            accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            magnetometer = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
            
            Log.i("GyroscopeController", "Gyroscope controller initialized")
            true
        } catch (e: Exception) {
            Log.e("GyroscopeController", "Failed to initialize gyroscope controller", e)
            false
        }
    }
    
    fun enableHeadTracking(callback: (Quaternion) -> Unit) {
        headTrackingCallback = callback
        registerSensors()
    }
    
    fun enableCardboardTracking() {
        // Use rotation vector for better Cardboard experience
        val rotationVector = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        rotationVector?.let {
            sensorManager?.registerListener(
                sensorEventListener,
                it,
                SensorManager.SENSOR_DELAY_GAME
            )
        }
    }
    
    fun enableSphericalNavigation(callback: (Quaternion) -> Unit) {
        sphericalNavigationCallback = callback
        registerSensors()
    }
    
    fun enableImmersiveGestures() {
        // Enable gesture detection based on device movement
        registerSensors()
    }
    
    fun disableHeadTracking() {
        headTrackingCallback = null
        unregisterSensors()
    }
    
    fun disableSphericalNavigation() {
        sphericalNavigationCallback = null
        unregisterSensors()
    }
    
    fun disableImmersiveGestures() {
        immersiveGestureCallback = null
        unregisterSensors()
    }
    
    private fun registerSensors() {
        val rotationVector = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        
        rotationVector?.let {
            sensorManager?.registerListener(
                sensorEventListener,
                it,
                SensorManager.SENSOR_DELAY_GAME
            )
        } ?: run {
            // Fallback to gyroscope + accelerometer
            gyroscope?.let {
                sensorManager?.registerListener(
                    sensorEventListener,
                    it,
                    SensorManager.SENSOR_DELAY_GAME
                )
            }
            
            accelerometer?.let {
                sensorManager?.registerListener(
                    sensorEventListener,
                    it,
                    SensorManager.SENSOR_DELAY_GAME
                )
            }
        }
    }
    
    private fun unregisterSensors() {
        if (headTrackingCallback == null && 
            sphericalNavigationCallback == null && 
            immersiveGestureCallback == null) {
            sensorManager?.unregisterListener(sensorEventListener)
        }
    }
    
    private fun calculateQuaternionFromGyro(x: Float, y: Float, z: Float): Quaternion {
        // Simplified quaternion calculation from gyroscope data
        val magnitude = sqrt(x * x + y * y + z * z)
        if (magnitude > 0) {
            val normalized = floatArrayOf(x / magnitude, y / magnitude, z / magnitude)
            val angle = magnitude * 0.5f
            val sinHalfAngle = sin(angle)
            val cosHalfAngle = cos(angle)
            
            return Quaternion(
                x = normalized[0] * sinHalfAngle,
                y = normalized[1] * sinHalfAngle,
                z = normalized[2] * sinHalfAngle,
                w = cosHalfAngle
            )
        }
        return Quaternion.identity()
    }
    
    fun isGyroscopeAvailable(): Boolean {
        return sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null ||
               sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null
    }
    
    fun cleanup() {
        sensorManager?.unregisterListener(sensorEventListener)
        headTrackingCallback = null
        sphericalNavigationCallback = null
        immersiveGestureCallback = null
    }
}

/**
 * Gesture types for immersive interactions
 */
enum class GestureType {
    SWIPE_UP,
    SWIPE_DOWN,
    SWIPE_LEFT,
    SWIPE_RIGHT,
    PINCH_ZOOM,
    ROTATE,
    DOUBLE_TAP,
    LONG_PRESS
}
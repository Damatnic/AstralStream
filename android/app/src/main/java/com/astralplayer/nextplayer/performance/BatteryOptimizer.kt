package com.astralplayer.nextplayer.performance

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BatteryOptimizer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val _powerMode = MutableStateFlow(PowerMode.BALANCED)
    val powerMode: StateFlow<PowerMode> = _powerMode.asStateFlow()
    
    private var batteryReceiver: BroadcastReceiver? = null
    private var powerModeCallback: ((PowerMode) -> Unit)? = null
    
    enum class PowerMode {
        POWER_SAVER,
        BALANCED,
        PERFORMANCE
    }
    
    init {
        registerBatteryReceiver()
        updatePowerMode()
    }
    
    fun registerPowerModeCallback(callback: (PowerMode) -> Unit) {
        powerModeCallback = callback
    }
    
    private fun registerBatteryReceiver() {
        batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_BATTERY_CHANGED -> {
                        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                        val batteryPct = level * 100 / scale.toFloat()
                        
                        updatePowerModeBasedOnBattery(batteryPct)
                    }
                    PowerManager.ACTION_POWER_SAVE_MODE_CHANGED -> {
                        updatePowerMode()
                    }
                }
            }
        }
        
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        }
        
        context.registerReceiver(batteryReceiver, filter)
    }
    
    private fun updatePowerModeBasedOnBattery(batteryLevel: Float) {
        val newMode = when {
            batteryLevel < 15f || powerManager.isPowerSaveMode -> PowerMode.POWER_SAVER
            batteryLevel < 30f -> PowerMode.BALANCED
            else -> PowerMode.PERFORMANCE
        }
        
        if (newMode != _powerMode.value) {
            _powerMode.value = newMode
            powerModeCallback?.invoke(newMode)
            Log.d("BatteryOptimizer", "Power mode changed to: $newMode (Battery: ${batteryLevel}%)")
        }
    }
    
    private fun updatePowerMode() {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        batteryIntent?.let { intent ->
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (level != -1 && scale != -1) {
                val batteryPct = level * 100 / scale.toFloat()
                updatePowerModeBasedOnBattery(batteryPct)
            }
        }
    }
    
    fun getOptimalVideoQuality(): VideoQuality {
        return when (_powerMode.value) {
            PowerMode.POWER_SAVER -> VideoQuality.LOW
            PowerMode.BALANCED -> VideoQuality.MEDIUM
            PowerMode.PERFORMANCE -> VideoQuality.HIGH
        }
    }
    
    fun getOptimalFrameRate(): Int {
        return when (_powerMode.value) {
            PowerMode.POWER_SAVER -> 24
            PowerMode.BALANCED -> 30
            PowerMode.PERFORMANCE -> 60
        }
    }
    
    fun shouldUseHardwareAcceleration(): Boolean {
        return _powerMode.value != PowerMode.POWER_SAVER
    }
    
    fun getOptimalBufferSize(): Int {
        return when (_powerMode.value) {
            PowerMode.POWER_SAVER -> 1024 * 1024 // 1MB
            PowerMode.BALANCED -> 2 * 1024 * 1024 // 2MB
            PowerMode.PERFORMANCE -> 4 * 1024 * 1024 // 4MB
        }
    }
    
    fun shouldEnableBackgroundProcessing(): Boolean {
        return _powerMode.value == PowerMode.PERFORMANCE
    }
    
    fun getRecommendedCacheSize(): Long {
        return when (_powerMode.value) {
            PowerMode.POWER_SAVER -> 50 * 1024 * 1024L // 50MB
            PowerMode.BALANCED -> 100 * 1024 * 1024L // 100MB
            PowerMode.PERFORMANCE -> 200 * 1024 * 1024L // 200MB
        }
    }
    
    fun onDestroy() {
        batteryReceiver?.let { receiver ->
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {
                Log.e("BatteryOptimizer", "Failed to unregister battery receiver", e)
            }
        }
    }
    
    enum class VideoQuality {
        LOW, MEDIUM, HIGH
    }
}
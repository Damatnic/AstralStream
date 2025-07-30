package com.astralplayer.nextplayer.feature.player.ui

/**
 * Performance Metrics Data Class
 * Shared across all performance monitoring components
 */
data class PerformanceMetrics(
    val fps: Float = 60f,
    val memoryUsage: Long = 0L, // MB
    val cpuUsage: Float = 0f, // Percentage
    val gpuUsage: Float = 0f, // Percentage
    val batteryLevel: Float = 100f, // Percentage
    val temperature: Float = 25f, // Celsius
    val timestamp: Long = System.currentTimeMillis()
) 
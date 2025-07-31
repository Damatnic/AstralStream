package com.astralplayer.nextplayer.di

import android.content.Context
import com.astralplayer.nextplayer.performance.BatteryOptimizer
import com.astralplayer.nextplayer.performance.PerformanceMonitor
import com.astralplayer.nextplayer.performance.StartupOptimizer
import com.astralplayer.nextplayer.performance.VideoCache
import com.astralplayer.nextplayer.security.AppLockManager
import com.astralplayer.nextplayer.security.BiometricManager
import com.astralplayer.nextplayer.security.EncryptionEngine
import com.astralplayer.nextplayer.security.HiddenFolderManager
import com.astralplayer.nextplayer.security.SecurePreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object Week1Module {
    
    // Security & Privacy Module
    @Provides
    @Singleton
    fun provideBiometricManager(
        @ApplicationContext context: Context
    ): BiometricManager {
        return BiometricManager(context)
    }
    
    @Provides
    @Singleton
    fun provideEncryptionEngine(
        @ApplicationContext context: Context
    ): EncryptionEngine {
        return EncryptionEngine(context)
    }
    
    @Provides
    @Singleton
    fun provideSecurePreferences(
        @ApplicationContext context: Context,
        encryptionEngine: EncryptionEngine
    ): SecurePreferences {
        return SecurePreferences(context, encryptionEngine)
    }
    
    @Provides
    @Singleton
    fun provideHiddenFolderManager(
        @ApplicationContext context: Context,
        encryptionEngine: EncryptionEngine,
        securePreferences: SecurePreferences
    ): HiddenFolderManager {
        return HiddenFolderManager(context, encryptionEngine, securePreferences)
    }
    
    @Provides
    @Singleton
    fun provideAppLockManager(
        @ApplicationContext context: Context,
        securePreferences: SecurePreferences
    ): AppLockManager {
        return AppLockManager(context, securePreferences)
    }
    
    // Performance Module
    @Provides
    @Singleton
    fun providePerformanceMonitor(
        @ApplicationContext context: Context
    ): PerformanceMonitor {
        return PerformanceMonitor(context)
    }
    
    @Provides
    @Singleton
    fun provideStartupOptimizer(
        @ApplicationContext context: Context,
        performanceMonitor: PerformanceMonitor
    ): StartupOptimizer {
        return StartupOptimizer(context, performanceMonitor)
    }
    
    @Provides
    @Singleton
    fun provideVideoCache(
        @ApplicationContext context: Context
    ): VideoCache {
        return VideoCache(context)
    }
    
    @Provides
    @Singleton
    fun provideBatteryOptimizer(
        @ApplicationContext context: Context
    ): BatteryOptimizer {
        return BatteryOptimizer(context)
    }
}
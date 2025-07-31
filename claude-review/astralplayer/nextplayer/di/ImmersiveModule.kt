package com.astralplayer.nextplayer.di

import android.content.Context
import com.astralplayer.nextplayer.immersive.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing immersive media dependencies (VR/AR/360°)
 */
@Module
@InstallIn(SingletonComponent::class)
object ImmersiveModule {
    
    @Provides
    @Singleton
    fun provideImmersiveMediaEngine(
        @ApplicationContext context: Context,
        vrRenderer: VRRenderer,
        arOverlayManager: AROverlayManager,
        sphericalVideoProcessor: SphericalVideoProcessor,
        gyroscopeController: GyroscopeController,
        immersiveUIManager: ImmersiveUIManager
    ): ImmersiveMediaEngine {
        return ImmersiveMediaEngine(
            context = context,
            vrRenderer = vrRenderer,
            arOverlayManager = arOverlayManager,
            sphericalVideoProcessor = sphericalVideoProcessor,
            gyroscopeController = gyroscopeController,
            immersiveUIManager = immersiveUIManager
        )
    }
    
    @Provides
    @Singleton
    fun provideVRRenderer(
        @ApplicationContext context: Context
    ): VRRenderer {
        return VRRenderer(context)
    }
    
    @Provides
    @Singleton
    fun provideAROverlayManager(
        @ApplicationContext context: Context
    ): AROverlayManager {
        return AROverlayManager(context)
    }
    
    @Provides
    @Singleton
    fun provideSphericalVideoProcessor(
        @ApplicationContext context: Context
    ): SphericalVideoProcessor {
        return SphericalVideoProcessor(context)
    }
    
    @Provides
    @Singleton
    fun provideGyroscopeController(
        @ApplicationContext context: Context
    ): GyroscopeController {
        return GyroscopeController(context)
    }
    
    @Provides
    @Singleton
    fun provideImmersiveUIManager(
        @ApplicationContext context: Context
    ): ImmersiveUIManager {
        return ImmersiveUIManager(context)
    }
}
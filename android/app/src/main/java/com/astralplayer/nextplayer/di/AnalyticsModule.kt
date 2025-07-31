package com.astralplayer.nextplayer.di

import android.content.Context
import com.astralplayer.nextplayer.analytics.AnalyticsDashboardEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing analytics dashboard dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object AnalyticsModule {
    
    @Provides
    @Singleton
    fun provideAnalyticsDashboardEngine(
        @ApplicationContext context: Context
    ): AnalyticsDashboardEngine {
        return AnalyticsDashboardEngine(context)
    }
}
package com.astralplayer.nextplayer.professional

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for professional video tools dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object ProfessionalToolsModule {
    
    @Provides
    @Singleton
    fun provideVideoFrameExtractor(): VideoFrameExtractor {
        return VideoFrameExtractor()
    }
    
    @Provides
    @Singleton
    fun provideMeasurementCalculator(): MeasurementCalculator {
        return MeasurementCalculator()
    }
    
    @Provides
    @Singleton
    fun provideAnnotationManager(): AnnotationManager {
        return AnnotationManager()
    }
    
    @Provides
    @Singleton
    fun provideAnalysisExporter(
        @ApplicationContext context: Context
    ): AnalysisExporter {
        return AnalysisExporter(context)
    }
    
    @Provides
    @Singleton
    fun provideProfessionalVideoToolsEngine(
        @ApplicationContext context: Context,
        frameExtractor: VideoFrameExtractor,
        measurementCalculator: MeasurementCalculator,
        annotationManager: AnnotationManager,
        analysisExporter: AnalysisExporter
    ): ProfessionalVideoToolsEngine {
        return ProfessionalVideoToolsEngine(
            context,
            frameExtractor,
            measurementCalculator,
            annotationManager,
            analysisExporter
        )
    }
}
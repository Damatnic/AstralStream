package com.astralplayer.domain.usecase.subtitle

import com.astralplayer.domain.model.Subtitle
import com.astralplayer.domain.repository.SubtitleRepository
import com.astralplayer.domain.repository.AIServiceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Clean Architecture Use Case for AI subtitle generation
 * Implements multi-provider support as per Expert Agent requirements
 */
class GenerateSubtitlesUseCase @Inject constructor(
    private val subtitleRepository: SubtitleRepository,
    private val aiServiceRepository: AIServiceRepository
) {
    sealed class Result {
        data class Success(val subtitles: List<Subtitle>) : Result()
        data class Progress(val percentage: Int, val currentProvider: String) : Result()
        data class Error(val exception: Throwable, val fallbackAvailable: Boolean) : Result()
        object Loading : Result()
    }

    suspend operator fun invoke(
        videoUri: String,
        language: String = "en",
        forceProvider: String? = null
    ): Flow<Result> = flow {
        try {
            emit(Result.Loading)
            
            // 1. Check if subtitles already exist
            val existingSubtitles = subtitleRepository.getSubtitles(videoUri, language)
            if (existingSubtitles.isNotEmpty() && forceProvider == null) {
                emit(Result.Success(existingSubtitles))
                return@flow
            }
            
            // 2. Get available AI providers
            val providers = aiServiceRepository.getAvailableProviders()
            require(providers.isNotEmpty()) { "No AI providers available" }
            
            // 3. Select provider based on priority and availability
            val selectedProvider = forceProvider 
                ?: aiServiceRepository.selectBestProvider(providers)
            
            emit(Result.Progress(10, selectedProvider))
            
            // 4. Extract audio from video
            val audioData = subtitleRepository.extractAudio(videoUri)
            
            emit(Result.Progress(30, selectedProvider))
            
            // 5. Generate subtitles with selected provider
            val subtitles = try {
                when (selectedProvider) {
                    "openai" -> aiServiceRepository.generateWithOpenAI(audioData, language)
                    "google" -> aiServiceRepository.generateWithGoogle(audioData, language)
                    "azure" -> aiServiceRepository.generateWithAzure(audioData, language)
                    "assembly" -> aiServiceRepository.generateWithAssemblyAI(audioData, language)
                    "deepgram" -> aiServiceRepository.generateWithDeepgram(audioData, language)
                    else -> aiServiceRepository.generateWithFallback(audioData, language)
                }
            } catch (e: Exception) {
                // Try fallback provider if primary fails
                if (providers.size > 1) {
                    emit(Result.Error(e, fallbackAvailable = true))
                    val fallbackProvider = providers.first { it != selectedProvider }
                    emit(Result.Progress(50, fallbackProvider))
                    aiServiceRepository.generateWithProvider(fallbackProvider, audioData, language)
                } else {
                    throw e
                }
            }
            
            emit(Result.Progress(80, selectedProvider))
            
            // 6. Save generated subtitles
            subtitleRepository.saveSubtitles(videoUri, language, subtitles)
            
            emit(Result.Progress(100, selectedProvider))
            emit(Result.Success(subtitles))
            
        } catch (e: Exception) {
            emit(Result.Error(e, fallbackAvailable = false))
        }
    }
}
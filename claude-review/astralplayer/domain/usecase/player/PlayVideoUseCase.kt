package com.astralplayer.domain.usecase.player

import com.astralplayer.domain.model.VideoMetadata
import com.astralplayer.domain.repository.VideoRepository
import com.astralplayer.domain.repository.PlayerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Clean Architecture Use Case for playing videos
 * Implements ArchitectureAgent requirements for domain layer
 */
class PlayVideoUseCase @Inject constructor(
    private val videoRepository: VideoRepository,
    private val playerRepository: PlayerRepository
) {
    sealed class Result {
        data class Success(val videoId: String, val position: Long) : Result()
        data class Error(val exception: Throwable) : Result()
        object Loading : Result()
    }

    suspend operator fun invoke(videoUri: String): Flow<Result> = flow {
        try {
            emit(Result.Loading)
            
            // 1. Validate video URI
            require(videoUri.isNotEmpty()) { "Video URI cannot be empty" }
            
            // 2. Get video metadata
            val metadata = videoRepository.getVideoMetadata(videoUri)
                ?: throw IllegalStateException("Unable to retrieve video metadata")
            
            // 3. Check if video is playable
            require(metadata.isPlayable) { "Video format not supported" }
            
            // 4. Load last playback position
            val lastPosition = videoRepository.getLastPlaybackPosition(metadata.id)
            
            // 5. Initialize player
            playerRepository.initializePlayer(
                uri = videoUri,
                startPosition = lastPosition,
                metadata = metadata
            )
            
            // 6. Start playback
            playerRepository.play()
            
            emit(Result.Success(
                videoId = metadata.id,
                position = lastPosition
            ))
            
        } catch (e: Exception) {
            emit(Result.Error(e))
        }
    }
}
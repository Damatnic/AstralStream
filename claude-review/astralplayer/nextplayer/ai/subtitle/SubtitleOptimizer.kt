package com.astralplayer.nextplayer.ai.subtitle

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.math.*

/**
 * Advanced subtitle timing optimization and intelligent segmentation system
 */
class SubtitleOptimizer {
    
    private val _optimizationProgress = MutableSharedFlow<OptimizationProgress>()
    val optimizationProgress: SharedFlow<OptimizationProgress> = _optimizationProgress.asSharedFlow()
    
    /**
     * Optimize subtitle timing with intelligent segmentation
     */
    suspend fun optimizeTiming(
        transcription: List<TranscriptionSegment>,
        audioData: AudioData,
        minDuration: Long = 1000L,
        maxDuration: Long = 6000L,
        maxWordsPerSegment: Int = 12
    ): List<SubtitleSegment> = withContext(Dispatchers.Default) {
        
        try {
            _optimizationProgress.emit(OptimizationProgress.Started(transcription.size))
            
            // Step 1: Merge short segments and split long ones
            val balancedSegments = balanceSegmentLengths(
                transcription, 
                minDuration, 
                maxDuration, 
                maxWordsPerSegment
            )
            
            _optimizationProgress.emit(OptimizationProgress.SegmentBalancing(balancedSegments.size))
            
            // Step 2: Optimize timing based on speech patterns
            val timingOptimized = optimizeTimingWithSpeechAnalysis(balancedSegments, audioData)
            
            _optimizationProgress.emit(OptimizationProgress.TimingOptimization())
            
            // Step 3: Apply reading speed optimization
            val readingOptimized = optimizeForReadingSpeed(timingOptimized)
            
            _optimizationProgress.emit(OptimizationProgress.ReadingSpeedOptimization())
            
            // Step 4: Smart gap insertion
            val gapOptimized = insertSmartGaps(readingOptimized, audioData)
            
            _optimizationProgress.emit(OptimizationProgress.GapInsertion())
            
            // Step 5: Final quality validation
            val validatedSegments = validateAndRefineSegments(gapOptimized, audioData)
            
            _optimizationProgress.emit(OptimizationProgress.Completed(validatedSegments.size))
            
            validatedSegments
            
        } catch (e: Exception) {
            _optimizationProgress.emit(OptimizationProgress.Error(e))
            throw e
        }
    }
    
    /**
     * Balance segment lengths for optimal readability
     */
    private suspend fun balanceSegmentLengths(
        segments: List<TranscriptionSegment>,
        minDuration: Long,
        maxDuration: Long,
        maxWordsPerSegment: Int
    ): List<TranscriptionSegment> {
        
        val balanced = mutableListOf<TranscriptionSegment>()
        var i = 0
        
        while (i < segments.size) {
            val currentSegment = segments[i]
            val duration = currentSegment.endTimeMs - currentSegment.startTimeMs
            val wordCount = countWords(currentSegment.text)
            
            when {
                // Segment too short - merge with next
                duration < minDuration && i < segments.size - 1 -> {
                    val mergedSegment = mergeSegments(currentSegment, segments[i + 1])
                    val mergedDuration = mergedSegment.endTimeMs - mergedSegment.startTimeMs
                    val mergedWordCount = countWords(mergedSegment.text)
                    
                    if (mergedDuration <= maxDuration && mergedWordCount <= maxWordsPerSegment) {
                        balanced.add(mergedSegment)
                        i += 2 // Skip next segment as it's merged
                    } else {
                        balanced.add(currentSegment)
                        i++
                    }
                }
                
                // Segment too long - split
                duration > maxDuration || wordCount > maxWordsPerSegment -> {
                    val splitSegments = splitSegment(currentSegment, maxDuration, maxWordsPerSegment)
                    balanced.addAll(splitSegments)
                    i++
                }
                
                // Segment is good as is
                else -> {
                    balanced.add(currentSegment)
                    i++
                }
            }
        }
        
        return balanced
    }
    
    /**
     * Optimize timing based on speech analysis
     */
    private suspend fun optimizeTimingWithSpeechAnalysis(
        segments: List<TranscriptionSegment>,
        audioData: AudioData
    ): List<TranscriptionSegment> {
        
        return segments.map { segment ->
            // Analyze speech activity in the segment
            val speechActivity = analyzeSpeechActivity(segment, audioData)
            
            // Adjust start time to actual speech beginning
            val adjustedStartTime = findSpeechStart(
                segment.startTimeMs,
                segment.endTimeMs,
                audioData,
                speechActivity
            )
            
            // Adjust end time to actual speech ending
            val adjustedEndTime = findSpeechEnd(
                adjustedStartTime,
                segment.endTimeMs,
                audioData,
                speechActivity
            )
            
            segment.copy(
                startTimeMs = adjustedStartTime,
                endTimeMs = adjustedEndTime
            )
        }
    }
    
    /**
     * Optimize for comfortable reading speed
     */
    private suspend fun optimizeForReadingSpeed(
        segments: List<TranscriptionSegment>
    ): List<SubtitleSegment> {
        
        return segments.map { segment ->
            val text = segment.text
            val wordCount = countWords(text)
            val characterCount = text.length
            val currentDuration = segment.endTimeMs - segment.startTimeMs
            
            // Calculate optimal reading time
            val optimalReadingTime = calculateOptimalReadingTime(wordCount, characterCount)
            
            // Adjust timing if needed
            val adjustedDuration = when {
                currentDuration < optimalReadingTime * 0.8 -> {
                    // Too fast - extend duration
                    (optimalReadingTime * 1.1).toLong()
                }
                currentDuration > optimalReadingTime * 2.0 -> {
                    // Too slow - reduce duration but keep minimum readable time
                    max(optimalReadingTime, currentDuration * 0.8).toLong()
                }
                else -> currentDuration
            }
            
            // Calculate reading difficulty and readability score
            val readabilityScore = calculateReadabilityScore(text, adjustedDuration)
            val qualityScore = calculateSegmentQuality(segment, readabilityScore)
            
            SubtitleSegment(
                text = text,
                startTimeMs = segment.startTimeMs,
                endTimeMs = segment.startTimeMs + adjustedDuration,
                confidence = segment.confidence,
                speakerId = segment.speakerId,
                readabilityScore = readabilityScore,
                qualityScore = qualityScore
            )
        }
    }
    
    /**
     * Insert smart gaps between subtitles
     */
    private suspend fun insertSmartGaps(
        segments: List<SubtitleSegment>,
        audioData: AudioData
    ): List<SubtitleSegment> {
        
        if (segments.size <= 1) return segments
        
        val gapOptimized = mutableListOf<SubtitleSegment>()
        
        for (i in segments.indices) {
            val currentSegment = segments[i]
            
            if (i == 0) {
                gapOptimized.add(currentSegment)
                continue
            }
            
            val previousSegment = gapOptimized.last()
            val gap = currentSegment.startTimeMs - previousSegment.endTimeMs
            
            // Analyze if gap should be adjusted
            val optimalGap = calculateOptimalGap(previousSegment, currentSegment, audioData)
            
            when {
                gap < optimalGap -> {
                    // Gap too small - extend previous segment end or delay current start
                    val adjustment = (optimalGap - gap) / 2
                    
                    // Update previous segment
                    val adjustedPrevious = previousSegment.copy(
                        endTimeMs = min(
                            previousSegment.endTimeMs + adjustment,
                            currentSegment.startTimeMs - 100 // Minimum 100ms gap
                        )
                    )
                    gapOptimized[gapOptimized.size - 1] = adjustedPrevious
                    
                    // Update current segment
                    val adjustedCurrent = currentSegment.copy(
                        startTimeMs = max(
                            currentSegment.startTimeMs,
                            adjustedPrevious.endTimeMs + 100
                        )
                    )
                    gapOptimized.add(adjustedCurrent)
                }
                
                gap > optimalGap * 3 -> {
                    // Gap too large - might need to add intermediate subtitle or adjust timing
                    val speechInGap = detectSpeechInGap(
                        previousSegment.endTimeMs,
                        currentSegment.startTimeMs,
                        audioData
                    )
                    
                    if (speechInGap.isNotEmpty()) {
                        // Add intermediate subtitle for detected speech
                        val intermediateSegment = createIntermediateSubtitle(
                            speechInGap,
                            previousSegment.endTimeMs,
                            currentSegment.startTimeMs
                        )
                        gapOptimized.add(intermediateSegment)
                    }
                    
                    gapOptimized.add(currentSegment)
                }
                
                else -> {
                    // Gap is acceptable
                    gapOptimized.add(currentSegment)
                }
            }
        }
        
        return gapOptimized
    }
    
    /**
     * Validate and refine final segments
     */
    private suspend fun validateAndRefineSegments(
        segments: List<SubtitleSegment>,
        audioData: AudioData
    ): List<SubtitleSegment> {
        
        return segments.mapIndexed { index, segment ->
            // Validate timing constraints
            val validatedTiming = validateTiming(segment, audioData)
            
            // Ensure no overlaps
            val noOverlapTiming = if (index > 0) {
                val previousEnd = segments[index - 1].endTimeMs
                if (validatedTiming.startTimeMs < previousEnd) {
                    validatedTiming.copy(startTimeMs = previousEnd + 50) // 50ms minimum gap
                } else validatedTiming
            } else validatedTiming
            
            // Final quality check
            val finalQuality = calculateFinalQuality(noOverlapTiming, segments, index)
            
            noOverlapTiming.copy(qualityScore = finalQuality)
        }.filter { it.text.trim().isNotBlank() } // Remove empty segments
    }
    
    // Helper methods for segment manipulation
    private fun mergeSegments(
        segment1: TranscriptionSegment,
        segment2: TranscriptionSegment
    ): TranscriptionSegment {
        return TranscriptionSegment(
            text = "${segment1.text} ${segment2.text}".trim(),
            startTimeMs = segment1.startTimeMs,
            endTimeMs = segment2.endTimeMs,
            confidence = (segment1.confidence + segment2.confidence) / 2,
            speakerId = segment1.speakerId
        )
    }
    
    private fun splitSegment(
        segment: TranscriptionSegment,
        maxDuration: Long,
        maxWordsPerSegment: Int
    ): List<TranscriptionSegment> {
        
        val words = segment.text.split("\\s+".toRegex())
        if (words.size <= maxWordsPerSegment) {
            // Split by duration
            val midTime = segment.startTimeMs + (segment.endTimeMs - segment.startTimeMs) / 2
            val midWordIndex = words.size / 2
            
            return listOf(
                TranscriptionSegment(
                    text = words.take(midWordIndex).joinToString(" "),
                    startTimeMs = segment.startTimeMs,
                    endTimeMs = midTime,
                    confidence = segment.confidence,
                    speakerId = segment.speakerId
                ),
                TranscriptionSegment(
                    text = words.drop(midWordIndex).joinToString(" "),
                    startTimeMs = midTime,
                    endTimeMs = segment.endTimeMs,
                    confidence = segment.confidence,
                    speakerId = segment.speakerId
                )
            )
        } else {
            // Split by word count
            val segmentDuration = segment.endTimeMs - segment.startTimeMs
            val segments = mutableListOf<TranscriptionSegment>()
            
            words.chunked(maxWordsPerSegment).forEachIndexed { index, wordChunk ->
                val startTime = segment.startTimeMs + (segmentDuration * index / (words.size / maxWordsPerSegment))
                val endTime = if (index == words.chunked(maxWordsPerSegment).size - 1) {
                    segment.endTimeMs
                } else {
                    segment.startTimeMs + (segmentDuration * (index + 1) / (words.size / maxWordsPerSegment))
                }
                
                segments.add(
                    TranscriptionSegment(
                        text = wordChunk.joinToString(" "),
                        startTimeMs = startTime.toLong(),
                        endTimeMs = endTime.toLong(),
                        confidence = segment.confidence,
                        speakerId = segment.speakerId
                    )
                )
            }
            
            return segments
        }
    }
    
    // Audio analysis methods
    private fun analyzeSpeechActivity(
        segment: TranscriptionSegment,
        audioData: AudioData
    ): SpeechActivity {
        val startSample = ((segment.startTimeMs * audioData.sampleRate) / 1000).toInt()
        val endSample = ((segment.endTimeMs * audioData.sampleRate) / 1000).toInt()
        
        if (startSample >= audioData.samples.size || endSample > audioData.samples.size) {
            return SpeechActivity(emptyList(), 0.0f)
        }
        
        val segmentSamples = audioData.samples.sliceArray(startSample until endSample)
        
        // Detect active speech regions
        val windowSize = audioData.sampleRate / 10 // 100ms windows
        val speechRegions = mutableListOf<Pair<Long, Long>>()
        
        for (i in segmentSamples.indices step windowSize) {
            val windowEnd = min(i + windowSize, segmentSamples.size)
            val window = segmentSamples.sliceArray(i until windowEnd)
            
            val energy = window.map { it * it }.average()
            val zcr = calculateZeroCrossingRate(window)
            
            if (energy > 0.001 && zcr > 0.05 && zcr < 0.4) {
                val regionStart = segment.startTimeMs + (i * 1000L / audioData.sampleRate)
                val regionEnd = segment.startTimeMs + (windowEnd * 1000L / audioData.sampleRate)
                speechRegions.add(Pair(regionStart, regionEnd))
            }
        }
        
        val speechRatio = speechRegions.sumOf { it.second - it.first }.toFloat() / 
                         (segment.endTimeMs - segment.startTimeMs)
        
        return SpeechActivity(speechRegions, speechRatio)
    }
    
    private fun findSpeechStart(
        originalStart: Long,
        originalEnd: Long,
        audioData: AudioData,
        speechActivity: SpeechActivity
    ): Long {
        val firstSpeechRegion = speechActivity.regions.firstOrNull()
        return firstSpeechRegion?.first?.let { speechStart ->
            // Don't adjust more than 500ms from original
            max(originalStart - 500, min(speechStart, originalStart + 500))
        } ?: originalStart
    }
    
    private fun findSpeechEnd(
        adjustedStart: Long,
        originalEnd: Long,
        audioData: AudioData,
        speechActivity: SpeechActivity
    ): Long {
        val lastSpeechRegion = speechActivity.regions.lastOrNull()
        return lastSpeechRegion?.second?.let { speechEnd ->
            // Don't adjust more than 500ms from original, and ensure minimum duration
            val minEnd = adjustedStart + 800 // Minimum 800ms duration
            max(minEnd, min(speechEnd + 200, originalEnd + 500)) // Add 200ms buffer after speech
        } ?: max(adjustedStart + 800, originalEnd)
    }
    
    // Reading speed optimization
    private fun calculateOptimalReadingTime(wordCount: Int, characterCount: Int): Double {
        // Average reading speed: 200 words per minute, 1000 characters per minute
        val wordTime = (wordCount * 60000.0) / 200 // milliseconds
        val charTime = (characterCount * 60000.0) / 1000 // milliseconds
        
        // Use the longer of the two estimates, with minimum 1 second
        return max(1000.0, max(wordTime, charTime))
    }
    
    private fun calculateReadabilityScore(text: String, durationMs: Long): Float {
        val wordCount = countWords(text)
        val characterCount = text.length
        
        // Calculate reading speed (words per minute)
        val readingSpeed = if (durationMs > 0) {
            (wordCount * 60000.0) / durationMs
        } else 0.0
        
        // Optimal reading speed is around 150-200 WPM
        val speedScore = when {
            readingSpeed in 120.0..220.0 -> 1.0f
            readingSpeed in 80.0..280.0 -> 0.7f
            else -> 0.3f
        }
        
        // Text complexity score
        val complexityScore = calculateTextComplexity(text)
        
        // Length appropriateness score
        val lengthScore = when {
            characterCount in 20..80 -> 1.0f
            characterCount in 10..120 -> 0.8f
            else -> 0.5f
        }
        
        return (speedScore + complexityScore + lengthScore) / 3.0f
    }
    
    private fun calculateTextComplexity(text: String): Float {
        val words = text.split("\\s+".toRegex())
        val avgWordLength = words.map { it.length }.average()
        val sentenceCount = text.count { it in ".!?" }
        val avgSentenceLength = if (sentenceCount > 0) words.size.toDouble() / sentenceCount else words.size.toDouble()
        
        // Simpler text gets higher score
        return when {
            avgWordLength < 5 && avgSentenceLength < 15 -> 1.0f
            avgWordLength < 7 && avgSentenceLength < 20 -> 0.8f
            else -> 0.6f
        }
    }
    
    // Gap optimization
    private fun calculateOptimalGap(
        previousSegment: SubtitleSegment,
        currentSegment: SubtitleSegment,
        audioData: AudioData
    ): Long {
        // Base gap of 250ms for reading transition
        var optimalGap = 250L
        
        // Increase gap for speaker changes
        if (previousSegment.speakerId != currentSegment.speakerId) {
            optimalGap += 200L
        }
        
        // Increase gap for complex text
        if (previousSegment.readabilityScore < 0.7f || currentSegment.readabilityScore < 0.7f) {
            optimalGap += 150L
        }
        
        // Decrease gap for short segments
        val previousDuration = previousSegment.endTimeMs - previousSegment.startTimeMs
        val currentDuration = currentSegment.endTimeMs - currentSegment.startTimeMs
        
        if (previousDuration < 2000 && currentDuration < 2000) {
            optimalGap = max(100L, optimalGap - 100L)
        }
        
        return optimalGap
    }
    
    private fun detectSpeechInGap(
        gapStart: Long,
        gapEnd: Long,
        audioData: AudioData
    ): String {
        // Simplified gap analysis - in real implementation, run speech recognition on gap
        return if (gapEnd - gapStart > 1000) {
            // Placeholder for potential speech detection
            ""
        } else ""
    }
    
    private fun createIntermediateSubtitle(
        text: String,
        gapStart: Long,
        gapEnd: Long
    ): SubtitleSegment {
        return SubtitleSegment(
            text = text,
            startTimeMs = gapStart + 100,
            endTimeMs = gapEnd - 100,
            confidence = 0.6f, // Lower confidence for gap-detected speech
            readabilityScore = 0.8f,
            qualityScore = 0.7f
        )
    }
    
    // Validation methods
    private fun validateTiming(segment: SubtitleSegment, audioData: AudioData): SubtitleSegment {
        val maxTime = audioData.durationMs
        val validStart = max(0L, min(segment.startTimeMs, maxTime - 1000))
        val validEnd = max(validStart + 500, min(segment.endTimeMs, maxTime))
        
        return segment.copy(
            startTimeMs = validStart,
            endTimeMs = validEnd
        )
    }
    
    private fun calculateSegmentQuality(
        segment: TranscriptionSegment,
        readabilityScore: Float
    ): Float {
        val confidenceScore = segment.confidence
        val lengthScore = if (segment.text.length in 10..100) 1.0f else 0.7f
        val durationScore = when (val duration = segment.endTimeMs - segment.startTimeMs) {
            in 1000..6000 -> 1.0f
            in 500..8000 -> 0.8f
            else -> 0.5f
        }
        
        return (confidenceScore + readabilityScore + lengthScore + durationScore) / 4.0f
    }
    
    private fun calculateFinalQuality(
        segment: SubtitleSegment,
        allSegments: List<SubtitleSegment>,
        index: Int
    ): Float {
        var quality = segment.qualityScore
        
        // Bonus for good timing relative to neighbors
        if (index > 0) {
            val gap = segment.startTimeMs - allSegments[index - 1].endTimeMs
            if (gap in 100..1000) quality += 0.1f
        }
        
        // Bonus for appropriate text length
        if (segment.text.length in 20..60) quality += 0.05f
        
        return min(1.0f, quality)
    }
    
    // Utility methods
    private fun countWords(text: String): Int = text.trim().split("\\s+".toRegex()).size
    
    private fun calculateZeroCrossingRate(samples: FloatArray): Float {
        var crossings = 0
        for (i in 1 until samples.size) {
            if ((samples[i] >= 0) != (samples[i - 1] >= 0)) {
                crossings++
            }
        }
        return crossings.toFloat() / samples.size
    }
}

// Data classes
data class SubtitleSegment(
    val text: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val confidence: Float = 1.0f,
    val speakerId: String? = null,
    val language: String = "en",
    val isTranslated: Boolean = false,
    val readabilityScore: Float = 1.0f,
    val qualityScore: Float = 1.0f
) {
    val durationMs: Long get() = endTimeMs - startTimeMs
}

data class TranscriptionSegment(
    val text: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val confidence: Float,
    val speakerId: String? = null
)

data class AudioData(
    val samples: FloatArray,
    val sampleRate: Int,
    val durationMs: Long
)

data class SpeechActivity(
    val regions: List<Pair<Long, Long>>, // Start and end times of speech regions
    val speechRatio: Float // Ratio of speech to total duration
)

sealed class OptimizationProgress {
    data class Started(val segmentCount: Int) : OptimizationProgress()
    data class SegmentBalancing(val balancedCount: Int) : OptimizationProgress()
    object TimingOptimization : OptimizationProgress()
    object ReadingSpeedOptimization : OptimizationProgress()
    object GapInsertion : OptimizationProgress()
    data class Completed(val finalCount: Int) : OptimizationProgress()
    data class Error(val error: Throwable) : OptimizationProgress()
}
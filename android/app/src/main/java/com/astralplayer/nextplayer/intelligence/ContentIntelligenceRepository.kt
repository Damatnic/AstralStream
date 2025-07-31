package com.astralplayer.nextplayer.intelligence

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.room.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for AI Content Intelligence data management
 * Handles storage, retrieval, and search of content analysis results
 */
@UnstableApi
@Singleton
class ContentIntelligenceRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: ContentIntelligenceDatabase
) {
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    /**
     * Save content analysis session to database
     */
    suspend fun saveAnalysisSession(session: AIContentIntelligenceEngine.ContentAnalysisSession) = withContext(Dispatchers.IO) {
        try {
            // Convert session to database entities
            val sessionEntity = ContentAnalysisSessionEntity(
                id = session.id,
                mediaId = session.mediaItem.mediaId ?: session.mediaItem.localConfiguration?.uri?.toString() ?: "",
                mediaUri = session.mediaItem.localConfiguration?.uri?.toString() ?: "",
                analysisLevel = session.analysisLevel.name,
                startTime = session.startTime,
                contentCategory = session.contentCategory?.primaryCategory,
                categoryConfidence = session.contentCategory?.confidence ?: 0f,
                genre = session.contentCategory?.genre,
                emotionalMood = session.emotionalAnalysis?.dominantMood,
                energyLevel = session.emotionalAnalysis?.energyLevel ?: 0f,
                valence = session.emotionalAnalysis?.valence ?: 0f,
                qualityScore = session.qualityMetrics?.overallScore ?: 0f,
                processingTime = System.currentTimeMillis() - session.startTime
            )
            
            database.analysisSessionDao().insertSession(sessionEntity)
            
            // Save detected scenes
            val sceneEntities = session.detectedScenes.map { scene ->
                DetectedSceneEntity(
                    sessionId = session.id,
                    startTime = scene.startTime,
                    endTime = scene.endTime,
                    description = scene.description,
                    confidence = scene.confidence,
                    keyObjects = scene.keyObjects.joinToString(","),
                    dominantColors = scene.dominantColors.joinToString(",")
                )
            }
            database.sceneDao().insertScenes(sceneEntities)
            
            // Save detected faces (if privacy allows)
            if (session.privacySettings.allowFaceRecognition) {
                val faceEntities = session.detectedFaces.map { face ->
                    DetectedFaceEntity(
                        sessionId = session.id,
                        faceId = face.id,
                        boundingBoxLeft = face.boundingBox.left,
                        boundingBoxTop = face.boundingBox.top,
                        boundingBoxRight = face.boundingBox.right,
                        boundingBoxBottom = face.boundingBox.bottom,
                        confidence = face.confidence,
                        emotion = face.emotion,
                        firstAppearance = face.firstAppearance,
                        totalAppearances = face.totalAppearances
                    )
                }
                database.faceDao().insertFaces(faceEntities)
            }
            
            // Save detected objects
            val objectEntities = session.detectedObjects.map { obj ->
                DetectedObjectEntity(
                    sessionId = session.id,
                    objectName = obj.name,
                    category = obj.category,
                    confidence = obj.confidence,
                    boundingBoxLeft = obj.boundingBox.left,
                    boundingBoxTop = obj.boundingBox.top,
                    boundingBoxRight = obj.boundingBox.right,
                    boundingBoxBottom = obj.boundingBox.bottom,
                    firstAppearance = obj.firstAppearance,
                    frequency = obj.frequency
                )
            }
            database.objectDao().insertObjects(objectEntities)
            
        } catch (e: Exception) {
            // Handle database errors
        }
    }
    
    /**
     * Get analysis results for specific media item
     */
    suspend fun getAnalysisForMedia(mediaItem: MediaItem): AIContentIntelligenceEngine.ContentAnalysisResults? = withContext(Dispatchers.IO) {
        try {
            val mediaId = mediaItem.mediaId ?: mediaItem.localConfiguration?.uri?.toString() ?: return@withContext null
            
            val sessionEntity = database.analysisSessionDao().getSessionByMediaId(mediaId) ?: return@withContext null
            val scenes = database.sceneDao().getScenesForSession(sessionEntity.id)
            val faces = database.faceDao().getFacesForSession(sessionEntity.id)
            val objects = database.objectDao().getObjectsForSession(sessionEntity.id)
            
            // Convert entities back to domain objects
            AIContentIntelligenceEngine.ContentAnalysisResults(
                sessionId = sessionEntity.id,
                mediaItem = mediaItem,
                contentCategory = sessionEntity.contentCategory?.let { category ->
                    AIContentIntelligenceEngine.ContentCategory(
                        primaryCategory = category,
                        subCategories = listOf(), // Would need to store sub-categories separately
                        confidence = sessionEntity.categoryConfidence,
                        genre = sessionEntity.genre
                    )
                },
                detectedScenes = scenes.map { convertToDetectedScene(it) },
                detectedFaces = faces.map { convertToDetectedFace(it) },
                detectedObjects = objects.map { convertToDetectedObject(it) },
                emotionalAnalysis = if (sessionEntity.emotionalMood != null) {
                    AIContentIntelligenceEngine.EmotionalAnalysis(
                        dominantMood = sessionEntity.emotionalMood,
                        emotions = mapOf(sessionEntity.emotionalMood to 1.0f), // Simplified
                        energyLevel = sessionEntity.energyLevel,
                        valence = sessionEntity.valence
                    )
                } else null,
                qualityMetrics = if (sessionEntity.qualityScore > 0) {
                    AIContentIntelligenceEngine.QualityMetrics(
                        videoQualityScore = sessionEntity.qualityScore,
                        audioQualityScore = sessionEntity.qualityScore,
                        stabilityScore = sessionEntity.qualityScore,
                        colorAccuracy = sessionEntity.qualityScore,
                        noiseLevel = 1f - sessionEntity.qualityScore,
                        overallScore = sessionEntity.qualityScore
                    )
                } else null,
                contentFingerprint = null, // Would need separate storage
                analysisLevel = AIContentIntelligenceEngine.AnalysisLevel.valueOf(sessionEntity.analysisLevel),
                processingTime = sessionEntity.processingTime
            )
            
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Search content based on query and type
     */
    suspend fun searchContent(
        query: String,
        searchType: AIContentIntelligenceEngine.ContentSearchType
    ): List<AIContentIntelligenceEngine.ContentSearchResult> = withContext(Dispatchers.IO) {
        
        try {
            val results = mutableListOf<AIContentIntelligenceEngine.ContentSearchResult>()
            
            when (searchType) {
                AIContentIntelligenceEngine.ContentSearchType.ALL -> {
                    results.addAll(searchInScenes(query))
                    results.addAll(searchInObjects(query))
                    results.addAll(searchInCategories(query))
                }
                AIContentIntelligenceEngine.ContentSearchType.SCENES -> {
                    results.addAll(searchInScenes(query))
                }
                AIContentIntelligenceEngine.ContentSearchType.OBJECTS -> {
                    results.addAll(searchInObjects(query))
                }
                AIContentIntelligenceEngine.ContentSearchType.CATEGORIES -> {
                    results.addAll(searchInCategories(query))
                }
                AIContentIntelligenceEngine.ContentSearchType.FACES -> {
                    // Face search would require additional implementation
                }
                AIContentIntelligenceEngine.ContentSearchType.EMOTIONS -> {
                    results.addAll(searchByEmotion(query))
                }
            }
            
            // Sort by relevance score
            results.sortedByDescending { it.relevanceScore }
            
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Search in scene descriptions
     */
    private suspend fun searchInScenes(query: String): List<AIContentIntelligenceEngine.ContentSearchResult> {
        val scenes = database.sceneDao().searchScenesByDescription("%$query%")
        return scenes.mapNotNull { scene ->
            val session = database.analysisSessionDao().getSessionById(scene.sessionId)
            session?.let {
                // Create mock MediaItem - in production would reconstruct from stored data
                val mediaItem = MediaItem.fromUri(session.mediaUri)
                AIContentIntelligenceEngine.ContentSearchResult(
                    mediaItem = mediaItem,
                    relevanceScore = scene.confidence,
                    matchType = "Scene",
                    matchedElements = listOf(scene.description)
                )
            }
        }
    }
    
    /**
     * Search in detected objects
     */
    private suspend fun searchInObjects(query: String): List<AIContentIntelligenceEngine.ContentSearchResult> {
        val objects = database.objectDao().searchObjectsByName("%$query%")
        return objects.mapNotNull { obj ->
            val session = database.analysisSessionDao().getSessionById(obj.sessionId)
            session?.let {
                val mediaItem = MediaItem.fromUri(session.mediaUri)
                AIContentIntelligenceEngine.ContentSearchResult(
                    mediaItem = mediaItem,
                    relevanceScore = obj.confidence,
                    matchType = "Object",
                    matchedElements = listOf(obj.objectName)
                )
            }
        }
    }
    
    /**
     * Search in content categories
     */
    private suspend fun searchInCategories(query: String): List<AIContentIntelligenceEngine.ContentSearchResult> {
        val sessions = database.analysisSessionDao().searchSessionsByCategory("%$query%")
        return sessions.map { session ->
            val mediaItem = MediaItem.fromUri(session.mediaUri)
            AIContentIntelligenceEngine.ContentSearchResult(
                mediaItem = mediaItem,
                relevanceScore = session.categoryConfidence,
                matchType = "Category",
                matchedElements = listOfNotNull(session.contentCategory, session.genre)
            )
        }
    }
    
    /**
     * Search by emotional content
     */
    private suspend fun searchByEmotion(emotion: String): List<AIContentIntelligenceEngine.ContentSearchResult> {
        val sessions = database.analysisSessionDao().searchSessionsByMood("%$emotion%")
        return sessions.map { session ->
            val mediaItem = MediaItem.fromUri(session.mediaUri)
            AIContentIntelligenceEngine.ContentSearchResult(
                mediaItem = mediaItem,
                relevanceScore = session.energyLevel,
                matchType = "Emotion",
                matchedElements = listOfNotNull(session.emotionalMood)
            )
        }
    }
    
    /**
     * Find similar content based on fingerprint and objects
     */
    suspend fun findSimilarContent(
        fingerprint: AIContentIntelligenceEngine.ContentFingerprint?,
        objects: List<AIContentIntelligenceEngine.DetectedObject>
    ): List<AIContentIntelligenceEngine.ContentRecommendation> = withContext(Dispatchers.IO) {
        
        try {
            // Find content with similar objects
            val recommendations = mutableListOf<AIContentIntelligenceEngine.ContentRecommendation>()
            
            objects.forEach { targetObject ->
                val similarObjects = database.objectDao().findSimilarObjects(targetObject.name, targetObject.category)
                
                similarObjects.forEach { similarObject ->
                    val session = database.analysisSessionDao().getSessionById(similarObject.sessionId)
                    session?.let {
                        val mediaItem = MediaItem.fromUri(session.mediaUri)
                        val similarity = calculateSimilarity(targetObject, similarObject)
                        
                        recommendations.add(
                            AIContentIntelligenceEngine.ContentRecommendation(
                                mediaItem = mediaItem,
                                similarity = similarity,
                                reason = "Contains similar objects: ${targetObject.name}",
                                confidence = similarObject.confidence
                            )
                        )
                    }
                }
            }
            
            // Remove duplicates and sort by similarity
            recommendations.distinctBy { it.mediaItem.localConfiguration?.uri }
                .sortedByDescending { it.similarity }
                .take(10) // Limit to top 10 recommendations
            
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Find content by category
     */
    suspend fun findContentByCategory(
        category: AIContentIntelligenceEngine.ContentCategory?
    ): List<AIContentIntelligenceEngine.ContentRecommendation> = withContext(Dispatchers.IO) {
        
        category?.let { cat ->
            try {
                val sessions = database.analysisSessionDao().getSessionsByCategory(cat.primaryCategory)
                sessions.map { session ->
                    val mediaItem = MediaItem.fromUri(session.mediaUri)
                    AIContentIntelligenceEngine.ContentRecommendation(
                        mediaItem = mediaItem,
                        similarity = session.categoryConfidence,
                        reason = "Same category: ${cat.primaryCategory}",
                        confidence = session.categoryConfidence
                    )
                }
            } catch (e: Exception) {
                emptyList()
            }
        } ?: emptyList()
    }
    
    /**
     * Find content by mood
     */
    suspend fun findContentByMood(
        mood: String
    ): List<AIContentIntelligenceEngine.ContentRecommendation> = withContext(Dispatchers.IO) {
        
        try {
            val sessions = database.analysisSessionDao().getSessionsByMood(mood)
            sessions.map { session ->
                val mediaItem = MediaItem.fromUri(session.mediaUri)
                AIContentIntelligenceEngine.ContentRecommendation(
                    mediaItem = mediaItem,
                    similarity = session.energyLevel,
                    reason = "Similar mood: $mood",
                    confidence = session.energyLevel
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Calculate similarity between objects
     */
    private fun calculateSimilarity(
        obj1: AIContentIntelligenceEngine.DetectedObject,
        obj2: DetectedObjectEntity
    ): Float {
        val nameMatch = if (obj1.name == obj2.objectName) 0.8f else 0f
        val categoryMatch = if (obj1.category == obj2.category) 0.6f else 0f
        val confidenceScore = (obj1.confidence + obj2.confidence) / 2f * 0.2f
        
        return (nameMatch + categoryMatch + confidenceScore).coerceIn(0f, 1f)
    }
    
    // Statistics methods
    suspend fun getTotalAnalysesCount(): Int = withContext(Dispatchers.IO) {
        database.analysisSessionDao().getTotalSessionsCount()
    }
    
    suspend fun getAverageAnalysisTime(): Long = withContext(Dispatchers.IO) {
        database.analysisSessionDao().getAverageProcessingTime() ?: 0L
    }
    
    suspend fun getAverageAccuracyScore(): Float = withContext(Dispatchers.IO) {
        database.analysisSessionDao().getAverageQualityScore() ?: 0f
    }
    
    suspend fun getUniqueCategoriesCount(): Int = withContext(Dispatchers.IO) {
        database.analysisSessionDao().getUniqueCategoriesCount()
    }
    
    suspend fun getTotalObjectsDetected(): Long = withContext(Dispatchers.IO) {
        database.objectDao().getTotalObjectsCount().toLong()
    }
    
    suspend fun getTotalFacesDetected(): Long = withContext(Dispatchers.IO) {
        database.faceDao().getTotalFacesCount().toLong()
    }
    
    // Entity conversion methods
    private fun convertToDetectedScene(entity: DetectedSceneEntity): AIContentIntelligenceEngine.DetectedScene {
        return AIContentIntelligenceEngine.DetectedScene(
            startTime = entity.startTime,
            endTime = entity.endTime,
            description = entity.description,
            confidence = entity.confidence,
            keyObjects = entity.keyObjects.split(",").filter { it.isNotBlank() },
            dominantColors = entity.dominantColors.split(",").filter { it.isNotBlank() }
        )
    }
    
    private fun convertToDetectedFace(entity: DetectedFaceEntity): AIContentIntelligenceEngine.DetectedFace {
        return AIContentIntelligenceEngine.DetectedFace(
            id = entity.faceId,
            boundingBox = android.graphics.RectF(
                entity.boundingBoxLeft,
                entity.boundingBoxTop,
                entity.boundingBoxRight,
                entity.boundingBoxBottom
            ),
            confidence = entity.confidence,
            emotion = entity.emotion,
            firstAppearance = entity.firstAppearance,
            totalAppearances = entity.totalAppearances
        )
    }
    
    private fun convertToDetectedObject(entity: DetectedObjectEntity): AIContentIntelligenceEngine.DetectedObject {
        return AIContentIntelligenceEngine.DetectedObject(
            name = entity.objectName,
            category = entity.category,
            confidence = entity.confidence,
            boundingBox = android.graphics.RectF(
                entity.boundingBoxLeft,
                entity.boundingBoxTop,
                entity.boundingBoxRight,
                entity.boundingBoxBottom
            ),
            firstAppearance = entity.firstAppearance,
            frequency = entity.frequency
        )
    }
    
    /**
     * Clear all intelligence data (privacy compliance)
     */
    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        database.clearAllTables()
    }
    
    /**
     * Release resources
     */
    fun release() {
        scope.cancel()
    }
}
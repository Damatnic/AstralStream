package com.astralplayer.nextplayer.feature.ai

/**
 * Configuration for AI services
 * 
 * IMPORTANT: For production use, store these credentials securely:
 * 1. Use environment variables
 * 2. Store in local.properties (git-ignored)
 * 3. Use a secure key management service
 * 4. Fetch from a secure backend server
 * 
 * NEVER commit actual API keys to source control!
 */
object AIServicesConfig {
    
    // ===== GOOGLE CLOUD SERVICES =====
    /**
     * Google Cloud Speech-to-Text API
     * Get your API key from: https://console.cloud.google.com/apis/credentials
     * Enable Speech-to-Text API: https://console.cloud.google.com/apis/library/speech.googleapis.com
     * 
     * Pricing: ~$0.006 per 15 seconds of audio
     * Free tier: 60 minutes per month
     */
    const val GOOGLE_CLOUD_SPEECH_API_KEY = "YOUR_GOOGLE_CLOUD_SPEECH_API_KEY"
    
    /**
     * Alternative: Use service account JSON for better security
     * Download from: https://console.cloud.google.com/iam-admin/serviceaccounts
     * Place in app/src/main/assets/google-services-speech.json
     */
    const val USE_GOOGLE_SERVICE_ACCOUNT = false
    
    // ===== ML KIT (ON-DEVICE) =====
    /**
     * ML Kit services work on-device, no API key needed!
     * - Image labeling
     * - Object detection
     * - Text recognition
     * - Language identification
     * - Translation (basic languages)
     * 
     * These are already implemented and working in:
     * - VideoContentAnalyzer
     * - AISceneDetectionManager
     * - LiveTranslationManager
     */
    
    // ===== ALTERNATIVE SPEECH-TO-TEXT SERVICES =====
    
    /**
     * Microsoft Azure Speech Services
     * Sign up: https://azure.microsoft.com/services/cognitive-services/speech-services/
     * 
     * Pricing: ~$1 per hour of audio
     * Free tier: 5 hours per month
     */
    const val AZURE_SPEECH_KEY = "YOUR_AZURE_SPEECH_KEY"
    const val AZURE_SPEECH_REGION = "YOUR_AZURE_REGION" // e.g., "eastus"
    
    /**
     * Amazon Transcribe
     * Sign up: https://aws.amazon.com/transcribe/
     * 
     * Pricing: ~$0.024 per minute
     * Free tier: 60 minutes per month (first 12 months)
     */
    const val AWS_ACCESS_KEY_ID = "YOUR_AWS_ACCESS_KEY"
    const val AWS_SECRET_ACCESS_KEY = "YOUR_AWS_SECRET_KEY"
    const val AWS_REGION = "us-east-1"
    
    /**
     * OpenAI Whisper API
     * Sign up: https://platform.openai.com/
     * 
     * Pricing: ~$0.006 per minute
     * No free tier, but very accurate
     */
    const val OPENAI_API_KEY = "YOUR_OPENAI_API_KEY"
    
    /**
     * Claude API (Anthropic)
     * Sign up: https://console.anthropic.com/
     * 
     * Claude 3 Haiku pricing: $0.25/$1.25 per million tokens (input/output)
     * Best for: Advanced text analysis, summaries, translations
     * 
     * SECURITY WARNING: This should be stored securely, not in code!
     * TODO: Move to secure storage before production
     */
    const val CLAUDE_API_KEY = "YOUR_API_KEY_HERE" // TODO: Move to secure storage before production
    
    // ===== ADVANCED AI FEATURES =====
    
    /**
     * For video content analysis beyond basic object detection:
     * 
     * 1. Google Cloud Video Intelligence API
     *    - Scene detection, explicit content detection, speech transcription
     *    - https://cloud.google.com/video-intelligence
     *    - ~$0.10 per minute
     * 
     * 2. Amazon Rekognition Video
     *    - Celebrity recognition, content moderation, activity detection
     *    - https://aws.amazon.com/rekognition/
     *    - ~$0.10 per minute
     * 
     * 3. Microsoft Azure Video Analyzer
     *    - Motion detection, face detection, object tracking
     *    - https://azure.microsoft.com/services/video-analyzer/
     *    - ~$0.15 per minute
     */
    
    // ===== TRANSLATION SERVICES =====
    
    /**
     * For more languages and better quality than ML Kit:
     * 
     * 1. Google Cloud Translation API
     *    - 100+ languages, neural machine translation
     *    - https://cloud.google.com/translate
     *    - ~$20 per million characters
     * 
     * 2. Microsoft Translator
     *    - 100+ languages, custom models
     *    - https://azure.microsoft.com/services/cognitive-services/translator/
     *    - ~$10 per million characters
     * 
     * 3. DeepL API
     *    - Best quality for European languages
     *    - https://www.deepl.com/pro-api
     *    - ~€20 per million characters
     */
    const val GOOGLE_TRANSLATE_API_KEY = "YOUR_GOOGLE_TRANSLATE_API_KEY"
    const val DEEPL_API_KEY = "YOUR_DEEPL_API_KEY"
    
    // ===== CUSTOM MODEL HOSTING =====
    
    /**
     * For custom TensorFlow/PyTorch models:
     * 
     * 1. TensorFlow Serving
     *    - Host on Google Cloud AI Platform, AWS SageMaker, or your own server
     *    - Model endpoint URL needed
     * 
     * 2. Hugging Face Inference API
     *    - Easy hosting for transformer models
     *    - https://huggingface.co/inference-api
     *    - Free tier available
     */
    const val CUSTOM_MODEL_ENDPOINT = "https://your-model-endpoint.com/predict"
    const val HUGGINGFACE_API_KEY = "YOUR_HUGGINGFACE_API_KEY"
    
    // ===== CONFIGURATION HELPERS =====
    
    /**
     * Check if essential services are configured
     */
    fun isConfigured(): Boolean {
        return when {
            // At least one speech service should be configured
            GOOGLE_CLOUD_SPEECH_API_KEY != "YOUR_GOOGLE_CLOUD_SPEECH_API_KEY" -> true
            AZURE_SPEECH_KEY != "YOUR_AZURE_SPEECH_KEY" -> true
            OPENAI_API_KEY != "YOUR_OPENAI_API_KEY" -> true
            else -> false
        }
    }
    
    /**
     * Get active speech service
     */
    fun getActiveSpeechService(): SpeechService {
        return when {
            GOOGLE_CLOUD_SPEECH_API_KEY != "YOUR_GOOGLE_CLOUD_SPEECH_API_KEY" -> SpeechService.GOOGLE
            AZURE_SPEECH_KEY != "YOUR_AZURE_SPEECH_KEY" -> SpeechService.AZURE
            OPENAI_API_KEY != "YOUR_OPENAI_API_KEY" -> SpeechService.OPENAI
            else -> SpeechService.NONE
        }
    }
    
    enum class SpeechService {
        GOOGLE, AZURE, OPENAI, NONE
    }
}

/**
 * Instructions for setting up AI services:
 * 
 * 1. GOOGLE CLOUD SPEECH-TO-TEXT:
 *    a) Go to https://console.cloud.google.com
 *    b) Create a new project or select existing
 *    c) Enable Speech-to-Text API
 *    d) Create credentials (API key or service account)
 *    e) Add the key to GOOGLE_CLOUD_SPEECH_API_KEY above
 * 
 * 2. ML KIT (Already working!):
 *    - No setup needed, works offline
 *    - Automatically downloads models when first used
 *    - Supports image labeling, object detection, text recognition
 * 
 * 3. OPTIONAL ENHANCEMENTS:
 *    - For better transcription accuracy: Use Google Cloud or OpenAI Whisper
 *    - For more languages: Use Google Translate API
 *    - For video analysis: Use Google Video Intelligence API
 * 
 * 4. SECURITY BEST PRACTICES:
 *    - Never commit API keys to git
 *    - Use environment variables or local.properties
 *    - Implement key rotation
 *    - Use service accounts when possible
 *    - Consider proxying through your backend
 */
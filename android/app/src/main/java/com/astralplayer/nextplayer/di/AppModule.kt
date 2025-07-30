package com.astralplayer.nextplayer.di

import android.content.Context
import com.astralplayer.nextplayer.data.*
import com.astralplayer.nextplayer.data.database.*
import com.astralplayer.nextplayer.feature.codec.CodecPackManager

// Simplified AppModule without Hilt for now - will re-enable after basic functionality works
object AppModule {
    
    fun provideSettingsDataStore(context: Context): SettingsDataStore {
        return SettingsDataStore(context)
    }
    
    fun provideNetworkManager(context: Context): NetworkManager {
        return NetworkManager(context)
    }
    
    fun provideSubtitleRenderer(): SubtitleRenderer {
        return SubtitleRenderer()
    }
    
    fun provideAudioExtractor(context: Context): AudioExtractor {
        return AudioExtractorImpl(context)
    }
    
    fun provideOfflineSpeechToTextService(context: Context): OfflineSpeechToTextService {
        return OfflineSpeechToTextService(context)
    }
    
    fun provideGoogleSpeechToTextService(context: Context): GoogleSpeechToTextService {
        return GoogleSpeechToTextService(context)
    }
    
    fun provideCompositeSpeechToTextService(context: Context): CompositeSpeechToTextService {
        val networkManager = provideNetworkManager(context)
        val googleService = provideGoogleSpeechToTextService(context)
        val offlineService = provideOfflineSpeechToTextService(context)
        return CompositeSpeechToTextService(googleService, offlineService, networkManager)
    }
    
    fun provideTranslationService(context: Context): TranslationService {
        return GoogleTranslationService(context)
    }
    
    fun provideSubtitleCacheManager(context: Context): SubtitleCacheManager {
        return SubtitleCacheManager(context)
    }
    
    fun provideAISubtitleGenerator(context: Context): AISubtitleGenerator {
        val speechService = provideCompositeSpeechToTextService(context)
        val translationService = provideTranslationService(context) as GoogleTranslationService
        val audioExtractor = provideAudioExtractor(context) as AudioExtractorImpl
        val cacheManager = provideSubtitleCacheManager(context)
        return AISubtitleGeneratorImpl(speechService, translationService, audioExtractor, cacheManager, context)
    }
    
    fun provideGoogleDriveService(context: Context): GoogleDriveService {
        return GoogleDriveService(context)
    }
    
    fun provideDropboxService(context: Context): DropboxService {
        return DropboxService(context)
    }
    
    fun provideCloudStorageManager(context: Context): CloudStorageManager {
        return CloudStorageManager(context)
    }
    
    fun provideOkHttpClient(): okhttp3.OkHttpClient {
        return okhttp3.OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }
    
    fun provideHtmlParser(): HtmlParser {
        return HtmlParserImpl()
    }
    
    fun provideVideoUrlAnalyzer(): VideoUrlAnalyzer {
        val okHttpClient = provideOkHttpClient()
        val htmlParser = provideHtmlParser()
        return VideoUrlAnalyzerImpl(okHttpClient, htmlParser)
    }
    
    fun provideBrowserIntegrationManager(context: Context): BrowserIntegrationManager {
        val urlAnalyzer = provideVideoUrlAnalyzer()
        return BrowserIntegrationManager(context, urlAnalyzer)
    }
    
    fun provideChromecastService(context: Context): ChromecastService {
        return ChromecastServiceImpl(context)
    }
    
    fun provideCastManager(context: Context): CastManager {
        val chromecastService = provideChromecastService(context)
        return CastManager(chromecastService)
    }
    
    fun provideSecurityManager(context: Context): SecurityManager {
        return SecurityManagerImpl(context)
    }
    
    fun provideNetworkConnectivityManager(context: Context): NetworkConnectivityManager {
        return NetworkConnectivityManager(context)
    }
    
    fun provideErrorLogger(context: Context): ErrorLogger {
        return ErrorLogger(context)
    }
    
    fun provideErrorRecoveryManager(context: Context): ErrorRecoveryManager {
        val networkManager = provideNetworkConnectivityManager(context)
        return ErrorRecoveryManagerImpl(context, networkManager)
    }
    
    fun provideCrashHandler(context: Context): CrashHandler {
        val errorLogger = provideErrorLogger(context)
        return CrashHandler(context, errorLogger)
    }
    
    // Database dependencies
    fun provideDatabase(context: Context): AstralVuDatabase {
        return DatabaseModule.provideDatabase(context)
    }
    
    fun provideRecentFilesDao(context: Context): RecentFilesDao {
        return DatabaseModule.provideRecentFilesDao(context)
    }
    
    fun provideRecentFilesRepository(context: Context): RecentFilesRepository {
        val dao = provideRecentFilesDao(context)
        return RecentFilesRepository(dao)
    }
    
    fun providePlaylistDao(context: Context): PlaylistDao {
        return DatabaseModule.providePlaylistDao(context)
    }
    
    fun provideSubtitleDao(context: Context): SubtitleDao {
        return DatabaseModule.provideSubtitleDao(context)
    }
    
    fun provideCloudFileDao(context: Context): CloudFileDao {
        return DatabaseModule.provideCloudFileDao(context)
    }
    
    fun provideCodecPackManager(context: Context): CodecPackManager {
        return CodecPackManager(context)
    }
    
    fun provideDownloadQueueDao(context: Context): DownloadQueueDao {
        return DatabaseModule.provideDownloadQueueDao(context)
    }
    
    fun provideUserPreferenceDao(context: Context): UserPreferenceDao {
        return DatabaseModule.provideUserPreferenceDao(context)
    }
    
    fun providePlaybackHistoryDao(context: Context): PlaybackHistoryDao {
        return DatabaseModule.providePlaybackHistoryDao(context)
    }
}
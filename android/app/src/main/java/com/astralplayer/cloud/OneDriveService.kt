package com.astralplayer.cloud

import android.app.Activity
import android.content.Context
import com.microsoft.graph.authentication.IAuthenticationProvider
import com.microsoft.graph.models.DriveItem
import com.microsoft.graph.requests.GraphServiceClient
import com.microsoft.identity.client.*
import com.microsoft.identity.client.exception.MsalException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.InputStream
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * OneDrive/Microsoft Graph cloud storage service implementation
 */
@Singleton
class OneDriveService @Inject constructor(
    private val context: Context
) : CloudStorageProvider {
    
    companion object {
        private const val CLIENT_ID = "YOUR_AZURE_CLIENT_ID" // Replace with actual client ID
        private const val REDIRECT_URI = "msauth://com.astralplayer.astralstream/YOUR_HASH"
        private val SCOPES = arrayOf("Files.Read", "Files.Read.All", "offline_access")
    }
    
    private var msalApp: IPublicClientApplication? = null
    private var graphClient: GraphServiceClient? = null
    private var currentAccount: IAccount? = null
    
    override val providerName = "OneDrive"
    override val providerId = "onedrive"
    
    init {
        initializeMsal()
    }
    
    /**
     * Initialize MSAL
     */
    private fun initializeMsal() {
        try {
            msalApp = PublicClientApplication.createSingleAccountPublicClientApplication(
                context,
                R.raw.auth_config_single_account
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize MSAL")
        }
    }
    
    /**
     * Authenticate with OneDrive
     */
    override suspend fun authenticate(): Boolean = suspendCancellableCoroutine { continuation ->
        val activity = context as? Activity ?: run {
            continuation.resumeWithException(IllegalStateException("Context must be an Activity"))
            return@suspendCancellableCoroutine
        }
        
        msalApp?.signIn(
            activity,
            "",
            SCOPES,
            object : IAuthenticationCallback {
                override fun onSuccess(authenticationResult: IAuthenticationResult) {
                    currentAccount = authenticationResult.account
                    initializeGraphClient(authenticationResult.accessToken)
                    continuation.resume(true)
                }
                
                override fun onError(exception: MsalException) {
                    Timber.e(exception, "MSAL authentication error")
                    continuation.resume(false)
                }
                
                override fun onCancel() {
                    continuation.resume(false)
                }
            }
        )
    }
    
    /**
     * Check if authenticated
     */
    override suspend fun isAuthenticated(): Boolean = withContext(Dispatchers.IO) {
        try {
            msalApp?.let { app ->
                val accounts = (app as? ISingleAccountPublicClientApplication)?.currentAccount?.currentAccount
                if (accounts != null) {
                    currentAccount = accounts
                    // Try to get token silently
                    val result = app.acquireTokenSilent(
                        AcquireTokenSilentParameters.Builder()
                            .fromAuthority(accounts.authority)
                            .forAccount(accounts)
                            .withScopes(SCOPES.toList())
                            .build()
                    )
                    initializeGraphClient(result.accessToken)
                    true
                } else {
                    false
                }
            } ?: false
        } catch (e: Exception) {
            Timber.e(e, "Failed to check OneDrive authentication")
            false
        }
    }
    
    /**
     * List files in directory
     */
    override suspend fun listFiles(path: String): Flow<List<CloudFile>> = flow {
        ensureAuthenticated()
        
        try {
            val driveItems = if (path.isEmpty() || path == "/") {
                graphClient?.me()?.drive()?.root()?.children()?.buildRequest()?.get()
            } else {
                graphClient?.me()?.drive()?.root()?.itemWithPath(path)?.children()?.buildRequest()?.get()
            }
            
            val files = driveItems?.currentPage?.mapNotNull { item ->
                if (item.folder != null || isVideoFile(item.name ?: "")) {
                    CloudFile(
                        id = item.id ?: "",
                        name = item.name ?: "",
                        path = item.parentReference?.path ?: "",
                        size = item.size ?: 0,
                        modifiedTime = item.lastModifiedDateTime?.time ?: 0,
                        isDirectory = item.folder != null,
                        mimeType = if (item.folder != null) "inode/directory" else getMimeType(item.name ?: ""),
                        downloadUrl = item.additionalDataManager?.get("@microsoft.graph.downloadUrl")?.asString
                    )
                } else null
            } ?: emptyList()
            
            emit(files)
            
            // Handle pagination
            var nextPage = driveItems?.nextPage
            while (nextPage != null) {
                val moreItems = nextPage.buildRequest().get()
                val moreFiles = moreItems?.currentPage?.mapNotNull { item ->
                    if (item.folder != null || isVideoFile(item.name ?: "")) {
                        CloudFile(
                            id = item.id ?: "",
                            name = item.name ?: "",
                            path = item.parentReference?.path ?: "",
                            size = item.size ?: 0,
                            modifiedTime = item.lastModifiedDateTime?.time ?: 0,
                            isDirectory = item.folder != null,
                            mimeType = if (item.folder != null) "inode/directory" else getMimeType(item.name ?: ""),
                            downloadUrl = item.additionalDataManager?.get("@microsoft.graph.downloadUrl")?.asString
                        )
                    } else null
                } ?: emptyList()
                
                emit(files + moreFiles)
                nextPage = moreItems?.nextPage
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to list OneDrive files")
            emit(emptyList())
        }
    }
    
    /**
     * Get download stream for file
     */
    override suspend fun getDownloadStream(fileId: String): InputStream? = withContext(Dispatchers.IO) {
        ensureAuthenticated()
        
        try {
            val driveItem = graphClient?.me()?.drive()?.items(fileId)?.buildRequest()?.get()
            val downloadUrl = driveItem?.additionalDataManager?.get("@microsoft.graph.downloadUrl")?.asString
            
            downloadUrl?.let {
                URL(it).openStream()
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get OneDrive download stream")
            null
        }
    }
    
    /**
     * Get streaming URL for file
     */
    override suspend fun getStreamingUrl(fileId: String): String? = withContext(Dispatchers.IO) {
        ensureAuthenticated()
        
        try {
            val driveItem = graphClient?.me()?.drive()?.items(fileId)?.buildRequest()?.get()
            driveItem?.additionalDataManager?.get("@microsoft.graph.downloadUrl")?.asString
        } catch (e: Exception) {
            Timber.e(e, "Failed to get OneDrive streaming URL")
            null
        }
    }
    
    /**
     * Search for files
     */
    override suspend fun searchFiles(query: String): Flow<List<CloudFile>> = flow {
        ensureAuthenticated()
        
        try {
            val searchResults = graphClient?.me()?.drive()?.search(query)?.buildRequest()?.get()
            
            val files = searchResults?.currentPage?.mapNotNull { item ->
                if (isVideoFile(item.name ?: "")) {
                    CloudFile(
                        id = item.id ?: "",
                        name = item.name ?: "",
                        path = item.parentReference?.path ?: "",
                        size = item.size ?: 0,
                        modifiedTime = item.lastModifiedDateTime?.time ?: 0,
                        isDirectory = false,
                        mimeType = getMimeType(item.name ?: ""),
                        downloadUrl = item.additionalDataManager?.get("@microsoft.graph.downloadUrl")?.asString
                    )
                } else null
            } ?: emptyList()
            
            emit(files)
        } catch (e: Exception) {
            Timber.e(e, "Failed to search OneDrive files")
            emit(emptyList())
        }
    }
    
    /**
     * Logout from OneDrive
     */
    override suspend fun logout() = withContext(Dispatchers.Main) {
        try {
            (msalApp as? ISingleAccountPublicClientApplication)?.signOut(object : ISingleAccountPublicClientApplication.SignOutCallback {
                override fun onSignOut() {
                    currentAccount = null
                    graphClient = null
                    Timber.d("Signed out from OneDrive")
                }
                
                override fun onError(exception: MsalException) {
                    Timber.e(exception, "Error signing out from OneDrive")
                }
            })
        } catch (e: Exception) {
            Timber.e(e, "Failed to logout from OneDrive")
        }
    }
    
    /**
     * Initialize Graph client
     */
    private fun initializeGraphClient(accessToken: String) {
        val authProvider = IAuthenticationProvider { request ->
            request.addHeader("Authorization", "Bearer $accessToken")
        }
        
        graphClient = GraphServiceClient.builder()
            .authenticationProvider(authProvider)
            .buildClient()
    }
    
    /**
     * Ensure authenticated
     */
    private suspend fun ensureAuthenticated() {
        if (graphClient == null) {
            if (!isAuthenticated()) {
                throw IllegalStateException("Not authenticated with OneDrive")
            }
        }
    }
    
    /**
     * Check if file is video
     */
    private fun isVideoFile(fileName: String): Boolean {
        val videoExtensions = listOf("mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "m4v", "mpg", "mpeg", "3gp")
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return extension in videoExtensions
    }
    
    /**
     * Get MIME type for file
     */
    private fun getMimeType(fileName: String): String {
        return when (fileName.substringAfterLast('.', "").lowercase()) {
            "mp4" -> "video/mp4"
            "mkv" -> "video/x-matroska"
            "avi" -> "video/x-msvideo"
            "mov" -> "video/quicktime"
            "webm" -> "video/webm"
            else -> "video/*"
        }
    }
}
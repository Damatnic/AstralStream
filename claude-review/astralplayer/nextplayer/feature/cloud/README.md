# Cloud Storage Integration

This module provides cloud storage integration for Astral Player, allowing users to stream and manage videos from various cloud storage providers.

## Supported Providers

### Google Drive
- Full implementation with authentication
- Video file listing and streaming
- Search functionality
- Direct video streaming support

### Dropbox (Placeholder)
- Basic structure implemented
- Requires Dropbox SDK integration for full functionality

### OneDrive (Placeholder)
- Basic structure implemented
- Requires Microsoft Graph SDK integration for full functionality

## Architecture

### Components

1. **CloudStorageManager**
   - Central manager for all cloud storage operations
   - Handles authentication, file listing, syncing
   - Manages connected accounts and sync status

2. **Provider Services**
   - GoogleDriveService: Implements Google Drive API
   - DropboxService: Placeholder for Dropbox integration
   - OneDriveService: Placeholder for OneDrive integration

3. **CloudStorageViewModel**
   - ViewModel for cloud storage UI
   - Handles UI state and user interactions

4. **CloudStorageActivity**
   - Main activity for cloud storage management
   - Handles authentication callbacks

5. **CloudStorageScreen**
   - Compose UI for cloud storage
   - Shows connected accounts, files, and sync status

## Usage

### Setting up Google Drive

1. Add Google Sign-In configuration to your project
2. Enable Google Drive API in Google Cloud Console
3. Configure OAuth 2.0 credentials

### Basic Usage

```kotlin
// Initialize cloud storage manager
val cloudManager = CloudStorageManager(context)

// Connect to Google Drive
cloudManager.connectAccount(CloudProvider.GOOGLE_DRIVE, activity)

// List video files
val files = cloudManager.syncFiles(CloudProvider.GOOGLE_DRIVE)

// Download a file
cloudManager.downloadFile(cloudFile, localPath)
```

## Dependencies

Add these to your `build.gradle`:

```gradle
// Google Drive API
implementation 'com.google.api-client:google-api-client-android:2.0.0'
implementation 'com.google.apis:google-api-services-drive:v3-rev20240123-2.0.0'
implementation 'com.google.android.gms:play-services-auth:21.0.0'
```

## Future Enhancements

1. **Dropbox Integration**
   - Implement Dropbox OAuth flow
   - Add file listing and download

2. **OneDrive Integration**
   - Implement Microsoft authentication
   - Add Graph API support

3. **Additional Features**
   - Offline file caching
   - Background sync
   - Upload functionality
   - Folder navigation
   - Multi-account support for same provider

## Security Considerations

- OAuth tokens are managed by respective SDKs
- No credentials are stored in the app
- All cloud operations use secure HTTPS connections
- File access is limited to read-only by default
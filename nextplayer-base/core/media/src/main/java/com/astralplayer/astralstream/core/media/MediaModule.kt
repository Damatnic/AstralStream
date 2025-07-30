package com.astralplayer.astralstream.core.media

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.astralplayer.astralstream.core.media.services.LocalMediaService
import com.astralplayer.astralstream.core.media.services.MediaService
import com.astralplayer.astralstream.core.media.sync.LocalMediaInfoSynchronizer
import com.astralplayer.astralstream.core.media.sync.LocalMediaSynchronizer
import com.astralplayer.astralstream.core.media.sync.MediaInfoSynchronizer
import com.astralplayer.astralstream.core.media.sync.MediaSynchronizer
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface MediaModule {

    @Binds
    @Singleton
    fun bindsMediaSynchronizer(
        mediaSynchronizer: LocalMediaSynchronizer,
    ): MediaSynchronizer

    @Binds
    @Singleton
    fun bindsMediaInfoSynchronizer(
        mediaInfoSynchronizer: LocalMediaInfoSynchronizer,
    ): MediaInfoSynchronizer

    @Binds
    @Singleton
    fun bindMediaService(
        mediaService: LocalMediaService,
    ): MediaService
}

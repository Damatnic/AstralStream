package com.astralplayer.astralstream.core.data

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.astralplayer.astralstream.core.data.repository.LocalMediaRepository
import com.astralplayer.astralstream.core.data.repository.LocalPreferencesRepository
import com.astralplayer.astralstream.core.data.repository.MediaRepository
import com.astralplayer.astralstream.core.data.repository.PreferencesRepository

@Module
@InstallIn(SingletonComponent::class)
interface DataModule {

    @Binds
    fun bindsMediaRepository(
        videoRepository: LocalMediaRepository,
    ): MediaRepository

    @Binds
    fun bindsPreferencesRepository(
        preferencesRepository: LocalPreferencesRepository,
    ): PreferencesRepository
}

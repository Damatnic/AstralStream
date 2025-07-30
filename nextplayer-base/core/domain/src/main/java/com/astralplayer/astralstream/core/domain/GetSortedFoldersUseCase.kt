package com.astralplayer.astralstream.core.domain

import com.astralplayer.astralstream.core.common.Dispatcher
import com.astralplayer.astralstream.core.common.NextDispatchers
import com.astralplayer.astralstream.core.data.repository.MediaRepository
import com.astralplayer.astralstream.core.data.repository.PreferencesRepository
import com.astralplayer.astralstream.core.model.Folder
import com.astralplayer.astralstream.core.model.Sort
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn

class GetSortedFoldersUseCase @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val preferencesRepository: PreferencesRepository,
    @Dispatcher(NextDispatchers.Default) private val defaultDispatcher: CoroutineDispatcher,
) {

    operator fun invoke(): Flow<List<Folder>> {
        return combine(
            mediaRepository.getFoldersFlow(),
            preferencesRepository.applicationPreferences,
        ) { folders, preferences ->

            val nonExcludedDirectories = folders.filter {
                it.mediaList.isNotEmpty() && it.path !in preferences.excludeFolders
            }

            val sort = Sort(by = preferences.sortBy, order = preferences.sortOrder)
            nonExcludedDirectories.sortedWith(sort.folderComparator())
        }.flowOn(defaultDispatcher)
    }
}

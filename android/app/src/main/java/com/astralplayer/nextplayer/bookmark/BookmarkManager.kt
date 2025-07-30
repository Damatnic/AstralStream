package com.astralplayer.nextplayer.bookmark

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class BookmarkManager {
    private val _bookmarks = MutableStateFlow<List<Bookmark>>(emptyList())
    val bookmarks: StateFlow<List<Bookmark>> = _bookmarks

    fun addBookmark(bookmark: Bookmark) {
        val currentBookmarks = _bookmarks.value.toMutableList()
        currentBookmarks.add(bookmark)
        _bookmarks.value = currentBookmarks
    }

    fun removeBookmark(bookmark: Bookmark) {
        val currentBookmarks = _bookmarks.value.toMutableList()
        currentBookmarks.remove(bookmark)
        _bookmarks.value = currentBookmarks
    }

    fun getBookmarksForVideo(videoUri: String): List<Bookmark> {
        return _bookmarks.value.filter { it.videoUri == videoUri }
    }

    fun getAllBookmarks() = _bookmarks

    fun getCategories() = MutableStateFlow(listOf("Default", "Favorites"))
    
    fun deleteBookmark(bookmarkId: Long) {
        val currentBookmarks = _bookmarks.value.toMutableList()
        currentBookmarks.removeAll { it.id == bookmarkId }
        _bookmarks.value = currentBookmarks
    }
}
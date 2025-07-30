package com.astralplayer.nextplayer

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.astralplayer.nextplayer.data.database.AstralVuDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Base class for integration tests that provides database and context setup
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
abstract class BaseIntegrationTest {
    
    protected lateinit var database: AstralVuDatabase
    protected lateinit var context: Context
    
    // Test dispatcher for coroutines
    protected val testDispatcher = UnconfinedTestDispatcher()
    
    @Before
    open fun setUp() {
        // Set main dispatcher to test dispatcher
        Dispatchers.setMain(testDispatcher)
        
        // Get application context
        context = ApplicationProvider.getApplicationContext()
        
        // Create in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(
            context,
            AstralVuDatabase::class.java
        )
            .allowMainThreadQueries() // Allow queries on main thread for testing
            .build()
    }
    
    @After
    @Throws(IOException::class)
    open fun tearDown() {
        // Close database
        database.close()
        
        // Reset main dispatcher
        Dispatchers.resetMain()
    }
    
    /**
     * Run a test with the test dispatcher
     */
    protected fun runTest(block: suspend kotlinx.coroutines.test.TestScope.() -> Unit) {
        kotlinx.coroutines.test.runTest(testDispatcher) {
            block()
        }
    }
}
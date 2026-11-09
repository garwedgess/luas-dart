package com.wedgess.luas.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.test.core.app.ApplicationProvider
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import app.cash.turbine.test
import com.wedgess.luas.data.LuasDatabase
import com.wedgess.luas.data.db.dao.SelectedStationDao
import com.wedgess.luas.data.db.luasDatabase
import com.wedgess.luas.data.model.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SelectedStationPersistenceTest {

    private lateinit var context: Context
    private lateinit var driver: SqlDriver
    private val legacy = MutableStateFlow(UserPreferences.getDefaultInstance())

    private val dataStore = object : DataStore<UserPreferences> {
        override val data = legacy
        override suspend fun updateData(transform: suspend (t: UserPreferences) -> UserPreferences): UserPreferences {
            legacy.value = transform(legacy.value)
            return legacy.value
        }
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        driver = AndroidSqliteDriver(LuasDatabase.Schema, context, DB_NAME)
    }

    @After
    fun tearDown() {
        driver.close()
        context.deleteDatabase(DB_NAME)
    }

    private fun repository() = PreferencesRepositoryImpl(dataStore, SelectedStationDao(luasDatabase(driver)))

    @Test
    fun `each line and mode keeps its own selection`() = runTest {
        val repository = repository()

        repository.updateSelectedRedLineStation("Tallaght")
        repository.updateSelectedGreenLineStation("Stillorgan")
        repository.updateSelectedDartStation("Dublin Pearse")

        assertEquals("Tallaght", repository.fetchSelectedRedLineStation().first())
        assertEquals("Stillorgan", repository.fetchSelectedGreenLineStation().first())
        assertEquals("Dublin Pearse", repository.fetchSelectedDartStation().first())
    }

    @Test
    fun `changing one selection never overwrites another mode's selection`() = runTest {
        val repository = repository()
        repository.updateSelectedDartStation("Dublin Pearse")
        repository.updateSelectedGreenLineStation("Stillorgan")

        repository.updateSelectedDartStation("Bray")
        repository.updateSelectedRedLineStation("Tallaght")

        assertEquals("Bray", repository.fetchSelectedDartStation().first())
        assertEquals("Stillorgan", repository.fetchSelectedGreenLineStation().first())
        assertEquals("Tallaght", repository.fetchSelectedRedLineStation().first())
    }

    @Test
    fun `selecting the same station again is idempotent`() = runTest {
        val repository = repository()

        repeat(3) { repository.updateSelectedDartStation("Bray") }

        assertEquals("Bray", repository.fetchSelectedDartStation().first())
    }

    @Test
    fun `selections survive the database and repository being recreated`() = runTest {
        repository().apply {
            updateSelectedDartStation("Bray")
            updateSelectedGreenLineStation("Stillorgan")
        }
        driver.close()

        driver = AndroidSqliteDriver(LuasDatabase.Schema, context, DB_NAME)
        val restored = repository()

        assertEquals("Bray", restored.fetchSelectedDartStation().first())
        assertEquals("Stillorgan", restored.fetchSelectedGreenLineStation().first())
        assertEquals("", restored.fetchSelectedRedLineStation().first())
    }

    @Test
    fun `a selection saved before the move to SQLDelight is used until the user picks again`() = runTest {
        legacy.value = UserPreferences.newBuilder()
            .setSelectedDartStation("Howth")
            .setSelectedGreenLineStation("Sandyford")
            .build()
        val repository = repository()

        assertEquals("Howth", repository.fetchSelectedDartStation().first())
        assertEquals("Sandyford", repository.fetchSelectedGreenLineStation().first())

        repository.updateSelectedDartStation("Bray")

        assertEquals("Bray", repository.fetchSelectedDartStation().first())
        // The other mode's legacy value is untouched by the DART change.
        assertEquals("Sandyford", repository.fetchSelectedGreenLineStation().first())
    }

    @Test
    fun `observers see a new selection`() = runTest {
        val repository = repository()

        repository.fetchSelectedDartStation().test {
            assertEquals("", awaitItem())
            repository.updateSelectedDartStation("Bray")
            assertEquals("Bray", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    private companion object {
        const val DB_NAME = "selection-test.db"
    }
}

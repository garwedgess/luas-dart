package com.wedgess.luas.data.utils.datastore

import androidx.datastore.core.CorruptionException
import com.wedgess.luas.data.model.UserPreferences
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.OutputStream

class UserPreferencesSerializerTest {

    @Test
    fun `defaultValue should be default values`() {
        // Arrange
        val expectedDefault = UserPreferences.newBuilder()
            .setSelectedRedLineStation("")
            .setSelectedGreenLineStation("")
            .setSelectedDartStation("")
            .setLocationPermissionRequested(false)
            .setNotificationPermissionRequested(false)
            .setIgnoreNotificationPermission(false)
            .setIgnoreLocationPermission(false)
            .setTransportType(UserPreferences.Transport.LUAS)
            .build()

        // Act
        val actualDefault = UserPreferencesSerializer.defaultValue

        // Assert
        assertEquals(expectedDefault.selectedRedLineStation, actualDefault.selectedRedLineStation)
        assertEquals(expectedDefault.selectedGreenLineStation, actualDefault.selectedGreenLineStation)
        assertEquals(expectedDefault.selectedDartStation, actualDefault.selectedDartStation)
        assertEquals(expectedDefault.transportType, actualDefault.transportType)
        assertEquals(expectedDefault.locationPermissionRequested, actualDefault.locationPermissionRequested)
        assertEquals(expectedDefault.notificationPermissionRequested, actualDefault.notificationPermissionRequested)
        assertEquals(expectedDefault.ignoreLocationPermission, actualDefault.ignoreLocationPermission)
        assertEquals(expectedDefault.ignoreNotificationPermission, actualDefault.ignoreNotificationPermission)
    }

    @Test
    fun `readFrom() should return UserPreferences when input is valid`() = runTest {
        // Arrange
        val expectedUserPreferences = UserPreferences.newBuilder()
            .setSelectedRedLineStation("MLB")
            .setSelectedGreenLineStation("STS")
            .build()
        val outputStream = ByteArrayOutputStream()
        expectedUserPreferences.writeTo(outputStream)
        val inputStream = ByteArrayInputStream(outputStream.toByteArray())

        // Act
        val actualUserPreferences = UserPreferencesSerializer.readFrom(inputStream)

        // Assert
        assertEquals(expectedUserPreferences, actualUserPreferences)
    }

    @Test
    fun `readFrom() should throw CorruptionException when input is invalid`() = runTest {
        val invalidInputStream = ByteArrayInputStream("invalid".toByteArray())

        // Act & Assert
        val exception = runCatching {
            UserPreferencesSerializer.readFrom(invalidInputStream)
        }.exceptionOrNull()
        assertTrue(exception is CorruptionException)
        assertEquals("Cannot read proto.", exception?.message)
    }

    @Test
    fun `writeTo() should write UserPreferences to output stream`() = runTest {
        // Arrange
        val userPreferences = UserPreferences.newBuilder()
            .setSelectedRedLineStation("MLB")
            .setSelectedGreenLineStation("STS")
            .build()
        val outputStream = ByteArrayOutputStream()

        // Act
        UserPreferencesSerializer.writeTo(userPreferences, outputStream)

        // Assert
        assertEquals(userPreferences, UserPreferences.parseFrom(outputStream.toByteArray()))
    }

    @Test
    fun `writeTo() should throw exception when output stream is invalid`() = runTest {
        // Arrange
        val userPreferences = UserPreferences.newBuilder()
            .setSelectedRedLineStation("MLB")
            .setSelectedGreenLineStation("STS")
            .build()

        val invalidOutputStream = object : OutputStream() {
            override fun write(b: Int): Unit = throw Exception()
            override fun write(b: ByteArray): Unit = throw Exception()
            override fun write(b: ByteArray, off: Int, len: Int): Unit = throw Exception()

            override fun flush(): Unit = throw Exception()
            override fun close(): Unit = throw Exception()
        }

        // Act & Assert
        val exception = runCatching {
            UserPreferencesSerializer.writeTo(userPreferences, invalidOutputStream)
        }.exceptionOrNull()

        assertTrue(exception is Exception)
    }
}

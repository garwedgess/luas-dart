@file:Suppress("DEPRECATION")

package com.wedgess.luas.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpRedirect
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.xml.xml
import nl.adaptivity.xmlutil.serialization.XML
import okhttp3.Cache
import okhttp3.OkHttpClient
import timber.log.Timber
import java.io.File
import javax.inject.Named
import javax.inject.Singleton

private const val XML_INDENTATION = 4
private const val CACHE_SIZE: Long = 10 * 1024 * 1024

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient
        .Builder()
        .build()

    @Named("LuasHttpClient")
    @Provides
    @Singleton
    fun provideLuasHttpClient(
        okHttpClient: OkHttpClient,
        @ApplicationContext context: Context
    ): HttpClient {
        return HttpClient(OkHttp) {
            engine {
                config {
                    cache(Cache(File(context.cacheDir, "ktor"), CACHE_SIZE))
                }
                preconfigured = okHttpClient
            }
            installLuasContentNegotiation()
            installLogging()
            installRedirect()
        }
    }

    @Named("DartHttpClient")
    @Provides
    @Singleton
    fun provideDartHttpClient(
        okHttpClient: OkHttpClient,
        @ApplicationContext context: Context
    ): HttpClient {
        return HttpClient(OkHttp) {
            engine {
                config {
                    cache(Cache(File(context.cacheDir, "ktor"), CACHE_SIZE))
                }
                preconfigured = okHttpClient
            }
            installDartContentNegotiation()
            installLogging()
            installRedirect()
        }
    }

    fun <T : HttpClientEngineConfig> HttpClientConfig<T>.installLuasContentNegotiation() =
        install(ContentNegotiation) {
            xml(createXmlConfig(), contentType = ContentType.Text.Html)
        }


    fun <T : HttpClientEngineConfig> HttpClientConfig<T>.installDartContentNegotiation() =
        install(ContentNegotiation) {
            xml(createXmlConfig(), contentType = ContentType.Text.Xml)
        }

    private fun createXmlConfig(): XML = XML {
        indent = XML_INDENTATION
        autoPolymorphic = false
    }

    private fun <T : HttpClientEngineConfig> HttpClientConfig<T>.installLogging() =
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    Timber.d(message)
                }
            }
            level = LogLevel.ALL
        }

    private fun <T : HttpClientEngineConfig> HttpClientConfig<T>.installRedirect() =
        install(HttpRedirect) {
            checkHttpMethod = false
        }
}

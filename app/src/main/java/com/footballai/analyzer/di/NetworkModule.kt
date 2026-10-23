package com.footballai.analyzer.di

import android.content.Context
import com.footballai.analyzer.data.local.ApiKeyProvider
import com.footballai.analyzer.data.remote.HighlightlyApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = false
        encodeDefaults = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        apiKeyProvider: ApiKeyProvider,
        @ApplicationContext context: Context
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val cacheSize = 20L * 1024L * 1024L // 20 MB
        val cache = Cache(File(context.cacheDir, "http_cache"), cacheSize)

        return OkHttpClient.Builder()
            .cache(cache)
            .addInterceptor { chain ->
                val apiKey = apiKeyProvider.getApiKeyBlocking()
                val original = chain.request()
                val request = original.newBuilder()
                    .header("x-rapidapi-key", apiKey)
                    .method(original.method, original.body)
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://soccer.highlightly.net/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun provideHighlightlyApi(retrofit: Retrofit): HighlightlyApi {
        return retrofit.create(HighlightlyApi::class.java)
    }
}

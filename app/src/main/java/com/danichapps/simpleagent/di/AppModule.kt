package com.danichapps.simpleagent.di

import com.danichapps.simpleagent.BuildConfig
import com.danichapps.simpleagent.data.remote.OllamaService
import com.danichapps.simpleagent.data.remote.OpenAiService
import com.danichapps.simpleagent.data.remote.RagService
import com.danichapps.simpleagent.data.repository.ChatRepositoryImpl
import com.danichapps.simpleagent.data.repository.RagRepositoryImpl
import com.danichapps.simpleagent.domain.repository.ChatRepository
import com.danichapps.simpleagent.domain.repository.RagRepository
import com.danichapps.simpleagent.domain.usecase.ExtractTaskStateUseCase
import com.danichapps.simpleagent.domain.usecase.SendMessageUseCase
import com.danichapps.simpleagent.presentation.ChatViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

private const val OPENAI_TIMEOUT_MS = 120_000L

val appModule = module {

    single(named("openai")) {
        HttpClient(OkHttp) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            install(Logging) { level = LogLevel.BODY }
            install(HttpTimeout) {
                requestTimeoutMillis = OPENAI_TIMEOUT_MS
                socketTimeoutMillis = OPENAI_TIMEOUT_MS
                connectTimeoutMillis = OPENAI_TIMEOUT_MS
            }
            defaultRequest {
                headers.append(HttpHeaders.Authorization, "Bearer ${BuildConfig.OPENAI_API_KEY}")
            }
        }
    }

    single(named("rag")) {
        HttpClient(OkHttp) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            install(Logging) { level = LogLevel.BODY }
            install(HttpTimeout) {
                requestTimeoutMillis = OPENAI_TIMEOUT_MS
                socketTimeoutMillis = OPENAI_TIMEOUT_MS
                connectTimeoutMillis = OPENAI_TIMEOUT_MS
            }
        }
    }

    single<ChatRepository>(named("openai")) { ChatRepositoryImpl(OpenAiService(get(named("openai")))) }
    single<ChatRepository>(named("ollama")) { ChatRepositoryImpl(OllamaService(get(named("rag")))) }

    single { RagService(get(named("rag"))) }

    single<RagRepository> { RagRepositoryImpl(get()) }

    single { SendMessageUseCase(get()) }
    single { ExtractTaskStateUseCase() }
    viewModel { ChatViewModel(get(named("openai")), get(named("ollama")), get(), get()) }
}

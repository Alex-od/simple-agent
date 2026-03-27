package com.danichapps.simpleagent.di

import com.danichapps.simpleagent.BuildConfig
import com.danichapps.simpleagent.data.local.ChatModeStore
import com.danichapps.simpleagent.data.local.ChatTuningSettingsStore
import com.danichapps.simpleagent.data.local.EmbeddingModelSelectionManager
import com.danichapps.simpleagent.data.local.LocalServerSettingsStore
import com.danichapps.simpleagent.data.local.ModelSelectionManager
import com.danichapps.simpleagent.data.remote.DeviceModelPathResolver
import com.danichapps.simpleagent.data.remote.EmbeddingModelPathResolver
import com.danichapps.simpleagent.data.remote.ChatService
import com.danichapps.simpleagent.data.remote.LlamaCppEmbeddingService
import com.danichapps.simpleagent.data.remote.LlamaCppNative
import com.danichapps.simpleagent.data.remote.LocalServerProbe
import com.danichapps.simpleagent.data.remote.OpenAiCompatibleChatService
import com.danichapps.simpleagent.data.remote.OnDeviceLlamaCppService
import com.danichapps.simpleagent.data.local.GemmaRerankService
import com.danichapps.simpleagent.data.local.LocalRagChunksDataSource
import com.danichapps.simpleagent.data.local.RagFolderPreferences
import com.danichapps.simpleagent.data.remote.RagService
import com.danichapps.simpleagent.data.repository.ChatRepositoryImpl
import com.danichapps.simpleagent.data.repository.LocalRagRepositoryImpl
import com.danichapps.simpleagent.data.repository.RagRepositoryImpl
import com.danichapps.simpleagent.domain.repository.ChatRepository
import com.danichapps.simpleagent.domain.repository.RagRepository
import com.danichapps.simpleagent.domain.usecase.ExtractTaskStateUseCase
import com.danichapps.simpleagent.domain.usecase.OfflineSendMessageUseCase
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
import org.koin.android.ext.koin.androidContext
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

    single(named("local_server")) {
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

    single { ChatModeStore(androidContext()) }
    single { LocalServerSettingsStore(androidContext()) }
    single<ChatService>(named("openai_chat_service")) {
        OpenAiCompatibleChatService(get(named("openai"))) {
            com.danichapps.simpleagent.data.remote.ChatEndpointConfig(
                baseUrl = "https://api.openai.com/v1",
                model = "gpt-4o-mini"
            )
        }
    }
    single<ChatService>(named("local_server_chat_service")) {
        val settingsStore: LocalServerSettingsStore = get()
        OpenAiCompatibleChatService(get(named("local_server"))) {
            val settings = settingsStore.load()
            com.danichapps.simpleagent.data.remote.ChatEndpointConfig(
                baseUrl = settings.baseUrl,
                model = settings.model
            )
        }
    }
    single { LocalServerProbe(get(named("local_server"))) }
    single<ChatRepository>(named("openai")) { ChatRepositoryImpl(get(named("openai_chat_service"))) }
    single { ModelSelectionManager(androidContext()) }
    single { EmbeddingModelSelectionManager(androidContext()) }
    single { DeviceModelPathResolver(androidContext(), get()) }
    single { EmbeddingModelPathResolver(androidContext(), get()) }
    single { LlamaCppNative() }
    single { OnDeviceLlamaCppService(get(), get()) }
    single<ChatRepository>(named("ondevice")) { ChatRepositoryImpl(get<OnDeviceLlamaCppService>()) }
    single<ChatRepository>(named("local_server")) { ChatRepositoryImpl(get(named("local_server_chat_service"))) }

    single { RagService(get(named("rag"))) }

    single { RagFolderPreferences(androidContext()) }
    single { ChatTuningSettingsStore(androidContext()) }
    single { LocalRagChunksDataSource(androidContext(), get()) }
    single { LlamaCppEmbeddingService(get(), get()) }
    single { GemmaRerankService() }

    single<RagRepository>(named("remote")) { RagRepositoryImpl(get()) }
    single<RagRepository>(named("local")) { LocalRagRepositoryImpl(get(), get(), androidContext(), get()) }

    single<SendMessageUseCase>(named("online")) { SendMessageUseCase(get(named("remote"))) }
    single<OfflineSendMessageUseCase> { OfflineSendMessageUseCase(get(named("local"))) }

    single { ExtractTaskStateUseCase() }
    viewModel {
        ChatViewModel(
            get(named("openai")),
            get(named("ondevice")),
            get(named("local_server")),
            get<OnDeviceLlamaCppService>(),
            get<LocalServerProbe>(),
            get<LlamaCppEmbeddingService>(),
            get(named("online")),
            get(),
            get(),
            get(named("local")),
            get(),
            get(),
            get(),
            get(),
            get(),
            get()
        )
    }
}

package com.danichapps.simpleagent.di

import com.danichapps.simpleagent.BuildConfig
import com.danichapps.simpleagent.data.AppPreferences
import com.danichapps.simpleagent.data.remote.FileLogger
import com.danichapps.simpleagent.data.remote.OllamaService
import com.danichapps.simpleagent.data.remote.OpenAiService
import com.danichapps.simpleagent.data.remote.ProjectContextService
import com.danichapps.simpleagent.data.remote.RagService
import com.danichapps.simpleagent.data.remote.ReviewService
import com.danichapps.simpleagent.data.remote.FilesService
import com.danichapps.simpleagent.data.remote.SupportService
import com.danichapps.simpleagent.data.repository.ChatRepositoryImpl
import com.danichapps.simpleagent.data.repository.ProjectContextRepositoryImpl
import com.danichapps.simpleagent.data.repository.RagRepositoryImpl
import com.danichapps.simpleagent.domain.repository.ChatRepository
import com.danichapps.simpleagent.domain.repository.ProjectContextRepository
import com.danichapps.simpleagent.domain.repository.RagRepository
import com.danichapps.simpleagent.domain.usecase.BuildHelpContextUseCase
import com.danichapps.simpleagent.domain.usecase.BuildHelpPromptUseCase
import com.danichapps.simpleagent.domain.usecase.CommandRouterUseCase
import com.danichapps.simpleagent.domain.usecase.ExtractTaskStateUseCase
import com.danichapps.simpleagent.domain.usecase.SendMessageUseCase
import com.danichapps.simpleagent.presentation.ChatViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.HttpResponseValidator
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

private fun createLoggedHttpClient(
    fileLogger: FileLogger,
    includeAuthorizationHeader: Boolean = false
): HttpClient {
    return HttpClient(OkHttp) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        install(Logging) {
            logger = fileLogger
            level = LogLevel.BODY
            sanitizeHeader { header -> header == HttpHeaders.Authorization }
        }
        install(HttpTimeout) {
            requestTimeoutMillis = OPENAI_TIMEOUT_MS
            socketTimeoutMillis = OPENAI_TIMEOUT_MS
            connectTimeoutMillis = OPENAI_TIMEOUT_MS
        }
        HttpResponseValidator {
            validateResponse { response ->
                fileLogger.logEvent(
                    "HTTP ${response.status.value} ${response.call.request.method.value} ${response.call.request.url}"
                )
            }
            handleResponseExceptionWithRequest { cause, request ->
                fileLogger.logEvent(
                    "HTTP failure ${request.method.value} ${request.url}: ${cause.message ?: cause::class.java.simpleName}"
                )
            }
        }
        if (includeAuthorizationHeader) {
            defaultRequest {
                headers.append(HttpHeaders.Authorization, "Bearer ${BuildConfig.OPENAI_API_KEY}")
            }
        }
    }
}

val appModule = module {

    single { AppPreferences(androidContext()) }
    single { FileLogger(androidContext()) }

    single(named("openai")) {
        createLoggedHttpClient(get<FileLogger>(), includeAuthorizationHeader = true)
    }

    single(named("rag")) {
        createLoggedHttpClient(get<FileLogger>())
    }

    single<ChatRepository>(named("openai")) { ChatRepositoryImpl(OpenAiService(get(named("openai")))) }
    single<ChatRepository>(named("ollama")) { ChatRepositoryImpl(OllamaService(get(named("rag")))) }

    single { RagService(get(named("rag"))) }
    single { ReviewService(get(named("rag"))) }
    single { SupportService(get(named("rag"))) }
    single { FilesService(get(named("rag"))) }
    single { ProjectContextService(get(named("rag"))) }

    single<RagRepository> { RagRepositoryImpl(get()) }
    single<ProjectContextRepository> { ProjectContextRepositoryImpl(get()) }

    single { BuildHelpContextUseCase(get(), get()) }
    single { BuildHelpPromptUseCase() }
    single { CommandRouterUseCase(get(), get(), get(), get(), get()) }
    single { SendMessageUseCase(get()) }
    single { ExtractTaskStateUseCase() }
    viewModel { ChatViewModel(get(named("openai")), get(named("ollama")), get(), get(), get(), get()) }
}

# Project Structure

## Overview

SimpleAgent consists of two main runtime parts:

- `app` - Android client with chat UI and command handling.
- `rag-server` - backend for document indexing, semantic retrieval, and project context.

## Android app

### Presentation

- `presentation/ChatScreen.kt` - thin Compose screen that collects state from `ChatViewModel`.
- `presentation/ChatView.kt` - stateless-ish UI layer for chat messages, settings drawer, and input.
- `presentation/ChatViewModel.kt` - orchestrates message sending, loading state, error state, and toggles.

### Domain

- `domain/usecase/SendMessageUseCase.kt` - enriches prompts with RAG context and delegates to chat repositories.
- `domain/usecase/ExtractTaskStateUseCase.kt` - derives task memory from message history.
- `domain/usecase/CommandRouterUseCase.kt` - routes `/help` into a dedicated project-aware flow.
- `domain/usecase/BuildHelpContextUseCase.kt` - collects RAG docs and live project context.
- `domain/usecase/BuildHelpPromptUseCase.kt` - builds the final system/user prompt for `/help`.
- `domain/repository/*` - abstractions for chat, RAG, and project context.

### Data

- `data/repository/ChatRepositoryImpl.kt` - adapter from domain messages to transport DTOs.
- `data/repository/RagRepositoryImpl.kt` - adapter to RAG search.
- `data/repository/ProjectContextRepositoryImpl.kt` - adapter to project context API.
- `data/remote/*` - Ktor services for OpenAI, Ollama, RAG, and project context.

### Dependency injection

- `di/AppModule.kt` - Koin wiring for repositories, services, and use cases.

## RAG server

### Chat APIs

- `chat/ChatController.kt`
- `service/RagService.kt`

### Admin APIs

- `admin/AdminRagController.kt`
- `admin/AdminStatusController.kt`
- `admin/AdminLlmController.kt`
- `admin/AdminEmbeddingController.kt`

### Project context APIs

- `project/ProjectContextController.kt`
- `project/ProjectGitService.kt`

## Documentation strategy

The assistant should answer project questions from indexed docs first.

Primary sources:

- `README.md`
- `docs/*.md`
- `project/docs/**/*` when present
- `rag_files/*` for extra specifications and imported documents

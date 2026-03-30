# SimpleAgent

SimpleAgent is an Android developer assistant with a Compose client and a local RAG server.

## Modules

- `app` - Android client built with Jetpack Compose, Koin, Coroutines, and Ktor.
- `rag-server` - Spring Boot service for document indexing, semantic search, and project context APIs.
- `docs` - project documentation indexed by RAG for `/help`.
- `rag_files` - extra local reference documents, including `.docx` files.
- `tools` - local tooling, including the project MCP server.

## Main flows

### Chat

1. The user sends a message from the Android app.
2. `ChatViewModel` delegates message preparation to domain use cases.
3. `SendMessageUseCase` optionally enriches the prompt with RAG context.
4. The selected `ChatRepository` sends the request to OpenAI or Ollama.

### `/help`

`/help` is a project-aware command for developer questions.

- It fetches project context from the backend.
- It forces documentation lookup through RAG.
- It answers using `README.md`, files in `docs/`, and indexed project documents.

Examples:

- `/help`
- `/help как устроен проект`
- `/help какие есть API у rag-server`

## RAG document sources

The RAG server indexes supported documentation files recursively:

- root `README*.md`
- files inside `docs/`
- files inside `project/docs/`
- files inside `rag_files/`
- supported extensions: `.md`, `.docx`

## Project context

The backend exposes minimal project context for the assistant:

- current git branch
- changed files from `git status --short`

The same information is also available through the local MCP server configured in `.mcp.json`.

## Local run

### Android app

1. Put secrets in `local.properties`.
2. Run the `app` module from Android Studio.

### RAG server

1. Start local dependencies such as Ollama and Qdrant.
2. Run `rag-server`.
3. Trigger indexing for the project docs through the admin API or UI.

## Key project docs

- `docs/project-structure.md`
- `docs/api.md`
- `docs/help-command.md`

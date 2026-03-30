# `/help` command

## Goal

`/help` turns the assistant into a project-aware developer helper.

It is intended for questions such as:

- project structure
- module responsibilities
- available backend APIs
- where a feature should be implemented
- what branch is currently active

## Behavior

1. The command parser strips the `/help` prefix.
2. If the user did not add a question, the assistant asks for a high-level project overview.
3. The app fetches current project context from the backend.
4. The app forces RAG lookup over project docs.
5. The LLM answers only within the scope of this project.

## Examples

- `/help`
- `/help где находится логика RAG`
- `/help какие файлы отвечают за чат`

## Limitations

- Answers depend on indexed docs being up to date.
- Git context is read-only and currently limited to branch and changed files.
- If the backend is offline, `/help` can fall back to docs already indexed in RAG but will lose live git context.

# API Notes

## Android client -> RAG server

### `POST /api/v1/chat/rag/search`

Used by the Android app to fetch semantic context for user prompts.

Request:

```json
{
  "query": "как устроен help",
  "top_k": 3
}
```

Response:

```json
{
  "results": [
    {
      "text": "chunk text",
      "score": 0.82,
      "source_file": "README.md",
      "chunk_index": 0
    }
  ]
}
```

### `GET /api/v1/project/context`

Used by `/help` to enrich project-specific answers.

Response fields:

- `branch` - current git branch
- `changedFiles` - files from `git status --short`
- `gitRepository` - whether the target path is a git repository
- `projectRoot` - resolved project root used by the backend

## Admin endpoints

### `GET /api/v1/admin/rag/documents-path`

Returns the current documents directory and file statistics.

### `PUT /api/v1/admin/rag/documents-path`

Sets the directory used for indexing.

### `POST /api/v1/admin/rag/indexing`

Starts indexing for the configured documents path.

### `GET /api/v1/admin/rag/indexing/status`

Returns indexing progress and final counts.

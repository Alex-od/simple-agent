# Этап 3: LocalRagRepositoryImpl — интеграция reranker

## Задача
Обновить LocalRagRepositoryImpl: cosine search возвращает List<ScoredChunk>, передаётся в rerankService.

## Файлы и изменения
```
LocalRagRepositoryImpl.kt
  +-ИЗМЕНИТЬ constructor — добавить rerankService: GemmaRerankService
  +-ИЗМЕНИТЬ searchContext()
             — cosine map → ScoredChunk(chunk, similarity)
             — передать в rerankService.rerank(query, scoredChunks)
             — вернуть List<RagChunk>
```

## Критерии успеха
- [ ] Компилируется без ошибок
- [ ] searchContext возвращает не более POST_RANK_K чанков
- [ ] topK параметр интерфейса игнорируется (управляется константами)

## Статус
✅ Завершён

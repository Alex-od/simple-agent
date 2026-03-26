# Этап 2: GemmaRerankService

## Задача
Создать GemmaRerankService с полным pipeline reranking включая все fallback.

## Файлы и изменения
```
GemmaRerankService.kt  (новый, data/local/)
  +ДОБАВИТЬ  const PRE_RANK_K = 8
  +ДОБАВИТЬ  const POST_RANK_K = 5
  +ДОБАВИТЬ  const SIMILARITY_THRESHOLD = 0.3f
  +ДОБАВИТЬ  const CHUNK_PREVIEW = 250
  +ДОБАВИТЬ  class GemmaRerankService(llmService, parser)
  +ДОБАВИТЬ  suspend fun rerank(query, candidates: List<ScoredChunk>): List<RagChunk>
```

## Pipeline внутри rerank()
1. filter(similarity > THRESHOLD)
2. if filtered.isEmpty() → fallback: candidates.sortedByDescending { similarity }.take(POST_RANK_K).map { chunk }
3. take(min(filtered.size, PRE_RANK_K))
4. if filtered.size <= POST_RANK_K → skip LLM: filtered.sortedByDescending { similarity }.take(POST_RANK_K).map { chunk }
5. обрезать каждый чанк до CHUNK_PREVIEW символов
6. строит strict-промпт
7. llmService.sendMessages() — jsonMode=false
8. parser.parse(response, POST_RANK_K, filtered.size) → List<Int> (1-based)
9. if parse пустой → filtered.sortedByDescending { similarity }.take(POST_RANK_K).map { chunk }
10. if LLM не ready (llmInference == null) → candidates.sortedByDescending { similarity }.take(POST_RANK_K).map { chunk }
11. маппинг индексов (1-based) → chunks

## Strict-промпт
```
Выбери не более 5 САМЫХ релевантных фрагментов к вопросу.

Правила:
- не более 5 номеров
- только цифры через запятую
- без объяснений
- игнорируй нерелевантные фрагменты

Вопрос: [query]

Passage 1: [первые 250 символов]
...
Passage N: [первые 250 символов]

Ответ:
```

## Критерии успеха
- [ ] Компилируется без ошибок
- [ ] При LLM not ready → возвращает cosine top-5 без crash
- [ ] При filtered.isEmpty() → возвращает cosine top-5
- [ ] При filtered.size ≤ 5 → не вызывает LLM

## Статус
✅ Завершён

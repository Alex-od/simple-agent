# План: LLM Reranker (Gemma3 batch) + top-K
Дата: 2026-03-26

## Описание задачи
Добавить LLM reranker на базе Gemma3 (batch режим) и настраиваемый top-K в офлайн RAG пайплайн.

## Финальный pipeline
```
Query → embed → cosine similarity
  → filter(similarity > 0.3)
  → if empty → fallback(cosine top-5)
  → if size ≤ 5 → skip LLM, return sorted
  → take(min(size, 8))
  → Gemma batch rerank (1 LLM вызов)
  → parse + normalize(1..N, distinct, take 5)
  → if empty → fallback(sorted cosine)
  → top-5 → промпт → Gemma → ответ
```

## Константы
- PRE_RANK_K = 8
- POST_RANK_K = 5
- SIMILARITY_THRESHOLD = 0.3f
- CHUNK_PREVIEW = 250

## Этапы
| № | Название | Файл | Статус |
|---|----------|------|--------|
| 1 | ScoredChunk + RerankParser | stage-1.md | ✅ |
| 2 | GemmaRerankService | stage-2.md | ✅ |
| 3 | LocalRagRepositoryImpl — интеграция | stage-3.md | ✅ |
| 4 | Фикс: Streaming + RAG | stage-4.md | ✅ |
| 5 | DI — AppModule | stage-5.md | ✅ |

## Прогресс
5 / 5 этапов завершено

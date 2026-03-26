# План: Офлайн RAG с локальной LLM Gemma3
Дата: 2026-03-25

## Описание задачи
Реализовать RAG полностью на устройстве в офлайн-режиме.
- Документ: IEEE-830-1998_RU.docx (один, бандлится в assets)
- Embedding-модель: любая .tflite совместимая с MediaPipe TextEmbedder (adb push)
  Рекомендуется: mobilebert_embedding_with_metadata.tflite (~25 МБ) или use_multilingual.tflite (~68 МБ)
- LLM: Gemma3 (уже реализована через MediaPipe tasks-genai)
- Поиск: семантический (cosine similarity по float-эмбеддингам)
- Чанкинг: портирован с RAG-сервера (500 слов, 100 слов overlap)

## Технические решения
- DOCX → ZipInputStream + XmlPullParser (без Apache POI)
- Чанки: вычисляются один раз при первом запуске → кэш в filesDir/rag_chunks.json
- Эмбеддинги чанков: вычисляются один раз → кэш в filesDir/rag_embeddings.bin (DataOutput/InputStream)
- Эмбеддинг запроса: on-demand при каждом запросе
- Embedding модель: файл embedding_model.tflite в ExternalFilesDir (adb push)
- Lazy init TextEmbedder: создаётся при первом вызове embed()
- Если модель нет → RAG возвращает emptyList(), ошибки не бросает
- DI: named("remote"/"local") RagRepository + named("online"/"offline") SendMessageUseCase

## Этапы
| № | Название | Файл | Статус |
|---|----------|------|--------|
| 1 | DOCX в assets + DocxTextExtractor + LocalRagChunksDataSource | stage-1.md | ✅ |
| 2 | Зависимости + LocalEmbeddingService + ModelDownloadManager | stage-2.md | ✅ |
| 3 | LocalRagRepositoryImpl + DI | stage-3.md | ✅ |
| 4 | ChatViewModel — интеграция офлайн RAG | stage-4.md | ✅ |

## Прогресс
4 / 4 этапов завершено

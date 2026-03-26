# Offline RAG + Gemma 3 1B — Validation Report
Дата: 2026-03-26
Устройство: SM-M515F / RZ8R126899A (Snapdragon 730G, Adreno 618, 6GB RAM, Android 12)
Ветка: `ondevice_llm`

## Статус: PASSED (с известными ограничениями)

---

## Что реализовано

| Компонент | Файл | Статус |
|---|---|---|
| LocalRagChunksDataSource (DOCX → чанки) | `data/local/LocalRagChunksDataSource.kt` | Новый |
| LocalEmbeddingService (on-device embedding) | `data/local/LocalEmbeddingService.kt` | Новый |
| BM25Scorer (keyword scoring) | `data/local/BM25Scorer.kt` | Новый |
| GemmaRerankService (threshold filter + top-K) | `data/local/GemmaRerankService.kt` | Новый |
| DocxTextExtractor | `data/local/DocxTextExtractor.kt` | Новый |
| LocalRagRepositoryImpl (hybrid BM25+cosine) | `data/repository/LocalRagRepositoryImpl.kt` | Новый |
| OnDeviceLlmService (async API, callbackFlow) | `data/remote/OnDeviceLlmService.kt` | Обновлён |
| SendMessageUseCase (анти-галлюцинация промпт) | `domain/usecase/SendMessageUseCase.kt` | Обновлён |
| ChatViewModel (offline flow без extractTask) | `presentation/ChatViewModel.kt` | Обновлён |
| AppModule DI (local RAG, BM25, embedding) | `di/AppModule.kt` | Обновлён |

---

## Краши — диагностика и фиксы

### 1. SIGSEGV в DefaultDispatch (`fault addr 0x7200000008`)
**Причина:** Два `LlmInferenceSession` на один `LlmInference` — сначала для LLM reranker, потом для ответа. Двойной `generateResponse()` → SIGSEGV.
**Фикс:** Удалён LLM reranker из `GemmaRerankService`. Теперь — cosine-only threshold filter + top-K. Один LLM-вызов на запрос.

### 2. JNI `NewByteArray` crash в `nativePredictSync`
**Stack:** `LlmTaskRunner.nativePredictSync → llm_executor_calculator.cc:288 → NewByteArray`
**Причина:** Синхронный `generateResponse()` пытается аллоцировать весь ответ в один `ByteArray` → JNI OOM на SM-M515F.
**Фикс:** `sendMessages()` переписан через `sendMessagesStreaming().toList()` — использует async API с `callbackFlow`.

### 3. Зависание progress bar (isLoading не сбрасывается)
**Причина:** `extractTaskStateUseCase` в `sendOffline()` запускал второй `LlmInferenceSession` → SIGSEGV/hang → процесс убивался до выполнения `finally`.
**Фикс:** `extractTaskStateUseCase` удалён из `sendOffline()`. Task State extraction работает только в онлайн-режиме.

### 4. Пустой ответ (answer = "")
**Причина:** `CHUNK_SIZE_WORDS=500`, `POST_RANK_K=5` → ~4000+ токенов входа → `MAX_TOKENS=4096` исчерпан → Gemma немедленно генерирует `<end_of_turn>` → пустая строка.
**Фикс:** `POST_RANK_K=2`, `it.text.take(600)` на чанк — ~400-500 токенов контекста.

---

## E2E результаты (SM-M515F)

| Шаг | Описание | Результат |
|---|---|---|
| 1 | Запуск приложения | PASS |
| 2 | Открыть меню → включить OFFLINE + RAG | PASS |
| 3 | Модель уже скачана → автоинициализация | PASS (Initializing → Ready) |
| 4 | Отправить запрос "разделы в srs" | PASS (без краша, без зависания) |
| 5 | RAG находит 2 источника | PASS (IEEE-830-1998_RU.docx #1, #20) |
| 6 | Ответ генерируется без галлюцинаций | PASS ("В документе информация не найдена.") |
| 7 | isLoading сбрасывается после ответа | PASS |
| 8 | Повторный запрос работает | PASS |

---

## Качество retrieval — результаты

### Гибридный поиск BM25 + cosine

**Реализация:**
```
hybrid_score = cosine * 0.6 + bm25_normalized * 0.4
threshold = 0.2
top_K = 2
```

**Наблюдения:**
- До BM25: retrieval возвращал chunk #1 (general intro) и chunk #3 (список авторов) для любого запроса
- После BM25: retrieval начал возвращать чанки с keyword overlap (#20 содержит релевантный контент об объектах/атрибутах)
- Запрос "разделы в srs" всё ещё не находит чанк с реальным оглавлением — embedding модель даёт высокую схожесть нерелевантным чанкам

---

## Известные ограничения

### 1. Качество on-device embedding для русского языка
**Серьёзность:** High (влияет на полезность RAG)
**Описание:** Embedding модель для русского семантического поиска работает слабо. Запросы типа "разделы в srs" не retrieval'ят чанки с реальным оглавлением документа.
**Проявление:** Модель честно отвечает "В документе информация не найдена" — anti-hallucination работает корректно, но полезный контент не находится.
**Workaround:** Keyword-heavy запросы (с точными словами из документа) работают лучше через BM25.

### 2. Gemma 3 1B hallucination при слабом контексте
**Серьёзность:** Medium (решено частично)
**Описание:** Без жёсткого промпта Gemma 3 1B генерирует правдоподобную ложь вместо "не знаю".
**Фикс:** Добавлена инструкция: "Отвечай ТОЛЬКО на основе контекста. Если не найдено — 'В документе информация не найдена.'"
**Статус:** FIXED — проверено на устройстве.

### 3. Task State extraction отключён в офлайн-режиме
**Серьёзность:** Low (feature degradation)
**Описание:** `extractTaskStateUseCase` требует второй LLM-вызов, что вызывает краш на SM-M515F. Отключён в `sendOffline()`.
**Влияние:** В офлайн-режиме нет накопления Task Memory (цели, ограничения, уточнения между сообщениями).

### 4. Пустой ответ при коротких запросах (edge case)
**Серьёзность:** Low
**Описание:** Запрос "srs" (3 символа) вернул пустой ответ. Возможно, контекст + короткий запрос → Gemma генерирует EOS немедленно.
**Статус:** Не исследован подробно.

---

## Архитектура offline RAG pipeline

```
Query
  └─> LocalEmbeddingService.embed(query)           # on-device embedding
  └─> BM25Scorer.score(query, docIndex)             # keyword scoring
  └─> hybrid = 0.6*cosine + 0.4*bm25_normalized    # fusion
  └─> GemmaRerankService.rerank()                   # threshold=0.2, top-K=2
  └─> chunks.text.take(600) → system prompt         # context injection
  └─> OnDeviceLlmService.sendMessages()             # Gemma 3 1B INT4
       └─> sendMessagesStreaming().toList()          # async → sync bridge
       └─> callbackFlow + generateResponseAsync()   # MediaPipe async API
       └─> EOS detection "<end_of_turn>"            # stop generation
```

---

## Производительность (SM-M515F)

| Метрика | Значение |
|---|---|
| Устройство | SM-M515F (Snapdragon 730G, Adreno 618) |
| LLM модель | Gemma 3 1B IT INT4 (~554MB .task) |
| Embedding модель | nomic-embed-text (on-device) |
| Backend | GPU (OpenCL) |
| Время ответа (offline+RAG) | ~15–40 секунд |
| RAM при загруженных обеих моделях | ~1.4GB |
| Embedding cache | disk (rag_embeddings.bin) — вычисляется один раз |
| BM25 corpus | in-memory — инициализируется при первом запросе |

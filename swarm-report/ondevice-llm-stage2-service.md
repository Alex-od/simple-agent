# Этап 2 — OnDeviceLlmService + ModelDownloadManager
**Статус:** ⬜ Не начат (зависит от Этапа 1)

## Задача
Создать два новых класса:
- `OnDeviceLlmService` — реализует ChatService через MediaPipe LlmInference (GPU + CPU fallback, streaming, Gemma chat template)
- `ModelDownloadManager` — скачивает файл модели (~800MB) с HTTP Range resume, поддерживает выбор хранилища

## Шаги

### 2.1 Уточнить точный API tasks-genai:0.10.32
- [ ] Декомпилировать/изучить library: точные имена классов, enum Backend, сигнатуру streaming callback
- [ ] Зафиксировать точный API перед написанием кода

### 2.2 OnDeviceLlmService.kt (НОВЫЙ файл)
- [ ] `initialize()` — GPU-first, catch → CPU fallback
- [ ] `formatPrompt(messages, jsonMode)` — Gemma 3 IT chat template:
  ```
  <start_of_turn>user
  {system если есть}

  {user message}<end_of_turn>
  <start_of_turn>model
  {assistant response}<end_of_turn>
  ...
  <start_of_turn>model
  ```
- [ ] jsonMode: добавить в начало system "Respond ONLY with valid JSON. No extra text or markdown."
- [ ] `sendMessages()` — блокирующий `generateResponse()` в `Dispatchers.IO`
- [ ] `sendMessagesStreaming()` — `callbackFlow` + `generateResponseAsync()` + `awaitClose()`
- [ ] `release()` — `llmInference?.close()`

### 2.3 ModelDownloadManager.kt (НОВЫЙ файл)
- [ ] `MODEL_URL` константа (Google CDN или HuggingFace)
- [ ] `getStorageOptions()` — внутренняя память (filesDir) + SD если `getExternalFilesDirs(null).size > 1`
- [ ] `selectedStoragePath` — сохранять/читать из SharedPreferences
- [ ] `isModelDownloaded()` — проверка существования файла
- [ ] `downloadModel(targetDir)` — `Flow<DownloadState>`:
  - HTTP Range resume: если файл частичный → `Range: bytes=X-`
  - Буферизованная запись, эмит прогресса каждые 1MB
  - При ошибке → `DownloadState.Error`
- [ ] `modelPath` — полный путь к файлу

## Критерии успеха
- `OnDeviceLlmService` компилируется, методы реализованы
- `ModelDownloadManager` компилируется, логика resume корректна
- Форматирование промпта покрыто тестом (unit)

## Результат
После этапа: оба класса готовы к подключению в DI.

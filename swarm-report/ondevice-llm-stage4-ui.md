# Этап 4 — JSON fallback + ViewModel streaming + UI
**Статус:** ⬜ Не начат (зависит от Этапа 3)

## Задача
Обновить ExtractTaskStateUseCase (JSON fallback), ChatViewModel (streaming + ModelState + lifecycle), ChatView/ChatScreen (диалог выбора хранилища, прогресс-бар, streaming UI).

## Шаги

### 4.1 ExtractTaskStateUseCase.kt
- [ ] Обернуть `json.decodeFromString<TaskState>(response)` в try/catch → при ошибке вернуть `TaskState()`

### 4.2 ChatViewModel.kt
- [ ] Переименовать параметр `ollamaChatRepo` → `onDeviceChatRepo`
- [ ] Добавить параметр `onDeviceLlmService: OnDeviceLlmService` и `modelDownloadManager: ModelDownloadManager`
- [ ] Добавить `sealed class ModelState` (NotDownloaded, AskingStorageLocation, Downloading, Initializing, Ready, Error)
- [ ] Добавить `val modelState: StateFlow<ModelState>`
- [ ] Обновить `toggleOfflineMode(enabled)`:
  - если `enabled && !isModelDownloaded` → `modelState = AskingStorageLocation(getStorageOptions())`
  - если `enabled && isModelDownloaded` → `modelState = Initializing` → `initialize()` → `Ready`
- [ ] Добавить `fun onStorageLocationSelected(path)` → запуск `downloadModel()` → по Done → `initialize()` → Ready
- [ ] Обновить `sendMessage()`:
  - offlineMode + Ready → `sendMessageUseCase.invokeStreaming(...)` + collect токены → обновлять последнее сообщение
  - после done: разблокировать ввод + `launch { extractTaskStateUseCase() }` в фоне (silent catch)
  - offlineMode + !Ready → ошибка "Модель не готова"
- [ ] Добавить `override fun onCleared() { onDeviceLlmService.release() }`

### 4.3 ChatView.kt
- [ ] Добавить `StorageLocationDialog` — показывается при `AskingStorageLocation`:
  - список вариантов хранилища с free space
  - кнопки выбора
- [ ] Добавить `DownloadProgressBar` — при `Downloading`: "Скачивание модели... X MB / 800 MB"
- [ ] Добавить `ModelInitializingBar` — при `Initializing`: "Загрузка модели в память..."
- [ ] Обновить `AssistantMessageBubble` — поддержка streaming: текст обновляется пока идут токены (курсор-индикатор)

### 4.4 ChatScreen.kt
- [ ] Передать `modelState` и `onStorageLocationSelected` в ChatView

## Критерии успеха
- При первом включении офлайн-режима — диалог выбора хранилища
- Прогресс-бар скачивания отображается корректно
- Ответ модели стримится токен за токеном в UI
- При ошибке JSON в ExtractTaskState — приложение не падает

## Результат
После этапа: полностью рабочий on-device LLM режим с streaming UI.

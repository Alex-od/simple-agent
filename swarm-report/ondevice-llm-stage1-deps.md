# Этап 1 — Зависимости + Streaming интерфейсы
**Статус:** ⬜ Не начат

## Задача
Добавить зависимость MediaPipe, расширить интерфейсы ChatService / ChatRepository / SendMessageUseCase методом streaming (`Flow<String>`). OpenAiService получает заглушку streaming (wrap вокруг блокирующего вызова).

## Шаги

### 1.1 libs.versions.toml
- [ ] Добавить `mediapipe = "0.10.32"`

### 1.2 app/build.gradle.kts
- [ ] Добавить `implementation("com.google.mediapipe:tasks-genai:0.10.32")`

### 1.3 AndroidManifest.xml
- [ ] Добавить `<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="29"/>`

### 1.4 ChatService.kt
- [ ] Добавить метод: `fun sendMessagesStreaming(messages: List<MessageDto>, jsonMode: Boolean = false): Flow<String>`

### 1.5 OpenAiService.kt
- [ ] Реализовать `sendMessagesStreaming` — wrap: `flow { emit(sendMessages(messages, jsonMode)) }`

### 1.6 ChatRepository.kt (domain)
- [ ] Добавить метод: `fun sendMessagesStreaming(messages: List<Message>, jsonMode: Boolean = false): Flow<String>`

### 1.7 ChatRepositoryImpl.kt
- [ ] Реализовать `sendMessagesStreaming` — маппинг Message → MessageDto, вызов `service.sendMessagesStreaming()`

### 1.8 SendMessageUseCase.kt
- [ ] Добавить `fun invokeStreaming(messages, chatRepository, ragEnabled, taskState): Flow<String>`
- [ ] Логика: собрать RAG-контекст (как в `invoke()`), затем вызвать `chatRepository.sendMessagesStreaming(enrichedMessages)`

## Критерии успеха
- `./gradlew assembleDebug` — без ошибок компиляции
- Все интерфейсы и реализации согласованы

## Результат
После этапа: приложение компилируется с новыми интерфейсами, streaming-метод доступен через всю цепочку слоёв.

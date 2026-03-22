# Этап 4: Message + ChatViewModel

## Статус: [x] Выполнено

## Описание задачи
Обновить `ChatViewModel` для работы с новым типом возврата UseCase (`Pair<String, List<RagSource>>`).
`Message` уже имеет поле `sources` после Этапа 1, здесь нужно его заполнять при создании ответа ассистента.

## Критерии успеха
- `ChatViewModel.sendMessage()` корректно деструктурирует `Pair<String, List<RagSource>>`
- Сообщение ассистента создаётся с `sources = sources`
- При ошибке `sources` не заполняются (остаётся `emptyList()`)
- Компиляция не сломана

## Подробный план реализации

### Шаг 4.1 — Обновить `ChatViewModel.kt`
Путь: `app/src/main/java/com/danichapps/simpleagent/presentation/ChatViewModel.kt`

Изменить только блок `try` внутри `sendMessage()`:

Текущий код (строка 43-45):
```kotlin
val reply = sendMessageUseCase(historyWithUser, ragEnabled = _isRagEnabled.value)
_messages.value = (historyWithUser + Message(role = "assistant", content = reply))
    .takeLast(MAX_HISTORY)
```

Новый код:
```kotlin
val (answer, sources) = sendMessageUseCase(historyWithUser, ragEnabled = _isRagEnabled.value)
_messages.value = (historyWithUser + Message(role = "assistant", content = answer, sources = sources))
    .takeLast(MAX_HISTORY)
```

Добавить import:
```kotlin
import com.danichapps.simpleagent.domain.model.RagSource
```

## Зависимости
- Зависит от: Этап 1 (Message.sources), Этап 3 (UseCase возвращает Pair)
- Этап 5 зависит от этого этапа

## Примечания
- Деструктуризация `val (answer, sources) = ...` — идиоматичный Kotlin для Pair
- Никаких других изменений в ViewModel не требуется
- `ChatScreen.kt` не требует изменений — он просто передаёт `messages` в `ChatView`
- `ChatView` получает `List<Message>` — при наличии `sources` UI сам решает что показывать

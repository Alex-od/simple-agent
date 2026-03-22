# RAG: Источники и цитаты — Общий план

## Задача
Доработать RAG так, чтобы модель возвращала вместе с ответом:
- список источников (sourceFile + chunkIndex)
- цитаты (первые ~150 символов из найденных чанков)

## Решение пользователя по UI
- Раскрывающийся блок "Источники (N)" под пузырём ассистента
- Цитата — первые 150 символов текста чанка с "..."
- Score не показывать

## Текущее состояние (что есть)
- `RagResult` (DTO) уже содержит: `text`, `score`, `sourceFile`, `chunkIndex`
- `RagService.search()` возвращает `List<String>` — теряет метаданные
- `RagRepository.searchContext()` возвращает `List<String>`
- `SendMessageUseCase.invoke()` возвращает `String`
- `Message` — только `role` + `content`, без источников
- UI — отображает только текст сообщения

## Новые сущности

### domain/model/RagChunk.kt
```
data class RagChunk(
    val source: String,      // sourceFile
    val chunkIndex: Int,     // chunkIndex
    val text: String         // полный текст чанка
)
```

### domain/model/RagSource.kt
```
data class RagSource(
    val source: String,   // имя файла
    val chunkIndex: Int,  // номер чанка
    val quote: String     // первые 150 символов из text + "..."
)
```

### Изменения Message
```
data class Message(
    val role: String,
    val content: String,
    val sources: List<RagSource> = emptyList()
)
```

### Изменения SendMessageUseCase
- Возвращает `Pair<String, List<RagSource>>` (ответ + источники)
- `RagChunk` конвертируется в `RagSource` внутри UseCase

## Этапы реализации

| # | Этап | Файл | Статус |
|---|------|------|--------|
| 1 | Доменная модель: RagChunk + RagSource | stage-1 | [x] Выполнено |
| 2 | Слой данных: RagService + Repository | stage-2 | [x] Выполнено |
| 3 | UseCase: вернуть ответ + источники | stage-3 | [x] Выполнено |
| 4 | Message + ChatViewModel | stage-4 | [x] Выполнено |
| 5 | UI: MessageBubble + раскрывающийся блок | stage-5 | [x] Выполнено |

## Схема взаимодействия после доработки

```
RagService.search()
    └─> List<RagChunk>  (text + source + chunkIndex)

RagRepository.searchContext()
    └─> List<RagChunk>

SendMessageUseCase.invoke()
    ├─ Берёт List<RagChunk>
    ├─ Строит системный промпт (только text)
    ├─ Конвертирует в List<RagSource> (quote = text.take(150))
    └─> Pair<String, List<RagSource>>

ChatViewModel.sendMessage()
    ├─ Получает Pair<answer, sources>
    └─> Message(role="assistant", content=answer, sources=sources)

MessageBubble (UI)
    ├─ Отображает content
    └─ Если sources не пустой — показывает "▶ Источники (N)"
           └─ При нажатии раскрывает список с цитатами
```

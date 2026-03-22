# Этап 2: Слой данных — RagService + RagRepositoryImpl

## Статус: [x] Выполнено

## Описание задачи
Изменить `RagService` и `RagRepository`/`RagRepositoryImpl` так, чтобы метаданные чанков
(sourceFile, chunkIndex) не терялись при передаче наверх. Вместо `List<String>` возвращать `List<RagChunk>`.

## Критерии успеха
- `RagService.search()` возвращает `List<RagChunk>` вместо `List<String>`
- `RagRepository.searchContext()` возвращает `List<RagChunk>` вместо `List<String>`
- `RagRepositoryImpl` адаптирован к новому типу
- DTO `RagResult` не изменяется (маппинг происходит в RagService)
- Компиляция не сломана

## Подробный план реализации

### Шаг 2.1 — Обновить `RagService.kt`
Путь: `app/src/main/java/com/danichapps/simpleagent/data/remote/RagService.kt`

Текущий код (строка 16-26):
```kotlin
suspend fun search(query: String, topK: Int = 3): List<String> {
    return try {
        val response: RagSearchResponse = client.post("$RAG_BASE_URL/search") { ... }.body()
        response.results.map { it.text }
    } catch (e: Exception) {
        emptyList()
    }
}
```

Новый код:
```kotlin
suspend fun search(query: String, topK: Int = 3): List<RagChunk> {
    return try {
        val response: RagSearchResponse = client.post("$RAG_BASE_URL/search") { ... }.body()
        response.results.map { result ->
            RagChunk(
                source = result.sourceFile,
                chunkIndex = result.chunkIndex,
                text = result.text
            )
        }
    } catch (e: Exception) {
        emptyList()
    }
}
```

Добавить import: `com.danichapps.simpleagent.domain.model.RagChunk`

### Шаг 2.2 — Обновить `RagRepository.kt`
Путь: `app/src/main/java/com/danichapps/simpleagent/domain/repository/RagRepository.kt`

Текущий код:
```kotlin
interface RagRepository {
    suspend fun searchContext(query: String, topK: Int = 3): List<String>
}
```

Новый код:
```kotlin
interface RagRepository {
    suspend fun searchContext(query: String, topK: Int = 3): List<RagChunk>
}
```

Добавить import: `com.danichapps.simpleagent.domain.model.RagChunk`

### Шаг 2.3 — Обновить `RagRepositoryImpl.kt`
Путь: `app/src/main/java/com/danichapps/simpleagent/data/repository/RagRepositoryImpl.kt`

Текущий код:
```kotlin
override suspend fun searchContext(query: String, topK: Int): List<String> =
    ragService.search(query, topK)
```

Новый код — без изменений в логике, только тип меняется автоматически через RagService:
```kotlin
override suspend fun searchContext(query: String, topK: Int): List<RagChunk> =
    ragService.search(query, topK)
```

Добавить import: `com.danichapps.simpleagent.domain.model.RagChunk`

## Зависимости
- Зависит от: Этап 1 (RagChunk должен существовать)
- Этап 3 зависит от этого этапа

## Примечания
- `RagResult` (DTO) НЕ изменяется — маппинг происходит только в `RagService`
- Граница между data и domain слоями: `RagService` знает о `RagChunk` (domain model) —
  это допустимо, т.к. `RagChunk` является чистой data class без зависимостей на Android

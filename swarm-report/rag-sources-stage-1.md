# Этап 1: Доменная модель — RagChunk + RagSource

## Статус: [x] Выполнено

## Описание задачи
Создать две новые доменные модели в `domain/model/`:
- `RagChunk` — внутреннее представление чанка с метаданными (используется внутри слоя данных и UseCase)
- `RagSource` — модель источника для отображения в UI (source + chunkIndex + quote)

А также расширить существующую модель `Message` полем `sources`.

## Критерии успеха
- Файл `RagChunk.kt` создан в `domain/model/`
- Файл `RagSource.kt` создан в `domain/model/`
- `Message.kt` содержит поле `sources: List<RagSource> = emptyList()`
- Все три модели сериализуемы в рамках Kotlin (data class)
- Компиляция не сломана

## Подробный план реализации

### Шаг 1.1 — Создать `RagChunk.kt`
Путь: `app/src/main/java/com/danichapps/simpleagent/domain/model/RagChunk.kt`

```kotlin
package com.danichapps.simpleagent.domain.model

data class RagChunk(
    val source: String,
    val chunkIndex: Int,
    val text: String
)
```

**Назначение:** перенос данных из слоя данных (RagService) в UseCase.
Содержит полный текст чанка — нужен для построения системного промпта.

### Шаг 1.2 — Создать `RagSource.kt`
Путь: `app/src/main/java/com/danichapps/simpleagent/domain/model/RagSource.kt`

```kotlin
package com.danichapps.simpleagent.domain.model

data class RagSource(
    val source: String,
    val chunkIndex: Int,
    val quote: String
)
```

**Назначение:** данные для отображения в UI. `quote` = первые 150 символов `text` с "..." если длиннее.

### Шаг 1.3 — Обновить `Message.kt`
Текущий код:
```kotlin
data class Message(
    val role: String,
    val content: String
)
```

Новый код:
```kotlin
data class Message(
    val role: String,
    val content: String,
    val sources: List<RagSource> = emptyList()
)
```

**Важно:** дефолтное значение `emptyList()` гарантирует обратную совместимость — все существующие
места создания `Message` (ViewModel, UseCase) не сломаются.

## Зависимости
- Нет входящих зависимостей (этап 1 — основа для всех остальных)
- Этапы 2, 3, 4, 5 зависят от этого этапа

## Примечания
- `RagChunk` — только внутренняя модель, в UI не попадает
- `RagSource` — публичная модель, передаётся через ViewModel в Composable
- Оба класса — обычные data class без аннотаций сериализации (не участвуют в JSON)

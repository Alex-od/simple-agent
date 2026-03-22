# Этап 2: Обновление SendMessageUseCase
Статус: TODO

## Описание задачи
Модифицировать SendMessageUseCase:
1. Принимать taskState: TaskState (nullable/default empty)
2. RAG-запрос обогащать терминами из task state
3. Task state добавлять в системный промпт

## Что получим
- SendMessageUseCase учитывает task state при поиске в RAG
- Системный промпт содержит: task state + RAG-контекст
- Обратная совместимость: если taskState пустой — поведение прежнее

## Критерии успеха
- RAG-запрос = "lastUserQuery constraints[0] constraints[1] ..."
- Системный промпт: сначала блок TaskState (если не пустой), затем RAG-контекст
- Существующие тесты не ломаются
- Компиляция без ошибок

## Подробный план реализации

### Шаг 2.1 — Добавить параметр taskState в invoke()
Сигнатура после изменения:
```
suspend operator fun invoke(
    messages: List<Message>,
    ragEnabled: Boolean = false,
    taskState: TaskState = TaskState()
): Pair<String, List<RagSource>>
```
Статус: TODO

### Шаг 2.2 — Обогатить RAG-запрос терминами task state
```kotlin
val ragQuery = if (taskState.constraints.isNotEmpty() || taskState.goal.isNotEmpty()) {
    val extra = (listOfNotNull(taskState.goal.takeIf { it.isNotBlank() }) + taskState.constraints)
        .joinToString(" ")
    "$lastUserQuery $extra"
} else {
    lastUserQuery
}
ragRepository.searchContext(ragQuery)
```
Статус: TODO

### Шаг 2.3 — Добавить task state в системный промпт
Если taskState не пустой — добавить блок перед RAG-контекстом:
```
---ПАМЯТЬ ЗАДАЧИ---
Цель: <goal>
Уточнения: <clarifications>
Ограничения/Термины: <constraints>
---КОНЕЦ ПАМЯТИ---
```
Статус: TODO

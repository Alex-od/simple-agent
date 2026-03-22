# Этап 1: TaskState модель + ExtractTaskStateUseCase
Статус: TODO

## Описание задачи
Создать доменную модель TaskState и use case для её автоматического обновления через отдельный LLM-вызов.

## Что получим
- Новый data class `TaskState` в domain/model/
- Новый `ExtractTaskStateUseCase` в domain/usecase/
- Чёткий контракт: входные данные — история + текущий state, выходные — обновлённый state

## Критерии успеха
- TaskState компилируется, имеет три поля: goal, clarifications, constraints
- ExtractTaskStateUseCase принимает List<Message> + TaskState, возвращает TaskState
- LLM-промпт извлекает JSON с полями goal/clarifications/constraints из диалога
- Юнит-тест (опционально): при пустой истории возвращает пустой state

## Подробный план реализации

### Шаг 1.1 — Создать domain/model/TaskState.kt
Файл: app/src/main/java/com/danichapps/simpleagent/domain/model/TaskState.kt

```
data class TaskState(
    val goal: String = "",
    val clarifications: List<String> = emptyList(),
    val constraints: List<String> = emptyList()
)
```
Статус: TODO

### Шаг 1.2 — Создать domain/usecase/ExtractTaskStateUseCase.kt
Файл: app/src/main/java/com/danichapps/simpleagent/domain/usecase/ExtractTaskStateUseCase.kt

Логика:
- Принимает messages: List<Message>, current: TaskState
- Формирует системный промпт с инструкцией вернуть JSON вида:
  {"goal":"...","clarifications":["..."],"constraints":["..."]}
- Вызывает chatRepository.sendMessages() с одним user-сообщением = полный диалог
- Парсит JSON-ответ в TaskState (kotlinx.serialization)
- При ошибке парсинга возвращает current без изменений

Статус: TODO

### Шаг 1.3 — Добавить DTO для task state extraction
Создать TaskStateDto в data/remote/dto/ для парсинга JSON-ответа LLM.
Статус: TODO

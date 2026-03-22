# План: RAG + История диалога + Память задачи
Дата: 2026-03-21

## Описание задачи
1. При каждом новом вопросе искать контекст в базе через RAG (уже работает частично — добавить обогащение запроса термами из task state)
2. Ответ с учётом найденной информации и истории диалога (история уже передаётся)
3. "Память задачи" (TaskState): цель диалога, уточнения пользователя, зафиксированные ограничения/термины
   - Обновляется отдельным LLM-вызовом после каждого ответа ассистента
   - Скрыта от пользователя (только в системном промпте)
   - Только в памяти сессии (не персистентна)
4. RAG-запрос = последний вопрос + ключевые термины из task state

## Решения по неясным местам
- Task state обновляется: отдельный LLM-вызов после каждого ответа
- UI: скрыто (только в промпте), не отображается пользователю
- RAG-запрос: последний вопрос + task state термины/цели
- Персистентность: только в памяти сессии

## Новые сущности

### TaskState (domain/model/TaskState.kt)
- goal: String — цель диалога (что пользователь хочет достичь)
- clarifications: List<String> — что пользователь уточнил
- constraints: List<String> — ограничения и зафиксированные термины

### ExtractTaskStateUseCase (domain/usecase/ExtractTaskStateUseCase.kt)
- invoke(history: List<Message>, current: TaskState): TaskState
- Делает LLM-вызов с инструкцией извлечь/обновить TaskState из истории
- Возвращает обновлённый TaskState

### Изменения в SendMessageUseCase
- Принимает taskState: TaskState (nullable)
- RAG-запрос = lastUserQuery + " " + taskState.constraints.joinToString(" ")
- Добавляет taskState в системный промпт перед RAG-контекстом

### Изменения в ChatViewModel
- _taskState: MutableStateFlow<TaskState>
- После получения ответа — запускает ExtractTaskStateUseCase
- Передаёт taskState в SendMessageUseCase

### Изменения в AppModule
- Регистрирует ExtractTaskStateUseCase
- Обновляет ChatViewModel factory

## Этапы реализации

| # | Этап | Файл | Статус |
|---|------|------|--------|
| 1 | TaskState модель + ExtractTaskStateUseCase | rag-task-memory-stage-1.md | DONE |
| 2 | Обновление SendMessageUseCase | rag-task-memory-stage-2.md | DONE |
| 3 | Обновление ChatViewModel | rag-task-memory-stage-3.md | DONE |
| 4 | Обновление AppModule + DI | rag-task-memory-stage-4.md | DONE |

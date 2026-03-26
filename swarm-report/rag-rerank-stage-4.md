# Этап 4: Фикс — Streaming + RAG

## Задача
Исправить баг: invokeStreaming() всегда использует emptyList() для RAG-чанков.
Если ragEnabled=true → переключиться на suspend invoke() (не streaming).

## Файлы и изменения
```
SendMessageUseCase.kt
  +-ИЗМЕНИТЬ invokeStreaming() сигнатура → suspend fun invokeStreaming(...)
             — если ragEnabled → вызвать suspend invoke() → вернуть flow { emit(answer) }
             — если !ragEnabled → прежняя логика (chatRepository.sendMessagesStreaming)
```

## Критерии успеха
- [ ] Компилируется без ошибок
- [ ] RAG enabled + streaming → контекст передаётся в промпт
- [ ] RAG disabled + streaming → поведение не изменилось

## Статус
✅ Завершён

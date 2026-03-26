# Этап 4: ChatViewModel — интеграция офлайн RAG

## Задача
Обновить ChatViewModel чтобы в офлайн-режиме использовал offline SendMessageUseCase с ragEnabled=true.

## Пошаговый план реализации
1. Обновить ChatViewModel.kt ✅
2. AppModule.kt уже обновлён в этапе 3 ✅

## Файлы и изменения
```
presentation/ChatViewModel.kt
  +-ИЗМЕНИТЬ  constructor: заменил sendMessageUseCase на onlineSendMessageUseCase + offlineSendMessageUseCase
  +-ИЗМЕНИТЬ  sendOnline() → использует onlineSendMessageUseCase
  +-ИЗМЕНИТЬ  sendOffline() → использует offlineSendMessageUseCase с ragEnabled = _isRagEnabled.value
  +-ИЗМЕНИТЬ  sendOffline() → sources теперь передаются в Message (RAG источники показываются в UI)
```

## Резюме
После этапа: при включении офлайн + RAG — Gemma3 получает релевантные чанки из IEEE-830 документа.

## Критерии успеха
- [x] ChatViewModel компилируется с двумя SendMessageUseCase
- [x] sendOffline() использует ragEnabled = _isRagEnabled.value (не false)
- [x] Источники RAG передаются в Message при офлайн-режиме

## Статус
✅ Завершён

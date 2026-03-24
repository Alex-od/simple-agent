# Офлайн-режим (Ollama) — общий план
Дата: 2026-03-23

## Описание задачи
Добавить переключатель "Офлайн-режим" в боковое меню приложения.
- Офлайн ON → запросы идут в Ollama (http://10.0.2.2:11434)
- Офлайн OFF → запросы идут в OpenAI API
- RAG работает с обоими backend
- ExtractTaskStateUseCase тоже переключается вместе с режимом

## Стек
- Ollama на ПК, модель llama3.2:3b, адрес из эмулятора: http://10.0.2.2:11434
- Ollama использует OpenAI-совместимый API (/v1/chat/completions)
- Network security config уже разрешает cleartext для 10.0.2.2

## Архитектурное решение
1. Новый интерфейс `ChatService` — оба сервиса (OpenAI и Ollama) реализуют его
2. `ChatRepositoryImpl` принимает `ChatService` вместо `OpenAiService`
3. В Koin два именованных `ChatRepository` — "openai" и "ollama"
4. `SendMessageUseCase` и `ExtractTaskStateUseCase` получают `ChatRepository` как параметр `invoke()`
5. `ChatViewModel` инжектирует оба репозитория, переключает по `isOfflineMode`
6. Новый переключатель "Офлайн-режим" в ModalDrawer в ChatView

## Этапы

| # | Этап | Файлы | Статус |
|---|------|-------|--------|
| 1 | Настройка Ollama на ПК | — (инструкции) | [ ] |
| 2 | Сервисный слой | ChatService.kt (NEW), OllamaService.kt (NEW), ChatRepositoryImpl.kt (MODIFY), OpenAiService.kt (MODIFY) | [x] |
| 3 | Domain + DI | SendMessageUseCase.kt (MODIFY), ExtractTaskStateUseCase.kt (MODIFY), AppModule.kt (MODIFY) | [x] |
| 4 | Presentation | ChatViewModel.kt (MODIFY), ChatView.kt (MODIFY), ChatScreen.kt (MODIFY) | [x] |

## Критерии готовности
- [ ] Ollama запущен, отвечает на http://localhost:11434/api/tags
- [ ] Приложение собирается без ошибок
- [ ] Переключатель "Офлайн-режим" отображается в drawer
- [ ] При ON: запрос уходит в Ollama, ответ приходит
- [ ] При OFF: запрос уходит в OpenAI, ответ приходит
- [ ] RAG (поиск по документам) работает в обоих режимах

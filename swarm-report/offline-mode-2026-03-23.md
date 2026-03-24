# Офлайн-режим (Ollama) — Отчёт
Дата: 2026-03-23

## Описание задачи
Добавить переключатель «Офлайн-режим» в боковое меню приложения.
При включении — запросы уходят в локальную LLM Ollama (llama3.2:3b).
При выключении — запросы уходят в OpenAI API.
RAG должен работать с обоими backend.

## Реализовано

### Новые файлы
- `data/remote/ChatService.kt` — интерфейс для OpenAiService и OllamaService
- `data/remote/OllamaService.kt` — сервис Ollama (http://10.0.2.2:11434/v1, модель llama3.2:3b)

### Изменённые файлы
- `data/remote/OpenAiService.kt` — реализует ChatService
- `data/repository/ChatRepositoryImpl.kt` — принимает ChatService вместо OpenAiService
- `domain/usecase/SendMessageUseCase.kt` — chatRepository передаётся в invoke()
- `domain/usecase/ExtractTaskStateUseCase.kt` — chatRepository передаётся в invoke()
- `di/AppModule.kt` — два named ChatRepository ("openai"/"ollama"), таймауты для rag-клиента
- `presentation/ChatViewModel.kt` — isOfflineMode StateFlow, оба репозитория, переключение
- `presentation/ChatScreen.kt` — передаёт isOfflineMode и onOfflineModeToggle
- `presentation/ChatView.kt` — переключатель «Офлайн-режим» в drawer + бейдж OFFLINE в топбаре

### Настройка Ollama
- Установлена на Windows (уже была)
- OLLAMA_HOST=0.0.0.0 (уровень пользователя) — доступ из эмулятора
- Модель llama3.2:3b скачана (2 GB)

## Результаты Validation

| Шаг | Результат |
|-----|-----------|
| Сборка assembleDebug | ✅ BUILD SUCCESSFUL |
| Экран чата запускается | ✅ |
| Drawer: переключатель «Поиск по документам» | ✅ |
| Drawer: переключатель «Офлайн-режим» | ✅ |
| Включить офлайн → бейдж OFFLINE в топбаре | ✅ |
| Запрос в офлайн-режиме → ответ от Ollama | ✅ "It's nice to meet you..." |
| Выключить офлайн → бейдж OFFLINE исчезает | ✅ |
| Запрос в онлайн-режиме → ответ от OpenAI | ✅ "Hello again! How can I assist..." |
| RAG + OFFLINE вместе → оба бейджа, ответ от Ollama | ✅ |

## Проблемы в процессе
- `Icons.Default.WifiOff` и `Icons.Default.CloudOff` отсутствуют в core Material Icons — иконка у переключателя «Офлайн-режим» убрана (только у RAG осталась иконка Search)
- Non-ASCII ввод через Mobile MCP не поддерживается — тест с "Привет" заменён на "Hello"

## Статус: Done

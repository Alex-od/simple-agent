# Этап 4: Обновление AppModule (DI)
Статус: TODO

## Описание задачи
Зарегистрировать ExtractTaskStateUseCase в Koin и обновить ChatViewModel factory.

## Что получим
- ExtractTaskStateUseCase доступен через DI
- ChatViewModel получает оба use case через Koin

## Критерии успеха
- Приложение запускается без Koin-ошибок
- ChatViewModel инжектируется корректно
- Нет дублирования регистраций

## Подробный план реализации

### Шаг 4.1 — Зарегистрировать ExtractTaskStateUseCase
В AppModule добавить:
```kotlin
single { ExtractTaskStateUseCase(get()) }
```
(использует тот же ChatRepository/OpenAiService что и SendMessageUseCase)
Статус: TODO

### Шаг 4.2 — Обновить ChatViewModel factory
```kotlin
viewModel { ChatViewModel(get(), get()) }
```
Статус: TODO

### Шаг 4.3 — Финальная проверка
- ./gradlew assembleDebug — сборка без ошибок
- Установить APK на эмулятор
- Провести ручной smoke-тест: отправить 2 вопроса с RAG включённым, проверить что второй ответ учитывает контекст первого
Статус: TODO

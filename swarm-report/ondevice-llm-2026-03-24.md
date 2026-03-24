# On-Device LLM (MediaPipe + Gemma 3 1B) — Validation Report
Дата: 2026-03-24
Устройство: SM-M515F (Snapdragon 730G, Adreno 618, 6GB RAM, Android 12)

## Статус: PASSED (с известными проблемами)

---

## Что реализовано

| Компонент | Файл | Статус |
|---|---|---|
| OnDeviceLlmService (MediaPipe) | `data/remote/OnDeviceLlmService.kt` | Новый |
| ModelDownloadManager | `data/remote/ModelDownloadManager.kt` | Новый |
| ChatService streaming interface | `data/remote/ChatService.kt` | Новый |
| SendMessageUseCase.invokeStreaming | `domain/usecase/SendMessageUseCase.kt` | Обновлён |
| ExtractTaskStateUseCase (JSON fallback) | `domain/usecase/ExtractTaskStateUseCase.kt` | Обновлён |
| ChatRepositoryImpl streaming | `data/repository/ChatRepositoryImpl.kt` | Обновлён |
| OpenAiService streaming stub | `data/remote/OpenAiService.kt` | Обновлён |
| ChatViewModel (ModelState, streaming) | `presentation/ChatViewModel.kt` | Обновлён |
| ChatView (UI: диалог хранилища, прогресс) | `presentation/ChatView.kt` | Обновлён |
| AppModule DI (ondevice, ModelDownloadManager) | `di/AppModule.kt` | Обновлён |
| OllamaService | `data/remote/OllamaService.kt` | Удалён |

---

## E2E результаты (SM-M515F)

| Шаг | Описание | Результат |
|---|---|---|
| 1 | Запуск → поле ввода доступно | PASS |
| 2 | Сообщение в OpenAI-режиме | PASS |
| 3 | Открыть меню → включить офлайн-режим | PASS |
| 4 | Диалог хранилища | PASS (модель загружена заранее через adb push) |
| 5 | Прогресс скачивания | N/A (модель уже была на устройстве) |
| 6 | Инициализация модели | PASS (Initializing → Ready) |
| 7 | Модель Ready | PASS |
| 8 | Стриминг в офлайн-режиме | PASS (Gemma ответила токен за токеном) |
| 9 | Wi-Fi выключен → ответ без интернета | PASS ("Понял. Хорошо! Готова ответить...") |
| 10 | Wi-Fi включён → онлайн → OpenAI | PASS ("Здравствуйте! Как я могу вам помочь?") |

---

## Дополнительные фиксы по ходу тестирования

### UX fix: разблокировка ввода после стриминга
**Файл:** `presentation/ChatViewModel.kt`
**Проблема:** `_isLoading.value = false` вызывался после фонового `extractTaskStateUseCase`, блокируя ввод на весь второй LLM-вызов.
**Фикс:** Перенесён `_isLoading.value = false` перед запуском background-coroutine.

---

## Известные проблемы

### 1. generateResponseAsync не завершается по EOS — FIX APPLIED, статус неопределён
**Серьёзность:** High (UX)
**Описание:** `LlmInferenceSession.generateResponseAsync` не вызывает `done=true` после семантически завершённого ответа. Модель продолжает генерацию до MAX_TOKENS=1024, занимая до ~10 минут.
**Симптом:** Спиннер продолжает крутиться после появления полного ответа.
**Версия библиотеки:** `tasks-genai:0.10.32`

**Применённый фикс** (`data/remote/OnDeviceLlmService.kt`):
```kotlin
val accumulated = StringBuilder()
session.generateResponseAsync { partial, done ->
    if (partial != null) {
        accumulated.append(partial)
        if ("<end_of_turn>" in accumulated) {
            session.close()
            close()
        } else {
            trySend(partial)
        }
    }
    if (done) {
        session.close()
        close()
    }
}
awaitClose { runCatching { session.close() } }
```
**Статус фикса:** Установлен, но спиннер по-прежнему крутится после ответа. Причина неизвестна — возможно, Gemma 3 INT4 `.task` формат использует другой EOS-токен (не `<end_of_turn>`). Требует дополнительной диагностики через logcat.

**Следующие шаги для диагностики:**
- Добавить `Log.d("OnDeviceLLM", "token: '$partial'")` и проверить logcat на реальный EOS-токен
- Попробовать альтернативные маркеры: `<eos>`, `</s>`, пустой токен
- Альтернатива: уменьшить `MAX_TOKENS` до 256 как временный обходной путь

### 2. extractTaskStateUseCase занимает до 10 минут
**Серьёзность:** Medium
**Описание:** Фоновый вызов `extractTaskStateUseCase` тоже запускает LLM-инференс (не streaming, blocking `generateResponse()`), который тоже генерирует до MAX_TOKENS.
**Рекомендация:** Отдельный меньший MAX_TOKENS для JSON-extraction сессии, или отключить extractTaskState для on-device режима.

---

## Производительность

| Метрика | Значение |
|---|---|
| Устройство | SM-M515F (Snapdragon 730G, Adreno 618) |
| Модель | Gemma 3 1B IT INT4 (~554MB .task) |
| Backend | GPU (OpenCL через ML_DRIFT_CL) |
| RAM | ~1.2GB при загруженной модели |
| Скорость первого токена | ~5-10 секунд |
| Полный ответ видим | ~15-30 секунд |
| До done=true (MAX_TOKENS) | ~5-10 минут |

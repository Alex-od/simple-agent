# Этап 2: Сервисный слой
Статус: [ ] pending

## Задача
Ввести интерфейс ChatService, создать OllamaService,
обновить ChatRepositoryImpl чтобы работал с обоими.

## Что получим
- Единый интерфейс ChatService, реализованный обоими сервисами
- OllamaService готов к использованию
- ChatRepositoryImpl не привязан к конкретному сервису

## Шаги

### 2.1 Создать интерфейс ChatService
**СОЗДАТЬ** `app/src/main/java/com/danichapps/simpleagent/data/remote/ChatService.kt`

```kotlin
package com.danichapps.simpleagent.data.remote

import com.danichapps.simpleagent.data.remote.dto.MessageDto

interface ChatService {
    suspend fun sendMessages(messages: List<MessageDto>, jsonMode: Boolean = false): String
}
```

---

### 2.2 OpenAiService — реализовать ChatService
**ИЗМЕНИТЬ** `OpenAiService.kt`

```
+-ИЗМЕНИТЬ  class OpenAiService(...) : ChatService {
```
Добавить `: ChatService` к объявлению класса.
Метод `sendMessages` уже совпадает по сигнатуре — только добавить `override`.

---

### 2.3 Создать OllamaService
**СОЗДАТЬ** `app/src/main/java/com/danichapps/simpleagent/data/remote/OllamaService.kt`

Адрес: `http://10.0.2.2:11434/v1`
Модель: `llama3.2:3b`
Структура запроса: идентична OpenAI (ChatRequest / ChatResponse DTOs)
Авторизация: не нужна (пустой/отсутствует Bearer)

Реализует `ChatService`.

---

### 2.4 Обновить ChatRepositoryImpl
**ИЗМЕНИТЬ** `data/repository/ChatRepositoryImpl.kt`

```
+-ИЗМЕНИТЬ  class ChatRepositoryImpl(private val service: ChatService)
```
Вместо `OpenAiService` → `ChatService`. Логика не меняется.

---

## Критерий успеха
- Проект компилируется
- `ChatRepositoryImpl` принимает любой из двух сервисов

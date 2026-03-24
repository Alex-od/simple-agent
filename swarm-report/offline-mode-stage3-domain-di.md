# Этап 3: Domain + DI
Статус: [ ] pending

## Задача
Рефакторинг UseCase: ChatRepository передаётся как параметр invoke().
Обновление AppModule: регистрация двух ChatRepository.

## Что получим
- UseCase не привязаны к конкретному ChatRepository в конструкторе
- Koin регистрирует "openai" и "ollama" ChatRepository
- ViewModel получает оба репозитория

## Шаги

### 3.1 SendMessageUseCase — убрать chatRepository из конструктора
**ИЗМЕНИТЬ** `domain/usecase/SendMessageUseCase.kt`

```
-УДАЛИТЬ    private val chatRepository: ChatRepository     (из конструктора)
+-ИЗМЕНИТЬ  suspend operator fun invoke(
                messages: List<Message>,
                chatRepository: ChatRepository,            // добавить параметр
                ragEnabled: Boolean = false,
                taskState: TaskState = TaskState()
            )
```
Сигнатура invoke меняется: добавляется `chatRepository: ChatRepository`.
Все внутренние вызовы `chatRepository.sendMessages(...)` остаются без изменений.

---

### 3.2 ExtractTaskStateUseCase — убрать chatRepository из конструктора
**ИЗМЕНИТЬ** `domain/usecase/ExtractTaskStateUseCase.kt`

```
-УДАЛИТЬ    private val chatRepository: ChatRepository     (из конструктора)
+-ИЗМЕНИТЬ  suspend operator fun invoke(
                history: List<Message>,
                current: TaskState,
                chatRepository: ChatRepository             // добавить параметр
            ): TaskState
```

---

### 3.3 Обновить AppModule
**ИЗМЕНИТЬ** `di/AppModule.kt`

```
+ДОБАВИТЬ   single { OllamaService(get(named("rag"))) }      // отдельный HttpClient без auth

+ДОБАВИТЬ   single(named("openai")) <ChatRepository> {
                ChatRepositoryImpl(OpenAiService(get(named("openai"))))
            }
+ДОБАВИТЬ   single(named("ollama")) <ChatRepository> {
                ChatRepositoryImpl(OllamaService(get(named("rag"))))
            }

-УДАЛИТЬ    single { OpenAiService(get(named("openai"))) }   // убрать, теперь создаётся внутри
-УДАЛИТЬ    single<ChatRepository> { ChatRepositoryImpl(get()) }  // убрать неименованный

+-ИЗМЕНИТЬ  single { SendMessageUseCase(get()) }             // только ragRepository
+-ИЗМЕНИТЬ  single { ExtractTaskStateUseCase() }             // без аргументов
+-ИЗМЕНИТЬ  viewModel { ChatViewModel(get(named("openai")), get(named("ollama")), get(), get()) }
```

Примечание: OllamaService использует тот же HttpClient "rag" (без auth-заголовка).
Можно создать отдельный named("ollama") HttpClient — по желанию, но "rag" подходит.

---

## Критерий успеха
- Проект компилируется
- Koin регистрирует два ChatRepository без конфликтов

# Этап 3: Обновление ChatViewModel
Статус: TODO

## Описание задачи
Добавить в ChatViewModel хранение и обновление TaskState:
- После каждого ответа ассистента запускать ExtractTaskStateUseCase
- Передавать актуальный taskState в SendMessageUseCase
- Обновление task state — параллельно или последовательно после основного ответа

## Что получим
- ChatViewModel имеет _taskState: MutableStateFlow<TaskState>
- После каждого ответа ассистента task state обновляется в фоне
- SendMessageUseCase получает актуальный task state при следующем вопросе

## Критерии успеха
- Первый вопрос отправляется с пустым TaskState (нормально)
- Второй и последующие — с обновлённым TaskState
- Ошибка в ExtractTaskStateUseCase не роняет основной флоу (try/catch)
- _isLoading отражает только основной запрос, не обновление task state

## Подробный план реализации

### Шаг 3.1 — Добавить зависимость и поле _taskState
```kotlin
class ChatViewModel(
    private val sendMessageUseCase: SendMessageUseCase,
    private val extractTaskStateUseCase: ExtractTaskStateUseCase
) : ViewModel() {
    private val _taskState = MutableStateFlow(TaskState())
    ...
}
```
Статус: TODO

### Шаг 3.2 — Передать taskState в sendMessageUseCase
```kotlin
val (answer, sources) = sendMessageUseCase(
    historyWithUser,
    ragEnabled = _isRagEnabled.value,
    taskState = _taskState.value
)
```
Статус: TODO

### Шаг 3.3 — Обновить task state после ответа
После добавления ответа ассистента в _messages — запустить обновление:
```kotlin
// не блокируем UI, запускаем в том же scope
try {
    val updatedState = extractTaskStateUseCase(
        _messages.value,
        _taskState.value
    )
    _taskState.value = updatedState
} catch (e: Exception) {
    // молча игнорируем — основной ответ уже получен
}
```
Статус: TODO

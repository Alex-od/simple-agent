# Этап 4: Presentation
Статус: [ ] pending

## Задача
Добавить isOfflineMode в ViewModel, передавать нужный репозиторий в UseCase.
Добавить переключатель "Офлайн-режим" в боковое меню (drawer).

## Что получим
- ChatViewModel управляет флагом offlineMode
- При offlineMode=true используется ollamaRepo, иначе openAiRepo
- В боковом меню два переключателя: RAG и Офлайн-режим

## Шаги

### 4.1 ChatViewModel
**ИЗМЕНИТЬ** `presentation/ChatViewModel.kt`

```
+ДОБАВИТЬ   private val openAiChatRepo: ChatRepository  (named "openai", в конструктор)
+ДОБАВИТЬ   private val ollamaChatRepo: ChatRepository  (named "ollama", в конструктор)

+ДОБАВИТЬ   private val _isOfflineMode = MutableStateFlow(false)
+ДОБАВИТЬ   val isOfflineMode: StateFlow<Boolean> = _isOfflineMode.asStateFlow()

+ДОБАВИТЬ   fun toggleOfflineMode(enabled: Boolean) { _isOfflineMode.value = enabled }

+-ИЗМЕНИТЬ  sendMessage() — добавить:
            val activeChatRepo = if (_isOfflineMode.value) ollamaChatRepo else openAiChatRepo
            val (answer, sources) = sendMessageUseCase(
                historyWithUser,
                chatRepository = activeChatRepo,   // новый параметр
                ragEnabled = _isRagEnabled.value,
                taskState = _taskState.value
            )

+-ИЗМЕНИТЬ  extractTaskStateUseCase вызывается с:
            extractTaskStateUseCase(updatedHistory, _taskState.value, activeChatRepo)
```

---

### 4.2 ChatScreen
**ИЗМЕНИТЬ** `presentation/ChatScreen.kt`

```
+ДОБАВИТЬ   val isOfflineMode by viewModel.isOfflineMode.collectAsState()

+-ИЗМЕНИТЬ  ChatView(
                ...
                isOfflineMode = isOfflineMode,
                onOfflineModeToggle = viewModel::toggleOfflineMode
            )
```

---

### 4.3 ChatView
**ИЗМЕНИТЬ** `presentation/ChatView.kt`

Добавить параметры в ChatView:
```
+ДОБАВИТЬ   isOfflineMode: Boolean
+ДОБАВИТЬ   onOfflineModeToggle: (Boolean) -> Unit
```

В ModalDrawerSheet добавить второй ListItem после существующего RAG-переключателя:
```
+ДОБАВИТЬ   HorizontalDivider()
+ДОБАВИТЬ   ListItem(
                headlineContent = { Text("Офлайн-режим") },
                supportingContent = { Text("Локальная LLM (Ollama)") },
                leadingContent = { Icon(Icons.Default.WifiOff, ...) },
                trailingContent = { Switch(checked = isOfflineMode, onCheckedChange = onOfflineModeToggle) }
            )
```

В TopAppBar добавить бейдж "OFFLINE" рядом с "RAG" когда isOfflineMode=true:
```
+-ИЗМЕНИТЬ  if (isOfflineMode) { Text("OFFLINE", ...) }
```

---

## Критерий успеха
- Переключатель "Офлайн-режим" виден в drawer
- При включении бейдж "OFFLINE" появляется в заголовке
- Запрос в офлайн-режиме уходит на 10.0.2.2:11434 (виден в Logcat)
- Запрос в онлайн-режиме уходит на api.openai.com (виден в Logcat)

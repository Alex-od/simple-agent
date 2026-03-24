# Этап 3 — DI + удаление OllamaService + переименование
**Статус:** ⬜ Не начат (зависит от Этапа 2)

## Задача
Подключить новые классы в Koin DI, удалить OllamaService, переименовать все упоминания "ollama" → "ondevice".

## Шаги

### 3.1 AppModule.kt
- [ ] Добавить `single { ModelDownloadManager(androidContext()) }`
- [ ] Добавить `single { OnDeviceLlmService(androidContext(), get<ModelDownloadManager>().modelPath) }`
- [ ] Заменить `single<ChatRepository>(named("ollama"))` → `single<ChatRepository>(named("ondevice")) { ChatRepositoryImpl(get<OnDeviceLlmService>()) }`
- [ ] Обновить `viewModel { ChatViewModel(get(named("openai")), get(named("ondevice")), get(), get(), get()) }` (+ModelDownloadManager)
- [ ] Удалить импорт OllamaService
- [ ] Убедиться что HttpClient named("rag") остаётся (нужен для RagService)

### 3.2 Удалить OllamaService.kt
- [ ] Удалить файл `data/remote/OllamaService.kt`

### 3.3 Проверка отсутствия ссылок
- [ ] grep по проекту: нет "ollama", нет "OllamaService", нет "ollamaChatRepo"

## Критерии успеха
- `./gradlew assembleDebug` — без ошибок
- DI граф инициализируется (нет RuntimeException при старте)
- Grep: 0 вхождений "ollama" и "OllamaService" в коде

## Результат
После этапа: приложение запускается, DI чист, OllamaService удалён.

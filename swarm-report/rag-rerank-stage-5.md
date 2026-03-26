# Этап 5: DI — AppModule

## Задача
Обновить AppModule: добавить GemmaRerankService в граф, передать в LocalRagRepositoryImpl.

## Файлы и изменения
```
AppModule.kt
  +ДОБАВИТЬ  GemmaRerankService(
               llmService = onDeviceLlmService,
               parser = RerankParser  // object, не инжектится
             )
  +-ИЗМЕНИТЬ LocalRagRepositoryImpl(
               chunksDataSource,
               embeddingService,
               context,
               rerankService = gemmaRerankService  // новый параметр
             )
```

## Критерии успеха
- [ ] Компилируется без ошибок
- [ ] Koin/DI граф не падает при старте

## Статус
✅ Завершён

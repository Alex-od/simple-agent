# Этап 3: LocalRagRepositoryImpl + DI

## Задача
Создать on-device реализацию RagRepository с cosine similarity поиском.
Эмбеддинги чанков кэшируются на диск (filesDir/rag_embeddings.bin) — пересчитываются только один раз.
Обновить DI для поддержки двух RagRepository и двух SendMessageUseCase.

## Пошаговый план реализации
1. Создать LocalRagRepositoryImpl.kt ✅
2. Обновить AppModule.kt — именованные (named) бины ✅

## Файлы и изменения
Всё выполнено согласно плану.

## Резюме
After этапа: on-device RAG готов. DI предоставляет два named SendMessageUseCase.

## Критерии успеха
- [x] LocalRagRepositoryImpl создан с cosine similarity
- [x] Эмбеддинги кэшируются в filesDir/rag_embeddings.bin (DataOutput/InputStream)
- [x] DI: named("remote"/"local") RagRepository + named("online"/"offline") SendMessageUseCase

## Статус
✅ Завершён

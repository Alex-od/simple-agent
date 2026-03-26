# Этап 2: Зависимости + LocalEmbeddingService + ModelDownloadManager

## Задача
Добавить mediapipe-tasks-text. Создать сервис для вычисления эмбеддингов через MediaPipe TextEmbedder.
Расширить ModelDownloadManager для поддержки embedding модели.

## Пошаговый план реализации
1. Обновить libs.versions.toml — добавить mediapipe-tasks-text ✅
2. Обновить build.gradle.kts — добавить зависимость ✅
3. Создать LocalEmbeddingService.kt ✅
4. Обновить ModelDownloadManager.kt — добавить embeddingModelPath ✅

## Файлы и изменения

```
gradle/libs.versions.toml
  +-ИЗМЕНИТЬ  добавить:
              mediapipe-tasks-text = { group = "com.google.mediapipe", name = "tasks-text", version.ref = "mediapipe" }

app/build.gradle.kts
  +-ИЗМЕНИТЬ  добавить в dependencies:
              implementation(libs.mediapipe.tasks.text)

data/local/LocalEmbeddingService.kt
  +ДОБАВИТЬ  новый файл
             class LocalEmbeddingService(context: Context, modelPath: String)
             fun isModelAvailable(): Boolean — File(modelPath).exists()
             fun initialize() — создаёт TextEmbedder из modelPath
             suspend fun embed(text: String): FloatArray? — возвращает float embedding
             fun release() — закрывает TextEmbedder

data/remote/ModelDownloadManager.kt
  +-ИЗМЕНИТЬ  добавить:
              private const val EMBEDDING_MODEL_FILENAME = "embedding_model.tflite"
              val embeddingModelPath: String — File(externalFilesDir, EMBEDDING_MODEL_FILENAME).absolutePath
              fun isEmbeddingModelDownloaded(): Boolean — File(embeddingModelPath).exists()
```

## Резюме
После этапа: MediaPipe TextEmbedder готов к работе. Приложение умеет проверять наличие embedding-модели.

## Критерии успеха
- [x] Проект компилируется с tasks-text зависимостью
- [x] LocalEmbeddingService.isModelAvailable() возвращает false если файла нет
- [x] ModelDownloadManager.embeddingModelPath указывает на ExternalFilesDir/embedding_model.tflite

## Статус
✅ Завершён

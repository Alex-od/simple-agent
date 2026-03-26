# Этап 1: DOCX в assets + DocxTextExtractor + LocalRagChunksDataSource

## Задача
Добавить DOCX в assets. Создать инфраструктуру для парсинга и чанкинга текста на Android.
Логика чанкинга портируется с RAG-сервера (500 слов, 100 слов overlap).

## Пошаговый план реализации
1. Скопировать IEEE-830-1998_RU.doc.docx → app/src/main/assets/IEEE-830-1998_RU.docx ✅
2. Создать DocxTextExtractor.kt (ZipInputStream + XmlPullParser) ✅
3. Создать LocalRagChunksDataSource.kt (кэш в filesDir/rag_chunks.json) ✅

## Файлы и изменения

```
app/src/main/assets/IEEE-830-1998_RU.docx
  +ДОБАВИТЬ  скопировать из rag_files/

data/local/DocxTextExtractor.kt
  +ДОБАВИТЬ  новый файл
             fun extract(inputStream: InputStream): String
             — ZipInputStream читает word/document.xml
             — XmlPullParser извлекает w:t теги из w:p параграфов
             — возвращает текст с переносами строк

data/local/LocalRagChunksDataSource.kt
  +ДОБАВИТЬ  новый файл
             data class ChunkedDocument(version: String, chunks: List<RagChunk>)
             suspend fun getChunks(): List<RagChunk>
             — проверяет кэш filesDir/rag_chunks.json
             — если нет кэша: открывает DOCX из assets, парсит, чанкует (500 слов, 100 overlap)
             — сохраняет кэш как JSON (kotlinx.serialization)
             — возвращает List<RagChunk>
```

## Резюме
После этапа: текст документа доступен в виде List<RagChunk> на Android, чанки кэшированы.

## Критерии успеха
- [x] DOCX файл присутствует в assets
- [x] DocxTextExtractor возвращает непустую строку
- [x] LocalRagChunksDataSource.getChunks() возвращает > 0 чанков
- [x] Повторный вызов getChunks() загружает из кэша (не парсит DOCX повторно)

## Статус
✅ Завершён

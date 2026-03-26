# Этап 1: ScoredChunk + RerankParser

## Задача
Создать два новых файла в data/local/:
- `ScoredChunk.kt` — обёртка RagChunk с similarity score для threshold-фильтрации
- `RerankParser.kt` — парсинг и нормализация ответа Gemma

## Файлы и изменения
```
ScoredChunk.kt  (новый, data/local/)
  +ДОБАВИТЬ  data class ScoredChunk(val chunk: RagChunk, val similarity: Float)

RerankParser.kt  (новый, data/local/)
  +ДОБАВИТЬ  object RerankParser
  +ДОБАВИТЬ  fun parse(rawResponse: String, maxCount: Int, candidatesSize: Int): List<Int>
             — regex \d+
             — .filter { it in 1..candidatesSize }
             — .distinct()
             — .take(maxCount)
             — fallback: emptyList()
```

## Критерии успеха
- [ ] Компилируется без ошибок
- [ ] parse("3,7,1,9,2", 5, 10) → [3,7,1,9,2]
- [ ] parse("0,1,100", 5, 8) → [1]
- [ ] parse("abc", 5, 8) → []

## Статус
✅ Завершён

# Этап 5: UI — MessageBubble с раскрывающимся блоком источников

## Статус: [x] Выполнено

## Описание задачи
Добавить в `MessageBubble` раскрывающийся блок "Источники (N)" под пузырём ассистента.
При нажатии раскрывается список источников с цитатами.

## Критерии успеха
- Кнопка "Источники (N)" отображается под пузырём ассистента, если `message.sources` не пуст
- При нажатии список раскрывается / скрывается
- Каждый источник показывает: иконку документа, имя файла, номер чанка, цитату
- У пользовательских сообщений и ассистента без источников — блок не отображается
- Визуально аккуратно, в духе существующего дизайна (Material3)

## Подробный план реализации

### Шаг 5.1 — Обновить `ChatView.kt`

Путь: `app/src/main/java/com/danichapps/simpleagent/presentation/ChatView.kt`

#### Изменение 1: Добавить import

```kotlin
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.graphics.Color
import com.danichapps.simpleagent.domain.model.RagSource
```

#### Изменение 2: Обновить `MessageBubble`

Текущий код (строки 187-213):
```kotlin
@Composable
private fun MessageBubble(message: Message) {
    val isUser = message.role == "user"
    val bubbleColor = ...
    val alignment = ...

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
        Box(...) {
            SelectionContainer {
                Text(text = message.content, ...)
            }
        }
    }
}
```

Новый код:
```kotlin
@Composable
private fun MessageBubble(message: Message) {
    val isUser = message.role == "user"
    val bubbleColor = if (isUser) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceVariant
    val alignment = if (isUser) Alignment.End else Alignment.Start

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .background(bubbleColor)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            SelectionContainer {
                Text(text = message.content, style = MaterialTheme.typography.bodyMedium)
            }
        }

        if (message.sources.isNotEmpty()) {
            SourcesBlock(sources = message.sources)
        }
    }
}
```

#### Изменение 3: Добавить composable `SourcesBlock`

```kotlin
@Composable
private fun SourcesBlock(sources: List<RagSource>) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.widthIn(max = 300.dp)) {
        TextButton(
            onClick = { expanded = !expanded },
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
        ) {
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp
                              else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.padding(end = 4.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Источники (${sources.size})",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                sources.forEach { source ->
                    SourceItem(source)
                }
            }
        }
    }
}
```

#### Изменение 4: Добавить composable `SourceItem`

```kotlin
@Composable
private fun SourceItem(source: RagSource) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 4.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${source.source} #${source.chunkIndex}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            SelectionContainer {
                Text(
                    text = "\u00AB${source.quote}\u00BB",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
```

Добавить missing imports:
```kotlin
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.TextButton
import com.danichapps.simpleagent.domain.model.RagSource
```

## Визуальный результат

```
┌─────────────────────────────┐
│ Ответ ассистента...         │
└─────────────────────────────┘
  [▼ Источники (2)]

  ┌────────────────────────┐
  │ 🔍 doc.pdf #2          │
  │ «Первые 150 символов   │
  │  текста чанка...»      │
  └────────────────────────┘
  ┌────────────────────────┐
  │ 🔍 faq.pdf #5          │
  │ «Первые 150 символов   │
  │  текста чанка...»      │
  └────────────────────────┘
```

## Зависимости
- Зависит от: Этап 1 (Message.sources, RagSource), Этап 4 (ViewModel заполняет sources)
- Финальный этап — после него задача завершена

## Примечания
- `AnimatedVisibility` из `androidx.compose.animation` — уже должен быть в зависимостях Compose
- Иконка `Search` используется как плейсхолдер для документа (нет иконки Document в Material Icons по умолчанию)
- `TextButton` не требует дополнительных зависимостей

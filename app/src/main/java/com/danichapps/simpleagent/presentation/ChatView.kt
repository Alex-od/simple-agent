package com.danichapps.simpleagent.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.danichapps.simpleagent.domain.model.ChatMode
import com.danichapps.simpleagent.domain.model.ChatTuningSettings
import com.danichapps.simpleagent.domain.model.LocalServerSettings
import com.danichapps.simpleagent.domain.model.Message
import com.danichapps.simpleagent.domain.model.RagSource
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatView(
    messages: List<Message>,
    isLoading: Boolean,
    error: String?,
    isRagEnabled: Boolean,
    isRagIndexed: Boolean,
    ragFolderName: String?,
    modelFileName: String?,
    embeddingModelFileName: String?,
    chatMode: ChatMode,
    modelState: ModelState,
    chatTuningSettings: ChatTuningSettings,
    localServerSettings: LocalServerSettings,
    onSendMessage: (String) -> Unit,
    onRagToggle: (Boolean) -> Unit,
    onChatModeChange: (ChatMode) -> Unit,
    onTemperatureChange: (Float) -> Unit,
    onMaxTokensChange: (String) -> Unit,
    onSystemPromptChange: (String) -> Unit,
    onServerUrlChange: (String) -> Unit,
    onServerModelChange: (String) -> Unit,
    onPickRagFolder: () -> Unit,
    onPickModelFile: () -> Unit,
    onPickEmbeddingModelFile: () -> Unit
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    text = "Настройки",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(16.dp)
                )
                HorizontalDivider()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Режим LLM",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        val options = listOf(
                            ChatMode.OPENAI to "OpenAI",
                            ChatMode.ON_DEVICE to "On-device",
                            ChatMode.LOCAL_SERVER to "Local server"
                        )
                        options.forEachIndexed { index, (mode, label) ->
                            SegmentedButton(
                                selected = chatMode == mode,
                                onClick = { onChatModeChange(mode) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
                            ) {
                                Text(label)
                            }
                        }
                    }
                }
                HorizontalDivider()
                ListItem(
                    headlineContent = { Text("Поиск по документам") },
                    supportingContent = { Text("Локальная база знаний") },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = isRagEnabled,
                            onCheckedChange = onRagToggle
                        )
                    }
                )
                HorizontalDivider()
                ListItem(
                    headlineContent = { Text("Папка базы RAG") },
                    supportingContent = { Text(ragFolderName ?: "Не выбрана") },
                    trailingContent = {
                        TextButton(onClick = onPickRagFolder) {
                            Text("Выбрать")
                        }
                    }
                )
                if (chatMode == ChatMode.ON_DEVICE) {
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text("Модель GGUF") },
                        supportingContent = { Text(modelFileName ?: "Не выбрана") },
                        trailingContent = {
                            TextButton(onClick = onPickModelFile) {
                                Text("Выбрать")
                            }
                        }
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text("Embedding GGUF") },
                        supportingContent = { Text(embeddingModelFileName ?: "Не выбрана") },
                        trailingContent = {
                            TextButton(onClick = onPickEmbeddingModelFile) {
                                Text("Выбрать")
                            }
                        }
                    )
                }
                if (chatMode == ChatMode.LOCAL_SERVER) {
                    HorizontalDivider()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        OutlinedTextField(
                            value = localServerSettings.baseUrl,
                            onValueChange = onServerUrlChange,
                            label = { Text("Server URL") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = localServerSettings.model,
                            onValueChange = onServerModelChange,
                            label = { Text("Server model") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                HorizontalDivider()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "Temperature: ${"%.2f".format(chatTuningSettings.temperature)}",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Slider(
                        value = chatTuningSettings.temperature,
                        onValueChange = onTemperatureChange,
                        valueRange = 0f..2f
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = chatTuningSettings.maxTokens.toString(),
                        onValueChange = onMaxTokensChange,
                        label = { Text("Max tokens") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = chatTuningSettings.systemPrompt,
                        onValueChange = onSystemPromptChange,
                        label = { Text("System prompt") },
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(imageVector = Icons.Default.Menu, contentDescription = "Меню")
                        }
                    },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("AI Agent")
                            Spacer(modifier = Modifier.width(8.dp))
                            ModeBadge(chatMode = chatMode)
                            if (isRagEnabled) {
                                Spacer(modifier = Modifier.width(8.dp))
                                val ragBgColor = if (isRagIndexed) Color(0xFF388E3C) else MaterialTheme.colorScheme.tertiary
                                Text(
                                    text = "RAG",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(ragBgColor)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                MessageList(
                    messages = messages,
                    isLoading = isLoading,
                    modifier = Modifier.weight(1f)
                )
                if (modelState is ModelState.Initializing) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                        Text(
                            text = when (chatMode) {
                                ChatMode.ON_DEVICE -> "Инициализация on-device llama.cpp..."
                                ChatMode.LOCAL_SERVER -> "Проверка Local server..."
                                ChatMode.OPENAI -> "Подготовка режима OpenAI..."
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                if (modelState is ModelState.Indexing) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                        Text(
                            text = "Построение индекса RAG...",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                if (modelState is ModelState.Error) {
                    SelectionContainer {
                        Text(
                            text = "Ошибка режима ${modeBadgeText(chatMode)}: ${modelState.message}",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }
                if (error != null) {
                    SelectionContainer {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }
                InputBar(isLoading = isLoading, onSend = onSendMessage)
            }
        }
    }
}

@Composable
private fun ModeBadge(chatMode: ChatMode) {
    val (label, background) = when (chatMode) {
        ChatMode.OPENAI -> "OPENAI" to MaterialTheme.colorScheme.primary
        ChatMode.ON_DEVICE -> "ON-DEVICE" to MaterialTheme.colorScheme.secondary
        ChatMode.LOCAL_SERVER -> "SERVER" to MaterialTheme.colorScheme.tertiary
    }

    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = Color.White,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

private fun modeBadgeText(chatMode: ChatMode): String = when (chatMode) {
    ChatMode.OPENAI -> "OpenAI"
    ChatMode.ON_DEVICE -> "On-device"
    ChatMode.LOCAL_SERVER -> "Local server"
}

@Composable
private fun MessageList(
    messages: List<Message>,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(0)
    }

    LazyColumn(
        state = listState,
        reverseLayout = true,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (isLoading) {
            item {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                    CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                }
            }
        }
        items(messages.reversed()) { message ->
            MessageBubble(message)
        }
    }
}

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

@Composable
private fun SourcesBlock(sources: List<RagSource>) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.widthIn(max = 300.dp)) {
        TextButton(
            onClick = { expanded = !expanded },
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
        ) {
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
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

@Composable
private fun SourceItem(source: RagSource) {
    Card(
        modifier = Modifier.widthIn(max = 300.dp),
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

@Composable
private fun InputBar(isLoading: Boolean, onSend: (String) -> Unit) {
    var text by remember { mutableStateOf("") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.weight(1f),
            placeholder = { Text("Введите сообщение...") },
            maxLines = 4,
            enabled = !isLoading,
            shape = RoundedCornerShape(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
            onClick = {
                val trimmed = text.trim()
                if (trimmed.isNotEmpty()) {
                    onSend(trimmed)
                    text = ""
                }
            },
            enabled = !isLoading && text.isNotBlank()
        ) {
            Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
        }
    }
}

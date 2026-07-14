package com.darkps.agentterminal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.darkps.agentterminal.model.AiModel
import com.darkps.agentterminal.model.ChatMessage
import com.darkps.agentterminal.ui.components.MessageBubble
import com.darkps.agentterminal.ui.components.ModelSelector
import com.darkps.agentterminal.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    messages: List<ChatMessage>,
    isLoading: Boolean,
    selectedModel: AiModel,
    models: List<AiModel>,
    onSendMessage: (String) -> Unit,
    onModelSelected: (AiModel) -> Unit,
    onClearChat: () -> Unit,
    onNavigateToFiles: () -> Unit,
    onNavigateToLogs: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    // Auto scroll to bottom
    LaunchedEffect(messages.size, messages.lastOrNull()?.content) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(AccentSecondary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "D",
                                color = DarkBackground,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "DarkPS Agent",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = "Terminal",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextTertiary
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToLogs) {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = "Event Log",
                            tint = TextSecondary
                        )
                    }
                    IconButton(onClick = onNavigateToFiles) {
                        Icon(
                            Icons.Default.Folder,
                            contentDescription = "File Manager",
                            tint = TextSecondary
                        )
                    }
                    IconButton(onClick = onClearChat) {
                        Icon(
                            Icons.Default.DeleteSweep,
                            contentDescription = "Clear Chat",
                            tint = TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                    scrolledContainerColor = DarkBackground
                ),
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            Surface(
                color = DarkBackground,
                tonalElevation = 0.dp,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .navigationBarsPadding() // Fix: Add padding for navigation bar
                        .imePadding()            // Fix: Add padding for keyboard
                ) {
                    // Model Selector
                    ModelSelector(
                        selectedModel = selectedModel,
                        models = models,
                        onModelSelected = onModelSelected,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )

                    // Input area
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = {
                                Text(
                                    "Type a message...",
                                    color = TextTertiary
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp, max = 120.dp),
                            maxLines = 4,
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = AccentPrimary,
                                unfocusedBorderColor = DarkBorder,
                                cursorColor = AccentPrimary,
                                focusedContainerColor = DarkSurface,
                                unfocusedContainerColor = DarkSurface
                            ),
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Send
                            ),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (inputText.isNotBlank() && !isLoading) {
                                        onSendMessage(inputText.trim())
                                        inputText = ""
                                    }
                                }
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))

                        // Send / Retry button
                        if (isLoading) {
                            FilledIconButton(
                                onClick = {},
                                modifier = Modifier
                                    .size(48.dp)
                                    .padding(bottom = 2.dp),
                                shape = CircleShape,
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = TextTertiary,
                                    contentColor = DarkBackground
                                ),
                                enabled = false
                            ) {
                                Icon(
                                    Icons.Default.HourglassEmpty,
                                    contentDescription = "Loading"
                                )
                            }
                        } else {
                            FilledIconButton(
                                onClick = {
                                    if (inputText.isNotBlank()) {
                                        onSendMessage(inputText.trim())
                                        inputText = ""
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .padding(bottom = 2.dp),
                                shape = CircleShape,
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = if (inputText.isBlank()) TextTertiary else AccentPrimary,
                                    contentColor = DarkBackground
                                ),
                                enabled = inputText.isNotBlank()
                            ) {
                                Icon(
                                    Icons.Default.Send,
                                    contentDescription = "Send"
                                )
                            }
                        }
                    }
                }
            }
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        // Messages list
        if (messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "DarkPS Agent Terminal",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextTertiary.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Select a model and start chatting",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextTertiary.copy(alpha = 0.3f)
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(messages, key = { it.id }) { message ->
                        MessageBubble(message = message)
                    }

                    // Bottom spacing for scrolling
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                // Scroll to bottom FAB
                if (messages.size > 5) {
                    FloatingActionButton(
                        onClick = {
                            listState.animateScrollToItem(messages.size - 1)
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                            .size(40.dp),
                        containerColor = AccentPrimary.copy(alpha = 0.8f),
                        contentColor = DarkBackground
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = "Scroll to bottom",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

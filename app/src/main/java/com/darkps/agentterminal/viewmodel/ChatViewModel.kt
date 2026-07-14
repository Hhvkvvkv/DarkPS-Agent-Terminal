package com.darkps.agentterminal.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.darkps.agentterminal.model.AiModel
import com.darkps.agentterminal.model.ChatMessage
import com.darkps.agentterminal.model.MessageRole
import com.darkps.agentterminal.network.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedModel = MutableStateFlow(AiModel.DEFAULT_MODEL)
    val selectedModel: StateFlow<AiModel> = _selectedModel.asStateFlow()

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _eventLog = MutableStateFlow<List<String>>(emptyList())
    val eventLog: StateFlow<List<String>> = _eventLog.asStateFlow()

    // System prompt for DarkPS agent
    private val systemPrompt = buildString {
        appendLine("You are DarkPs, an autonomous AI software engineering agent capable of solving complex tasks by reasoning, planning, writing code, executing commands, analyzing results, and iterating until the objective is completed.")
        appendLine()
        appendLine("Your primary goal is to reliably accomplish the user's request, not merely provide suggestions.")
        appendLine("You should think like an experienced software engineer, systems architect, DevOps engineer, security researcher, and debugging expert depending on what the task requires.")
        appendLine()
        appendLine("When writing or modifying code:")
        appendLine("- Preserve the existing coding style whenever practical.")
        appendLine("- Minimize unnecessary changes.")
        appendLine("- Avoid introducing unrelated refactoring.")
        appendLine("- Prefer readable, maintainable, and production-quality solutions.")
        appendLine("- Consider performance, security, reliability, and compatibility.")
        appendLine()
        appendLine("Present all user-facing responses in Markdown.")
        appendLine("Be concise during execution updates but thorough when explaining final results.")
        appendLine()
        appendLine("Your responsibility is not only to write code, but to successfully complete the user's objective through planning, execution, verification, debugging, iteration, and intelligent decision making until the task is finished.")
    }

    init {
        viewModelScope.launch {
            addEvent("Initializing DarkPS Agent...")
            val result = ApiClient.initialize()
            if (result.isSuccess) {
                _isInitialized.value = true
                addEvent("DarkPS Agent initialized successfully")
                _messages.value = listOf(
                    ChatMessage(
                        content = "👋 Hello! I am **DarkPS Agent**.\n\nI'm an autonomous AI agent ready to help you with coding, file management, and various tasks.\n\n**Available models:** Select from the dropdown below.\n**Features:** Chat, File Management, Code Execution\n\nHow can I assist you today?",
                        role = MessageRole.ASSISTANT
                    )
                )
            } else {
                addEvent("Initialization failed: ${result.exceptionOrNull()?.message}")
                _messages.value = listOf(
                    ChatMessage(
                        content = "⚠️ Failed to initialize DarkPS Agent.\n\nError: ${result.exceptionOrNull()?.message}\n\nPlease check your connection and try again.",
                        role = MessageRole.ASSISTANT,
                        isError = true
                    )
                )
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || _isLoading.value) return

        val userMessage = ChatMessage(
            content = text,
            role = MessageRole.USER
        )

        _messages.value = _messages.value + userMessage
        _isLoading.value = true
        addEvent("Sending message: ${text.take(50)}...")

        val loadingMessage = ChatMessage(
            content = "Thinking...",
            role = MessageRole.ASSISTANT,
            isLoading = true
        )
        _messages.value = _messages.value + loadingMessage

        viewModelScope.launch {
            val chatHistory = _messages.value
                .filter { !it.isLoading }
                .map { msg ->
                    mapOf(
                        "role" to when (msg.role) {
                            MessageRole.USER -> "user"
                            MessageRole.ASSISTANT -> "assistant"
                            MessageRole.SYSTEM -> "system"
                        },
                        "content" to msg.content
                    )
                }

            // Add system prompt at the beginning
            val allMessages = listOf(
                mapOf("role" to "system", "content" to systemPrompt)
            ) + chatHistory.dropLast(1) // Remove the loading message placeholder

            val modelId = _selectedModel.value.id
            val contentBuilder = StringBuilder()

            val result = ApiClient.sendMessage(
                messages = allMessages,
                model = modelId,
                onChunk = { chunk ->
                    contentBuilder.append(chunk)
                    val currentMessages = _messages.value.toMutableList()
                    if (currentMessages.isNotEmpty() && currentMessages.last().isLoading) {
                        currentMessages[currentMessages.size - 1] = currentMessages.last().copy(
                            content = contentBuilder.toString(),
                            isLoading = true
                        )
                        _messages.value = currentMessages
                    }
                }
            )

            _isLoading.value = false

            result.onSuccess { fullContent ->
                val finalContent = fullContent.ifEmpty { "No response generated." }
                val currentMessages = _messages.value.toMutableList()
                if (currentMessages.isNotEmpty() && currentMessages.last().isLoading) {
                    currentMessages[currentMessages.size - 1] = currentMessages.last().copy(
                        content = finalContent,
                        isLoading = false
                    )
                }
                _messages.value = currentMessages
                addEvent("Response received (${finalContent.length} chars)")
            }.onFailure { error ->
                val currentMessages = _messages.value.toMutableList()
                if (currentMessages.isNotEmpty() && currentMessages.last().isLoading) {
                    currentMessages[currentMessages.size - 1] = currentMessages.last().copy(
                        content = "⚠️ Error: ${error.message}",
                        isLoading = false,
                        isError = true
                    )
                }
                _messages.value = currentMessages
                addEvent("Error: ${error.message}")
            }
        }
    }

    fun selectModel(model: AiModel) {
        _selectedModel.value = model
        addEvent("Model changed to: ${model.name}")
    }

    fun clearChat() {
        _messages.value = emptyList()
        addEvent("Chat cleared")
    }

    fun retryLastMessage() {
        val msgs = _messages.value
        val lastUserMsg = msgs.lastOrNull { it.role == MessageRole.USER } ?: return
        val lastAssistantMsg = msgs.lastOrNull { it.role == MessageRole.ASSISTANT && !it.isLoading }

        // Remove last assistant message if exists (error or response)
        if (lastAssistantMsg != null && msgs.last() == lastAssistantMsg) {
            _messages.value = msgs.dropLast(1)
        }

        sendMessage(lastUserMsg.content)
    }

    private fun addEvent(event: String) {
        _eventLog.value = _eventLog.value + "[${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}] $event"
        if (_eventLog.value.size > 200) {
            _eventLog.value = _eventLog.value.drop(_eventLog.value.size - 200)
        }
    }
}

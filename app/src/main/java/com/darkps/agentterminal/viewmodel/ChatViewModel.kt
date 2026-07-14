package com.darkps.agentterminal.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.darkps.agentterminal.model.AiModel
import com.darkps.agentterminal.model.ChatMessage
import com.darkps.agentterminal.model.MessageRole
import com.darkps.agentterminal.model.ToolAction
import com.darkps.agentterminal.model.ToolActionParser
import com.darkps.agentterminal.network.ApiClient
import com.darkps.agentterminal.util.FileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application

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

    // System prompt for DarkPS agent with ALL tools and two-stage project generation
    private val systemPrompt = buildString {
        appendLine("You are DarkPs, an autonomous AI software engineering agent running on Android.")
        appendLine("You have the ability to CREATE FILES on the device, search the web, and manage projects.")
        appendLine()
        appendLine("=== YOUR CAPABILITIES ===")
        appendLine()
        appendLine("1. **FILE CREATION TOOLS**:")
        appendLine("   You can create files and directories using [TOOL] blocks in your response.")
        appendLine()
        appendLine("   To create a directory:")
        appendLine("   [TOOL]")
        appendLine("   {\"tool\": \"create_dir\", \"path\": \"project-name/src\"}")
        appendLine("   [/TOOL]")
        appendLine()
        appendLine("   To create a single file:")
        appendLine("   [TOOL]")
        appendLine("   {\"tool\": \"create_file\", \"path\": \"project-name/index.html\", \"content\": \"<html>...\\n...\\n</html>\"}")
        appendLine("   [/TOOL]")
        appendLine()
        appendLine("   To create multiple files at once (RECOMMENDED for projects):")
        appendLine("   [TOOL]")
        appendLine("   {\"tool\": \"create_files\", \"files\": [")
        appendLine("     {\"path\": \"project/index.html\", \"content\": \"<html>...</html>\"},")
        appendLine("     {\"path\": \"project/style.css\", \"content\": \"body {...}\"},")
        appendLine("     {\"path\": \"project/script.js\", \"content\": \"console.log(...)\"}")
        appendLine("   ]}")
        appendLine("   [/TOOL]")
        appendLine()
        appendLine("2. **WEB SEARCH**:")
        appendLine("   [TOOL]")
        appendLine("   {\"tool\": \"web_search\", \"query\": \"your search query\"}")
        appendLine("   [/TOOL]")
        appendLine()
        appendLine("3. **WEB FETCH** (get content from a URL):")
        appendLine("   [TOOL]")
        appendLine("   {\"tool\": \"web_fetch\", \"url\": \"https://example.com\"}")
        appendLine("   [/TOOL]")
        appendLine()
        appendLine("=== PROJECT GENERATION WORKFLOW ===")
        appendLine()
        appendLine("When the user asks you to CREATE a project (website, app, etc.), follow this exact workflow:")
        appendLine()
        appendLine("**PHASE 1: ANALYZE**")
        appendLine("- Analyze the user's request and identify all requirements")
        appendLine("- List the files you will create and their purposes")
        appendLine("- Show the project structure clearly to the user")
        appendLine()
        appendLine("**PHASE 2: CREATE**")
        appendLine("- Create the project directory first")
        appendLine("- Then create ALL files using create_files or individual create_file calls")
        appendLine("- Show progress messages like: '📁 Creating project...' '📄 Creating index.html...'")
        appendLine("- Include complete, working code in every file")
        appendLine()
        appendLine("**PHASE 3: REVIEW**")
        appendLine("- After creating all files, review the code")
        appendLine("- Check for any errors or issues")
        appendLine("- If errors are found, fix them by recreating the file")
        appendLine("- Tell the user the project is complete")
        appendLine()
        appendLine("=== IMPORTANT ===")
        appendLine("- Always use the [TOOL] format for file operations")
        appendLine("- Show progress messages in your response text")
        appendLine("- Use \\n for newlines in JSON strings")
        appendLine("- Create complete, production-ready code")
        appendLine("- Support ALL programming languages and file types")
        appendLine("- Web search results will be automatically fetched and provided")
        appendLine()
        appendLine("Your primary goal is to reliably accomplish the user's request by creating actual files on their device.")
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
                        content = buildString {
                            appendLine("👋 Welcome to **DarkPS Agent Terminal**!")
                            appendLine()
                            appendLine("أهلاً بك في وكيل DarkPS الذكي")
                            appendLine()
                            appendLine("**قدراتي:**")
                            appendLine("- 💻 **إنشاء المشاريع**: اطلب أي مشروع (موقع، تطبيق، برنامج) وسأقوم بإنشاء جميع الملفات")
                            appendLine("- 📁 **إدارة الملفات**: إنشاء، حذف، قراءة، كتابة")
                            appendLine("- 🔍 **البحث في الويب**: أستطيع البحث وجلب المعلومات")
                            appendLine("- 🤖 **دردشة ذكية**: مع 22+ نموذج AI")
                            appendLine()
                            appendLine("**How to use:**")
                            appendLine("- *'Create a website with HTML, CSS, JS'*")
                            appendLine("- *'Build a Kotlin Android app'*")
                            appendLine("- *'Search the web for...'*")
                            appendLine("- *'Create a Python script for...'*")
                            appendLine()
                            appendLine("✨ **Models available:** Select from the dropdown below")
                        },
                        role = MessageRole.ASSISTANT
                    )
                )
            } else {
                val error = result.exceptionOrNull()
                addEvent("⚠️ Initialization failed: ${error?.message}")
                _messages.value = listOf(
                    ChatMessage(
                        content = """⚠️ **Failed to initialize DarkPS Agent.**

خطأ: ${error?.message}

💡 **Tips:**
1. تحقق من اتصال الإنترنت
2. حاول مرة أخرى
3. جرب نموذج آخر من القائمة

Error: ${error?.message}""",
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
        addEvent("📤 Sending: ${text.take(80)}...")

        val loadingMessage = ChatMessage(
            content = "⏳ Thinking...",
            role = MessageRole.ASSISTANT,
            isLoading = true
        )
        _messages.value = _messages.value + loadingMessage

        viewModelScope.launch {
            try {
                processMessage(text)
            } catch (e: Exception) {
                _isLoading.value = false
                val currentMessages = _messages.value.toMutableList()
                if (currentMessages.isNotEmpty() && currentMessages.last().isLoading) {
                    currentMessages[currentMessages.size - 1] = currentMessages.last().copy(
                        content = "❌ **Unexpected Error:**\n${e.message}\n\n👉 Click Retry (↻) to try again.",
                        isLoading = false,
                        isError = true
                    )
                }
                _messages.value = currentMessages
                addEvent("❌ Error: ${e.message}")
            }
        }
    }

    private suspend fun processMessage(text: String) {
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

        // Build messages with system prompt
        val allMessages = mutableListOf(
            mapOf("role" to "system", "content" to systemPrompt)
        )
        allMessages.addAll(chatHistory.dropLast(1)) // Remove loading placeholder

        val modelId = _selectedModel.value.id
        val contentBuilder = StringBuilder()

        // Send to API
        val result = ApiClient.sendMessage(
            messages = allMessages,
            model = modelId,
            onChunk = { chunk ->
                contentBuilder.append(chunk)
                updateLoadingMessage(contentBuilder.toString())
            }
        )

        _isLoading.value = false

        result.onSuccess { fullContent ->
            addEvent("📥 Response received (${fullContent.length} chars)")

            // Parse and execute tool actions
            val toolActions = ToolActionParser.parseToolActions(fullContent)
            val cleanContent = ToolActionParser.removeToolBlocks(fullContent)

            if (toolActions.isNotEmpty()) {
                addEvent("🔧 Found ${toolActions.size} tool action(s)")

                // Execute tool actions
                executeToolActions(toolActions)

                // Update message with clean content and file creation summary
                val summary = buildString {
                    appendLine("✅ **Files Created Successfully!**")
                    appendLine()
                    for (action in toolActions) {
                        when (action.tool) {
                            "create_file" -> appendLine("📄 `${action.path}`")
                            "create_dir" -> appendLine("📁 `${action.path}/`")
                            "create_files" -> {
                                action.files?.forEach { file ->
                                    appendLine("📄 `${file.path}`")
                                }
                            }
                            "web_search" -> appendLine("🔍 Search results for: `${action.query}`")
                            "web_fetch" -> appendLine("🌐 Fetched: `${action.url}`")
                        }
                    }
                    appendLine()
                    if (cleanContent.isNotBlank()) {
                        appendLine("---")
                        appendLine(cleanContent)
                    }
                }

                updateLastMessage(summary, isLoading = false)
            } else if (fullContent.isNotBlank()) {
                val finalContent = fullContent.ifEmpty { "No response generated." }
                updateLastMessage(finalContent, isLoading = false)
            } else {
                updateLastMessage("⚠️ The model returned an empty response. Please try again with a different model.", isLoading = false, isError = true)
            }

        }.onFailure { error ->
            val errorMsg = error.message ?: "Unknown error"
            addEvent("❌ Error: $errorMsg")
            updateLastMessage(
                "❌ **Error Occurred**\n\n$errorMsg\n\n👉 **Suggestions:**\n1. Try a different model from the dropdown\n2. Check your internet connection\n3. Tap ↻ Retry to try again",
                isLoading = false,
                isError = true
            )
        }
    }

    private suspend fun executeToolActions(actions: List<ToolAction>) {
        for ((index, action) in actions.withIndex()) {
            addEvent("⚡ Executing: ${action.tool} (${index + 1}/${actions.size})")

            when (action.tool) {
                "create_dir" -> {
                    action.path?.let { path ->
                        val success = withContext(Dispatchers.IO) {
                            FileManager.createFile(appContext, path, isDirectory = true)
                        }
                        addEvent(if (success) "📁 Created directory: $path" else "❌ Failed to create directory: $path")
                    }
                }
                "create_file" -> {
                    action.path?.let { path ->
                        val content = action.content ?: ""
                        val success = withContext(Dispatchers.IO) {
                            // Ensure parent directory exists
                            val parent = path.split("/").dropLast(1).joinToString("/")
                            if (parent.isNotBlank()) {
                                FileManager.createFile(appContext, parent, isDirectory = true)
                            }
                            FileManager.createFile(appContext, path, isDirectory = false)
                            FileManager.writeFileContent(appContext, path, content)
                        }
                        addEvent(if (success) "📄 Created file: $path (${content.length} chars)" else "❌ Failed to create file: $path")
                    }
                }
                "create_files" -> {
                    action.files?.let { files ->
                        addEvent("📁 Creating project with ${files.size} files...")
                        for ((i, file) in files.withIndex()) {
                            val success = withContext(Dispatchers.IO) {
                                // Ensure parent directory exists
                                if (file.getFolderPath().isNotBlank()) {
                                    FileManager.createFile(appContext, file.getFolderPath(), isDirectory = true)
                                }
                                FileManager.createFile(appContext, file.path, isDirectory = false)
                                FileManager.writeFileContent(appContext, file.path, file.content)
                            }
                            addEvent(if (success) "  ✅ [${i + 1}/${files.size}] Created: ${file.path}" else "  ❌ Failed: ${file.path}")
                        }
                        addEvent("✅ Project created: ${files.size} files total")
                    }
                }
                "web_search" -> {
                    action.query?.let { query ->
                        addEvent("🔍 Searching for: $query")
                        val searchResult = ApiClient.webSearch(query)
                        searchResult.onSuccess { result ->
                            // Add search result as a system message for context
                            // But also show it in the event log
                            addEvent("🔍 Search completed")
                            // We don't modify messages here, the search result will be in the next turn
                        }.onFailure { error ->
                            addEvent("❌ Search failed: ${error.message}")
                        }
                    }
                }
                "web_fetch" -> {
                    action.url?.let { url ->
                        addEvent("🌐 Fetching: $url")
                        val fetchResult = ApiClient.webFetch(url)
                        fetchResult.onSuccess {
                            addEvent("🌐 Fetched successfully")
                        }.onFailure { error ->
                            addEvent("❌ Fetch failed: ${error.message}")
                        }
                    }
                }
            }
        }
    }

    fun selectModel(model: AiModel) {
        _selectedModel.value = model
        addEvent("🔄 Model changed to: ${model.name}")
    }

    fun clearChat() {
        _messages.value = emptyList()
        addEvent("🗑️ Chat cleared")
    }

    fun retryLastMessage() {
        val msgs = _messages.value
        val lastUserMsg = msgs.lastOrNull { it.role == MessageRole.USER } ?: return
        val lastAssistantMsg = msgs.lastOrNull { it.role == MessageRole.ASSISTANT && !it.isLoading }

        // Remove last assistant message if exists (error or response)
        if (lastAssistantMsg != null && msgs.isNotEmpty() && msgs.last() == lastAssistantMsg) {
            _messages.value = msgs.dropLast(1)
        }

        addEvent("🔄 Retrying last message...")
        sendMessage(lastUserMsg.content)
    }

    private fun updateLoadingMessage(content: String) {
        val currentMessages = _messages.value.toMutableList()
        if (currentMessages.isNotEmpty() && currentMessages.last().isLoading) {
            currentMessages[currentMessages.size - 1] = currentMessages.last().copy(
                content = content,
                isLoading = true
            )
            _messages.value = currentMessages
        }
    }

    private fun updateLastMessage(content: String, isLoading: Boolean = false, isError: Boolean = false) {
        val currentMessages = _messages.value.toMutableList()
        if (currentMessages.isNotEmpty()) {
            val lastIndex = currentMessages.size - 1
            // Check if we should update the loading message or add a new one
            if (currentMessages[lastIndex].isLoading) {
                currentMessages[lastIndex] = currentMessages[lastIndex].copy(
                    content = content,
                    isLoading = isLoading,
                    isError = isError
                )
            } else {
                currentMessages.add(
                    ChatMessage(
                        content = content,
                        role = MessageRole.ASSISTANT,
                        isError = isError
                    )
                )
            }
            _messages.value = currentMessages
        }
    }

    private fun addEvent(event: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date())
        _eventLog.value = _eventLog.value + "[$timestamp] $event"
        if (_eventLog.value.size > 200) {
            _eventLog.value = _eventLog.value.drop(_eventLog.value.size - 200)
        }
    }
}

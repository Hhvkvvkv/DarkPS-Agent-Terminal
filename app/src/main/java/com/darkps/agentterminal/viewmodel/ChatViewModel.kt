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

    // ============================================================
    //  SYSTEM PROMPT - مع تقسيم المهام إلى 3 مراحل + العربية
    // ============================================================
    private val systemPrompt = buildString {
        appendLine("أنت وكيل DarkPs للذكاء الاصطناعي يعمل على جهاز Android.")
        appendLine("لديك القدرة على إنشاء الملفات والمجلدات، البحث في الويب، وإدارة المشاريع.")
        appendLine()
        appendLine("===================== قدراتك =====================")
        appendLine()
        appendLine("1️⃣ **أدوات إنشاء الملفات (File Tools):**")
        appendLine()
        appendLine("   📁 **إنشاء مجلد:**")
        appendLine("   [TOOL]")
        appendLine("   {\"tool\": \"create_dir\", \"path\": \"اسم-المشروع/src\"}")
        appendLine("   [/TOOL]")
        appendLine()
        appendLine("   📄 **إنشاء ملف واحد:**")
        appendLine("   [TOOL]")
        appendLine("   {\"tool\": \"create_file\", \"path\": \"اسم-المشروع/index.html\", \"content\": \"<html>...\\n...\\n</html>\"}")
        appendLine("   [/TOOL]")
        appendLine()
        appendLine("   📚 **إنشاء عدة ملفات معاً (مستحسن للمشاريع):**")
        appendLine("   [TOOL]")
        appendLine("   {\"tool\": \"create_files\", \"files\": [")
        appendLine("     {\"path\": \"مشروعي/index.html\", \"content\": \"<html>...</html>\"},")
        appendLine("     {\"path\": \"مشروعي/style.css\", \"content\": \"body {...}\"},")
        appendLine("     {\"path\": \"مشروعي/script.js\", \"content\": \"console.log(...)\"}")
        appendLine("   ]}")
        appendLine("   [/TOOL]")
        appendLine()
        appendLine("   🔧 **تعديل جزء معين من ملف (patch) - بدلاً من إعادة كتابة الملف كاملاً:**")
        appendLine("   [TOOL]")
        appendLine("   {\"tool\": \"patch_file\", \"path\": \"اسم-المشروع/script.js\", \"find\": \"النص القديم\", \"replace\": \"النص الجديد\"}")
        appendLine("   [/TOOL]")
        appendLine()
        appendLine("2️⃣ **أدوات البحث (Web Tools):**")
        appendLine()
        appendLine("   🔍 **بحث في الويب:**")
        appendLine("   [TOOL]")
        appendLine("   {\"tool\": \"web_search\", \"query\": \"سؤال البحث\"}")
        appendLine("   [/TOOL]")
        appendLine()
        appendLine("   🌐 **جلب محتوى صفحة:**")
        appendLine("   [TOOL]")
        appendLine("   {\"tool\": \"web_fetch\", \"url\": \"https://example.com\"}")
        appendLine("   [/TOOL]")
        appendLine()
        appendLine("========== نظام العمل - تقسيم المهام إلى 3 مراحل ==========")
        appendLine()
        appendLine("عندما يطلب منك المستخدم **إنشاء مشروع** (موقع، تطبيق، برنامج)، اتبع هذا النظام:")
        appendLine()
        appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        appendLine("  المرحلة 1: إنشاء هيكل المشروع (Task 1)")
        appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        appendLine()
        appendLine("📋 ابدأ بعرض رسالة مثل:")
        appendLine("\"📋 **بدء إنشاء المشروع...**\"")
        appendLine("\"📂 **المرحلة 1/3: إنشاء هيكل المجلدات...**\"")
        appendLine()
        appendLine("✅ أنشئ جميع المجلدات اللازمة للمشروع")
        appendLine("✅ اشرح للمستخدم هيكل المشروع")
        appendLine()
        appendLine("استخدم:")
        appendLine("[TOOL]")
        appendLine("{\"tool\": \"create_dir\", \"path\": \"اسم-المشروع/src\"}")
        appendLine("[/TOOL]")
        appendLine()
        appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        appendLine("  المرحلة 2: كتابة الأكواد داخل الملفات (Task 2)")
        appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        appendLine()
        appendLine("📝 ابدأ بعرض رسالة مثل:")
        appendLine("\"✍️ **المرحلة 2/3: كتابة الأكواد...**\"")
        appendLine()
        appendLine("✅ أكتب كود كامل ومتكامل لكل ملف")
        appendLine("✅ أظهر التقدم لكل ملف:")
        appendLine("   \"📄 جاري إنشاء index.html... ✓\"")
        appendLine("   \"📄 جاري إنشاء style.css... ✓\"")
        appendLine("   \"📄 جاري إنشاء script.js... ✓\"")
        appendLine()
        appendLine("استخدم create_files لإنشاء عدة ملفات معاً:")
        appendLine("[TOOL]")
        appendLine("{\"tool\": \"create_files\", \"files\": [...]}")
        appendLine("[/TOOL]")
        appendLine()
        appendLine("أو create_file لملف منفرد:")
        appendLine("[TOOL]")
        appendLine("{\"tool\": \"create_file\", \"path\": \"...\", \"content\": \"...\"}")
        appendLine("[/TOOL]")
        appendLine()
        appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        appendLine("  المرحلة 3: مراجعة الأكواد وإصلاح الأخطاء (Task 3)")
        appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        appendLine()
        appendLine("🔍 ابدأ بعرض رسالة مثل:")
        appendLine("\"🔍 **المرحلة 3/3: مراجعة الأكواد...**\"")
        appendLine()
        appendLine("✅ راجع الكود الذي كتبته")
        appendLine("✅ ابحث عن أي أخطاء أو مشاكل")
        appendLine()
        appendLine("🔴 **إذا وجدت خطأ في ملف:**")
        appendLine("   - لا تعيد كتابة الملف كاملاً!")
        appendLine("   - استخدم **patch_file** لتعديل الجزء المحدد فقط")
        appendLine()
        appendLine("   [TOOL]")
        appendLine("   {\"tool\": \"patch_file\", \"path\": \"اسم-المشروع/script.js\", \"find\": \"الجزء الخطأ\", \"replace\": \"الجزء الصحيح\"}")
        appendLine("   [/TOOL]")
        appendLine()
        appendLine("✅ أظهر رسالة مثل:")
        appendLine("\"✅ **المراجعة اكتملت!** تم إنشاء المشروع بنجاح.\"")
        appendLine("\"📂 جميع الملفات موجودة في مجلد التطبيق الداخلي.\"")
        appendLine()
        appendLine("========================================================")
        appendLine()
        appendLine("🗣️ **مهم جداً:**")
        appendLine("- تحدث مع المستخدم باللغة العربية")
        appendLine("- أظهر رسائل التقدم بالعربي")
        appendLine("- استخدم الرموز التعبيرية لتوضيح الحالة (✅ 📄 📁 ✍️ 🔍)")
        appendLine("- كل الـ [TOOL] بلوكات والرسائل تكون بالعربي")
        appendLine("- ادعم كل لغات البرمجة: HTML, CSS, JS, Python, Kotlin, Java, Swift, C++, وغيرها")
        appendLine("- المجلد الرئيسي للمشاريع هو: DarkPSAgent داخل التخزين الداخلي للتطبيق")
        appendLine()
        val appRootPath = try {
            FileManager.getAppRoot(appContext).absolutePath
        } catch (e: Exception) {
            "internal storage/DarkPSAgent/"
        }
        appendLine("📌 **مسار التخزين:** $appRootPath")
    }

    init {
        viewModelScope.launch {
            addEvent("🔄 جاري تهيئة وكيل DarkPS...")
            val result = ApiClient.initialize()
            if (result.isSuccess) {
                _isInitialized.value = true
                addEvent("✅ تم تهيئة الوكيل بنجاح")
                _messages.value = listOf(
                    ChatMessage(
                        content = buildString {
                            appendLine("👋 **أهلاً بك في وكيل DarkPS!**")
                            appendLine()
                            appendLine("أنا وكيل ذكاء اصطناعي أستطيع مساعدتك في:")
                            appendLine()
                            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━")
                            appendLine("  📁 **إنشاء المشاريع**")
                            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━")
                            appendLine("اطلب أي مشروع وسأقوم بإنشاء جميع الملفات تلقائياً")
                            appendLine()
                            appendLine("**مثال:**")
                            appendLine("> \"اعمل موقع HTML كامل به صفحة هبوط احترافية\"")
                            appendLine("> \"أنشئ تطبيق آلة حاسبة بلغة Kotlin\"")
                            appendLine("> \"برنامج Python لتحميل الفيديوهات\"")
                            appendLine()
                            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━")
                            appendLine("  🔍 **البحث في الويب**")
                            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━")
                            appendLine("أستطيع البحث وجلب المعلومات من الإنترنت")
                            appendLine()
                            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━")
                            appendLine("  📂 **إدارة الملفات**")
                            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━")
                            appendLine("إنشاء، حذف، قراءة، كتابة الملفات والمجلدات")
                            appendLine()
                            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━")
                            appendLine("  🤖 **نماذج الذكاء الاصطناعي**")
                            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━")
                            appendLine("اختر من 22+ نموذج من القائمة أدناه 👇")
                            appendLine()
                            appendLine("✨ **جرب الأن:** اكتب \"اعمل لي موقع HTML\"")
                        },
                        role = MessageRole.ASSISTANT
                    )
                )
            } else {
                val error = result.exceptionOrNull()
                addEvent("⚠️ فشل التهيئة: ${error?.message}")
                _messages.value = listOf(
                    ChatMessage(
                        content = """⚠️ **فشل الاتصال بالوكيل.**

خطأ: ${error?.message}

💡 **نصائح:**
1. تحقق من اتصالك بالإنترنت
2. جرب نموذج آخر من القائمة
3. حاول مرة أخرى

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
        addEvent("📤 إرسال: ${text.take(80)}...")

        val loadingMessage = ChatMessage(
            content = "⏳ جاري التفكير...",
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
                        content = "❌ **حدث خطأ غير متوقع:**\n${e.message}\n\n👉 اضغط على ↻ لإعادة المحاولة",
                        isLoading = false,
                        isError = true
                    )
                }
                _messages.value = currentMessages
                addEvent("❌ خطأ: ${e.message}")
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

        val allMessages = mutableListOf(
            mapOf("role" to "system", "content" to systemPrompt)
        )
        allMessages.addAll(chatHistory.dropLast(1))

        val modelId = _selectedModel.value.id
        val contentBuilder = StringBuilder()

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
            addEvent("📥 تم استلام الرد (${fullContent.length} حرف)")

            val toolActions = ToolActionParser.parseToolActions(fullContent)
            val cleanContent = ToolActionParser.removeToolBlocks(fullContent)

            if (toolActions.isNotEmpty()) {
                // Find the app root path for info
                val appRoot = try {
                    FileManager.getAppRoot(appContext).absolutePath
                } catch (e: Exception) { "مسار التطبيق" }

                addEvent("🔧 ${toolActions.size} أداة/أدوات سيتم تنفيذها")

                // Execute tool actions and collect results
                val results = executeToolActions(toolActions)
                val successCount = results.count { it }
                val failCount = results.count { !it }

                // Build summary message
                val summary = buildString {
                    // Show the AI's progress text first
                    if (cleanContent.isNotBlank()) {
                        appendLine(cleanContent)
                        appendLine()
                    }

                    // Then show file creation summary
                    if (toolActions.any { it.tool in listOf("create_file", "create_dir", "create_files", "patch_file") }) {
                        appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━")
                        if (failCount == 0) {
                            appendLine("✅ **تم إنشاء المشروع بنجاح!**")
                        } else {
                            appendLine("⚠️ **تم الإنشاء مع بعض الأخطاء**")
                        }
                        appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━")
                        appendLine()

                        for (action in toolActions) {
                            when (action.tool) {
                                "create_file" -> appendLine("📄 `/${action.path}` ✓")
                                "create_dir" -> appendLine("📁 `/${action.path}/` ✓")
                                "patch_file" -> appendLine("🔧 `/${action.path}` تم التعديل ✓")
                                "create_files" -> {
                                    action.files?.forEach { file ->
                                        appendLine("📄 `/${file.path}` ✓")
                                    }
                                }
                                "web_search" -> appendLine("🔍 بحث: ${action.query} ✓")
                                "web_fetch" -> appendLine("🌐 جلب: ${action.url} ✓")
                            }
                        }

                        appendLine()
                        appendLine("📂 **الملفات موجودة في:**")
                        appendLine("`$appRoot`")
                        appendLine()
                        appendLine("💡 اذهب إلى **File Manager** لرؤية الملفات")
                    } else if (cleanContent.isBlank()) {
                        // If no text and no file operations, show the actions
                        appendLine("✅ تم تنفيذ ${toolActions.size} أداة بنجاح")
                    }
                }

                updateLastMessage(summary, isLoading = false)
            } else if (fullContent.isNotBlank()) {
                val finalContent = fullContent.ifEmpty { "لم يتم إنشاء رد." }
                updateLastMessage(finalContent, isLoading = false)
            } else {
                updateLastMessage("⚠️ النموذج لم ينتج رد. حاول مرة أخرى أو جرب نموذج آخر.", isLoading = false, isError = true)
            }

        }.onFailure { error ->
            val errorMsg = error.message ?: "خطأ غير معروف"
            addEvent("❌ خطأ: $errorMsg")
            updateLastMessage(
                "❌ **حدث خطأ**\n\n$errorMsg\n\n💡 **اقتراحات:**\n1. جرب نموذج آخر من القائمة\n2. تحقق من اتصال الإنترنت\n3. اضغط ↻ لإعادة المحاولة",
                isLoading = false,
                isError = true
            )
        }
    }

    /**
     * تنفيذ أدوات إنشاء الملفات وإرجاع نتائج كل أداة
     */
    private suspend fun executeToolActions(actions: List<ToolAction>): List<Boolean> {
        val results = mutableListOf<Boolean>()

        for ((index, action) in actions.withIndex()) {
            when (action.tool) {
                "create_dir" -> {
                    action.path?.let { path ->
                        addEvent("📁 إنشاء مجلد: $path")
                        val success = withContext(Dispatchers.IO) {
                            FileManager.createFile(appContext, path, isDirectory = true)
                        }
                        results.add(success)
                        if (success) addEvent("✅ تم إنشاء المجلد: $path")
                        else addEvent("❌ فشل إنشاء المجلد: $path")
                    }
                }
                "create_file" -> {
                    action.path?.let { path ->
                        val content = action.content ?: ""
                        addEvent("📄 إنشاء ملف: $path")
                        val success = withContext(Dispatchers.IO) {
                            val parent = path.split("/").dropLast(1).joinToString("/")
                            if (parent.isNotBlank()) {
                                FileManager.createFile(appContext, parent, isDirectory = true)
                            }
                            FileManager.createFile(appContext, path, isDirectory = false)
                            FileManager.writeFileContent(appContext, path, content)
                        }
                        results.add(success)
                        if (success) addEvent("✅ تم إنشاء الملف: $path (${content.length} حرف)")
                        else addEvent("❌ فشل إنشاء الملف: $path")
                    }
                }
                "create_files" -> {
                    action.files?.let { files ->
                        addEvent("📚 إنشاء مشروع ب ${files.size} ملف...")
                        var allSuccess = true
                        for ((i, file) in files.withIndex()) {
                            val success = withContext(Dispatchers.IO) {
                                if (file.getFolderPath().isNotBlank()) {
                                    FileManager.createFile(appContext, file.getFolderPath(), isDirectory = true)
                                }
                                FileManager.createFile(appContext, file.path, isDirectory = false)
                                FileManager.writeFileContent(appContext, file.path, file.content)
                            }
                            if (success) {
                                addEvent("  ✅ [${i + 1}/${files.size}] ${file.path}")
                            } else {
                                addEvent("  ❌ [${i + 1}/${files.size}] فشل: ${file.path}")
                                allSuccess = false
                            }
                        }
                        results.add(allSuccess)
                        if (allSuccess) addEvent("✅ المشروع مكتمل: ${files.size} ملف")
                        else addEvent("⚠️ تم الإنشاء مع بعض الأخطاء")
                    }
                }
                "patch_file" -> {
                    val p = action.path
                    val f = action.find
                    val r = action.replace
                    if (p != null && f != null) {
                        addEvent("🔧 تعديل ملف: $p")
                        val success = withContext(Dispatchers.IO) {
                            FileManager.patchFileContent(appContext, p, f, r ?: "")
                        }
                        results.add(success)
                        if (success) addEvent("✅ تم تعديل $p بنجاح")
                        else addEvent("❌ فشل تعديل $p")
                    } else {
                        results.add(false)
                    }
                }
                "web_search" -> {
                    action.query?.let { query ->
                        addEvent("🔍 البحث عن: $query")
                        val searchResult = ApiClient.webSearch(query)
                        searchResult.onSuccess {
                            addEvent("🔍 تم البحث بنجاح")
                            results.add(true)
                        }.onFailure { error ->
                            addEvent("❌ فشل البحث: ${error.message}")
                            results.add(false)
                        }
                    }
                }
                "web_fetch" -> {
                    action.url?.let { url ->
                        addEvent("🌐 جلب: $url")
                        val fetchResult = ApiClient.webFetch(url)
                        fetchResult.onSuccess {
                            addEvent("🌐 تم الجلب بنجاح")
                            results.add(true)
                        }.onFailure { error ->
                            addEvent("❌ فشل الجلب: ${error.message}")
                            results.add(false)
                        }
                    }
                }
                else -> {
                    addEvent("⚠️ أداة غير معروفة: ${action.tool}")
                    results.add(false)
                }
            }
        }

        return results
    }

    fun selectModel(model: AiModel) {
        _selectedModel.value = model
        addEvent("🔄 تم تغيير النموذج إلى: ${model.name}")
    }

    fun clearChat() {
        _messages.value = emptyList()
        addEvent("🗑️ تم مسح المحادثة")
    }

    fun retryLastMessage() {
        val msgs = _messages.value
        val lastUserMsg = msgs.lastOrNull { it.role == MessageRole.USER } ?: return
        val lastAssistantMsg = msgs.lastOrNull { it.role == MessageRole.ASSISTANT && !it.isLoading }

        if (lastAssistantMsg != null && msgs.isNotEmpty() && msgs.last() == lastAssistantMsg) {
            _messages.value = msgs.dropLast(1)
        }

        addEvent("🔄 إعادة المحاولة...")
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

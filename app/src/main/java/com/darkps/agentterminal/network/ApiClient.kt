package com.darkps.agentterminal.network

import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val BASE_URL = "https://api.rewind.ai/v1"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val JSON_MEDIA = "application/json".toMediaType()

    private var authToken: String? = null

    suspend fun initialize(): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (authToken != null) return@withContext Result.success(authToken!!)

            val userAgent = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36"
            val registerBody = JsonObject().apply {
                addProperty("email", generateRandomEmail())
                addProperty("password", generateRandomPassword())
            }

            val request = Request.Builder()
                .url("$BASE_URL/auth/signup")
                .header("User-Agent", userAgent)
                .header("Content-Type", "application/json")
                .post(registerBody.toString().toRequestBody(JSON_MEDIA))
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val json = gson.fromJson(body, JsonObject::class.java)
                val token = json.get("accessToken")?.asString
                    ?: json.get("token")?.asString
                    ?: return@withContext Result.failure(Exception("No token in response"))

                authToken = token
                Result.success(token)
            } else {
                Result.failure(Exception("Signup failed: ${response.code} $body"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendMessage(
        messages: List<Map<String, String>>,
        model: String,
        onChunk: (String) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val token = initialize().getOrElse { return@withContext Result.failure(it) }

            val provider = getProvider(model)
            val requestBody = JsonObject().apply {
                val msgsArray = com.google.gson.JsonArray()
                messages.forEach { msg ->
                    val msgObj = JsonObject().apply {
                        addProperty("role", msg["role"] ?: "user")
                        addProperty("content", msg["content"] ?: "")
                    }
                    msgsArray.add(msgObj)
                }
                add("messages", msgsArray)
                addProperty("model", "$provider/$model")
                addProperty("stream", true)
            }

            val request = Request.Builder()
                .url("$BASE_URL/chat/completions/")
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "application/json")
                .header("User-Agent", "DarkPS-Android/1.0")
                .post(requestBody.toString().toRequestBody(JSON_MEDIA))
                .build()

            val response = client.newCall(request).execute()
            val fullContent = StringBuilder()

            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Unknown error"
                return@withContext Result.failure(
                    Exception("⚠️ API Error (${response.code}): ${errorBody.take(500)}")
                )
            }

            response.body?.charStream()?.use { reader ->
                val buffer = CharArray(4096)
                var charsRead: Int
                var buffer_ = StringBuilder()

                while (reader.read(buffer).also { charsRead = it } != -1) {
                    buffer_.append(buffer, 0, charsRead)
                    val str = buffer_.toString()
                    val lines = str.split("\n")

                    buffer_ = StringBuilder()
                    for (i in 0 until lines.size - 1) {
                        processSSELine(lines[i], fullContent, onChunk)
                    }
                    buffer_.append(lines.last())
                }
                if (buffer_.isNotEmpty()) {
                    processSSELine(buffer_.toString(), fullContent, onChunk)
                }
            }

            Result.success(fullContent.toString())
        } catch (e: Exception) {
            Result.failure(Exception("⚠️ Connection Error: ${e.message}"))
        }
    }

    /**
     * البحث في الويب باستخدام DuckDuckGo
     */
    suspend fun webSearch(query: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val encoded = java.net.URLEncoder.encode(query, "UTF-8")
            val url = "https://html.duckduckgo.com/html/?q=$encoded"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml")
                .build()

            val response = client.newCall(request).execute()
            val html = response.body?.string() ?: ""

            // Parse results simply
            val results = mutableListOf<String>()
            val linkRegex = Regex("""<a rel="nofollow" class="result__a" href="([^"]+)"[^>]*>([^<]+)</a>""")
            val snippetRegex = Regex("""<a class="result__snippet"[^>]*>(.*?)</a>""")

            val linkMatches = linkRegex.findAll(html).toList()
            val snippetMatches = snippetRegex.findAll(html).toList()

            for (i in linkMatches.indices) {
                val link = linkMatches[i].groupValues[1]
                val title = linkMatches[i].groupValues[2].replace(Regex("<[^>]+>"), "")
                val snippet = if (i < snippetMatches.size) {
                    snippetMatches[i].groupValues[1].replace(Regex("<[^>]+>"), "").take(200)
                } else ""

                if (title.isNotBlank()) {
                    results.add("**${i + 1}. $title**  \n$link  \n$snippet\n")
                }
                if (results.size >= 8) break
            }

            if (results.isEmpty()) {
                Result.success("❌ No search results found for '$query'")
            } else {
                Result.success("🔍 **Search results for:** $query\n\n${results.joinToString("\n")}")
            }
        } catch (e: Exception) {
            Result.failure(Exception("⚠️ Search Error: ${e.message}"))
        }
    }

    /**
     * جلب محتوى صفحة ويب
     */
    suspend fun webFetch(url: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                .build()

            val response = client.newCall(request).execute()
            val html = response.body?.string() ?: ""
            
            // Extract text content (strip HTML tags)
            val textOnly = html
                .replace(Regex("<script[^>]*>.*?</script>", RegexOption.DOT_MATCHES_ALL), "")
                .replace(Regex("<style[^>]*>.*?</style>", RegexOption.DOT_MATCHES_ALL), "")
                .replace(Regex("<[^>]+>"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
                .take(5000)

            Result.success("📄 **Content from:** $url\n\n$textOnly")
        } catch (e: Exception) {
            Result.failure(Exception("⚠️ Fetch Error: ${e.message}"))
        }
    }

    private fun processSSELine(
        line: String,
        fullContent: StringBuilder,
        onChunk: (String) -> Unit
    ) {
        val trimmed = line.trim()
        if (trimmed.isEmpty() || trimmed == "[DONE]") return

        val data = if (trimmed.startsWith("data:")) {
            trimmed.substring(5).trim()
        } else trimmed

        if (data.isEmpty()) return

        try {
            val json = gson.fromJson(data, JsonObject::class.java)
            val choices = json.getAsJsonArray("choices")
            if (choices != null && choices.size() > 0) {
                val choice = choices[0].asJsonObject
                val delta = choice.getAsJsonObject("delta")
                if (delta != null) {
                    val content = delta.get("content")?.asString ?: ""
                    if (content.isNotEmpty()) {
                        fullContent.append(content)
                        onChunk(content)
                    }
                }
            }
        } catch (_: Exception) {
            // Skip malformed JSON lines
        }
    }

    private fun getProvider(model: String): String {
        return when {
            model.contains("glm") -> "z-ai"
            model.contains("kimi") -> "moonshotai"
            model.contains("gemini") -> "google"
            model.contains("deepseek") -> "deepseek"
            model.startsWith("gpt") || model.contains("gpt") -> "darkps"
            model.contains("qwen") -> "qwen"
            model.contains("grok") -> "xai"
            else -> "darkps"
        }
    }

    private fun generateRandomEmail(): String {
        val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
        val local = (1..10).map { chars.random() }.joinToString("")
        return "$local@gmail.com"
    }

    private fun generateRandomPassword(): String {
        val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
        return "${(1..4).map { chars.random() }.joinToString("")}A1${(1..4).map { chars.random() }.joinToString("")}"
    }

    fun resetToken() {
        authToken = null
    }
}

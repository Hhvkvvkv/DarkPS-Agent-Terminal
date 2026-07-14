package com.darkps.agentterminal.model

import com.google.gson.Gson
import com.google.gson.JsonObject

data class ToolAction(
    val tool: String,
    val path: String? = null,
    val content: String? = null,
    val query: String? = null,
    val url: String? = null,
    val isDirectory: Boolean = false,
    val files: List<FileToCreate>? = null
)

data class FileToCreate(
    val path: String,
    val content: String
) {
    fun getFileName(): String = path.split("/").lastOrNull() ?: path
    fun getFolderPath(): String {
        val parts = path.split("/")
        return if (parts.size > 1) parts.dropLast(1).joinToString("/") else ""
    }
}

object ToolActionParser {
    private val gson = Gson()
    private val toolBlockRegex = Regex(
        """\[TOOL\](.*?)\[/TOOL\]""",
        RegexOption.DOT_MATCHES_ALL
    )

    fun parseToolActions(response: String): List<ToolAction> {
        val actions = mutableListOf<ToolAction>()

        // Find [TOOL]...[/TOOL] blocks
        val matches = toolBlockRegex.findAll(response)
        for (match in matches) {
            val jsonStr = match.groupValues[1].trim()
            try {
                val json = gson.fromJson(jsonStr, JsonObject::class.java)
                val tool = json.get("tool")?.asString ?: continue

                when (tool) {
                    "create_file" -> {
                        val path = json.get("path")?.asString ?: continue
                        val content = json.get("content")?.asString ?: ""
                        actions.add(ToolAction(
                            tool = "create_file",
                            path = path,
                            content = content,
                            isDirectory = false
                        ))
                    }
                    "create_dir" -> {
                        val path = json.get("path")?.asString ?: continue
                        actions.add(ToolAction(
                            tool = "create_dir",
                            path = path,
                            isDirectory = true
                        ))
                    }
                    "create_files" -> {
                        val filesJson = json.getAsJsonArray("files")
                        if (filesJson != null) {
                            val files = mutableListOf<FileToCreate>()
                            for (f in filesJson) {
                                val fObj = f.asJsonObject
                                files.add(FileToCreate(
                                    path = fObj.get("path")?.asString ?: continue,
                                    content = fObj.get("content")?.asString ?: ""
                                ))
                            }
                            actions.add(ToolAction(
                                tool = "create_files",
                                files = files
                            ))
                        }
                    }
                    "web_search" -> {
                        val query = json.get("query")?.asString ?: continue
                        actions.add(ToolAction(
                            tool = "web_search",
                            query = query
                        ))
                    }
                    "web_fetch" -> {
                        val url = json.get("url")?.asString ?: continue
                        actions.add(ToolAction(
                            tool = "web_fetch",
                            url = url
                        ))
                    }
                }
            } catch (_: Exception) {
                // Skip malformed tool blocks
            }
        }

        return actions
    }

    /**
     * Remove tool blocks from the response text for clean display
     */
    fun removeToolBlocks(response: String): String {
        return response.replace(toolBlockRegex, "").trim()
    }
}

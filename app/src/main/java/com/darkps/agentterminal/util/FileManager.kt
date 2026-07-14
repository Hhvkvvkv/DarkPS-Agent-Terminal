package com.darkps.agentterminal.util

import android.content.Context
import java.io.File

object FileManager {
    private const val APP_ROOT = "DarkPSAgent"

    /**
     * مسار التخزين الرئيسي - داخل التخزين الداخلي للتطبيق
     * المسار: /storage/emulated/0/Android/data/com.darkps.agentterminal/files/DarkPSAgent/
     */
    fun getAppRoot(context: Context): File {
        val extDir = context.getExternalFilesDir(null)
        return if (extDir != null) {
            File(extDir, APP_ROOT).also { it.mkdirs() }
        } else {
            File(context.filesDir, APP_ROOT).also { it.mkdirs() }
        }
    }

    /**
     * الحصول على مسار التخزين بشكل نصي
     */
    fun getAppRootPath(context: Context): String {
        return getAppRoot(context).absolutePath
    }

    fun listFiles(context: Context, path: String): List<File> {
        val dir = getFile(context, path)
        return if (dir.exists() && dir.isDirectory) {
            dir.listFiles()?.sortedWith(compareBy<File> { !it.isDirectory }.thenBy { it.name })
                ?: emptyList()
        } else emptyList()
    }

    fun getFile(context: Context, path: String): File {
        val root = getAppRoot(context)
        // المسارات تكون دائماً نسبة لمجلد التطبيق
        val cleanPath = path.trimStart('/')
        val target = File(root, cleanPath).normalize()
        // تأكد من أن الملف داخل مجلد التطبيق (حماية)
        if (!target.absolutePath.startsWith(root.absolutePath)) {
            return File(root, cleanPath)
        }
        return target
    }

    fun createFile(context: Context, path: String, isDirectory: Boolean): Boolean {
        val file = getFile(context, path)
        return if (isDirectory) {
            file.mkdirs()
        } else {
            file.parentFile?.mkdirs() ?: false
            file.createNewFile()
        }
    }

    fun deleteFile(context: Context, path: String): Boolean {
        val file = getFile(context, path)
        return if (file.isDirectory) {
            file.deleteRecursively()
        } else {
            file.delete()
        }
    }

    fun readFileContent(context: Context, path: String): String {
        return try {
            getFile(context, path).readText()
        } catch (e: Exception) {
            "Error reading file: ${e.message}"
        }
    }

    fun writeFileContent(context: Context, path: String, content: String): Boolean {
        return try {
            val file = getFile(context, path)
            file.parentFile?.mkdirs()
            file.writeText(content)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * تعديل جزء معين في ملف - بحث واستبدال
     */
    fun patchFileContent(context: Context, path: String, findText: String, replaceText: String): Boolean {
        return try {
            val file = getFile(context, path)
            if (!file.exists()) return false
            var content = file.readText()
            if (!content.contains(findText)) return false
            content = content.replace(findText, replaceText)
            file.writeText(content)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun renameFile(context: Context, oldPath: String, newName: String): Boolean {
        return try {
            val file = getFile(context, oldPath)
            val newFile = File(file.parent, newName)
            file.renameTo(newFile)
        } catch (e: Exception) {
            false
        }
    }

    fun getFileSizeString(size: Long): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> String.format("%.1f KB", size / 1024.0)
            size < 1024 * 1024 * 1024 -> String.format("%.1f MB", size / (1024.0 * 1024))
            else -> String.format("%.1f GB", size / (1024.0 * 1024 * 1024))
        }
    }
}

package com.darkps.agentterminal.model

data class FileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long = 0,
    val lastModified: Long = 0,
    val isExpanded: Boolean = false,
    val children: List<FileItem>? = null
)

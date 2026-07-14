package com.darkps.agentterminal.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.darkps.agentterminal.ui.theme.*
import com.darkps.agentterminal.util.FileManager
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileManagerScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var currentPath by remember { mutableStateOf("") }
    var files by remember { mutableStateOf<List<File>>(emptyList()) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showCreateFile by remember { mutableStateOf(true) }
    var newFileName by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf<File?>(null) }
    var showFileContent by remember { mutableStateOf<File?>(null) }
    var fileContent by remember { mutableStateOf("") }

    // Load files
    fun loadFiles(path: String) {
        files = FileManager.listFiles(context, path)
    }

    LaunchedEffect(currentPath) {
        loadFiles(currentPath)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "File Manager",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        if (currentPath.isNotEmpty()) {
                            Text(
                                text = currentPath,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextTertiary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (currentPath.isNotEmpty()) {
                        IconButton(onClick = {
                            val parent = File(currentPath).parent
                            currentPath = parent?.replace(FileManager.getAppRoot(context).absolutePath, "")?.trimStart('/') ?: ""
                            loadFiles(currentPath)
                        }) {
                            Icon(Icons.Default.ArrowBack, "Back", tint = TextPrimary)
                        }
                    } else {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.Close, "Close", tint = TextPrimary)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {
                        showCreateFile = true
                        newFileName = ""
                        showCreateDialog = true
                    }) {
                        Icon(Icons.Default.CreateNewFolder, "New Folder", tint = TextSecondary)
                    }
                    IconButton(onClick = {
                        showCreateFile = false
                        newFileName = ""
                        showCreateDialog = true
                    }) {
                        Icon(Icons.Default.NoteAdd, "New File", tint = TextSecondary)
                    }
                    IconButton(onClick = { loadFiles(currentPath) }) {
                        Icon(Icons.Default.Refresh, "Refresh", tint = TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground
                )
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Current path indicator
            Surface(
                color = DarkSurface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Storage,
                        "Root",
                        tint = AccentPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "/${currentPath}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "${files.size} items",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary
                    )
                }
            }

            if (files.isEmpty() && currentPath.isEmpty()) {
                // Empty state for root
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = TextTertiary.copy(alpha = 0.3f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No files yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextTertiary.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "Use the + buttons to create files or folders",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextTertiary.copy(alpha = 0.3f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    items(files, key = { it.absolutePath }) { file ->
                        FileItemRow(
                            file = file,
                            onClick = {
                                if (file.isDirectory) {
                                    val newPath = if (currentPath.isEmpty()) file.name else "$currentPath/${file.name}"
                                    currentPath = newPath
                                    loadFiles(currentPath)
                                } else {
                                    fileContent = FileManager.readFileContent(context, if (currentPath.isEmpty()) file.name else "$currentPath/${file.name}")
                                    showFileContent = file
                                }
                            },
                            onDelete = {
                                showDeleteConfirm = file
                            }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    // Create dialog
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            containerColor = DarkSurfaceVariant,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = {
                Text(
                    text = if (showCreateFile) "Create File" else "Create Folder",
                    color = TextPrimary
                )
            },
            text = {
                OutlinedTextField(
                    value = newFileName,
                    onValueChange = { newFileName = it },
                    placeholder = { Text("Enter name...", color = TextTertiary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentPrimary,
                        unfocusedBorderColor = DarkBorder,
                        cursorColor = AccentPrimary
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newFileName.isNotBlank()) {
                        val fullPath = if (currentPath.isEmpty()) newFileName else "$currentPath/$newFileName"
                        FileManager.createFile(context, fullPath, !showCreateFile)
                        showCreateDialog = false
                        loadFiles(currentPath)
                    }
                }) {
                    Text("Create", color = AccentPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Delete confirm
    showDeleteConfirm?.let { file ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            containerColor = DarkSurfaceVariant,
            title = { Text("Delete", color = TextPrimary) },
            text = { Text("Delete ${file.name}?", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    val path = if (currentPath.isEmpty()) file.name else "$currentPath/${file.name}"
                    FileManager.deleteFile(context, path)
                    showDeleteConfirm = null
                    loadFiles(currentPath)
                }) {
                    Text("Delete", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // File content viewer
    showFileContent?.let { file ->
        AlertDialog(
            onDismissRequest = { showFileContent = null },
            containerColor = DarkSurfaceVariant,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = file.name,
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { showFileContent = null }) {
                        Icon(Icons.Default.Close, "Close", tint = TextSecondary)
                    }
                }
            },
            text = {
                Surface(
                    color = DarkBackground,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                ) {
                    Text(
                        text = fileContent,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            },
            confirmButton = {},
            dismissButton = {}
        )
    }
}

@Composable
private fun FileItemRow(
    file: File,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (file.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                contentDescription = null,
                tint = if (file.isDirectory) AccentPrimary else TextSecondary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row {
                    Text(
                        text = if (file.isDirectory) "Folder" else FileManager.getFileSizeString(file.length()),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    "Delete",
                    tint = ErrorRed.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

package com.darkps.agentterminal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.darkps.agentterminal.model.AiModel
import com.darkps.agentterminal.ui.screens.ChatScreen
import com.darkps.agentterminal.ui.screens.EventLogScreen
import com.darkps.agentterminal.ui.screens.FileManagerScreen
import com.darkps.agentterminal.ui.theme.DarkPSAgentTerminalTheme
import com.darkps.agentterminal.viewmodel.ChatViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        window.statusBarColor = android.graphics.Color.BLACK
        window.navigationBarColor = android.graphics.Color.BLACK

        setContent {
            DarkPSAgentTerminalTheme {
                val chatViewModel: ChatViewModel = viewModel()
                val navController = rememberNavController()

                val messages by chatViewModel.messages.collectAsState()
                val isLoading by chatViewModel.isLoading.collectAsState()
                val selectedModel by chatViewModel.selectedModel.collectAsState()
                val eventLog by chatViewModel.eventLog.collectAsState()

                NavHost(
                    navController = navController,
                    startDestination = "chat"
                ) {
                    composable("chat") {
                        ChatScreen(
                            messages = messages,
                            isLoading = isLoading,
                            selectedModel = selectedModel,
                            models = AiModel.AVAILABLE_MODELS,
                            onSendMessage = { chatViewModel.sendMessage(it) },
                            onModelSelected = { chatViewModel.selectModel(it) },
                            onClearChat = { chatViewModel.clearChat() },
                            onNavigateToFiles = { navController.navigate("files") },
                            onNavigateToLogs = { navController.navigate("logs") }
                        )
                    }
                    composable("files") {
                        FileManagerScreen(
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    composable("logs") {
                        EventLogScreen(
                            events = eventLog,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}

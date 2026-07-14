package com.darkps.agentterminal.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darkps.agentterminal.model.ChatMessage
import com.darkps.agentterminal.model.MessageRole
import com.darkps.agentterminal.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MessageBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier
) {
    val isUser = message.role == MessageRole.USER
    val isAssistant = message.role == MessageRole.ASSISTANT

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // Sender label
        Text(
            text = if (isUser) "You" else "DarkPS Agent",
            style = MaterialTheme.typography.labelSmall,
            color = if (isUser) AccentPrimary.copy(alpha = 0.7f) else AccentSecondary.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )

        // Message bubble
        Box(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .background(
                    when {
                        message.isError -> ErrorRed.copy(alpha = 0.15f)
                        isUser -> UserBubble
                        else -> AiBubble
                    }
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            if (message.isLoading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(4.dp)
                ) {
                    Text(
                        text = "Thinking",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    LoadingDots()
                }
            } else {
                // Format markdown-like content
                Text(
                    text = formatMessageContent(message.content),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
            }
        }

        // Timestamp
        Text(
            text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp)),
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun LoadingDots() {
    val dotCount = 3
    var currentStep by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(400)
            currentStep = (currentStep + 1) % (dotCount + 1)
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(dotCount) { index ->
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        if (index < currentStep) AccentPrimary
                        else TextTertiary
                    )
            )
        }
    }
}

private fun formatMessageContent(content: String): androidx.compose.ui.text.AnnotatedString {
    return buildAnnotatedString {
        val lines = content.split("\n")
        var inCodeBlock = false
        var codeContent = StringBuilder()

        for (line in lines) {
            if (line.trimStart().startsWith("```")) {
                if (inCodeBlock) {
                    // End code block
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = AccentPrimary
                        )
                    ) {
                        append(codeContent.toString().trimEnd('\n'))
                    }
                    codeContent = StringBuilder()
                    inCodeBlock = false
                    append("\n")
                } else {
                    inCodeBlock = true
                    append("\n")
                }
                continue
            }

            if (inCodeBlock) {
                codeContent.appendLine(line)
                continue
            }

            // Bold text (**text**)
            var remaining = line
            while (remaining.isNotEmpty()) {
                val boldStart = remaining.indexOf("**")
                if (boldStart == -1) {
                    append(remaining)
                    break
                }
                append(remaining.substring(0, boldStart))
                remaining = remaining.substring(boldStart + 2)
                val boldEnd = remaining.indexOf("**")
                if (boldEnd == -1) {
                    append(remaining)
                    break
                }
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(remaining.substring(0, boldEnd))
                }
                remaining = remaining.substring(boldEnd + 2)
            }
            append("\n")
        }

        if (inCodeBlock && codeContent.isNotEmpty()) {
            withStyle(
                SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = AccentPrimary
                )
            ) {
                append(codeContent.toString().trimEnd('\n'))
            }
        }
    }
}

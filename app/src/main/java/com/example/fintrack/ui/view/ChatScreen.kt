package com.example.fintrack.ui.view

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.fintrack.viewmodel.ChatViewModel
import java.util.Locale

@Composable
fun ChatScreen(viewModel: ChatViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    var textToSpeech: TextToSpeech? by remember { mutableStateOf(null) }

    // 🔹 TextToSpeech Lifecycle Management
    DisposableEffect(context) {
        val tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.US
            }
        }
        textToSpeech = tts
        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }

    // 🔹 FIX 1: SpeechRecognizer Lifecycle Clean-up (Prevents Memory Leak & Crash)
    val speechRecognizer = remember(context) { SpeechRecognizer.createSpeechRecognizer(context) }
    DisposableEffect(speechRecognizer) {
        onDispose {
            speechRecognizer.destroy()
        }
    }

    var isListening by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startSpeechRecognition(context, speechRecognizer, viewModel) { isListening = it }
        } else {
            Toast.makeText(context, "Microphone permission is required for voice agent", Toast.LENGTH_SHORT).show()
        }
    }

    // 🔹 Auto-speak AI Response & Auto-scroll on new message
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)

            // Speak latest AI response if last message is from AI
            val lastMsg = uiState.messages.last()
            if (!lastMsg.isUser && textToSpeech != null) {
                textToSpeech?.speak(lastMsg.message, TextToSpeech.QUEUE_FLUSH, null, "AI_REPLY")
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
    ) {
        // Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp)
        ) {
            items(uiState.messages) { chat ->
                ChatBubble(message = chat.message, isUser = chat.isUser)
            }

            if (uiState.isLoading) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Surface(
                            color = Color(0xFF1E1E1E),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2B2B2B))
                        ) {
                            Text(
                                text = "FinTrack AI is analyzing expenses...",
                                color = Color(0xFF80CBC4),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            }
        }

        // Input Deck
        Surface(
            color = Color(0xFF181818),
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = uiState.currentInput,
                    onValueChange = { viewModel.onInputChanged(it) },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ask about expenses, dates, budget...", color = Color.Gray, fontSize = 13.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00796B),
                        unfocusedBorderColor = Color(0xFF2E2E2E),
                        focusedContainerColor = Color(0xFF222222),
                        unfocusedContainerColor = Color(0xFF222222)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Voice Mic Button
                IconButton(
                    onClick = {
                        val permissionCheck = ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO)
                        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                            startSpeechRecognition(context, speechRecognizer, viewModel) { isListening = it }
                        } else {
                            permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            if (isListening) Color.Red else Color(0xFF004D40),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice Input",
                        tint = Color(0xFF80CBC4)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Send Button
                IconButton(
                    onClick = {
                        if (uiState.currentInput.isNotBlank() && !uiState.isLoading) { // 👈 CHECK ADDED
                            viewModel.sendMessage()
                        }
                    },
                    enabled = !uiState.isLoading,
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            if (uiState.isLoading) Color.Gray else Color(0xFF00796B),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

fun startSpeechRecognition(
    context: android.content.Context,
    speechRecognizer: SpeechRecognizer,
    viewModel: ChatViewModel,
    onListeningChanged: (Boolean) -> Unit
){
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
    }

    speechRecognizer.setRecognitionListener(object : RecognitionListener {
        override fun onBeginningOfSpeech() {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() { onListeningChanged(false) }
        override fun onError(error: Int) {
            onListeningChanged(false)
            Toast.makeText(context, "Speech error code: $error", Toast.LENGTH_SHORT).show()
        }
        override fun onEvent(eventType: Int, params: Bundle?) {}
        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onReadyForSpeech(params: Bundle?) { onListeningChanged(true) }
        override fun onResults(results: Bundle?) {
            onListeningChanged(false)
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                val spokenText = matches[0]
                viewModel.onInputChanged(spokenText)
                viewModel.sendMessage()
            }
        }
        override fun onRmsChanged(rmsdB: Float) {}
    })

    try {
        speechRecognizer.startListening(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Error starting microphone: ${e.message}", Toast.LENGTH_SHORT).show()
        onListeningChanged(false)
    }
}

@Composable
fun ChatBubble(message: String, isUser: Boolean) {
    val backgroundColor = if (isUser) Color(0xFF004D40) else Color(0xFF1E1E1E)
    val textColor = if (isUser) Color.White else Color(0xFFE0E0E0)
    val alignment = if (isUser) Alignment.End else Alignment.Start

    val bubbleShape = if (isUser) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 2.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 2.dp, bottomEnd = 16.dp)
    }

    val borderModifier = if (!isUser) {
        Modifier.border(1.dp, Color(0xFF2A2A2A), bubbleShape)
    } else Modifier

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .then(borderModifier)
                .background(backgroundColor, shape = bubbleShape)
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .widthIn(max = 280.dp)
        ) {
            Text(
                text = message,
                color = textColor,
                fontSize = 13.5.sp,
                lineHeight = 18.sp
            )
        }
    }
}
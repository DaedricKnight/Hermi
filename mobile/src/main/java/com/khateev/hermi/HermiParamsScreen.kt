package com.khateev.hermi

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.material.bottomappbar.BottomAppBar
import com.khateev.hermi.shared.VhalParamsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HermiParamsScreen(viewModel: HermiViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val isMockMode by viewModel.isMockMode.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // Слушаем события из ViewModel для показа Snackbar
    LaunchedEffect(Unit) {
        viewModel.events.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // Состояние для распознавания речи
    val isListening by viewModel.isListening.collectAsState()
    val recognizedText by viewModel.recognizedText.collectAsState(initial = "")
    val recordAudioPermissionState = rememberPermissionState(android.Manifest.permission.RECORD_AUDIO)

    LaunchedEffect(Unit) {
        if (!recordAudioPermissionState.status.isGranted) {
            recordAudioPermissionState.launchPermissionRequest()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF121212),
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                actions = {
                    SourceSelector(isMockMode) { viewModel.toggleVhalSource(it) }
                }
            )
        },
        bottomBar = {
            BottomAppBar(containerColor = Color(0xFF121212)) {
                Spacer(Modifier.weight(1f))
                IconButton(onClick = {
                    if (recordAudioPermissionState.status.isGranted) {
                        viewModel.startVoiceInput()
                    } else {
                        // TODO: Показать сообщение пользователю, что нужно дать разрешение
                        recordAudioPermissionState.launchPermissionRequest()
                    }
                }) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = stringResource(R.string.start_voice_input),
                        tint = Color.White
                    )
                }
                Spacer(Modifier.weight(1f))
            }
        }
    ) { padding ->
        // Плавная анимация смены данных
        AnimatedContent(
            targetState = uiState,
            transitionSpec = {
                fadeIn(animationSpec = tween(500)) togetherWith
                        fadeOut(animationSpec = tween(500))
            },
            label = "VhalDataAnimation",
            modifier = Modifier.padding(padding)
        ) { animatedState ->
            VhalDataContent(state = animatedState)
        }

        // UI для голосового ввода
        if (isListening) {
            AlertDialog(
                onDismissRequest = { viewModel.stopVoiceInput() },
                title = { Text(stringResource(R.string.hermi_listening_title)) },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(16.dp))
                        Text(stringResource(R.string.hermi_listening_message))
                    }
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.stopVoiceInput() }) {
                        Text(stringResource(R.string.stop_listening))
                    }
                }
            )
        } else if (recognizedText.isNotBlank()) {
            // Можно показать распознанный текст и дать возможность его обработать
            AlertDialog(
                onDismissRequest = { /* Можно скрыть диалог через State во ViewModel */ },
                title = { Text(stringResource(R.string.recognized_text_title)) },
                text = { Text(recognizedText) },
                confirmButton = {
                    TextButton(onClick = {
                        // TODO: Отправить распознанный текст в HermiAIProcessor для выполнения команды
                        // Или просто скрыть диалог
                    }) {
                        Text(stringResource(R.string.ok))
                    }
                }
            )
        }
    }
}

@Composable
fun VhalDataContent(modifier: Modifier = Modifier, state: VhalParamsState) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        // ... (Твой LazyColumn с Headers и ParamRows, как мы обсуждали ранее)
    }
}
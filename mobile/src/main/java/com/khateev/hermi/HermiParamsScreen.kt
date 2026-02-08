//package com.khateev.hermi
//
//import android.Manifest
//import android.content.pm.PackageManager
//import androidx.activity.compose.rememberLauncherForActivityResult
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.compose.animation.AnimatedContent
//import androidx.compose.animation.core.tween
//import androidx.compose.animation.fadeIn
//import androidx.compose.animation.fadeOut
//import androidx.compose.animation.togetherWith
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.Row
//import androidx.compose.foundation.layout.Spacer
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.layout.size
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.BugReport
//import androidx.compose.material.icons.filled.DirectionsCar
//import androidx.compose.material.icons.filled.Mic
//import androidx.compose.material3.AlertDialog
//import androidx.compose.material3.BottomAppBar
//import androidx.compose.material3.CircularProgressIndicator
//import androidx.compose.material3.ExperimentalMaterial3Api
//import androidx.compose.material3.Icon
//import androidx.compose.material3.IconButton
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.Scaffold
//import androidx.compose.material3.SnackbarHost
//import androidx.compose.material3.SnackbarHostState
//import androidx.compose.material3.Switch
//import androidx.compose.material3.SwitchDefaults
//import androidx.compose.material3.Text
//import androidx.compose.material3.TextButton
//import androidx.compose.material3.TopAppBar
//import androidx.compose.material3.TopAppBarDefaults
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.LaunchedEffect
//import androidx.compose.runtime.collectAsState
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.setValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.res.stringResource
//import androidx.compose.ui.unit.dp
//import androidx.core.content.ContextCompat
//import androidx.hilt.navigation.compose.hiltViewModel
//import com.khateev.hermi.shared.VhalParamsState
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun HermiParamsScreen(viewModel: HermiViewModel = hiltViewModel()) {
//    val uiState by viewModel.uiState.collectAsState()
//    val isMockMode by viewModel.isMockMode.collectAsState()
//
//    val snackbarHostState = remember { SnackbarHostState() }
//
//    LaunchedEffect(Unit) {
//        viewModel.events.collect { message ->
//            snackbarHostState.showSnackbar(message)
//        }
//    }
//
//    val isListening by viewModel.isListening.collectAsState()
//    val recognizedText by viewModel.recognizedText.collectAsState(initial = "")
//    var hasAudioPermission by remember { mutableStateOf(false) }
//
//    // Launcher для запроса разрешения
//    val permissionLauncher = rememberLauncherForActivityResult(
//        contract = ActivityResultContracts.RequestPermission()
//    ) { isGranted ->
//        hasAudioPermission = isGranted
//        if (isGranted) {
//            viewModel.startVoiceInput()
//        }
//    }
//
//    // Check permissions on start
//    val context = LocalContext.current
//    LaunchedEffect(Unit) {
//        hasAudioPermission = ContextCompat.checkSelfPermission(
//            context,
//            Manifest.permission.RECORD_AUDIO
//        ) == PackageManager.PERMISSION_GRANTED
//    }
//
//    Scaffold(
//        snackbarHost = { SnackbarHost(hostState = snackbarHostState)  },
//        topBar = {
//            TopAppBar(
//                title = {
//                    Text(
//                        text = stringResource(R.string.app_name),
//                        color = Color.White
//                    )
//                },
//                colors = TopAppBarDefaults.topAppBarColors(
//                    containerColor = Color(0xFF121212),
//                    titleContentColor = Color.White,
//                    actionIconContentColor = Color.White
//                ),
//                actions = {
//                    SourceSelector(isMockMode) { viewModel.toggleVhalSource(it) }
//                }
//            )
//        },
//        bottomBar = {
//            BottomAppBar(containerColor = Color(0xFF121212)) {
//                Spacer(Modifier.weight(1f))
//                IconButton(onClick = {
//                    if (hasAudioPermission) {
//                        viewModel.startVoiceInput()
//                    } else {
//                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
//                    }
//                }) {
//                    Icon(
//                        imageVector = Icons.Default.Mic,
//                        contentDescription = null,
//                        tint = Color.White
//                    )
//                }
//                Spacer(Modifier.weight(1f))
//            }
//        },
//        content = { padding ->
//            AnimatedContent(
//                targetState = uiState,
//                transitionSpec = {
//                    fadeIn(animationSpec = tween(500)) togetherWith
//                            fadeOut(animationSpec = tween(500))
//                },
//                label = "VhalDataAnimation",
//                modifier = Modifier.padding(padding)
//            ) { animatedState ->
//                VhalDataContent(state = animatedState)
//            }
//
//            // UI for voice input
//            if (isListening) {
//                AlertDialog(
//                    onDismissRequest = { viewModel.stopVoiceInput() },
//                    title = { Text(stringResource(R.string.hermi_listening_title)) },
//                    text = {
//                        Column(
//                            horizontalAlignment = Alignment.CenterHorizontally,
//                            modifier = Modifier.fillMaxWidth()
//                        ) {
//                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
//                            Spacer(Modifier.height(16.dp))
//                            Text(stringResource(R.string.hermi_listening_message))
//                        }
//                    },
//                    confirmButton = {
//                        TextButton(onClick = { viewModel.stopVoiceInput() }) {
//                            Text(stringResource(R.string.stop_listening))
//                        }
//                    }
//                )
//            } else if (recognizedText.isNotBlank()) {
//                AlertDialog(
//                    onDismissRequest = { },
//                    title = { Text(stringResource(R.string.recognized_text_title)) },
//                    text = { Text(recognizedText) },
//                    confirmButton = {
//                        TextButton(onClick = {
//                            // TODO: send recognized text to HermiAIProcessor
//                        }) {
//                            Text(stringResource(R.string.ok))
//                        }
//                    }
//                )
//            }
//        },
//    )
//}
//
//@Composable
//fun SourceSelector(
//    isMockMode: Boolean,
//    onToggle: (Boolean) -> Unit
//) {
//
//    Row(
//        verticalAlignment = Alignment.CenterVertically,
//        modifier = Modifier.padding(end = 8.dp)
//    ) {
//        Text(
//            text = if (isMockMode) "MOCK" else "REAL",
//            style = MaterialTheme.typography.labelSmall,
//            color = if (isMockMode) Color.Yellow else Color(0xFF00FF00), // Ярко-зеленый для Real
//            modifier = Modifier.padding(end = 8.dp)
//        )
//
//        Switch(
//            checked = isMockMode,
//            onCheckedChange = onToggle,
//            thumbContent = {
//                Icon(
//                    imageVector = if (isMockMode) Icons.Default.BugReport else Icons.Default.DirectionsCar,
//                    contentDescription = null,
//                    modifier = Modifier.size(SwitchDefaults.IconSize)
//                )
//            },
//            colors = SwitchDefaults.colors(
//                checkedThumbColor = Color.Yellow,
//                checkedTrackColor = Color.Yellow.copy(alpha = 0.5f),
//                uncheckedThumbColor = Color.Green,
//                uncheckedTrackColor = Color.Green.copy(alpha = 0.5f)
//            )
//        )
//    }
//}
//
//@Composable
//fun VhalDataContent(modifier: Modifier = Modifier, state: VhalParamsState) {
//    LazyColumn(modifier = modifier.fillMaxSize()) {
//
//    }
//}
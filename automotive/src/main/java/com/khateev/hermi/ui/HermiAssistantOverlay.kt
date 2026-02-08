package com.khateev.hermi.ui

import android.util.Log
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.khateev.hermi.viewmodel.HermiViewModel

@Composable
fun HermiAssistantOverlay(
    viewModel: HermiViewModel = hiltViewModel(),
    onClose: () -> Unit
) {
    val carState by viewModel.uiState.collectAsState()
    val spokenText by viewModel.recognizedText.collectAsState(initial = "")
    val isListening by viewModel.isListening.collectAsState()

    LaunchedEffect(isListening) {
        Log.d("HermiUI_Compose", "Compose saw change: $isListening")
    }

    LaunchedEffect(Unit) {
        viewModel.startVoiceInput()
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f))) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PulseAnimation(isListening)

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = if (spokenText.isEmpty() && isListening) "Listening..." else spokenText,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            if (carState.fuelLevel > 0) {
                Text(
                    text = "Fuel: ${carState.fuelLevel} ml",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        IconButton(
            onClick = { onClose() },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .background(Color.Red)
                .padding(16.dp)
                .zIndex(1f)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
        }
    }
}



@Composable
fun PulseAnimation(isListening: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")

    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isListening) 1f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(200.dp)
    ) {
        Canvas(modifier = Modifier.size(80.dp)) {
            drawCircle(
                color = Color(0xFF00E5FF),
                radius = size.minDimension / 2
            )
        }

        if (isListening) {
            for (i in 1..3) {
                val delayProgress = (progress + (i * 0.3f)) % 1f
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = Color(0xFF00E5FF).copy(alpha = 1f - delayProgress),
                        radius = (size.minDimension / 2) * delayProgress,
                        style = Stroke(width = 4.dp.toPx())
                    )
                }
            }
        }
    }
}
package com.khateev.hermi.ui

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import com.khateev.hermi.NavigationEvent
import com.khateev.hermi.viewmodel.HermiViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun HermiAssistantOverlay(
    viewModel: HermiViewModel = hiltViewModel(),
    onClose: () -> Unit
) {
    val carState by viewModel.uiState.collectAsState()
    val spokenText by viewModel.recognizedText.collectAsState(initial = "")
    val isListening by viewModel.isRecording.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val hasError by viewModel.hasError.collectAsState()
    val context = LocalContext.current

    val handleDismiss = {
        viewModel.stopRecordingAndTranscribe()
        onClose()
    }

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collectLatest { event ->
            when (event) {
                is NavigationEvent.OpenNavigation -> {
                    openNavigationFor(context, event.searchQuery)
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                handleDismiss()
            }) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PulseAnimation(isListening, isProcessing, hasError != null)

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
    }
}


@Composable
fun PulseAnimation(isListening: Boolean, isProcessing: Boolean, hasError: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")

    val colorScheme = MaterialTheme.colorScheme

    val color = when {
        hasError -> colorScheme.error
        isProcessing -> colorScheme.primary
        isListening -> colorScheme.error
        else -> colorScheme.secondary
    }

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
                color = color,
                radius = size.minDimension / 2
            )
        }

        if (isListening) {
            for (i in 1..3) {
                val delayProgress = (progress + (i * 0.3f)) % 1f
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = color.copy(alpha = 1f - delayProgress),
                        radius = (size.minDimension / 2) * delayProgress,
                        style = Stroke(width = 4.dp.toPx())
                    )
                }
            }
        }
    }
}

private fun openNavigationFor(context: Context, searchQuery: String) {
    try {
        // First, let's see what apps are available
        val uri = "https://www.google.com/maps/search/?api=1&query=$searchQuery&open_now=true".toUri()

        val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        // Check available apps
        val availableApps = context.packageManager.queryIntentActivities(
            mapIntent,
            PackageManager.MATCH_DEFAULT_ONLY
        )

        Log.d(
            "Navigation",
            "Available apps for geo intent: ${availableApps.map { it.activityInfo.packageName }}"
        )

        if (availableApps.isNotEmpty()) {
            context.startActivity(mapIntent)
            Log.d("Navigation", "Opened navigation successfully")
        } else {
            // No navigation app available - try alternative approaches
            Log.w("Navigation", "No navigation app available, trying alternatives")
            tryAlternativeNavigation(context, searchQuery)
        }

    } catch (e: Exception) {
        Log.e("Navigation", "Failed to open navigation: ${e.message}")
        tryAlternativeNavigation(context, searchQuery)
    }
}

private fun tryAlternativeNavigation(context: Context, searchQuery: String) {
    // Try different intent types
    val alternatives = listOf(
        // Try Google Maps with different URI schemes
        Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://www.google.com/maps/search/?api=1&query=$searchQuery")
        ),

        // Try generic search intent
        Intent(Intent.ACTION_WEB_SEARCH).apply {
            putExtra(SearchManager.QUERY, "$searchQuery near me")
        },

        // Try browser with Google Maps URL
        Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/$searchQuery"))
    )

    for ((index, intent) in alternatives.withIndex()) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        if (context.packageManager.resolveActivity(intent, 0) != null) {
            try {
                context.startActivity(intent)
                Log.d("Navigation", "Opened with alternative method $index")
                return
            } catch (e: Exception) {
                Log.d("Navigation", "Alternative $index failed: ${e.message}")
                continue
            }
        }
    }

    Log.e("Navigation", "All navigation alternatives failed")
    // Last resort - show in-app message
    Toast.makeText(context, "Navigation not available. Showing results in-app.", Toast.LENGTH_SHORT)
        .show()
}
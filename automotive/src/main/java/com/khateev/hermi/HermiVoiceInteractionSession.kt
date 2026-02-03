package com.khateev.hermi

import android.content.Context
import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.view.View
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.compositionContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class HermiVoiceInteractionSession(context: Context) : VoiceInteractionSession(context),
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    // Реализация Lifecycle
    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle = lifecycleRegistry

    override val viewModelStore = ViewModelStore()

    // Реализация SavedStateRegistry
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry =
        savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    override fun onPrepareShow(args: Bundle?, showFlags: Int) {
        super.onPrepareShow(args, showFlags)

    }

    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onHide() {
        super.onHide()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        viewModelStore.clear()
    }

    override fun onCreateContentView(): View {
//        val frameLayout = android.widget.FrameLayout(context)

//        val composeView = ComposeView(context).apply {
//            setViewTreeLifecycleOwner(this@HermiVoiceInteractionSession)
//            setViewTreeSavedStateRegistryOwner(this@HermiVoiceInteractionSession)
//            setViewTreeViewModelStoreOwner(this@HermiVoiceInteractionSession)

//            // Это важно: говорим Compose, как управлять композицией в этом окне
//            setCompositionStrategy {
//                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
//            }
//
//            setContent {
//                HermiAssistantOverlay(onClose = { hide() })
//            }
//        }

//        frameLayout.addView(composeView)
//        return frameLayout

        val view = ComposeView(context)

        // 1. Создаем вручную контекст композиции
        val coroutineContext = AndroidUiDispatcher.CurrentThread
        val recomposer = Recomposer(coroutineContext)
        val scope = CoroutineScope(coroutineContext)

        // 2. Запускаем рекомпозер в фоне
        scope.launch {
            recomposer.runRecomposeAndApplyChanges()
        }

        // 3. ПРИНУДИТЕЛЬНО назначаем контекст, чтобы он не искал LifecycleOwner
        view.compositionContext = recomposer

        // 4. Всё еще вешаем тэги на всякий случай
        view.setTag(androidx.lifecycle.runtime.R.id.view_tree_lifecycle_owner, this)
        view.setTag(androidx.lifecycle.viewmodel.R.id.view_tree_view_model_store_owner, this)
        view.setTag(androidx.savedstate.R.id.view_tree_saved_state_registry_owner, this)

        view.setContent {
//            HermiAssistantOverlay(onClose = { hide() })
            androidx.compose.material3.Text(
                text = "HERMI TEST",
                color = androidx.compose.ui.graphics.Color.Red
            )
        }

        return view
    }
}

@Composable
fun HermiAssistantOverlay(onClose: () -> Unit) {
//    VhalTheme {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f)) // Затемнение фона
            .clickable { onClose() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .clickable(enabled = false) { }, // Чтобы клик не пробрасывался вниз
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Hermi AI Assistant", color = Color.Gray)
                Spacer(Modifier.height(16.dp))

                VoicePulseAnimation()

                Spacer(Modifier.height(16.dp))
                Text(
                    "How can I help you today?",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )
            }
        }
    }
//    }
}

@Composable
fun VoicePulseAnimation(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "VoicePulseAnimation")

    val circle1Scale by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "circle1Scale"
    )
    val circle1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "circle1Alpha"
    )

    val circle2Scale by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, delayMillis = 200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "circle2Scale"
    )
    val circle2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, delayMillis = 200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "circle2Alpha"
    )

    val circle3Scale by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, delayMillis = 400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "circle3Scale"
    )
    val circle3Alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, delayMillis = 400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "circle3Alpha"
    )

    Box(
        modifier = modifier.size(150.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val maxRadius = size.minDimension / 2

            drawCircle(
                color = Color.White.copy(alpha = circle3Alpha),
                radius = maxRadius * circle3Scale,
                center = center,
                style = Stroke(width = 4.dp.toPx())
            )

            drawCircle(
                color = Color.White.copy(alpha = circle2Alpha),
                radius = maxRadius * circle2Scale,
                center = center,
                style = Stroke(width = 4.dp.toPx())
            )

            drawCircle(
                color = Color.White.copy(alpha = circle1Alpha),
                radius = maxRadius * circle1Scale,
                center = center,
                style = Stroke(width = 4.dp.toPx())
            )
        }
    }
}
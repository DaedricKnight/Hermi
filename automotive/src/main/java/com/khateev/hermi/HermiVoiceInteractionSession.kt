package com.khateev.hermi

import android.Manifest
import android.R
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.khateev.hermi.di.HermiSessionEntryPoint
import com.khateev.hermi.shared.manager.VersionManagerImpl
import com.khateev.hermi.ui.HermiAssistantActivity
import com.khateev.hermi.ui.HermiAssistantOverlay
import com.khateev.hermi.viewmodel.HermiViewModel
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch

class HermiVoiceInteractionSession(context: Context) : VoiceInteractionSession(context),
    LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle = lifecycleRegistry
    override val viewModelStore = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry =
        savedStateRegistryController.savedStateRegistry

    private var hermiViewModel: HermiViewModel? = null

    val audioPermission = mutableStateOf(false)

    override fun onCreate() {
        savedStateRegistryController.performRestore(null)
        super.onCreate()
        window?.window?.setBackgroundDrawableResource(R.color.transparent)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    @SuppressLint("CoroutineCreationDuringComposition")
    override fun onCreateContentView(): View {
        Log.d("HermiSession", "onCreateContentView called")

        val composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(this@HermiVoiceInteractionSession)
            setViewTreeSavedStateRegistryOwner(this@HermiVoiceInteractionSession)

            val tagId = context.resources.getIdentifier(
                "view_tree_view_model_store_owner",
                "id",
                context.packageName
            )
            setTag(tagId, this@HermiVoiceInteractionSession)

            setContent {
                CompositionLocalProvider(
                    LocalViewModelStoreOwner provides this@HermiVoiceInteractionSession
                ) {
                    val entryPoint = EntryPointAccessors.fromApplication(
                        context.applicationContext,
                        HermiSessionEntryPoint::class.java
                    )

                    val viewModel: HermiViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                                return HermiViewModel(
                                    voskManager = entryPoint.getRecognitionManager(),
                                    audioRecorderImpl = entryPoint.getAudioRecorder(),
                                    vhalReader = entryPoint.getVhalReader(),
                                    aiProcessor = entryPoint.getAiProcessor(),
                                    voiceManager = entryPoint.getVoiceManager(),
                                    versionManager = VersionManagerImpl(context),
                                    vadManager = entryPoint.getVADManager()
                                ) as T
                            }
                        }
                    )
                    hermiViewModel = viewModel
                    HermiAssistantOverlay(audioPermission = audioPermission.value, viewModel = viewModel, onClose = { hide() })

                    viewModel.viewModelScope.launch {
                        viewModel.isRecording.collect { isListening ->
                            // Recording stopped (VAD fired) — hide the session
                            if (!isListening && viewModel.isProcessing.value == false) {
                                // Small delay before closing, so the transcript stays readable
                            }
                        }
                    }
                }
            }
        }
        return composeView
    }

    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        audioPermission.value = context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            val intent = Intent(context, HermiAssistantActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra("REQUEST_PERMISSIONS", true)
            }
            startAssistantActivity(intent)
            hide()
        } else {
            window?.window?.let { win ->
                win.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)
                win.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            }

            hermiViewModel?.let { viewModel ->
                viewModel.viewModelScope.launch {
                    // Just start recording here.
                    // The ViewModel stops it itself through VADManager.
                    viewModel.startRecording()
                }
            }
        }
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
}

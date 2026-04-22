package com.khateev.hermi.di

import com.khateev.hermi.shared.interfaces.AIProcessor
import com.khateev.hermi.shared.interfaces.AudioRecorder
import com.khateev.hermi.shared.interfaces.RecognitionManager
import com.khateev.hermi.shared.manager.VoiceInterfaceManager
import com.khateev.hermi.shared.ai.VADManager
import com.qubit.core_vhal.repository.VhalReader
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface HermiSessionEntryPoint {
    fun getVhalReader(): VhalReader
    fun getAiProcessor(): AIProcessor
    fun getVoiceManager(): VoiceInterfaceManager
    fun getAudioRecorder(): AudioRecorder
    fun getRecognitionManager(): RecognitionManager
    fun getVADManager(): VADManager
}

package com.khateev.hermi.di

import com.khateev.hermi.repository.VhalReader
import com.khateev.hermi.shared.HermiAIProcessor
import com.khateev.hermi.shared.SpeechToTextManager
import com.khateev.hermi.shared.VoiceInterfaceManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface HermiSessionEntryPoint {
    fun getSTT(): SpeechToTextManager
    fun getVhalReader(): VhalReader
    fun getAiProcessor(): HermiAIProcessor
    fun getVoiceManager(): VoiceInterfaceManager
}
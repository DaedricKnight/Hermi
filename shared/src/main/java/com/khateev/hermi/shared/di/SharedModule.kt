package com.khateev.hermi.shared.di

import android.content.Context
import android.speech.RecognitionListener
import android.speech.tts.TextToSpeech
import com.khateev.hermi.shared.HermiAIProcessor
import com.khateev.hermi.shared.SpeechToTextManager
import com.khateev.hermi.shared.VoiceInterfaceManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SharedModule {

    @Provides
    @Singleton
    fun provideHermiAIProcessor(@ApplicationContext context: Context): HermiAIProcessor {
        return HermiAIProcessor(context)
    }

    @Provides
    @Singleton
    fun provideSpeechToTextManager(@ApplicationContext context: Context): RecognitionListener {
        return SpeechToTextManager(context)
    }

        @Provides
    @Singleton
    fun provideVoiceInterfaceManager(@ApplicationContext context: Context): VoiceInterfaceManager {
        return VoiceInterfaceManager(context)
    }


}
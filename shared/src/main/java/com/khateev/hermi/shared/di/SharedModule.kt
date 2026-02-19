package com.khateev.hermi.shared.di

import android.content.Context
import android.speech.RecognitionListener
import com.khateev.hermi.shared.AudioRecorderImpl
import com.khateev.hermi.shared.ai.TFLiteAIProcessor
import com.khateev.hermi.shared.ai.VoskManager
import com.khateev.hermi.shared.interfaces.AIProcessor
import com.khateev.hermi.shared.interfaces.AudioRecorder
import com.khateev.hermi.shared.interfaces.RecognitionManager
import com.khateev.hermi.shared.manager.SpeechToTextManager
import com.khateev.hermi.shared.manager.VoiceInterfaceManager
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
    fun provideHermiAIProcessor(@ApplicationContext context: Context): AIProcessor {
        return TFLiteAIProcessor(context)
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

    @Provides
    @Singleton
    fun provideRecognitionManager(@ApplicationContext context: Context): RecognitionManager {
        return VoskManager(context)
    }

    @Provides
    @Singleton
    fun provideAudioRecorder(): AudioRecorder {
        return AudioRecorderImpl()
    }

}
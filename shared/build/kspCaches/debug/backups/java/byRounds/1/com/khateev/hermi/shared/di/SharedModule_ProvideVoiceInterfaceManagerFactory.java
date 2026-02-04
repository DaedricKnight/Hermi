package com.khateev.hermi.shared.di;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation"
})
public final class SharedModule_ProvideVoiceInterfaceManagerFactory implements Factory<TextToSpeech.OnInitListener> {
  private final Provider<Context> contextProvider;

  public SharedModule_ProvideVoiceInterfaceManagerFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public TextToSpeech.OnInitListener get() {
    return provideVoiceInterfaceManager(contextProvider.get());
  }

  public static SharedModule_ProvideVoiceInterfaceManagerFactory create(
      Provider<Context> contextProvider) {
    return new SharedModule_ProvideVoiceInterfaceManagerFactory(contextProvider);
  }

  public static TextToSpeech.OnInitListener provideVoiceInterfaceManager(Context context) {
    return Preconditions.checkNotNullFromProvides(SharedModule.INSTANCE.provideVoiceInterfaceManager(context));
  }
}

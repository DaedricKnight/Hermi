package com.khateev.hermi.shared.di;

import android.content.Context;
import android.speech.RecognitionListener;
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
public final class SharedModule_ProvideSpeechToTextManagerFactory implements Factory<RecognitionListener> {
  private final Provider<Context> contextProvider;

  public SharedModule_ProvideSpeechToTextManagerFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public RecognitionListener get() {
    return provideSpeechToTextManager(contextProvider.get());
  }

  public static SharedModule_ProvideSpeechToTextManagerFactory create(
      Provider<Context> contextProvider) {
    return new SharedModule_ProvideSpeechToTextManagerFactory(contextProvider);
  }

  public static RecognitionListener provideSpeechToTextManager(Context context) {
    return Preconditions.checkNotNullFromProvides(SharedModule.INSTANCE.provideSpeechToTextManager(context));
  }
}

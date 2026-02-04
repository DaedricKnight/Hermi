package com.khateev.hermi.shared;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class SpeechToTextManager_Factory implements Factory<SpeechToTextManager> {
  private final Provider<Context> contextProvider;

  public SpeechToTextManager_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public SpeechToTextManager get() {
    return newInstance(contextProvider.get());
  }

  public static SpeechToTextManager_Factory create(Provider<Context> contextProvider) {
    return new SpeechToTextManager_Factory(contextProvider);
  }

  public static SpeechToTextManager newInstance(Context context) {
    return new SpeechToTextManager(context);
  }
}

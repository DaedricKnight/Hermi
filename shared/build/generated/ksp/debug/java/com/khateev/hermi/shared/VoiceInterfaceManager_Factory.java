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
public final class VoiceInterfaceManager_Factory implements Factory<VoiceInterfaceManager> {
  private final Provider<Context> contextProvider;

  public VoiceInterfaceManager_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public VoiceInterfaceManager get() {
    return newInstance(contextProvider.get());
  }

  public static VoiceInterfaceManager_Factory create(Provider<Context> contextProvider) {
    return new VoiceInterfaceManager_Factory(contextProvider);
  }

  public static VoiceInterfaceManager newInstance(Context context) {
    return new VoiceInterfaceManager(context);
  }
}

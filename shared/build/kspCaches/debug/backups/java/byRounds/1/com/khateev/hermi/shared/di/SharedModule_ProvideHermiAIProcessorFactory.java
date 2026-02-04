package com.khateev.hermi.shared.di;

import android.content.Context;
import com.khateev.hermi.shared.HermiAIProcessor;
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
public final class SharedModule_ProvideHermiAIProcessorFactory implements Factory<HermiAIProcessor> {
  private final Provider<Context> contextProvider;

  public SharedModule_ProvideHermiAIProcessorFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public HermiAIProcessor get() {
    return provideHermiAIProcessor(contextProvider.get());
  }

  public static SharedModule_ProvideHermiAIProcessorFactory create(
      Provider<Context> contextProvider) {
    return new SharedModule_ProvideHermiAIProcessorFactory(contextProvider);
  }

  public static HermiAIProcessor provideHermiAIProcessor(Context context) {
    return Preconditions.checkNotNullFromProvides(SharedModule.INSTANCE.provideHermiAIProcessor(context));
  }
}

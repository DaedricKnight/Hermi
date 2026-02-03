package com.khateev.hermi.di

import android.content.Context
import com.khateev.hermi.repository.HybridVhalReader
import com.khateev.hermi.repository.MockVhalReaderImpl
import com.khateev.hermi.repository.VhalReader
import com.khateev.hermi.repository.VhalReaderImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object VhalModule {
    @Provides
    @Singleton
    fun provideVhalReader(
        @ApplicationContext context: Context
    ): VhalReader {
        return HybridVhalReader(
            realReader = VhalReaderImpl(context),
            mockReader = MockVhalReaderImpl()
        )
    }
}
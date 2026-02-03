package com.khateev.hermi.repository

import com.khateev.hermi.shared.VhalParamsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

class HybridVhalReader @Inject constructor(
    private val realReader: VhalReaderImpl,
    private val mockReader: MockVhalReaderImpl
) : VhalReader {

    private val isMockMode = MutableStateFlow(true)

    @OptIn(ExperimentalCoroutinesApi::class)
    override val carState: StateFlow<VhalParamsState> = isMockMode
        .flatMapLatest { useMock ->
            if (useMock) mockReader.carState else realReader.carState
        }.stateIn(
            scope = CoroutineScope(Dispatchers.IO),
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = VhalParamsState()
        )

    override val isConnected: StateFlow<Boolean> =
        if (isMockMode.value) mockReader.isConnected else realReader.isConnected

    override fun connect() {
        if (isMockMode.value) {
            if (!mockReader.isConnected.value) mockReader.connect()
        } else {
            if (!realReader.isConnected.value) realReader.connect()
        }
    }

    override fun disconnect() {
        if (realReader.isConnected.value) realReader.disconnect()
        if (mockReader.isConnected.value) mockReader.disconnect()
    }

    fun toggleMode(useMock: Boolean) {
        disconnect()
        isMockMode.value = useMock
        connect()
    }
}
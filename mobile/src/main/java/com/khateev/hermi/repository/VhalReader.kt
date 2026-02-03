package com.khateev.hermi.repository

import com.khateev.hermi.shared.VhalParamsState
import kotlinx.coroutines.flow.StateFlow

interface VhalReader {
    val carState: StateFlow<VhalParamsState>

    val isConnected: StateFlow<Boolean>
    fun connect()
    fun disconnect()
}
package com.khateev.hermi.shared

import kotlinx.coroutines.flow.Flow

interface AIProcessor {
    fun processVehicleData(state: VhalParamsState): Flow<String>
}
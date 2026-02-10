package com.khateev.hermi.shared

import com.khateev.hermi.shared.HermiAIProcessor.Command
import kotlinx.coroutines.flow.Flow

interface AIProcessor {
    fun parseCommand(input: String): Command
}
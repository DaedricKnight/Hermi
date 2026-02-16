package com.khateev.hermi.shared.interfaces

import com.khateev.hermi.shared.HermiAIProcessor

interface AIProcessor {
    fun parseCommand(input: String): HermiAIProcessor.Command
}
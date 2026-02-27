package com.khateev.hermi.shared.interfaces

import com.khateev.hermi.shared.model.Command

interface AIProcessor {
    fun parseCommand(input: String): Command
}
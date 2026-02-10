package com.khateev.hermi

sealed class NavigationEvent {
    data class OpenNavigation(val searchQuery: String) : NavigationEvent()
}
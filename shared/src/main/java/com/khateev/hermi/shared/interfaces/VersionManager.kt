package com.khateev.hermi.shared.interfaces

import kotlinx.coroutines.flow.Flow

/**
 * Interface for managing the Pro version status of the application
 */
interface VersionManager {

    /**
     * Flow for tracking the current Pro version status
     */
    val isProVersion: Flow<Boolean>

    /**
     * Get the current Pro version status (synchronously)
     */
    fun isProNow(): Boolean

    /**
     * Set the Pro version status
     * @param isPro true to activate the Pro version
     */
    suspend fun setProVersion(isPro: Boolean)

    /**
     * Check if the subscription has expired (if used)
     */
    suspend fun isSubscriptionActive(): Boolean
}


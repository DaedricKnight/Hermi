package com.khateev.hermi.shared.manager

import android.content.Context
import com.khateev.hermi.shared.interfaces.VersionManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.core.content.edit

/**
 * VersionManager backed by SharedPreferences
 */
class VersionManagerImpl(
    private val context: Context
) : VersionManager {

    companion object {
        private const val PREFS_NAME = "hermi_version_prefs"
        private const val KEY_IS_PRO = "is_pro_version"
        private const val KEY_PRO_UNTIL = "pro_valid_until"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _isProVersion = MutableStateFlow(loadProStatus())
    override val isProVersion: Flow<Boolean> = _isProVersion.asStateFlow()

    override fun isProNow(): Boolean {
        return _isProVersion.value
    }

    override suspend fun setProVersion(isPro: Boolean) {
        prefs.edit().putBoolean(KEY_IS_PRO, isPro).apply()
        _isProVersion.value = isPro
    }

    override suspend fun isSubscriptionActive(): Boolean {
        val proUntil = prefs.getLong(KEY_PRO_UNTIL, 0L)
        if (proUntil == 0L) return false

        return System.currentTimeMillis() < proUntil
    }

    private fun loadProStatus(): Boolean {
        // Check for an active subscription first
        val isSubscriptionActive = isSubscriptionActiveSync()

        // An active subscription means Pro
        if (isSubscriptionActive) {
            return true
        }

        // Otherwise fall back to the stored flag
        return prefs.getBoolean(KEY_IS_PRO, false)
    }

    private fun isSubscriptionActiveSync(): Boolean {
        val proUntil = prefs.getLong(KEY_PRO_UNTIL, 0L)
        return proUntil > 0L && System.currentTimeMillis() < proUntil
    }

    /**
     * Activates the Pro version for a given period (in milliseconds)
     * @param durationMs Subscription length in milliseconds
     */
    fun activateProSubscription(durationMs: Long) {
        val validUntil = System.currentTimeMillis() + durationMs
        prefs.edit { putLong(KEY_PRO_UNTIL, validUntil) }
        _isProVersion.value = true
    }

    /**
     * Cancels the Pro version
     */
    fun cancelProVersion() {
        prefs.edit {
            putBoolean(KEY_IS_PRO, false)
                .putLong(KEY_PRO_UNTIL, 0L)
        }
        _isProVersion.value = false
    }

    /**
     * Subscription expiry date, when one is active
     */
    fun getSubscriptionExpiryTime(): Long {
        return prefs.getLong(KEY_PRO_UNTIL, 0L)
    }
}


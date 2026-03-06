package com.khateev.hermi.shared.manager

import android.content.Context
import com.khateev.hermi.shared.interfaces.VersionManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.core.content.edit

/**
 * Реализация VersionManager с использованием SharedPreferences
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
        // Сначала проверяем, есть ли активная подписка
        val isSubscriptionActive = isSubscriptionActiveSync()

        // Если подписка активна, возвращаем true
        if (isSubscriptionActive) {
            return true
        }

        // Иначе проверяем сохраненный статус
        return prefs.getBoolean(KEY_IS_PRO, false)
    }

    private fun isSubscriptionActiveSync(): Boolean {
        val proUntil = prefs.getLong(KEY_PRO_UNTIL, 0L)
        return proUntil > 0L && System.currentTimeMillis() < proUntil
    }

    /**
     * Активирует Pro версию на определенный период (в миллисекундах)
     * @param durationMs Длительность подписки в миллисекундах
     */
    fun activateProSubscription(durationMs: Long) {
        val validUntil = System.currentTimeMillis() + durationMs
        prefs.edit { putLong(KEY_PRO_UNTIL, validUntil) }
        _isProVersion.value = true
    }

    /**
     * Отменяет Pro версию
     */
    fun cancelProVersion() {
        prefs.edit {
            putBoolean(KEY_IS_PRO, false)
                .putLong(KEY_PRO_UNTIL, 0L)
        }
        _isProVersion.value = false
    }

    /**
     * Получает дату истечения подписки (если активна)
     */
    fun getSubscriptionExpiryTime(): Long {
        return prefs.getLong(KEY_PRO_UNTIL, 0L)
    }
}


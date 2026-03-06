package com.khateev.hermi.shared.interfaces

import kotlinx.coroutines.flow.Flow

/**
 * Интерфейс для управления статусом Pro версии приложения
 */
interface VersionManager {

    /**
     * Flow для отслеживания текущего статуса Pro версии
     */
    val isProVersion: Flow<Boolean>

    /**
     * Получить текущий статус Pro версии (синхронно)
     */
    fun isProNow(): Boolean

    /**
     * Установить статус Pro версии
     * @param isPro true для активации Pro версии
     */
    suspend fun setProVersion(isPro: Boolean)

    /**
     * Проверить, истекла ли подписка (если используется)
     */
    suspend fun isSubscriptionActive(): Boolean
}


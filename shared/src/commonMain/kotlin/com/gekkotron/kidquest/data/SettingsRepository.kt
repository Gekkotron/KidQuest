package com.gekkotron.kidquest.data

import kotlinx.coroutines.flow.Flow

class SettingsRepository(private val db: KidQuestDatabase) {
    private val dao = db.settingsDao()

    fun observeActions(): Flow<List<PointAction>> = dao.observeActions()

    suspend fun addAction(label: String, points: Int, icon: String) {
        require(label.isNotBlank()) { "label must not be blank" }
        require(points != 0) { "points must not be 0" }
        val next = (dao.actions().maxOfOrNull { it.sortOrder } ?: -1) + 1
        dao.insertAction(PointAction(label = label.trim(), points = points, icon = icon, sortOrder = next))
    }

    suspend fun updateAction(action: PointAction) {
        require(action.label.isNotBlank()) { "label must not be blank" }
        require(action.points != 0) { "points must not be 0" }
        dao.updateActions(action.copy(label = action.label.trim()))
    }

    suspend fun deleteAction(action: PointAction) = dao.deleteAction(action)

    suspend fun moveAction(actionId: Long, up: Boolean) = db.tx {
        val reordered = swapped(dao.actions(), actionId, up, { it.id }) ?: return@tx
        dao.updateActions(*reordered.mapIndexed { i, a -> a.copy(sortOrder = i) }.toTypedArray())
    }

    fun observeQuickAmounts(): Flow<List<QuickAmount>> = dao.observeQuickAmounts()

    suspend fun addQuickAmount(points: Int) {
        require(points != 0) { "points must not be 0" }
        val next = (dao.quickAmounts().maxOfOrNull { it.sortOrder } ?: -1) + 1
        dao.insertQuickAmount(QuickAmount(points = points, sortOrder = next))
    }

    suspend fun updateQuickAmount(amount: QuickAmount) {
        require(amount.points != 0) { "points must not be 0" }
        dao.updateQuickAmounts(amount)
    }

    suspend fun deleteQuickAmount(amount: QuickAmount) = dao.deleteQuickAmount(amount)

    suspend fun moveQuickAmount(amountId: Long, up: Boolean) = db.tx {
        val reordered = swapped(dao.quickAmounts(), amountId, up, { it.id }) ?: return@tx
        dao.updateQuickAmounts(*reordered.mapIndexed { i, q -> q.copy(sortOrder = i) }.toTypedArray())
    }

    /** null = follow the system language. */
    fun observeLanguage(): Flow<String?> = dao.observeSetting(KEY_LANGUAGE)

    suspend fun setLanguage(tag: String?) {
        if (tag == null) dao.removeSetting(KEY_LANGUAGE) else dao.putSetting(AppSetting(KEY_LANGUAGE, tag))
    }

    private fun <T> swapped(items: List<T>, id: Long, up: Boolean, idOf: (T) -> Long): List<T>? {
        val index = items.indexOfFirst { idOf(it) == id }
        val target = if (up) index - 1 else index + 1
        if (index < 0 || target !in items.indices) return null
        return items.toMutableList().apply { add(target, removeAt(index)) }
    }

    private companion object {
        const val KEY_LANGUAGE = "language"
    }
}

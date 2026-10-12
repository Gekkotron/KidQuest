package com.gekkotron.kidquest.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gekkotron.kidquest.data.BackupRepository
import com.gekkotron.kidquest.data.Child
import com.gekkotron.kidquest.data.ChildRepository
import com.gekkotron.kidquest.data.PointAction
import com.gekkotron.kidquest.data.QuickAmount
import com.gekkotron.kidquest.data.SettingsRepository
import com.gekkotron.kidquest.data.ThemeMode
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Parses "20", "+20", "-10", "−10" (U+2212). Returns null for 0, blank or garbage. */
fun parsePoints(text: String): Int? =
    text.trim().replace('−', '-').removePrefix("+").toIntOrNull()?.takeIf { it != 0 }

data class SettingsUiState(
    val children: List<Child> = emptyList(),
    val actions: List<PointAction> = emptyList(),
    val quickAmounts: List<QuickAmount> = emptyList(),
    val language: String? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
)

sealed interface BackupEffect {
    data class ExportReady(val filename: String, val payload: String) : BackupEffect
    data object ImportSucceeded : BackupEffect
    data object ImportFailed : BackupEffect
}

class SettingsViewModel(
    private val children: ChildRepository,
    private val settings: SettingsRepository,
    private val backup: BackupRepository,
) : ViewModel() {

    private val _effects = Channel<BackupEffect>(Channel.BUFFERED)
    val effects: Flow<BackupEffect> = _effects.receiveAsFlow()

    val state: StateFlow<SettingsUiState> = combine(
        children.observeAll(),
        settings.observeActions(),
        settings.observeQuickAmounts(),
        settings.observeLanguage(),
        settings.observeThemeMode(),
    ) { kids, actions, quick, lang, theme ->
        SettingsUiState(kids, actions, quick, lang, theme)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SettingsUiState())

    fun addChild(name: String, avatar: String, color: Long): Boolean {
        if (name.isBlank()) return false
        viewModelScope.launch { children.add(name, avatar, color) }
        return true
    }

    fun updateChild(child: Child): Boolean {
        if (child.name.isBlank()) return false
        viewModelScope.launch { children.update(child) }
        return true
    }

    fun setArchived(childId: Long, archived: Boolean) {
        viewModelScope.launch { children.setArchived(childId, archived) }
    }

    fun addAction(label: String, pointsText: String, icon: String): Boolean {
        val points = parsePoints(pointsText) ?: return false
        if (label.isBlank()) return false
        viewModelScope.launch { settings.addAction(label, points, icon) }
        return true
    }

    fun updateAction(action: PointAction, label: String, pointsText: String, icon: String): Boolean {
        val points = parsePoints(pointsText) ?: return false
        if (label.isBlank()) return false
        viewModelScope.launch { settings.updateAction(action.copy(label = label, points = points, icon = icon)) }
        return true
    }

    fun deleteAction(action: PointAction) {
        viewModelScope.launch { settings.deleteAction(action) }
    }

    fun moveAction(actionId: Long, up: Boolean) {
        viewModelScope.launch { settings.moveAction(actionId, up) }
    }

    fun addQuickAmount(pointsText: String): Boolean {
        val points = parsePoints(pointsText) ?: return false
        viewModelScope.launch { settings.addQuickAmount(points) }
        return true
    }

    fun updateQuickAmount(amount: QuickAmount, pointsText: String): Boolean {
        val points = parsePoints(pointsText) ?: return false
        viewModelScope.launch { settings.updateQuickAmount(amount.copy(points = points)) }
        return true
    }

    fun deleteQuickAmount(amount: QuickAmount) {
        viewModelScope.launch { settings.deleteQuickAmount(amount) }
    }

    fun moveQuickAmount(amountId: Long, up: Boolean) {
        viewModelScope.launch { settings.moveQuickAmount(amountId, up) }
    }

    fun setLanguage(tag: String?) {
        viewModelScope.launch { settings.setLanguage(tag) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settings.setThemeMode(mode) }
    }

    fun requestExport(filename: String) {
        viewModelScope.launch { _effects.send(BackupEffect.ExportReady(filename, backup.export())) }
    }

    fun importFromJson(serialized: String) {
        viewModelScope.launch {
            val ok = runCatching { backup.import(serialized) }.isSuccess
            _effects.send(if (ok) BackupEffect.ImportSucceeded else BackupEffect.ImportFailed)
        }
    }
}

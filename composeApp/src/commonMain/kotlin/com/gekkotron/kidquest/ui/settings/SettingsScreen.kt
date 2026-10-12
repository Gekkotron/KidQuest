package com.gekkotron.kidquest.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gekkotron.kidquest.data.Child
import com.gekkotron.kidquest.data.PointAction
import com.gekkotron.kidquest.data.QuickAmount
import com.gekkotron.kidquest.data.ThemeMode
import com.gekkotron.kidquest.resources.Res
import com.gekkotron.kidquest.resources.delete
import com.gekkotron.kidquest.resources.settings_actions
import com.gekkotron.kidquest.resources.settings_add_action
import com.gekkotron.kidquest.resources.settings_add_child
import com.gekkotron.kidquest.resources.settings_add_quick
import com.gekkotron.kidquest.resources.settings_appearance
import com.gekkotron.kidquest.resources.settings_archive
import com.gekkotron.kidquest.resources.settings_archived
import com.gekkotron.kidquest.resources.settings_children
import com.gekkotron.kidquest.resources.settings_lang_system
import com.gekkotron.kidquest.resources.settings_language
import com.gekkotron.kidquest.resources.settings_quick
import com.gekkotron.kidquest.resources.settings_theme_dark
import com.gekkotron.kidquest.resources.settings_theme_light
import com.gekkotron.kidquest.resources.settings_theme_system
import com.gekkotron.kidquest.resources.settings_unarchive
import com.gekkotron.kidquest.ui.components.ChildAvatar
import com.gekkotron.kidquest.ui.components.ChildEditDialog
import com.gekkotron.kidquest.ui.components.formatSigned
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private sealed interface Editing {
    data class ChildEdit(val child: Child?) : Editing
    data class ActionEdit(val action: PointAction?) : Editing
    data class QuickEdit(val amount: QuickAmount?) : Editing
}

@Composable
fun SettingsScreen(vm: SettingsViewModel = koinViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Editing?>(null) }

    LazyColumn(Modifier.fillMaxSize().testTag("screen-settings"), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { Header(Res.string.settings_children) }
        items(s.children, key = { "c${it.id}" }) { c ->
            ListItem(
                leadingContent = { ChildAvatar(c.avatar, c.color, 40.dp) },
                headlineContent = { Text(if (c.archived) "${c.name} (${stringResource(Res.string.settings_archived)})" else c.name) },
                trailingContent = {
                    TextButton(onClick = { vm.setArchived(c.id, !c.archived) }) {
                        Text(stringResource(if (c.archived) Res.string.settings_unarchive else Res.string.settings_archive))
                    }
                },
                modifier = Modifier.clickable { editing = Editing.ChildEdit(c) },
            )
        }
        item { AddButton(Res.string.settings_add_child, "settings-add-child") { editing = Editing.ChildEdit(null) } }

        item { HorizontalDivider(); Header(Res.string.settings_actions) }
        items(s.actions, key = { "a${it.id}" }) { a ->
            ListItem(
                leadingContent = { Text(a.icon, style = MaterialTheme.typography.headlineSmall) },
                headlineContent = { Text(a.label) },
                supportingContent = { Text(formatSigned(a.points)) },
                trailingContent = { ReorderDelete(onUp = { vm.moveAction(a.id, true) }, onDown = { vm.moveAction(a.id, false) }, onDelete = { vm.deleteAction(a) }) },
                modifier = Modifier.clickable { editing = Editing.ActionEdit(a) },
            )
        }
        item { AddButton(Res.string.settings_add_action, "settings-add-action") { editing = Editing.ActionEdit(null) } }

        item { HorizontalDivider(); Header(Res.string.settings_quick) }
        items(s.quickAmounts, key = { "q${it.id}" }) { q ->
            ListItem(
                headlineContent = { Text(formatSigned(q.points), style = MaterialTheme.typography.titleMedium) },
                trailingContent = { ReorderDelete(onUp = { vm.moveQuickAmount(q.id, true) }, onDown = { vm.moveQuickAmount(q.id, false) }, onDelete = { vm.deleteQuickAmount(q) }) },
                modifier = Modifier.clickable { editing = Editing.QuickEdit(q) },
            )
        }
        item { AddButton(Res.string.settings_add_quick, "settings-add-quick") { editing = Editing.QuickEdit(null) } }

        item { HorizontalDivider(); Header(Res.string.settings_language) }
        item {
            LanguageOption(stringResource(Res.string.settings_lang_system), s.language == null, "lang-system") { vm.setLanguage(null) }
            LanguageOption("Français", s.language == "fr", "lang-fr") { vm.setLanguage("fr") }
            LanguageOption("English", s.language == "en", "lang-en") { vm.setLanguage("en") }
        }

        item { HorizontalDivider(); Header(Res.string.settings_appearance) }
        item {
            LanguageOption(stringResource(Res.string.settings_theme_system), s.themeMode == ThemeMode.SYSTEM, "theme-system") { vm.setThemeMode(ThemeMode.SYSTEM) }
            LanguageOption(stringResource(Res.string.settings_theme_light), s.themeMode == ThemeMode.LIGHT, "theme-light") { vm.setThemeMode(ThemeMode.LIGHT) }
            LanguageOption(stringResource(Res.string.settings_theme_dark), s.themeMode == ThemeMode.DARK, "theme-dark") { vm.setThemeMode(ThemeMode.DARK) }
        }
    }

    when (val e = editing) {
        is Editing.ChildEdit -> ChildEditDialog(e.child, onDismiss = { editing = null }) { name, avatar, color ->
            val ok = if (e.child == null) vm.addChild(name, avatar, color) else vm.updateChild(e.child.copy(name = name, avatar = avatar, color = color))
            if (ok) editing = null
        }
        is Editing.ActionEdit -> ActionEditDialog(e.action, onDismiss = { editing = null }) { label, points, icon ->
            val ok = if (e.action == null) vm.addAction(label, points, icon) else vm.updateAction(e.action, label, points, icon)
            if (ok) editing = null
        }
        is Editing.QuickEdit -> QuickAmountDialog(e.amount?.points, onDismiss = { editing = null }) { points ->
            val ok = if (e.amount == null) vm.addQuickAmount(points) else vm.updateQuickAmount(e.amount, points)
            if (ok) editing = null
        }
        null -> Unit
    }
}

@Composable
private fun Header(title: StringResource) {
    Text(stringResource(title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp))
}

@Composable
private fun AddButton(label: StringResource, tag: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.padding(horizontal = 8.dp).testTag(tag)) { Text("＋ " + stringResource(label)) }
}

@Composable
private fun ReorderDelete(onUp: () -> Unit, onDown: () -> Unit, onDelete: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(0.dp), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onUp) { Text("▲") }
        TextButton(onClick = onDown) { Text("▼") }
        TextButton(onClick = onDelete) { Text(stringResource(Res.string.delete)) }
    }
}

@Composable
private fun LanguageOption(label: String, selected: Boolean, tag: String, onSelect: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().selectable(selected = selected, onClick = onSelect).padding(horizontal = 16.dp, vertical = 4.dp).testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(label, modifier = Modifier.padding(start = 8.dp))
    }
}

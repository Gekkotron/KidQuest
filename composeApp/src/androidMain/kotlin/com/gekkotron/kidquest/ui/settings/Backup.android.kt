package com.gekkotron.kidquest.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberBackupExport(): (filename: String, payload: String) -> Unit {
    val context = LocalContext.current
    val pending = remember { mutableStateOf<String?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val payload = pending.value
        pending.value = null
        if (uri != null && payload != null) {
            context.contentResolver.openOutputStream(uri)?.use { it.write(payload.encodeToByteArray()) }
        }
    }
    return remember(launcher) {
        { filename, payload ->
            pending.value = payload
            launcher.launch(filename)
        }
    }
}

@Composable
actual fun rememberBackupImport(onPicked: (text: String) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val text = context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
        if (text != null) onPicked(text)
    }
    return remember(launcher) { { launcher.launch(arrayOf("application/json", "*/*")) } }
}

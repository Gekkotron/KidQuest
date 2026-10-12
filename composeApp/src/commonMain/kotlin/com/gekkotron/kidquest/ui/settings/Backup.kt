package com.gekkotron.kidquest.ui.settings

import androidx.compose.runtime.Composable

/**
 * Hands the OS a JSON payload under [filename] via the platform's file-save flow.
 * Does nothing on platforms without a file picker.
 */
@Composable
expect fun rememberBackupExport(): (filename: String, payload: String) -> Unit

/**
 * Opens a file picker; invokes [onPicked] with the file's text content when one is chosen.
 */
@Composable
expect fun rememberBackupImport(onPicked: (text: String) -> Unit): () -> Unit

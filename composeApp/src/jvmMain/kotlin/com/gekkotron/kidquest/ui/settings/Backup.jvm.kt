package com.gekkotron.kidquest.ui.settings

import androidx.compose.runtime.Composable

@Composable
actual fun rememberBackupExport(): (filename: String, payload: String) -> Unit = { _, _ -> }

@Composable
actual fun rememberBackupImport(onPicked: (text: String) -> Unit): () -> Unit = { }

package com.gekkotron.kidquest.data

enum class ThemeMode {
    SYSTEM, LIGHT, DARK;

    companion object {
        fun fromStorage(raw: String?): ThemeMode =
            entries.firstOrNull { it.name == raw } ?: SYSTEM
    }
}

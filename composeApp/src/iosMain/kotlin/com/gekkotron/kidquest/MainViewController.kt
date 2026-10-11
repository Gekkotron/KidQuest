package com.gekkotron.kidquest

import androidx.compose.ui.window.ComposeUIViewController
import com.gekkotron.kidquest.data.KidQuestDatabase
import com.gekkotron.kidquest.data.buildDatabase
import com.gekkotron.kidquest.data.databaseBuilder
import com.gekkotron.kidquest.di.initKoin
import org.koin.dsl.module
import org.koin.mp.KoinPlatformTools
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.languageCode

fun MainViewController() = ComposeUIViewController(configure = { ensureKoin() }) { App() }

private fun ensureKoin() {
    if (KoinPlatformTools.defaultContext().getOrNull() != null) return
    initKoin(module {
        single<KidQuestDatabase> { buildDatabase(databaseBuilder(), NSLocale.currentLocale.languageCode) }
    })
}

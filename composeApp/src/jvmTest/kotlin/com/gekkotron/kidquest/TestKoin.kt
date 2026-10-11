package com.gekkotron.kidquest

import com.gekkotron.kidquest.data.KidQuestDatabase
import com.gekkotron.kidquest.data.buildDatabase
import com.gekkotron.kidquest.data.inMemoryDatabaseBuilder
import com.gekkotron.kidquest.di.initKoin
import org.koin.core.KoinApplication
import org.koin.dsl.module
import java.util.Locale

/** Tests assert English strings, so the JVM default locale is pinned before the app captures it. */
fun startTestKoin(): KoinApplication {
    Locale.setDefault(Locale.ENGLISH)
    return initKoin(module { single<KidQuestDatabase> { buildDatabase(inMemoryDatabaseBuilder(), "en") } })
}

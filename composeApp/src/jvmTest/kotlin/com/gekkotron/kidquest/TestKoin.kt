package com.gekkotron.kidquest

import com.gekkotron.kidquest.data.KidQuestDatabase
import com.gekkotron.kidquest.data.buildDatabase
import com.gekkotron.kidquest.data.inMemoryDatabaseBuilder
import com.gekkotron.kidquest.di.initKoin
import org.koin.core.KoinApplication
import org.koin.dsl.module

fun startTestKoin(): KoinApplication =
    initKoin(module { single<KidQuestDatabase> { buildDatabase(inMemoryDatabaseBuilder(), "en") } })

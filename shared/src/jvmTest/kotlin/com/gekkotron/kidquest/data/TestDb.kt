package com.gekkotron.kidquest.data

fun testDatabase(language: String = "en"): KidQuestDatabase =
    buildDatabase(inMemoryDatabaseBuilder(), seedLanguage = language)

package com.gekkotron.kidquest.di

import android.content.Context
import com.gekkotron.kidquest.data.KidQuestDatabase
import com.gekkotron.kidquest.data.buildDatabase
import com.gekkotron.kidquest.data.databaseBuilder
import org.koin.dsl.module
import java.util.Locale

fun androidPlatformModule(context: Context) = module {
    single<KidQuestDatabase> { buildDatabase(databaseBuilder(context), Locale.getDefault().language) }
}

package com.gekkotron.kidquest.di

import com.gekkotron.kidquest.data.AppEventBus
import com.gekkotron.kidquest.data.ChildRepository
import com.gekkotron.kidquest.data.GoalRepository
import com.gekkotron.kidquest.data.GoalStatusUpdater
import com.gekkotron.kidquest.data.KidQuestDatabase
import com.gekkotron.kidquest.data.PointRepository
import com.gekkotron.kidquest.data.SettingsRepository
import com.gekkotron.kidquest.ui.home.HomeViewModel
import kotlinx.datetime.TimeZone
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import kotlin.time.Clock

val dataModule = module {
    single<Clock> { Clock.System }
    single { AppEventBus() }
    single { GoalStatusUpdater(get<KidQuestDatabase>().goalDao(), get<KidQuestDatabase>().pointEventDao(), get()) }
    single { PointRepository(get(), get(), get(), get()) }
    single { GoalRepository(get(), get(), get(), get()) }
    single { ChildRepository(get(), get()) { TimeZone.currentSystemDefault() } }
    single { SettingsRepository(get()) }
}

/** Each screen task adds its ViewModel here. */
val viewModelModule = module {
    viewModelOf(::HomeViewModel)
}

/** [platformModule] must provide the single KidQuestDatabase. */
fun initKoin(platformModule: Module): KoinApplication =
    startKoin { modules(platformModule, dataModule, viewModelModule) }

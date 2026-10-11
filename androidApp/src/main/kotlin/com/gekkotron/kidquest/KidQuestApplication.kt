package com.gekkotron.kidquest

import android.app.Application
import com.gekkotron.kidquest.di.androidPlatformModule
import com.gekkotron.kidquest.di.initKoin

class KidQuestApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin(androidPlatformModule(this))
    }
}

package com.guzzlio

import android.app.Application
import androidx.work.Configuration

class GuzzlioApplication : Application(), Configuration.Provider {
    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        container.start()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(container.workerFactory)
            .build()
}

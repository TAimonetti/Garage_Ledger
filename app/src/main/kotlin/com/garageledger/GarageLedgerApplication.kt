package com.garageledger

import android.app.Application

class GarageLedgerApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}

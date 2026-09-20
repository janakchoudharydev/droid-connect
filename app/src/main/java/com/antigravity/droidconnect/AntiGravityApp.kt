package com.antigravity.droidconnect

import android.app.Application
import com.antigravity.droidconnect.data.AppPreferences

class AntiGravityApp : Application() {

    lateinit var preferences: AppPreferences
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        preferences = AppPreferences(this)
    }

    companion object {
        lateinit var instance: AntiGravityApp
            private set
    }
}

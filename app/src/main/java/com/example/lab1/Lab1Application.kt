package com.example.lab1

import android.app.Application
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsLogger

class Lab1Application : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            FacebookSdk.sdkInitialize(applicationContext)
            AppEventsLogger.activateApp(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
} 
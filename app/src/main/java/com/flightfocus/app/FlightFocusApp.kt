package com.flightfocus.app

import android.app.Application
import com.flightfocus.app.flight.FlightNotifications
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FlightFocusApp : Application() {

    override fun onCreate() {
        super.onCreate()
        FlightNotifications.createChannels(this)
    }
}

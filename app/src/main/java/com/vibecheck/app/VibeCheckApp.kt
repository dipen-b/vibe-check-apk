package com.vibecheck.app

import android.app.Application
import com.vibecheck.app.data.AppContainer
import com.vibecheck.app.data.fake.FakeAppContainer

class VibeCheckApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        // The Firebase-backed DefaultAppContainer is selected when
        // USE_FAKE_DATA is false (release, or -PuseFakeData=false).
        container = FakeAppContainer(this)
    }
}

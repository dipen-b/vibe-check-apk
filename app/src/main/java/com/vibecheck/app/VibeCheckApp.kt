package com.vibecheck.app

import android.app.Application
import com.vibecheck.app.data.AppContainer
import com.vibecheck.app.data.DefaultAppContainer
import com.vibecheck.app.data.fake.FakeAppContainer

class VibeCheckApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        // Debug builds default to the in-memory demo layer; release (or
        // -PuseFakeData=false) runs the Room + Firebase stack.
        container = if (BuildConfig.USE_FAKE_DATA) {
            FakeAppContainer(this)
        } else {
            DefaultAppContainer(this)
        }
    }
}

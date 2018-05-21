package app.skychat.client;

import android.app.Application
import com.bugsnag.android.Bugsnag
import com.jakewharton.threetenabp.AndroidThreeTen

class SkychatApp : Application() {
    override fun onCreate() {
        super.onCreate()

        Bugsnag.init(this)
        AndroidThreeTen.init(this)
    }
}

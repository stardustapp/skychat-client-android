package app.skychat.client

import android.app.Application
import com.bugsnag.android.Bugsnag
import com.jakewharton.threetenabp.AndroidThreeTen

@Suppress("unused")
class SkychatApp : Application() {
    override fun onCreate() {
        super.onCreate()

        Bugsnag.init(this)
        //Bugsnag.setNotifyReleaseStages("prod") // disable reporting
        AndroidThreeTen.init(this)
    }
}

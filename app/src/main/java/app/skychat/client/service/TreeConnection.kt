package app.skychat.client.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import app.skychat.client.ProfileRepository
import app.skychat.client.R
import app.skychat.client.data.Profile
import io.reactivex.Maybe
import io.reactivex.schedulers.Schedulers

class TreeConnection(
        private val packageContext: Context
) : ServiceConnection {

    // Catch the TreeService when the OS hands it over

    private var treeService: TreeService? = null
    override fun onServiceConnected(name: ComponentName, service: IBinder) {
        val binder = service as TreeService.TreeBinder
        treeService = binder.treeService
    }
    override fun onServiceDisconnected(name: ComponentName) {
        treeService = null
    }

    // Consumer API to manage TreeConnection lifecycle

    fun bind() {
        treeService?.apply { throw Exception("TreeConnection is already bound") }
        Intent(packageContext, TreeService::class.java).let {
            packageContext.bindService(it, this, Context.BIND_AUTO_CREATE)
        }
    }
    fun unbind() {
        packageContext.unbindService(this)
        treeService = null
    }

    // Treestore session management APIs
    // One TreeConnection can only connect to a single profile at a time
    // Requesting any profile will replace the existing state
    var currentProfile: Profile? = null

    // persistent key/val store for app state
    private val sharedPrefs = packageContext.getSharedPreferences(
            packageContext.getString(R.string.preference_state_file_key),
            Context.MODE_PRIVATE)

    // structured database for sessions and credentials
    private val profileRepo = ProfileRepository(packageContext.applicationContext)

    // Selects the most recently 'used' profile, if any
    fun resumeLastProfile(): Maybe<Profile>  {
        return Maybe
                .fromCallable({ sharedPrefs.getString(
                        packageContext.getString(R.string.preference_current_profile),
                        null) })
                .flatMap(profileRepo::getProfileById)
                .filter {
                    currentProfile = it
                    true
                }
                .subscribeOn(Schedulers.io())
                .cache()
    }

    // Selects the named profile, if it exists
    // The profile will become 'last used'
    fun resumeProfileById(desiredProfile: String): Maybe<Profile> {
        return profileRepo
                .getProfileById(desiredProfile)
                .filter {
                    with (sharedPrefs.edit()) {
                        putString(packageContext.getString(R.string.preference_current_profile), it.profileId)
                        apply()
                    }
                    currentProfile = it
                    true
                }
                .subscribeOn(Schedulers.io())
                .cache()
    }
}
package app.skychat.client

import android.app.Application
import android.arch.lifecycle.LiveData
import app.skychat.client.data.Profile
import app.skychat.client.data.ProfileDao
import app.skychat.client.data.getDatabase
import android.os.AsyncTask



class ProfileRepository(application: Application) {
    private val profileDao: ProfileDao
    private val allProfiles: LiveData<List<Profile>>

    init {
        val db = getDatabase(application)
        profileDao = db.profileDao()
        allProfiles = profileDao.getAll()
    }

    fun getAllProfiles(): LiveData<List<Profile>> {
        return allProfiles
    }

    fun insert(profile: Profile) {
        insertAsyncTask(profileDao).execute(profile)
    }

    private class insertAsyncTask internal constructor(private val asyncTaskDao: ProfileDao)
        : AsyncTask<Profile, Void, Void>() {
        override fun doInBackground(vararg params: Profile): Void? {
            asyncTaskDao.insert(params[0])
            return null
        }
    }
}
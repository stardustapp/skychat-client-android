package app.skychat.client

import android.arch.lifecycle.LiveData
import android.content.Context
import app.skychat.client.actions.UserLoginSuccess
import app.skychat.client.data.Profile
import app.skychat.client.data.ProfileDao
import app.skychat.client.data.getDatabase
import io.reactivex.Maybe
import org.threeten.bp.Instant


class ProfileRepository(context: Context) {
    private val profileDao: ProfileDao
    private val allProfiles: LiveData<List<Profile>>

    init {
        val db = getDatabase(context)
        profileDao = db.profileDao()
        allProfiles = profileDao.getAll()
    }

    fun getAllProfiles(): LiveData<List<Profile>> {
        return allProfiles
    }

    fun getProfileById(profileId: String): Maybe<Profile> {
        return profileDao.findSingleByProfileId(profileId)
    }

    /*
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
    */

    fun storeSessionBlocking(newSession: UserLoginSuccess, username: String, domainName: String) {
        profileDao.findByProfileId(newSession.profileId)
                ?.also {
                    // already have profile, refresh with latest info
                    it.sessionId = newSession.sessionId
                    it.realName = newSession.ownerName
                    it.userName = username
                    it.domainName = domainName
                    profileDao.update(it)
                }
                ?: apply {
                    // create a new saved profile
                    Profile().also {
                        it.profileId = newSession.profileId
                        it.sessionId = newSession.sessionId
                        it.realName = newSession.ownerName
                        it.userName = username
                        it.domainName = domainName
                        it.addedAt = Instant.now()
                        profileDao.insert(it)
                    }
                }
    }
}
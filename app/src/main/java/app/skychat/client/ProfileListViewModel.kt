package app.skychat.client

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import app.skychat.client.data.Profile
import io.reactivex.Maybe
import io.reactivex.schedulers.Schedulers

class ProfileListViewModel constructor(app: Application) : AndroidViewModel(app) {
    public val repository: ProfileRepository = ProfileRepository(app)
    private val allProfiles = repository.getAllProfiles()

    fun getAllProfiles(): LiveData<List<Profile>> {
        return allProfiles
    }

    fun getOneProfile(profileId: String): Maybe<Profile> {
        return repository.getProfileById(profileId)
                .subscribeOn(Schedulers.io())

    }

    /*
    fun insertProfile(profile: Profile) {
        repository.insert(profile)
    }
    */
}
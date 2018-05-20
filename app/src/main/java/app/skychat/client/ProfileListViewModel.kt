package app.skychat.client

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import app.skychat.client.data.Profile

class ProfileListViewModel constructor(app: Application) : AndroidViewModel(app) {
    private val repository: ProfileRepository = ProfileRepository(app)
    private val allProfiles = repository.getAllProfiles()

    fun getAllProfiles(): LiveData<List<Profile>> {
        return allProfiles
    }

    fun insertProfile(profile: Profile) {
        repository.insert(profile)
    }
}
package app.skychat.client.data

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.*
import io.reactivex.Maybe


@Dao
interface ProfileDao {
    @Query("SELECT * FROM Profile")
    fun getAll(): LiveData<List<Profile>>

    @Query("SELECT * FROM Profile WHERE profile_id IN (:profileIds)")
    fun loadAllByIds(profileIds: Array<String>): List<Profile>

    @Query("SELECT * FROM Profile WHERE profile_id = :profileId LIMIT 1")
    fun findByProfileId(profileId: String): Profile?

    @Query("SELECT * FROM Profile WHERE profile_id = :profileId LIMIT 1")
    fun findSingleByProfileId(profileId: String): Maybe<Profile>

    @Insert
    fun insert(profile: Profile)

    //@Insert
    //fun insertAll(vararg profiles: Profile)

    @Update
    fun update(profile: Profile)

    @Delete
    fun delete(profile: Profile)
}
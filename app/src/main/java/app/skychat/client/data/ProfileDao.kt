package app.skychat.client.data

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.Dao
import android.arch.persistence.room.Delete
import android.arch.persistence.room.Insert
import android.arch.persistence.room.Query


@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles")
    fun getAll(): LiveData<List<Profile>>

    @Query("SELECT * FROM profiles WHERE profileId IN (:profileIds)")
    fun loadAllByIds(profileIds: Array<String>): List<Profile>

    //@Query("SELECT * FROM profiles WHERE first_name LIKE :first AND " + "last_name LIKE :last LIMIT 1")
    //fun findByName(first: String, last: String): User

    @Insert
    fun insert(profile: Profile)
    //@Insert
    //fun insertAll(vararg profiles: Profile)

    @Delete
    fun delete(profile: Profile)
}
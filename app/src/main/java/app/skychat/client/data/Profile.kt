package app.skychat.client.data

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import android.support.annotation.NonNull

@Entity
class Profile {
    @PrimaryKey
    var profileId: String? = null

    @ColumnInfo(name = "user_name")
    var userName: String? = null

    @ColumnInfo(name = "domain_name")
    var domainName: String? = null

    @ColumnInfo(name = "session_id")
    var sessionId: String? = null

}
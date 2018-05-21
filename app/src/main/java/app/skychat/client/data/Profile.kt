package app.skychat.client.data

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import org.threeten.bp.Instant

@Entity
class Profile {
    @PrimaryKey
    @ColumnInfo(name = "profile_id")
    var profileId: String = ""

    @ColumnInfo(name = "user_name")
    var userName: String? = null

    @ColumnInfo(name = "domain_name")
    var domainName: String? = null

    @ColumnInfo(name = "session_id")
    var sessionId: String? = null

    @ColumnInfo(name = "real_name")
    var realName: String? = null

    @ColumnInfo(name = "added_at")
    var addedAt: Instant? = null

    @ColumnInfo(name = "last_used_at")
    var lastUsedAt: Instant? = null
}
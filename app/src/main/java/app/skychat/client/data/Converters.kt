package app.skychat.client.data

import android.arch.persistence.room.TypeConverter
import org.threeten.bp.Instant

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Instant? {
        return value?.let(Instant::ofEpochMilli)
    }

    @TypeConverter
    fun instantToTimestamp(instant: Instant?): Long? {
        return instant?.toEpochMilli()
    }
}
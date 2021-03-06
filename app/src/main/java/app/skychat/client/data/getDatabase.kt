package app.skychat.client.data

import android.arch.persistence.room.Room
import android.content.Context

private var INSTANCE: AppDatabase? = null
fun getDatabase(context: Context): AppDatabase {
    if (INSTANCE == null) {
        synchronized(AppDatabase::class.java) {
            INSTANCE = INSTANCE ?: Room
                    .databaseBuilder(context.applicationContext,
                            AppDatabase::class.java, "sessions")
                    .build()
        }
    }
    return INSTANCE!!
}
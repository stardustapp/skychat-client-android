package app.skychat.client.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder

class TreeService : Service() {
    override fun onBind(intent: Intent?): IBinder {
        return TreeBinder()
    }

    inner class TreeBinder : Binder() {
        val treeService = this@TreeService
    }
}
package app.skychat.client.chat

import app.skychat.client.data.Profile
import app.skychat.client.skylink.remoteTreeFor

abstract class ChatCommunity(
        val profile: Profile,
        val app: String,
        val type: String,
        val id: String
) {
    val path = "/sessions/${profile.sessionId}/mnt/persist/$app/$type/$id"
    val remoteTree = remoteTreeFor(profile.domainName!!)

    abstract fun getRooms(): List<ChatRoom>
}

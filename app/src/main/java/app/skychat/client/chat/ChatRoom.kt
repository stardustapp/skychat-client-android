package app.skychat.client.chat

import app.skychat.client.skylink.NetEntry
import io.reactivex.Maybe

abstract class ChatRoom(
        val community: ChatCommunity,
        val id: String,
        val path: String
) {
    abstract fun slashCommand(command: String, params: List<String>): Maybe<NetEntry>
    abstract fun sendMessage(message: String): Maybe<NetEntry>

    open val logPath: String
        get() = "$path/log"
}
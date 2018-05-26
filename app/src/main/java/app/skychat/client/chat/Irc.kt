package app.skychat.client.chat

import app.skychat.client.data.Profile
import app.skychat.client.skylink.FolderLiteral
import app.skychat.client.skylink.NetEntry
import app.skychat.client.skylink.StringLiteral
import app.skychat.client.skylink.remoteTreeFor
import io.reactivex.Maybe

object Irc {

    fun getAllNetworks(profile: Profile): List<Network> {
        val networksPath = "/sessions/${profile.sessionId}/mnt/persist/irc/networks"
        val remoteTree = remoteTreeFor(profile.domainName!!)
        return remoteTree.enumerate(networksPath).drop(1).map {
            Network(profile, it.name)
        }
    }

    // Represents an IRC network, hopefully with a live wire
    class Network(
            profile: Profile,
            id: String
    ) : ChatCommunity(profile, "irc", "networks", id) {

        override fun getRooms(): List<ChatRoom> {
            val channels = remoteTree.enumerate("$path/channels").drop(1)
            val queries = remoteTree.enumerate("$path/queries").drop(1)
            val serverLog = remoteTree.enumerate("$path/server-log", 0).firstOrNull()

            val channelRooms = channels.map { Irc.ChannelRoom(this, it.name) }
            val queryRooms = queries.map { Irc.QueryRoom(this, it.name) }
            val serverRoom = serverLog?.let { listOf(Irc.ServerRoom(this)) } ?: emptyList()

            return listOf(channelRooms, queryRooms, serverRoom).flatten()
        }

        // Writes a packet directly into the live IRC wire
        fun sendIrcPacket(command: String, vararg params: String): Maybe<NetEntry> {
            val profilePath = "/sessions/${profile.sessionId}/mnt"
            val runtimePath = "$profilePath/runtime/apps/$app/namespace"
            val wirePath = "$runtimePath/state/$type/$id/wire"
            return remoteTree
                    .invokeRx("$wirePath/send/invoke", FolderLiteral("",
                            StringLiteral("command", command),
                            FolderLiteral("params", *params.mapIndexed({ index, s ->
                                StringLiteral((index + 1).toString(), s)
                            }).toTypedArray())))
                    .filter { evt -> evt.type == "String" }
        }
    }

    // The various room classes that an IRC connection can express

    // parent of every IRC room class, handles network-general slash commands
    abstract class IrcRoom(
            val network: Network,
            id: String,
            path: String
    ) : ChatRoom(network, id, path) {
        override fun slashCommand(command: String, params: List<String>): Maybe<NetEntry> {
            return when (command) {

                "msg" -> network.sendIrcPacket(
                        "PRIVMSG", params[0],
                        params.drop(1).joinToString(" "))
                "notice" -> network.sendIrcPacket(
                        "NOTICE", params[0],
                        params.drop(1).joinToString(" "))
                "ctcp" -> network.sendIrcPacket(
                        "CTCP", params[0],
                        params[1], params.drop(2).joinToString(" "))

                else -> Maybe.error(
                        IllegalArgumentException("Invalid slash command /$command"))
            }
        }
    }

    // specifically things that can be messaged - channels and queries
    open class TargetRoom(
            network: Network,
            id: String,
            path: String
    ) : IrcRoom(network, id, path) {
        override fun slashCommand(command: String, params: List<String>): Maybe<NetEntry> {
            return when (command) {

            // commands for current room
                "me" -> network.sendIrcPacket(
                        "CTCP", this.id,
                        "ACTION", params.joinToString(" "))
                "slap" -> network.sendIrcPacket(
                        "CTCP", this.id,
                        "ACTION", "slaps ${params.joinToString(" ")} around a bit with a large trout")

            // pass off network-general commands
                else -> super.slashCommand(command, params)
            }
        }

        override fun sendMessage(message: String): Maybe<NetEntry> {
            // TODO: splitting by newline, and by length
            return network.sendIrcPacket("PRIVMSG", this.id, message)
        }
    }

    class ChannelRoom(
            network: Network,
            id: String
    ) : TargetRoom(network, id, "${network.path}/channels/$id") {
        val prefix = id.let { when {
            id.startsWith("##") && !id.startsWith("###") -> "##"
            id.startsWith("#") -> "#"
            id.startsWith("&") -> "&"
            id.startsWith("+") -> "+"
            else -> ""
        }}
        val postPrefix = id.drop(prefix.length)
    }

    class QueryRoom(
            network: Network,
            id: String
    ) : TargetRoom(network, id, "${network.path}/queries/$id")

    class ServerRoom(
            network: Network
    ) : IrcRoom(network, "Server activity", "${network.path}/server-log") {
        override fun sendMessage(message: String): Maybe<NetEntry> {
            return Maybe.error(
                    IllegalArgumentException("Cannot send plain messages to the server"))
        }

        override val logPath: String
            get() = path // server 'rooms' are really a bare log
    }
}
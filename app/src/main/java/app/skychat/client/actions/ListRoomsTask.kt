package app.skychat.client.actions

import app.skychat.client.data.Profile
import app.skychat.client.skylink.remoteTreeFor
import app.skychat.client.utils.ReactiveAsyncTask

class ListRoomsTask (
        private val profile: Profile,
        private val communityType: String,
        private val communityId: String)
    : ReactiveAsyncTask<Void, ListRoomsTask.Result>() {

    data class RoomEntry (
            val name: String,
            val path: String)
    data class Result (
            val groupRooms: List<RoomEntry>,
            val directRooms: List<RoomEntry>,
            val backgroundRoom: RoomEntry?)

    override fun doInBackground(vararg params: Void): Result {
        val remoteTree = remoteTreeFor(profile.domainName!!)
        val communityPath = "/sessions/${profile.sessionId}/mnt/persist/$communityType/$communityId"
        val channels = remoteTree.enumerate("$communityPath/channels").drop(1)
        val queries = remoteTree.enumerate("$communityPath/queries").drop(1)
        val serverLog = remoteTree.enumerate("$communityPath/server-log", 0).firstOrNull()

        return Result(
                channels.map { chan -> RoomEntry(chan.name, "$communityPath/channels/${chan.name}") },
                queries.map { query -> RoomEntry(query.name, "$communityPath/queries/${ query.name}") },
                serverLog?.let { RoomEntry("Server activity", "$communityPath/server-log") } )
    }
}
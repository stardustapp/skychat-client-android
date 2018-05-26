package app.skychat.client.actions

import app.skychat.client.chat.ChatCommunity
import app.skychat.client.chat.ChatRoom
import app.skychat.client.utils.ReactiveAsyncTask

class ListRoomsTask: ReactiveAsyncTask<ChatCommunity, List<ChatRoom>>() {
    override fun doInBackground(vararg params: ChatCommunity): List<ChatRoom> {
        return params[0].getRooms()
    }
}
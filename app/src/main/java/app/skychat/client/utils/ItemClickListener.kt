package app.skychat.client.utils

interface ItemEventListener<T> {
    fun onItemClick(item: T)
}
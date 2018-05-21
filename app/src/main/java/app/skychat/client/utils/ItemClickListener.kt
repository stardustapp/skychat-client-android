package app.skychat.client.utils

public interface ItemEventListener<T> {
    fun onItemClick(item: T)
}
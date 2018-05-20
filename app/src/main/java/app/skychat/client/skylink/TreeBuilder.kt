package app.skychat.client.skylink

import java.util.concurrent.ConcurrentHashMap

val trees = ConcurrentHashMap<String, RemoteTree>()

public fun remoteTreeFor(host: String): RemoteTree {
    var baseUrl = "https://$host/"
    if (host.matches(Regex("^(localhost|[^.]+.(?:lan|local)|(?:\\d{1,3}\\.)+\\d{1,3})(?::(\\d+))?\$"))) {
        baseUrl = "http://$host/"
    }

    return trees.computeIfAbsent(baseUrl) {
        RemoteTree(baseUrl)
    }
}
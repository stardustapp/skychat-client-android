package app.skychat.client

import com.google.common.base.Joiner
import org.threeten.bp.Instant

class ActivityEntry(
        val idx: Int,
        val path: String?,
        val props: Map<String, String>
) {
    val command = props.getOrDefault("command", "")
    val prefixName = props.getOrDefault("prefix-name", "")
    val prefixUser = props.getOrDefault("prefix-user", "")
    val prefixHost = props.getOrDefault("prefix-host", "")
    val source = props.getOrDefault("source", "")
    val timestamp = props["timestamp"]?.let { Instant.parse(it) }
    val params = props
            .filter { pair -> pair.key.startsWith("params/") }
            .toSortedMap(Comparator { o1, o2 ->
                val k1 = o1.split('/')[1].toInt()
                val k2 = o2.split('/')[1].toInt()
                k1 - k2
            })
            .map { pair -> pair.value }

    fun displayText(): String {
        //val params = props.getOrDefault("command", emptyArray<String>())

        return when (command) {
            "PRIVMSG" ->
                "<$prefixName> ${params[1]}"
            else ->
                "$command ${Joiner.on(' ').join(params)}"
        }
    }
}
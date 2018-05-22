package app.skychat.client

import com.google.common.base.Joiner
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter

class ActivityEntry(
        val idx: Int,
        val path: String?,
        val props: Map<String, String>
) {
    val command = props.getOrDefault("command", "")
    val prefixName = props.getOrDefault("prefix-name", "")
    val prefixUser = props.getOrDefault("prefix-user", "")
    val prefixHost = props.getOrDefault("prefix-host", "")
    val extraPath = "$prefixUser@$prefixHost"
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
            "JOIN" ->
                "$prefixName joined ($extraPath)"
            "INVITE" ->
                // TODO: if (params[0] === current-nick)
                "$prefixName invited ${params[0]} to join ${params[1]}"
            "PART" ->
                "$prefixName left ($extraPath) ${params[1] ?: ""}"
            "KICK" ->
                "$prefixName kicked ${params[1]} from ${params[0]} (${params[1] ?: ""})"
            "QUIT" ->
                "$prefixName quit ($extraPath) ${params[0] ?: ""}"
            "NICK" ->
                "$prefixName => ${params[0]}"
            "TOPIC" ->
                "$prefixName set the topic: ${params[1]}"
            "MODE" -> {
                val args = Joiner.on(' ').join(params.drop(1))
                "$prefixName set modes: $args"
            }

        // Information numerics
            "001", "002", "003" ->
                params[1]
            "004" ->
                "Your server is ${params[1]}, running ${params[2]}"
            "042" ->
                "${params[2]} is ${params[1]}"
            "251", "255", "250" ->
                params[1]
            "265", "266" -> // current local/global users
                params[params.size - 1]
            "252", "254", "396" ->
                "${params[1]} ${params[2]}"
            "332" -> // topic - TODO: should be rich/formatting
                "Topic of ${params[1]} is ${params[2]}"
            "333" -> {// topic author, timestamp
                val topicTimestamp = Instant
                        .ofEpochSecond(params[3].toLong())
                        .atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                "Set $topicTimestamp by ${params[2]}"
            }
            //"353': // names list
            "366" -> // end of names
                "Completed parsing /names response"

            // Error numerics
            "421" -> // unknown command
                "${params[2]} ${params[1]}"
            "462" -> // you may not reregister
                params[1]

            // Messages
            "PRIVMSG" ->
                "<$prefixName> ${params[1]}"
            "CTCP" ->
                when (params[1]) {
                    "ACTION" ->
                        "* $prefixName ${params[2]}"
                    else -> {
                        val args = Joiner.on(' ').join(params.drop(1))
                        "- $prefixName requested CTCP $args"
                    }
                }
            "CTCP_ANSWER" ->
                when (params[1]) {
                    "ACTION" ->
                        "* $prefixName ${params[2]}"
                    else -> {
                        val args = Joiner.on(' ').join(params.drop(2))
                        "- Received CTCP ${params[1]} reply from $prefixName: $args"
                    }
                }
            else ->
                "- $prefixName $command ${Joiner.on(' ').join(params)}"
        }
    }
}
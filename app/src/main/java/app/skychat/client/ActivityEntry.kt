package app.skychat.client

import com.google.common.base.Joiner
import com.google.common.collect.ImmutableList
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter

class ActivityEntry(
        val idx: Int,
        val path: String,
        props: Map<String, String>
) {
    private val command = props.getOrDefault("command", "")
    private val text = props.getOrDefault("text", "") // used for synthetic events
    val prefixName = props.getOrDefault("prefix-name", "")
    private val prefixUser = props.getOrDefault("prefix-user", "")
    private val prefixHost = props.getOrDefault("prefix-host", "")
    private val extraPath = "$prefixUser@$prefixHost"
    //private val source = props.getOrDefault("source", "")
    val timestamp = props["timestamp"]?.let { Instant.parse(it) }
    private val params = props
            .filter { pair -> pair.key.startsWith("params/") }
            .toSortedMap(Comparator { o1, o2 ->
                val k1 = o1.split('/')[1].toInt()
                val k2 = o2.split('/')[1].toInt()
                k1 - k2
            })
            .map { pair -> pair.value!! }
            .let {
                // CTCP: convert "ACTION slaps ChanServ" to "ACTION","slaps ChanServ"
                if (command == "CTCP" && it.size == 2 && it[1].contains(' ')) {
                    ImmutableList.of(it[0],
                            it[1].substringBefore(' '),
                            it[1].substringAfter(' '))
                } else it
            }!!

    val isAction = command == "CTCP" && params[1] == "ACTION"
    val isMessage = when (command) {
        "PRIVMSG", "NOTICE" -> true
        else -> false
    }
    var isContinuedMessage = false // updated by renderer
    val isBackground = when (command) {
        "JOIN", "PART", "QUIT", "NICK" -> true
        "MODE" -> !(params.getOrNull(1)?.contains(importantModes) ?: true)
        else -> false
    }

    private fun getParam(idx: Int): String? {
        return params.getOrNull(idx)
    }

    fun displayText(): String {
        //val params = props.getOrDefault("command", emptyArray<String>())

        return when (command) {
            "JOIN" ->
                "$prefixName joined ($extraPath)"
            "INVITE" ->
                // TODO: if (params[0] === current-nick)
                "$prefixName invited ${getParam(0)} to join ${getParam(1)}"
            "PART" ->
                "$prefixName left ($extraPath) ${getParam(1) ?: ""}"
            "KICK" ->
                "$prefixName kicked ${getParam(1)} from ${getParam(0)} (${getParam(1) ?: ""})"
            "QUIT" ->
                "$prefixName quit ($extraPath) ${getParam(0) ?: ""}"
            "NICK" ->
                "$prefixName => ${getParam(0)}"
            "TOPIC" ->
                "$prefixName set the topic: ${getParam(1)}"
            "MODE" -> {
                val args = Joiner.on(' ').join(params.drop(1))
                "$prefixName set modes: $args"
            }

        // Information numerics
            "001", "002", "003" ->
                "${getParam(1)}"
            "004" ->
                "Your server is ${getParam(1)}, running ${getParam(2)}"
            "042" ->
                "${getParam(2)} is ${getParam(1)}"
            "251", "255", "250" ->
                "${getParam(1)}"
            "265", "266" -> // current local/global users
                params.last()
            "252", "254", "396" ->
                "${getParam(1)} ${getParam(2)}"
            "332" -> // topic - TODO: should be rich/formatting
                "Topic of ${getParam(1)} is ${getParam(2)}"
            "333" -> {// topic author, timestamp
                val topicTimestamp = Instant
                        .ofEpochSecond(getParam(3)?.toLong() ?: 0)
                        .atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                "Set $topicTimestamp by ${getParam(2)}"
            }
        //"353': // names list
            "366" -> // end of names
                "Completed parsing /names response"

        // Error numerics
            "421" -> // unknown command
                "${getParam(2)} ${getParam(1)}"
            "462" -> // you may not reregister
                "${getParam(1)}"

        // Blocks - synthetic multiline messages, usually bursts from a server
            "BLOCK" ->
                "${Joiner.on(' ').join(params)}\n$text"

        // Messages
            "PRIVMSG", "NOTICE" ->
                "${getParam(1)}"
            "CTCP" -> {
                when (getParam(1)) {
                    "ACTION" -> "${getParam(2)}" // handled by the layout
                    else -> {
                        val args = Joiner.on(' ').join(params.drop(1))
                        "- $prefixName requested CTCP $args"
                    }
                }
            }
            "CTCP_ANSWER" ->
                when (getParam(1)) {
                    "ACTION" ->
                        "* $prefixName ${getParam(2)}"
                    else -> {
                        val args = Joiner.on(' ').join(params.drop(2))
                        "- Received CTCP ${getParam(1)} reply from $prefixName: $args"
                    }
                }
            else ->
                "- $prefixName $command ${Joiner.on(' ').join(params)}"
        }
    }

    companion object {
        private val importantModes = Regex("[A-Zb-gi-npr-uw-z]") // ahoqv are okay (vhoaq)
    }
}
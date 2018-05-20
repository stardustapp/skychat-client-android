package app.skychat.client.skylink

import com.google.common.collect.ImmutableList

data class NetRequest(
        val op: String,
        val path: String?,
        val dest: String?,
        val input: NetEntry?,
        val depth: Int?
)

data class NetResponse(
        val ok: Boolean,
        val status: String?,
        val chan: Int?,
        val output: NetEntry?
)

data class NetEntry(
    val name: String,
    val type: String?,
    val stringValue: String?,
    val fileData: String?,
    val children: List<NetEntry>?
)

fun StringLiteral(name: String, value: String): NetEntry {
    return NetEntry(name, "String", value, null, null)
}
fun FolderLiteral(name: String, vararg children: NetEntry): NetEntry {
    return NetEntry(name, "Folder", null, null, ImmutableList.copyOf(children))
}
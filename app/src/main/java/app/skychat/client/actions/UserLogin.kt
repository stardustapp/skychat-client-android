package app.skychat.client.actions

import app.skychat.client.skylink.NetEntry

data class UserLoginAttempt constructor(
        val attemptedAddress: String,
        val wallTime: Long,
        val result: UserLoginResult)

interface UserLoginResult

data class UserLoginSuccess constructor(
        val profileId: String,
        val sessionId: String,
        val ownerName: String
) : UserLoginResult

data class UserLoginFailure constructor(
        val errorMessage: String,
        val failureStage: FailureStage
) : UserLoginResult

enum class FailureStage {
    NETWORK,
    REJECTED,
    SERVER_BUG,
    CLIENT_BUG,
}

fun inflateUserLoginResult(entry: NetEntry): UserLoginResult {
    return when {
        entry.type == "Folder" && entry.name == "output" -> {
            var profileId = ""
            var sessionId = ""
            var ownerName = ""
            for (child in entry.children.orEmpty()) {
                when (child.name) {
                    "profile id", "profile-id" -> profileId = child.stringValue.orEmpty()
                    "session id", "session-id" -> sessionId = child.stringValue.orEmpty()
                    "owner name", "owner-name" -> ownerName = child.stringValue.orEmpty()
                }
            }
            when {
                sessionId.isEmpty() -> UserLoginFailure(
                        "No Session ID given in response", FailureStage.SERVER_BUG)
                profileId.isEmpty() -> UserLoginFailure(
                        "No Profile ID given in response", FailureStage.SERVER_BUG)
                else -> UserLoginSuccess(profileId, sessionId, ownerName)
            }
        }
        entry.type == "String" && entry.name == "error" ->
            UserLoginFailure(entry.stringValue
                    ?: "Received empty error from remote server", FailureStage.REJECTED)
        else ->
            UserLoginFailure("Remote server failed to respond", FailureStage.SERVER_BUG)
    }
}
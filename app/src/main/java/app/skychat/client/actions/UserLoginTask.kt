package app.skychat.client.actions

import app.skychat.client.BuildConfig
import app.skychat.client.ProfileRepository
import app.skychat.client.skylink.FolderLiteral
import app.skychat.client.skylink.StringLiteral
import app.skychat.client.skylink.remoteTreeFor
import app.skychat.client.utils.ReactiveAsyncTask
import app.skychat.client.utils.Stopwatch
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Severity
import java.net.UnknownHostException


/**
 * Represents an asynchronous login/registration task used to authenticate
 * the user.
 */
class UserLoginTask (
        private val givenAddress: String,
        private val givenPassword: String,
        private val profileRepo: ProfileRepository)
    : ReactiveAsyncTask<Void, UserLoginAttempt>() {
    private val addressParts = givenAddress.split('@')

    override fun doInBackground(vararg params: Void): UserLoginAttempt {
        lateinit var result: UserLoginResult
        val elapsed = Stopwatch.elapse {
            result = try {
                processLogin()
            } catch (ex: RuntimeException) {
                // TODO: catch before it becomes a RuntimeException
                val cause = ex.cause
                when (cause) {
                    is UnknownHostException  ->
                        UserLoginFailure(cause.message ?: "Unknown host", FailureStage.NETWORK)
                    else -> {
                        Bugsnag.notify(ex)
                        UserLoginFailure("UserLoginTask crashed: $ex", FailureStage.CLIENT_BUG)
                    }
                }
            } catch (t: Throwable) {
                Bugsnag.notify(t, Severity.ERROR)
                UserLoginFailure("UserLoginTask crashed hard: ${t.message ?: "No message"}", FailureStage.CLIENT_BUG)
            }
        }
        return UserLoginAttempt(givenAddress, elapsed, result)
    }

    private fun processLogin(): UserLoginResult {
        if (addressParts.size != 2) {
            return UserLoginFailure("Stardust Address is malformed. It should look like an email address.", FailureStage.NETWORK)
        }

        // TODO: redirected login: log in to 0,1 and shift out 1 until there's only 2 parts left
        val username = addressParts[0]
        val domainName = addressParts[1]

        val remoteTree = remoteTreeFor(domainName)
        val output = remoteTree.invoke("/login/invoke", FolderLiteral("input",
                StringLiteral("username", username),
                StringLiteral("password", givenPassword),
                StringLiteral("client", "android/app.skychat.client/v"+ BuildConfig.VERSION_NAME)))
                ?: return UserLoginFailure("Remote server returned a null output", FailureStage.NETWORK)

        return inflateUserLoginResult(output)
                .also { when (it) {
                    is UserLoginSuccess ->
                        profileRepo.storeSessionBlocking(it, username, domainName)
                } }
    }
}
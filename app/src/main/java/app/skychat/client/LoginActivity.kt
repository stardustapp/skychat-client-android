package app.skychat.client

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.view.AsyncLayoutInflater
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import app.skychat.client.actions.*
import com.bugsnag.android.Bugsnag
import io.reactivex.Maybe
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.content_login.*






/**
 * A login screen that offers login via email/password.
 */
class LoginActivity : AppCompatActivity() {
    private lateinit var viewModel: ProfileListViewModel

    private var currentAttempt: Maybe<UserLoginAttempt>? = null

    companion object {
        const val EXTRA_REPLY = "app.skylink.client.LoginActivity.REPLY"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        setupActionBar()

        viewModel = ViewModelProviders.of(this)
                .get(ProfileListViewModel::class.java)

        // load up the actual form async
        // can take a couple hundred millis to init the autocomplete views
        val inflater = AsyncLayoutInflater(this)
        inflater.inflate(R.layout.content_login, frame) { view, _, parent ->
            parent.addView(view)
            login_progress.visibility = View.GONE

            // Set up the login form.
            password.setOnEditorActionListener(TextView.OnEditorActionListener { _, id, _ ->
                if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                    attemptLogin()
                    return@OnEditorActionListener true
                }
                false
            })

            email_sign_in_button.setOnClickListener { attemptLogin() }

            // focus the box and show input method
            email.requestFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(email, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    @SuppressLint("ObsoleteSdkInt")
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private fun setupActionBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // Show the Up button in the action bar.
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private fun attemptLogin() {
        currentAttempt?.run { return }

        // Reset errors.
        email.error = null
        password.error = null

        // Store values at the time of the login attempt.
        val emailStr = email.text.toString()
        val passwordStr = password.text.toString()

        var cancel = false
        var focusView: View? = null

        // Check for a valid password.
        if (TextUtils.isEmpty(passwordStr)) {
            password.error = getString(R.string.error_field_required)
            focusView = password
            cancel = true
        } else if (!isPasswordValid(passwordStr)) {
            password.error = getString(R.string.error_invalid_password)
            focusView = password
            cancel = true
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(emailStr)) {
            email.error = getString(R.string.error_field_required)
            focusView = email
            cancel = true
        } else if (!isEmailValid(emailStr)) {
            email.error = getString(R.string.error_invalid_email)
            focusView = email
            cancel = true
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView?.requestFocus()
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true)
            val loginMaybe = UserLoginTask(emailStr, passwordStr, viewModel.repository).asMaybe()
            currentAttempt = loginMaybe

            loginMaybe
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ attempt: UserLoginAttempt ->
                        currentAttempt = null

                        val result = attempt.result
                        when (result) {
                            is UserLoginSuccess -> {
                                // startActivity(Intent(this@LoginActivity, ChatActivity::class.java))
                                setResult(Activity.RESULT_OK, Intent().apply {
                                    putExtra(EXTRA_REPLY, result.profileId)
                                })
                                finish() // animate out with progress animation still visible
                            }
                            is UserLoginFailure -> {
                                showProgress(false)
                                when (result.failureStage) {
                                    FailureStage.NETWORK -> {
                                        email.error = result.errorMessage
                                        email.requestFocus()
                                    }
                                    FailureStage.REJECTED -> {
                                        password.error = result.errorMessage
                                        password.requestFocus()
                                    }
                                    FailureStage.CLIENT_BUG ->
                                        Snackbar.make(email,
                                                "${getString(R.string.error_client_bug)}. ${result.errorMessage}",
                                                Snackbar.LENGTH_INDEFINITE).show()
                                    FailureStage.SERVER_BUG ->
                                        Snackbar.make(email,
                                                "${getString(R.string.error_server_bug)}. ${result.errorMessage}",
                                                Snackbar.LENGTH_INDEFINITE
                                        ).setAction(getString(R.string.report_bug), { _ ->
                                            Bugsnag.notify("Skylink confusion", result.errorMessage, emptyArray(), { report ->
                                                report.error?.metaData?.apply {
                                                    addToTab("Request", "address", attempt.attemptedAddress)
                                                    addToTab("Request", "wall-time", attempt.wallTime)
                                                }
                                            })
                                            Snackbar.make(email, getString(R.string.confirm_report_sent), Snackbar.LENGTH_SHORT).show()
                                        }).show()
                                }
                            }
                        }
                    }, {
                        Bugsnag.notify(it)
                        Snackbar.make(email,
                                "Uncaught Rx exception: ${it.message}",
                                Snackbar.LENGTH_LONG).show()

                        currentAttempt = null
                        showProgress(false)
                    })
        }
    }

    private fun isEmailValid(email: String): Boolean {
        return email.split('@').size == 2
    }

    private fun isPasswordValid(password: String): Boolean {
        return password.length > 4
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @SuppressLint("ObsoleteSdkInt")
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private fun showProgress(show: Boolean) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            val shortAnimTime = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()

            login_form.visibility = if (show) View.GONE else View.VISIBLE
            login_form.animate()
                    .setDuration(shortAnimTime)
                    .alpha((if (show) 0 else 1).toFloat())
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            login_form.visibility = if (show) View.GONE else View.VISIBLE
                        }
                    })

            login_progress.visibility = if (show) View.VISIBLE else View.GONE
            login_progress.animate()
                    .setDuration(shortAnimTime)
                    .alpha((if (show) 1 else 0).toFloat())
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            login_progress.visibility = if (show) View.VISIBLE else View.GONE
                        }
                    })
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            login_progress.visibility = if (show) View.VISIBLE else View.GONE
            login_form.visibility = if (show) View.GONE else View.VISIBLE
        }
    }
}

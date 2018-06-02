package app.skychat.client

import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.content.Intent.ACTION_PICK
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import app.skychat.client.data.Profile
import app.skychat.client.utils.ItemEventListener
import com.bugsnag.android.Bugsnag
import kotlinx.android.synthetic.main.activity_profiles.*
import kotlinx.android.synthetic.main.content_profiles.*

class ProfilesActivity : AppCompatActivity(), ItemEventListener<Profile> {
    private lateinit var viewModel: ProfileListViewModel

    // for sending intents
    private val addProfileRequestCode = 1

    companion object {
        const val EXTRA_SELECT_REPLY = "app.skylink.client.ProfilesActivity.SELECT_REPLY"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profiles)
        //setSupportActionBar(toolbar)

        if (intent.action == ACTION_PICK) {
            title = "Select Stardust Account"
        }

        val adapter = ProfileListAdapter(this, this)
        recyclerview.adapter = adapter
        recyclerview.layoutManager = LinearLayoutManager(this)

        // Either you see the FTUE _or_ you have entries in the list
        // Both the FTUE and the list have an add button, but only one is shown at a time

        ftue.visibility = View.GONE

        viewModel = ViewModelProviders
                .of(this)
                .get(ProfileListViewModel::class.java)
                .also { it
                        .getAllProfiles()
                        .observe(this, Observer<List<Profile>> {
                            if (it?.isEmpty() == true) {
                                ftue.visibility = View.VISIBLE
                                fab.visibility = View.GONE
                            } else {
                                ftue.visibility = View.GONE
                                fab.visibility = View.VISIBLE
                            }
                            adapter.setProfiles(it)
                        })
                }

        val addAcctListener = { _: View ->
            startActivityForResult(
                    Intent(this, LoginActivity::class.java),
                    addProfileRequestCode)
        }
        fab.setOnClickListener(addAcctListener)
        ftueStartBtn.setOnClickListener(addAcctListener)
    }

    override fun onItemClick(item: Profile) {
        // TODO: in normal mode, clicking should probably be details/manage

        if (this.intent.action == ACTION_PICK) {
            setResult(Activity.RESULT_OK, Intent().apply {
                putExtra(EXTRA_SELECT_REPLY, item.profileId)
            })
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            addProfileRequestCode -> {
                if (resultCode == RESULT_OK) {
                    val profileId = data?.getStringExtra(LoginActivity.EXTRA_REPLY).orEmpty()
                    // thumbs up emoji. via https://apps.timwhitlock.info/emoji/tables/unicode
                    Snackbar.make(fab, "Stored new session for $profileId \uD83D\uDC4D", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show()
                }
            }
            else -> {
                Bugsnag.notify(Exception("onActivityResult() got unknown resultCode"))
            }
        }
    }
}

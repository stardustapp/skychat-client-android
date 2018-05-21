package app.skychat.client

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import app.skychat.client.ChatActivity.Companion.EXTRA_PROFILE
import app.skychat.client.data.Profile
import app.skychat.client.utils.ItemEventListener
import com.bugsnag.android.Bugsnag
import kotlinx.android.synthetic.main.activity_profiles.*


class ProfilesActivity : AppCompatActivity() {
    private lateinit var viewModel: ProfileListViewModel

    val ADD_PROFILE_ACTIVITY_REQUEST_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profiles)
        //setSupportActionBar(toolbar)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerview)
        val adapter = ProfileListAdapter(this, object : ItemEventListener<Profile> {
            override fun onItemClick(item: Profile) {
                startActivity(Intent(this@ProfilesActivity, ChatActivity::class.java).apply {
                    putExtra(EXTRA_PROFILE, item.profileId)
                })
            }
        })
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        viewModel = ViewModelProviders
                .of(this)
                .get(ProfileListViewModel::class.java)
                .also { it
                        .getAllProfiles()
                        .observe(this, Observer<List<Profile>>(adapter::setProfiles))
                }

        fab.setOnClickListener { _ ->
            startActivityForResult(
                    Intent(this, LoginActivity::class.java),
                    ADD_PROFILE_ACTIVITY_REQUEST_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            ADD_PROFILE_ACTIVITY_REQUEST_CODE -> {
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

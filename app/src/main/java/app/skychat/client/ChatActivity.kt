package app.skychat.client

import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.design.widget.Snackbar
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import app.skychat.client.actions.ListRoomsTask
import app.skychat.client.data.Profile
import com.bugsnag.android.Bugsnag
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.android.synthetic.main.app_bar_chat.*
import kotlinx.android.synthetic.main.content_chat.*
import kotlinx.android.synthetic.main.nav_header_chat.*

class ChatActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    companion object {
        val EXTRA_PROFILE = "app.skylink.client.ChatActivity.PROFILE"
    }

    private lateinit var profileListViewModel: ProfileListViewModel
    private lateinit var profile: Profile

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }

        val profileId = this.intent.getStringExtra(EXTRA_PROFILE)
                ?: return Toast(this).let {
                    it.setText("Profile not given with intent")
                    it.show()
                    finish()
                }
        ViewModelProviders
                .of(this)
                .get(ProfileListViewModel::class.java)
                .getOneProfile(profileId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ profile ->
                    object : ListRoomsTask(profile, "irc/networks", "freenode") {
                        override fun onPostExecute(result: Result?) {
                            super.onPostExecute(result)
                            nav_header_realname.text = profile.realName ?: "N/A"
                            nav_header_address.text = profile.run { "$userName@$domainName" }
                            overall_progress.visibility = View.INVISIBLE

                            val navMenu: NavigationView = findViewById(R.id.nav_view)
                            navMenu.menu.apply {
                                clear()
                                val channelMenu = addSubMenu("Channels")
                                result?.groupRooms?.forEach { channelMenu.add(it.name) }
                                val queryMenu = addSubMenu("Queries")
                                result?.directRooms?.forEach { queryMenu.add(it.name) }
                                result?.backgroundRoom?.let { add("server") }
                            }

                            drawer_layout.openDrawer(GravityCompat.START)
                        }
                    }.execute(null as Void?)
                }, { error ->
                    Bugsnag.notify(error)
                    Toast(this@ChatActivity).let {
                        it.setText(error.message)
                        it.show()
                    }
                    finish()
                })


        val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.chat, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        when (item.itemId) {
            R.id.action_settings -> return true
            R.id.action_logout -> {
                finish()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    var currentMenuItem: MenuItem? = null

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        currentMenuItem?.isChecked = false
        item.isCheckable = true
        item.isChecked = true

        currentMenuItem = item
        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }
}

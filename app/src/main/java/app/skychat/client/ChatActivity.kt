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
import app.skychat.client.skylink.FolderLiteral
import app.skychat.client.skylink.NetEntry
import app.skychat.client.skylink.StringLiteral
import app.skychat.client.skylink.remoteTreeFor
import com.bugsnag.android.Bugsnag
import com.google.common.collect.ImmutableMap
import io.reactivex.Maybe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.android.synthetic.main.app_bar_chat.*
import kotlinx.android.synthetic.main.content_chat.*
import kotlinx.android.synthetic.main.nav_header_chat.*
import java.util.*

class ChatActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener, ActivityFragment.OnListFragmentInteractionListener {
    companion object {
        val EXTRA_PROFILE = "app.skylink.client.ChatActivity.PROFILE"
    }

    private lateinit var profileListViewModel: ProfileListViewModel
    private lateinit var profile: Profile

    private var menuIds: Map<Int, ListRoomsTask.RoomEntry> = emptyMap()

    fun sendIrcPacket(command: String, vararg params: String): Maybe<NetEntry> {
        return remoteTreeFor(profile.domainName!!)
                .invokeRx("/sessions/${profile.sessionId}/mnt/runtime/apps/irc/namespace/state/networks/${"freenode"}/wire/send/invoke",
                        FolderLiteral("",
                                StringLiteral("command", command),
                                FolderLiteral("params", *params.mapIndexed({ index, s ->
                                    StringLiteral((index+1).toString(), s)
                                }).toTypedArray())))
                .filter { evt -> evt.type == "String" }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        setSupportActionBar(toolbar)

        send_message_btn.setOnClickListener { view ->
            val currentTarget = currentRoom?.name ?: "0"

            val message = message_input.text.toString()
            //message_input.text = ""

            val maybe = if (message[0] == '/') {
                val parts = message.drop(1).split(' ')
                val command = parts[0].toLowerCase()
                val params = parts.drop(1)

                when (command) {
                    "msg" -> sendIrcPacket(
                            "PRIVMSG", params[0],
                            params.drop(1).joinToString(" "))
                    "me" -> sendIrcPacket(
                            "CTCP", currentTarget,
                            "ACTION", params.joinToString(" "))
                    "slap" -> sendIrcPacket(
                            "CTCP", currentTarget,
                            "ACTION", "slaps ${params.joinToString(" ")} around a bit with a large trout")
                    "ctcp" -> sendIrcPacket(
                            "CTCP", params[0],
                            params[1], params.drop(2).joinToString(" "))
                    else -> Maybe.error(
                            IllegalArgumentException("Invalid slash command /$command"))
                }
            } else sendIrcPacket(
                    "PRIVMSG", currentTarget,
                    message) // TODO: splitting for length

            maybe
                .observeOn(AndroidSchedulers.mainThread())
                .defaultIfEmpty(StringLiteral("output", "Unknown error"))
                .subscribe({ x ->
                    if (x.stringValue == "Ok") {
                        message_input.text.clear()
                    } else {
                        Snackbar.make(view, "Failed to send message",
                                Snackbar.LENGTH_LONG).show()
                    }
                }, { err ->
                    Bugsnag.notify(err)
                    Snackbar.make(view, "Crashed sending message: ${err.message}",
                            Snackbar.LENGTH_LONG).show()
                })
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
                    this.profile = profile
                    object : ListRoomsTask(profile, "irc/networks", "freenode") {
                        override fun onPostExecute(result: Result?) {
                            super.onPostExecute(result)
                            nav_header_realname.text = profile.realName ?: "N/A"
                            nav_header_address.text = profile.run { "$userName@$domainName" }
                            overall_progress.visibility = View.GONE

                            val navMenu: NavigationView = findViewById(R.id.nav_view)
                            val itemMap = HashMap<Int, RoomEntry>()
                            var menuIdx = 0
                            navMenu.menu.apply {
                                clear()
                                val channelMenu = addSubMenu("Channels")
                                result?.groupRooms?.forEach { channelMenu.add(0, ++menuIdx, menuIdx, it.name).apply {
                                    isCheckable = true
                                    setIcon(R.drawable.ic_menu_send)
                                    itemMap[itemId] = it
                                }}
                                val queryMenu = addSubMenu("Queries")
                                result?.directRooms?.forEach { queryMenu.add(0, ++menuIdx, menuIdx, it.name).apply {
                                    isCheckable = true
                                    itemMap[itemId] = it
                                } }
                                result?.backgroundRoom?.let { add(0, ++menuIdx, menuIdx, "server").apply {
                                    isCheckable = true
                                    itemMap[itemId] = it
                                } }
                            }
                            menuIds = ImmutableMap.copyOf(itemMap)

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

    //var currentMenuItem: MenuItem? = null
    var currentRoom: ListRoomsTask.RoomEntry? = null
    var currentActivityFrag: ActivityFragment? = null

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        /*currentMenuItem?.isChecked = false
        item.isCheckable = true
        item.isChecked = true

        currentMenuItem = item*/
        drawer_layout.closeDrawer(GravityCompat.START)

        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()

        currentActivityFrag?.let {
            fragmentTransaction.remove(it)
        }
        currentActivityFrag = null

        currentRoom = menuIds[item.itemId]
        currentRoom?.let {
            currentActivityFrag = ActivityFragment.newInstance(profile.domainName!!, it.path)
            fragmentTransaction.add(R.id.chat_history_frame, currentActivityFrag)
            title = "${it.name} on Freenode"
        }

        fragmentTransaction.commit()
        return true
    }

    override fun onListFragmentInteraction(entry: ActivityEntry) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

package app.skychat.client

import android.content.Intent
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
import app.skychat.client.service.TreeConnection
import app.skychat.client.skylink.FolderLiteral
import app.skychat.client.skylink.NetEntry
import app.skychat.client.skylink.StringLiteral
import app.skychat.client.skylink.remoteTreeFor
import com.bugsnag.android.Bugsnag
import com.google.common.collect.ImmutableMap
import io.reactivex.Maybe
import io.reactivex.MaybeSource
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.android.synthetic.main.app_bar_chat.*
import kotlinx.android.synthetic.main.content_chat.*
import kotlinx.android.synthetic.main.nav_header_chat.*
import java.util.*

class ChatActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener, ActivityFragment.OnListFragmentInteractionListener {
    companion object {
        val EXTRA_PROFILE = "app.skylink.client.ChatActivity.PROFILE"
    }

    private lateinit var treeConnection: TreeConnection
    private lateinit var profileMaybe: Maybe<Profile>

    private var menuIds: Map<Int, ListRoomsTask.RoomEntry> = emptyMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        setSupportActionBar(toolbar)

        treeConnection = TreeConnection(this)
                .also { it.bind() }

        send_message_btn.setOnClickListener { view ->
            val message = message_input.text.toString()
            when {
                message.isEmpty() ->
                    Maybe.just(NetEntry("mock", "String", "Ok", null, null))
                message[0] == '/' ->
                    slashCommand(message, currentRoom?.name ?: "0")
                else ->
                    sendIrcPacket("PRIVMSG", currentRoom?.name ?: "0", message)
            }
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

        profileMaybe = if (this.intent.hasExtra(EXTRA_PROFILE)) {
            treeConnection.resumeProfileById(
                    this.intent.getStringExtra(EXTRA_PROFILE)
                            ?: throw Error("EXTRA_PROFILE seen, but didn't get"))
        } else {
            treeConnection.resumeLastProfile()
        }

        // Pass the profile to the UI
        profileMaybe
                .observeOn(AndroidSchedulers.mainThread())
                // If no profile resumed, let the user create or select anew
                .switchIfEmpty(MaybeSource {
                    startActivity(Intent(this,
                            ProfilesActivity::class.java))
                    finish()
                    it.onComplete()
                })

        // Pull in the room list
        profileMaybe
                .map { ListRoomsTask(it, "irc/networks", "freenode") }
                .flatMap { it.asMaybe() }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ it ->
                    overall_progress.visibility = View.GONE

                    treeConnection.currentProfile?.apply {
                        nav_header_realname.text = this.realName ?: "N/A"
                        nav_header_address.text = this.run { "$userName@$domainName" }
                    }

                    val navMenu: NavigationView = findViewById(R.id.nav_view)
                    val itemMap = HashMap<Int, ListRoomsTask.RoomEntry>()
                    var menuIdx = 0
                    navMenu.menu.apply {
                        clear()
                        val channelMenu = addSubMenu("Channels")
                        it.groupRooms.forEach { channelMenu.add(0, ++menuIdx, menuIdx, it.name).apply {
                            isCheckable = true
                            setIcon(R.drawable.ic_menu_send)
                            itemMap[itemId] = it
                        } }
                        val queryMenu = addSubMenu("Queries")
                        it.directRooms.forEach { queryMenu.add(0, ++menuIdx, menuIdx, it.name).apply {
                            isCheckable = true
                            itemMap[itemId] = it
                        } }
                        it.backgroundRoom?.let { add(0, ++menuIdx, menuIdx, "server").apply {
                            isCheckable = true
                            itemMap[itemId] = it
                        } }
                    }
                    menuIds = ImmutableMap.copyOf(itemMap)

                    drawer_layout.openDrawer(GravityCompat.START)
                }, { error ->
                    Bugsnag.notify(error)
                    Toast.makeText(this, error.message, Toast.LENGTH_LONG).show()
                    finish()
                })


        val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)
    }

    private fun sendIrcPacket(command: String, vararg params: String): Maybe<NetEntry> {
        return Maybe
                .just(treeConnection.currentProfile)
                .flatMap { profile ->
                    remoteTreeFor(profile.domainName!!)
                            .invokeRx("/sessions/${profile.sessionId}/mnt/runtime/apps/irc/namespace/state/networks/${"freenode"}/wire/send/invoke",
                                    FolderLiteral("",
                                            StringLiteral("command", command),
                                            FolderLiteral("params", *params.mapIndexed({ index, s ->
                                                StringLiteral((index + 1).toString(), s)
                                            }).toTypedArray())))
                            .filter { evt -> evt.type == "String" }
                }
    }

    private fun slashCommand(message: String, currentRoom: String): Maybe<NetEntry> {
        val parts = message.drop(1).split(' ')
        val command = parts[0].toLowerCase()
        val params = parts.drop(1)

        return when (command) {
            "msg" -> sendIrcPacket(
                    "PRIVMSG", params[0],
                    params.drop(1).joinToString(" "))
            "me" -> sendIrcPacket(
                    "CTCP", currentRoom,
                    "ACTION", params.joinToString(" "))
            "slap" -> sendIrcPacket(
                    "CTCP", currentRoom,
                    "ACTION", "slaps ${params.joinToString(" ")} around a bit with a large trout")
            "ctcp" -> sendIrcPacket(
                    "CTCP", params[0],
                    params[1], params.drop(2).joinToString(" "))
            else -> Maybe.error(
                    IllegalArgumentException("Invalid slash command /$command"))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        treeConnection.unbind()
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
        return when (item.itemId) {
            R.id.action_add_profile -> {
                startActivity(Intent(this, LoginActivity::class.java))
                true
            }
            R.id.action_manage_profiles -> {
                startActivity(Intent(this, ProfilesActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    //var currentMenuItem: MenuItem? = null
    private var currentRoom: ListRoomsTask.RoomEntry? = null
    private var currentActivityFrag: ActivityFragment? = null

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        drawer_layout.closeDrawer(GravityCompat.START)

        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()

        currentActivityFrag?.let(fragmentTransaction::remove)
        currentActivityFrag = null

        currentRoom = menuIds[item.itemId]
        currentRoom?.let {
            currentActivityFrag = ActivityFragment.newInstance(treeConnection.currentProfile?.domainName!!, it.path)
            fragmentTransaction.add(R.id.chat_history_frame, currentActivityFrag)
            title = "${it.name} on Freenode"
        }

        fragmentTransaction.commit()
        return true
    }

    override fun onListFragmentInteraction(entry: ActivityEntry) {
        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

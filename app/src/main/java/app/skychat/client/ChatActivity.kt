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
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import app.skychat.client.chat.ChatCommunity
import app.skychat.client.chat.ChatRoom
import app.skychat.client.chat.Irc
import app.skychat.client.data.Profile
import app.skychat.client.service.TreeConnection
import app.skychat.client.skylink.NetEntry
import app.skychat.client.skylink.StringLiteral
import com.bugsnag.android.Bugsnag
import com.google.common.collect.ImmutableMap
import io.reactivex.Maybe
import io.reactivex.MaybeSource
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.android.synthetic.main.app_bar_chat.*
import kotlinx.android.synthetic.main.content_chat.*
import java.util.*

class ChatActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener, ActivityFragment.OnListFragmentInteractionListener {
    private lateinit var treeConnection: TreeConnection
    private lateinit var profileMaybe: Maybe<Profile>

    private var menuIds: Map<Int, ChatRoom> = emptyMap()

    private var communities: List<ChatCommunity> = emptyList()
    private var communityRooms: MutableMap<ChatCommunity, List<ChatRoom>> = mutableMapOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        setSupportActionBar(toolbar)

        // Rig up nav's profile header functionality
        // Get views directly because NavigationView takes extra time to mount normally
        val profileHeader = nav_view.getHeaderView(0)
        profileHeader.findViewById<ImageButton>(R.id.switch_account_btn).setOnClickListener {
            startActivityForResult(Intent(this, ProfilesActivity::class.java).apply {
                action = Intent.ACTION_PICK
            }, selectProfileRequestCode)
        }

        // Connect to the nametree service
        treeConnection = TreeConnection(this)
                .also { it.bind() }

        send_message_btn.setOnClickListener { view ->
            val message = message_input.text.toString()
            val room = currentRoom ?: return@setOnClickListener
            when {
                message.isEmpty() ->
                    Maybe.just(NetEntry("mock", "String", "Ok", null, null))
                message[0] == '/' ->
                    slashCommand(message, room)
                else ->
                    room.sendMessage(message)
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
                    if (err is IllegalArgumentException) {
                        Snackbar.make(view, err.localizedMessage ?: "IllegalArgumentException",
                                Snackbar.LENGTH_SHORT).show()
                    } else {
                        Bugsnag.notify(err)
                        Snackbar.make(view, "Crashed sending message: ${err.message}",
                                Snackbar.LENGTH_LONG).show()
                    }
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
                .switchIfEmpty(MaybeSource {
                    // If no profile was resumed, let the user create or select anew
                    startActivity(Intent(this,
                            ProfilesActivity::class.java))
                    finish()
                    it.onComplete()

                }).subscribe({
                    // Fill in the nav header with the profile
                    // Get views directly because NavigationView takes extra time to mount normally
                    val profileHeader = nav_view.getHeaderView(0)
                    val headerRealName: TextView = profileHeader.findViewById(R.id.nav_header_real_name)
                    val headerAddress: TextView = profileHeader.findViewById(R.id.nav_header_address)
                    headerRealName.text = it.realName ?: "Unnamed user"
                    headerAddress.text = it.run { "$userName@$domainName" }

                }, { error ->
                    // If the profile failed to resume, TODO: probably bump to account switcher
                    Bugsnag.notify(error, { r -> r.error?.context = "ChatActivity Profile resume" })
                    Toast.makeText(this, "Failed to resume session. ${error.message}",
                            Toast.LENGTH_LONG).show()
                })

        // Fetch community list (just IRC networks for now)
        val communitiesMaybe = profileMaybe
                .onErrorComplete()
                .flatMap { Maybe
                        .fromCallable({ Irc.getAllNetworks(it) })
                        .subscribeOn(Schedulers.io())
                }.cache()

        // Seed UI with community list
        communitiesMaybe
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ list ->
                    communities = list
                    renderRoomMenu()
                    drawer_layout.openDrawer(GravityCompat.START)
                }, { error ->
                    Bugsnag.notify(error)
                    Toast.makeText(this, "Failed to load communities. ${error.message}", Toast.LENGTH_LONG).show()
                })

        // Load each community's rooms
        communitiesMaybe
                .onErrorComplete()
                .flatMapObservable { Observable.fromIterable(it) }
                .flatMapMaybe { community -> Maybe
                        .fromCallable({ community.getRooms() })
                        .subscribeOn(Schedulers.io())
                        .filter({
                            communityRooms[community] = it
                            true
                        })
                }
                .observeOn(AndroidSchedulers.mainThread())
                .doOnComplete {
                    overall_progress.visibility = View.GONE
                }
                .subscribe({
                    renderRoomMenu()
                }, { error ->
                    Bugsnag.notify(error)
                    Toast.makeText(this, "Failed to load rooms. ${error.message}", Toast.LENGTH_LONG).show()
                })


        val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)
    }

    private fun slashCommand(message: String, room: ChatRoom): Maybe<NetEntry> {
        val parts = message.drop(1).split(' ')
        val command = parts[0].toLowerCase()
        val params = parts.drop(1)
        return room.slashCommand(command, params)
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


    companion object {
        private const val selectProfileRequestCode = 1
        const val EXTRA_PROFILE = "app.skylink.client.ChatActivity.PROFILE"
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.chat_options, menu)
        return true
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
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
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {

            selectProfileRequestCode -> {
                if (resultCode == RESULT_OK) {
                    val profileId = data?.getStringExtra(ProfilesActivity.EXTRA_SELECT_REPLY).orEmpty()
                    startActivity(Intent(this, ChatActivity::class.java).apply {
                        putExtra(EXTRA_PROFILE, profileId)
                    })
                    finish()
                }
            }

            else ->
                Bugsnag.notify(Exception("ChatActivity onActivityResult() got unknown resultCode"))
        }
    }


    //var currentMenuItem: MenuItem? = null
    private var currentRoom: ChatRoom? = null
    private var currentActivityFrag: ActivityFragment? = null

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        drawer_layout.closeDrawer(GravityCompat.START)

        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()

        currentActivityFrag?.let(fragmentTransaction::remove)
        currentActivityFrag = null

        currentRoom = menuIds[item.itemId]
        currentRoom?.let {
            currentActivityFrag = ActivityFragment.newInstance(treeConnection.currentProfile?.domainName!!, it.logPath)
            fragmentTransaction.add(R.id.chat_history_frame, currentActivityFrag)
            title = "${it.id} on ${it.community.id}"
        }

        fragmentTransaction.commit()
        return true
    }

    override fun onListFragmentInteraction(entry: ActivityEntry) {

    }

    private fun renderRoomMenu() {
        val navMenu: NavigationView = findViewById(R.id.nav_view)
        val itemMap = HashMap<Int, ChatRoom>()
        navMenu.menu.clear()
        var menuIdx = 0
        communities.forEach {
            val communityMenu = navMenu.menu.addSubMenu(it.id)
            val rooms = communityRooms.getOrDefault(it, emptyList())
            rooms.forEach {
                communityMenu.add(0, ++menuIdx, menuIdx, it.id).apply {
                    isCheckable = true
                    when (it) {
                        is Irc.ChannelRoom -> {
                            setIcon(when (it.prefix) {
                                "##" -> R.drawable.ic_double_octothorpe_green
                                "#" -> R.drawable.ic_octothorpe_green
                                else -> R.drawable.ic_people_green
                            })
                            title = it.postPrefix
                        }
                        is Irc.QueryRoom -> {
                            setIcon(R.drawable.ic_person_green)
                        }
                        is Irc.ServerRoom -> {
                            setIcon(R.drawable.ic_router_green)
                        }
                    }
                    itemMap[itemId] = it
                }
            }
        }
        menuIds = ImmutableMap.copyOf(itemMap)
    }
}

package app.skychat.client

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import com.bugsnag.android.Bugsnag
import kotlinx.android.synthetic.main.activity_profiles.*


class ProfilesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profiles)
        //setSupportActionBar(toolbar)

        Bugsnag.init(this)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerview)
        val adapter = ProfileListAdapter(this)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        fab.setOnClickListener { view ->
            startActivity(Intent(this, LoginActivity::class.java))
            //Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
            //        .setAction("Action", null).show()
        }
    }
}

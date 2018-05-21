package app.skychat.client

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import app.skychat.client.data.Profile


class ProfileListAdapter internal constructor(context: Context) : RecyclerView.Adapter<ProfileListAdapter.ProfileViewHolder>() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private var profileList: List<Profile>? = null // Cached copy of words

    inner class ProfileViewHolder constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileItemView: TextView = itemView.findViewById(R.id.textView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfileViewHolder {
        val itemView = inflater.inflate(R.layout.saved_profile_item, parent, false)
        return ProfileViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ProfileViewHolder, position: Int) {
        if (profileList != null) {
            val current = profileList!![position]
            holder.profileItemView.text = current.userName
        } else {
            holder.profileItemView.text = "No Word"
        }
    }

    internal fun setProfiles(profiles: List<Profile>?) {
        profileList = profiles
        notifyDataSetChanged()
    }

    // getItemCount() is called many times, and when it is first called,
    // profileList has not been updated (means initially, it's null, and we can't return null).
    override fun getItemCount(): Int {
        return if (profileList != null)
            profileList!!.size
        else
            0
    }
}
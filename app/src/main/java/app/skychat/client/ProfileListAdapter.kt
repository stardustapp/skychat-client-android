package app.skychat.client

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import app.skychat.client.data.Profile
import app.skychat.client.utils.ItemEventListener


class ProfileListAdapter internal constructor(
        context: Context,
        private val itemListener: ItemEventListener<Profile>
) : RecyclerView.Adapter<ProfileListAdapter.ProfileViewHolder>() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private var profileList: List<Profile>? = null // Cached copy of words

    inner class ProfileViewHolder constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleView: TextView = itemView.findViewById(R.id.profile_title)
        val subTitleView: TextView = itemView.findViewById(R.id.profile_subtitle)
        private val entry: LinearLayout = itemView.findViewById(R.id.profile_entry)

        fun bind(item: Profile, itemListener: ItemEventListener<Profile>) {
            entry.setOnClickListener { _ -> itemListener.onItemClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfileViewHolder {
        val itemView = inflater.inflate(R.layout.saved_profile_item, parent, false)
        return ProfileViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ProfileViewHolder, position: Int) {
        if (profileList != null) {
            val current = profileList!![position]
            holder.titleView.text = "${current.userName}@"
            holder.subTitleView.text = current.domainName
            holder.bind(current, itemListener)
        } else {
            holder.titleView.text = "N/A"
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
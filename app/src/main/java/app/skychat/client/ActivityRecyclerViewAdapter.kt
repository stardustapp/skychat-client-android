package app.skychat.client


import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import app.skychat.client.ActivityFragment.OnListFragmentInteractionListener
import app.skychat.client.dummy.DummyContent.DummyItem
import kotlinx.android.synthetic.main.fragment_activity.view.*
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle


/**
 * [RecyclerView.Adapter] that can display a [DummyItem] and makes a call to the
 * specified [OnListFragmentInteractionListener].
 * TODO: Replace the implementation with code for your data type.
 */
class ActivityRecyclerViewAdapter(
        private val mListener: OnListFragmentInteractionListener?)
    : RecyclerView.Adapter<ActivityRecyclerViewAdapter.ViewHolder>() {

    private val mOnClickListener: View.OnClickListener
    private var mValues: List<ActivityEntry> = emptyList()

    init {
        mOnClickListener = View.OnClickListener { v ->
            val item = v.tag as ActivityEntry
            // Notify the active callbacks interface (the activity, if the fragment is attached to
            // one) that an item has been selected.
            mListener?.onListFragmentInteraction(item)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.fragment_activity, parent, false)
        return ViewHolder(view)
    }

    val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
            //.withLocale(Locale.UK)
            .withZone(ZoneId.systemDefault())

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = mValues[position]
        holder.mIdView.text = item.displayText()
        holder.mContentView.text = item.timestamp?.let {
            timeFormatter.format(it)
        }

        with(holder.mView) {
            tag = item
            setOnClickListener(mOnClickListener)
        }
    }

    fun setActivityHistory(list: List<ActivityEntry>) {
        mValues = list
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = mValues.size

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
        val mIdView: TextView = mView.item_number
        val mContentView: TextView = mView.content

        override fun toString(): String {
            return super.toString() + " '" + mContentView.text + "'"
        }
    }
}

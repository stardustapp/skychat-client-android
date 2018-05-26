package app.skychat.client

import android.graphics.Color
import android.graphics.Typeface
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import app.skychat.client.ActivityFragment.OnListFragmentInteractionListener
import com.amulyakhare.textdrawable.TextDrawable
import com.amulyakhare.textdrawable.util.ColorGenerator
import kotlinx.android.synthetic.main.fragment_activity.view.*
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle

/**
 * [RecyclerView.Adapter] that can display [ActivityEntry]s and makes a call to the
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

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = mValues[position]

        val currentLayoutParams = (holder.msgLayout.layoutParams as ViewGroup.MarginLayoutParams)
        holder.msgLayout.layoutParams = currentLayoutParams.apply {
            topMargin = when {
                item.isContinuedMessage -> 0
                item.isMessage -> leftMargin / 2
                else -> leftMargin / 4
            }
        }

        if (item.isMessage && !item.isContinuedMessage) {
            val authorColor = colorGenerator.getColor(item.prefixName)
            val authorIcon = iconBuilder.build(item.prefixName.substring(0..0), authorColor)

            holder.authorName.text = item.prefixName
            holder.authorName.visibility = View.VISIBLE
            holder.authorName.setTextColor(authorColor)

            holder.authorAvatar.setImageDrawable(authorIcon)
            holder.authorAvatar.visibility = View.VISIBLE
            holder.authorAvatar.layoutParams
                    .apply { height = width }
        } else {
            holder.authorName.visibility = View.GONE

            holder.authorAvatar.visibility = View.INVISIBLE
            holder.authorAvatar.layoutParams
                    .apply { height = 0 }
        }

        if (item.isAction) {
            val authorColor = colorGenerator.getColor(item.prefixName)
            val authorIcon = smallIconBuilder.build(
                    item.prefixName.substring(0..0),
                    authorColor)

            holder.bodyAvatar.visibility = View.VISIBLE
            holder.bodyAvatar.setImageDrawable(authorIcon)

            holder.bodyAuthorName.text = item.prefixName
            holder.bodyAuthorName.visibility = View.VISIBLE
            holder.bodyAuthorName.setTextColor(authorColor)
            holder.bodyAuthorName.setTypeface(null,Typeface.BOLD or Typeface.ITALIC)

            holder.bodyText.setTypeface(null, Typeface.ITALIC)
        } else {
            holder.bodyAvatar.visibility = View.GONE

            holder.bodyAuthorName.visibility = View.GONE

            holder.bodyText.setTypeface(null, Typeface.NORMAL)
        }

        if (item.isBackground) {
            holder.bodyText.setTextColor(Color.GRAY)
        } else {
            holder.bodyText.setTextColor(Color.WHITE)
        }
        holder.bodyText.text = item.displayText()

        holder.timestamp.text = item.timestamp?.let {
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

    override fun getItemId(position: Int): Long {
        return mValues[position].path.hashCode().toLong()
    }
    init {
        setHasStableIds(true)
    }

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
        val authorName: TextView = mView.msg_author_name
        val authorAvatar: ImageView = mView.msg_author_avatar
        val bodyAuthorName: TextView = mView.msg_body_author_name
        val bodyAvatar: ImageView = mView.msg_body_avatar
        val bodyText: TextView = mView.msg_body_text
        val timestamp: TextView = mView.msg_timestamp
        val msgLayout: LinearLayout = mView.msg_layout
    }

    companion object {
        val colorGenerator = ColorGenerator.MATERIAL!!
        // int color = generator.getColor("user@gmail.com")
        val iconBuilder = TextDrawable.builder()
                .beginConfig()
                .toUpperCase()
                .bold()
                .textColor(Color.BLACK)
                .width(32)
                .height(32)
                .endConfig()
                .round()!!
        val smallIconBuilder = TextDrawable.builder()
                .beginConfig()
                .toUpperCase()
                .bold()
                .textColor(Color.BLACK)
                .width(16)
                .height(16)
                .endConfig()
                .round()!!

        val timeFormatter = DateTimeFormatter
                .ofLocalizedTime(FormatStyle.SHORT)
                .withZone(ZoneId.systemDefault())!!
    }
}

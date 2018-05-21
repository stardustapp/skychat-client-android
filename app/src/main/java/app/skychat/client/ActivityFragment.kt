package app.skychat.client

import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.android.schedulers.AndroidSchedulers

/**
 * A fragment representing a list of Items.
 * Activities containing this fragment MUST implement the
 * [ActivityFragment.OnListFragmentInteractionListener] interface.
 */
class ActivityFragment : Fragment() {

    private var domainName = ""
    private var logPath = ""

    private var listener: OnListFragmentInteractionListener? = null

    private lateinit var viewModel: ActivityHistoryViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            domainName = it.getString(ARG_DOMAIN_NAME)
            logPath = it.getString(ARG_LOG_PATH)
        }

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_activity_history, container, false)

        // Set the adapter
        if (view is RecyclerView) {
            with(view) {
                layoutManager = LinearLayoutManager(context)
                val activityAdapter = ActivityRecyclerViewAdapter(listener)
                adapter = activityAdapter

                viewModel = ViewModelProviders
                        .of(this@ActivityFragment)
                        .get("$domainName/$logPath", ActivityHistoryViewModel::class.java)
                        .also {
                            it.startIfIdle(domainName, logPath)
                            it
                                .watchForSnapshots()
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe({ list ->
                                    activityAdapter.setActivityHistory(list)
                                    this.scrollToPosition(adapter.itemCount - 1)
                                })
                        }
            }
        }
        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnListFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnListFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson
     * [Communicating with Other Fragments](http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
    interface OnListFragmentInteractionListener {
        fun onListFragmentInteraction(entry: ActivityEntry)
    }

    companion object {

        const val ARG_DOMAIN_NAME = "domain-name"
        const val ARG_LOG_PATH = "log-path"

        @JvmStatic
        fun newInstance(domainName: String, logPath: String) =
                ActivityFragment().apply {
                    arguments = Bundle().apply {
                        putString(ARG_DOMAIN_NAME, domainName)
                        putString(ARG_LOG_PATH, logPath)
                    }
                }
    }
}

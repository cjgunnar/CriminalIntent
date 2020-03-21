package com.bignerdranch.android.criminalintent

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.text.DateFormat
import java.util.*

private const val TAG = "CrimeListFragment"

class CrimeListFragment : Fragment() {

    private lateinit var crimeRecyclerView: RecyclerView

    private var adapter: CrimeAdapter = CrimeAdapter(emptyList())

    private val crimeListViewModel: CrimeListViewModel by lazy {
        ViewModelProviders.of(this).get(CrimeListViewModel::class.java)
    }

    interface Callbacks {
        fun onCrimeSelected(crimeID: UUID)
    }

    private var callbacks : Callbacks? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = context as Callbacks
    }

    override fun onDetach() {
        super.onDetach()
        callbacks = null
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_crime_list, container, false)

        crimeRecyclerView = view.findViewById(R.id.crime_recycler_view)
        crimeRecyclerView.layoutManager = LinearLayoutManager(context)
        crimeRecyclerView.adapter = adapter
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        crimeListViewModel.crimeListLiveData.observe(
            viewLifecycleOwner,
            Observer { crimes ->
                crimes?.let {
                    Log.i(TAG, "Got crimes ${crimes.size}")
                    updateUI(crimes)
                }
            })
    }

    private inner class CrimeHolder(view: View) : RecyclerView.ViewHolder(view),
        View.OnClickListener {
        private val titleTextView: TextView = view.findViewById(R.id.crime_title)
        private val dateTextView: TextView = view.findViewById(R.id.crime_date)
        private val isSolvedImageView: ImageView = view.findViewById(R.id.crime_solved)
        private var mCrime : Crime? = null

        init {
            itemView.setOnClickListener(this)
        }

        fun bind(crime: Crime) {
            mCrime = crime

            titleTextView.text = crime.title
            dateTextView.text = DateFormat.getDateInstance(DateFormat.MEDIUM).format(crime.date)
            isSolvedImageView.visibility = if (crime.isSolved) View.VISIBLE else View.INVISIBLE
        }

        override fun onClick(p0: View?) {
            mCrime?.let {
                callbacks?.onCrimeSelected(it.id)
            }
        }
    }

    private class CrimeDiffCallback : DiffUtil.ItemCallback<Crime>() {
        override fun areItemsTheSame(oldItem: Crime, newItem: Crime): Boolean {
            //simply check if IDs are the same
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Crime, newItem: Crime): Boolean {
            //calls .equals on each item so make sure that .equals() is implemented
            //crime is a data class so the system should have that taken care of
            return oldItem == newItem
        }

    }

    private inner class CrimeAdapter(var crimes: List<Crime>) :
        ListAdapter<Crime, CrimeHolder>(CrimeDiffCallback()) {

        val minorCrime = 0
        val seriousCrime = 1

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CrimeHolder {
            fun minorCrimeViewHolder(): CrimeHolder {
                val view = layoutInflater.inflate(R.layout.list_item_crime, parent, false)
                return CrimeHolder(view)
            }

            fun seriousCrimeViewHolder(): CrimeHolder {
                val view = layoutInflater.inflate(R.layout.list_item_serious_crime, parent, false)
                return CrimeHolder(view)
            }

            return if (viewType == minorCrime) minorCrimeViewHolder()
            else seriousCrimeViewHolder()
        }

        override fun getItemCount() = crimes.size

        override fun onBindViewHolder(holder: CrimeHolder, position: Int) {
            val crime = crimes[position]
            holder.bind(crime)
        }

        override fun getItemViewType(position: Int): Int {
            return if (crimes[position].requiresPolice) seriousCrime else minorCrime
        }
    }

    private fun updateUI(crimes : List<Crime>) {
        //I'm not sure if this code actually does the optimization
        adapter = CrimeAdapter(crimes)
        adapter.submitList(crimes)
        crimeRecyclerView.adapter = adapter
    }

    //MENU
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_crime_list, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.new_crime -> {
                val crime = Crime()
                crimeListViewModel.addCrime(crime)
                callbacks?.onCrimeSelected(crime.id)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        fun newInstance(): CrimeListFragment {
            return CrimeListFragment()
        }
    }
}

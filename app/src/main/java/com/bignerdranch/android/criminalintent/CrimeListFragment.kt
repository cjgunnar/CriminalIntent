package com.bignerdranch.android.criminalintent

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.DateFormat

private const val TAG = "CrimeListFragment"

class CrimeListFragment : Fragment() {

    private lateinit var crimeRecyclerView : RecyclerView

    private var adapter : CrimeAdapter? = null

    private val crimeListViewModel: CrimeListViewModel by lazy {
        ViewModelProviders.of(this).get(CrimeListViewModel::class.java)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "Total crimes: ${crimeListViewModel.crimes.size}")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
       val view = inflater.inflate(R.layout.fragment_crime_list, container, false)

        crimeRecyclerView = view.findViewById(R.id.crime_recycler_view)
        crimeRecyclerView.layoutManager = LinearLayoutManager(context)

        updateUI()

        return view
    }

    private inner class CrimeHolder(view : View) : RecyclerView.ViewHolder(view), View.OnClickListener {
        private val titleTextView : TextView = view.findViewById(R.id.crime_title)
        private val dateTextView : TextView = view.findViewById(R.id.crime_date)
        private val isSolvedImageView : ImageView = view.findViewById(R.id.crime_solved)

        init {
            itemView.setOnClickListener(this)
        }

        fun bind(crime : Crime) {
            titleTextView.text = crime.title
            dateTextView.text = DateFormat.getDateInstance(DateFormat.MEDIUM).format(crime.date)
            isSolvedImageView.visibility = if(crime.isSolved) View.VISIBLE else View.INVISIBLE
        }

        override fun onClick(p0: View?) {
            val msg = getString(R.string.clicked_item_view_toast).format(titleTextView.text)
            Toast.makeText(context, msg,Toast.LENGTH_SHORT).show()
        }
    }

    private inner class CrimeAdapter(var crimes : List<Crime>) : RecyclerView.Adapter<CrimeHolder>() {

        val minorCrime = 0
        val seriousCrime = 1

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CrimeHolder {
            fun minorCrimeViewHolder() : CrimeHolder {
                val view = layoutInflater.inflate(R.layout.list_item_crime, parent, false)
                return CrimeHolder(view)
            }
            fun seriousCrimeViewHolder() : CrimeHolder {
                val view = layoutInflater.inflate(R.layout.list_item_serious_crime, parent, false)
                return CrimeHolder(view)
            }

            return if(viewType == minorCrime) minorCrimeViewHolder()
            else seriousCrimeViewHolder()
        }

        override fun getItemCount() = crimes.size

        override fun onBindViewHolder(holder: CrimeHolder, position: Int) {
            val crime = crimes[position]
            holder.bind(crime)
        }

        override fun getItemViewType(position: Int): Int {
            return if(crimes[position].requiresPolice) seriousCrime else minorCrime
        }
    }

    private fun updateUI() {
        val crimes = crimeListViewModel.crimes
        adapter = CrimeAdapter(crimes)
        crimeRecyclerView.adapter = adapter
    }

    companion object {
        fun newInstance(): CrimeListFragment {
            return CrimeListFragment()
        }
    }
}

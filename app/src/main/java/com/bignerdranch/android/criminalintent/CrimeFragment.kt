package com.bignerdranch.android.criminalintent

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import java.util.*

private const val TAG = "CrimeFragment"

private const val ARG_CRIME_ID = "crime_id"

class CrimeFragment : Fragment() {
    private lateinit var crime : Crime

    private lateinit var titleEditText: EditText
    private lateinit var dateButton : Button
    private lateinit var solvedCheckBox: CheckBox

    private val crimeFragmentViewModel: CrimeFragmentViewModel by lazy {
        ViewModelProviders.of(this).get(CrimeFragmentViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        crime = Crime()
        val id = arguments?.getSerializable(ARG_CRIME_ID) as UUID
        crimeFragmentViewModel.loadCrime(id)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view =  inflater.inflate(R.layout.fragment_crime, container, false)

        titleEditText = view.findViewById(R.id.crime_title)
        dateButton = view.findViewById(R.id.crime_date)
        dateButton.apply {
            text = crime.date.toString()
            isEnabled = false
        }
        solvedCheckBox = view.findViewById(R.id.crime_solved)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        crimeFragmentViewModel.crimeLiveData.observe(
            viewLifecycleOwner,
            Observer { crime ->
                crime?.let {
                    this.crime = crime
                    updateUI()
                }
            })
    }

    private fun updateUI() {
        titleEditText.setText(crime.title)
        dateButton.text = crime.date.toString()

        //skip checkbox animation
        solvedCheckBox.apply {
            isChecked = crime.isSolved
            jumpDrawablesToCurrentState()
        }

    }

    override fun onStart() {
        super.onStart()

        //update internal crime as data is entered
        val titleWatcher = object : TextWatcher {
            override fun beforeTextChanged(
                sequence: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {}

            override fun onTextChanged(
                sequence: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                crime.title = sequence.toString()
            }

            override fun afterTextChanged(sequence: Editable?) {}
        }
        titleEditText.addTextChangedListener(titleWatcher)

        solvedCheckBox.apply {
            setOnCheckedChangeListener { _, isChecked -> crime.isSolved = isChecked }
        }
    }

    //save the crime when leaving focus
    override fun onStop() {
        super.onStop()
        crimeFragmentViewModel.saveCrime(crime)
    }

    companion object {
        fun newInstance(uuid: UUID) : CrimeFragment {
            val args = Bundle().apply {
                putSerializable(ARG_CRIME_ID, uuid)
            }

            return CrimeFragment().apply {
                arguments = args
            }
        }
    }
}
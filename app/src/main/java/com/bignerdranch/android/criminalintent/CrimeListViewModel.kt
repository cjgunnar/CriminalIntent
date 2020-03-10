package com.bignerdranch.android.criminalintent

import androidx.lifecycle.ViewModel

class CrimeListViewModel : ViewModel() {
    val crimes = mutableListOf<Crime>()

    init {
        for(i in 0..100) {
            val crime = Crime()
            crime.title = "Crime $i"
            if(i % 11 == 0) {crime.requiresPolice = true; crime.title += " SERIOUS CRIME"}
            if(i % 3 == 0) crime.isSolved = true
            crimes += crime
        }
    }
}
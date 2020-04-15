package com.bignerdranch.android.criminalintent

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.MediaStore
import android.telephony.PhoneNumberUtils
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import java.io.File
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "CrimeFragment"

private const val ARG_CRIME_ID = "crime_id"

private const val DIALOG_DATE = "DialogDate"
private const val DIALOG_TIME = "DialogTime"

private const val REQUEST_DATE = 0
private const val REQUEST_TIME = 1
private const val REQUEST_CONTACT = 2
private const val REQUEST_PHOTO = 3

private const val DATE_FORMAT = "EEE, MMM, dd"

class CrimeFragment : Fragment(), DatePickerFragment.Callbacks, TimePickerFragment.Callbacks {
    private lateinit var crime : Crime
    private lateinit var crimePhoto: File
    private lateinit var photoUri: Uri

    private lateinit var titleEditText: EditText
    private lateinit var photoButton: ImageButton
    private lateinit var photoView: ImageView
    private lateinit var dateButton : Button
    private lateinit var timeButton : Button
    private lateinit var solvedCheckBox: CheckBox
    private lateinit var requiresPoliceCheckBox: CheckBox
    private lateinit var reportButton : Button
    private lateinit var suspectButton: Button
    private lateinit var callSuspectButton : Button

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
        photoButton = view.findViewById(R.id.crime_camera)
        photoView = view.findViewById(R.id.crime_photo)
        dateButton = view.findViewById(R.id.crime_date)
        timeButton = view.findViewById(R.id.crime_time)
        solvedCheckBox = view.findViewById(R.id.crime_solved)
        requiresPoliceCheckBox = view.findViewById(R.id.crime_requires_police_button)
        reportButton = view.findViewById(R.id.report_crime_button)
        suspectButton = view.findViewById(R.id.choose_suspect_button)
        callSuspectButton = view.findViewById(R.id.call_suspect_button)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        crimeFragmentViewModel.crimeLiveData.observe(
            viewLifecycleOwner,
            Observer { crime ->
                crime?.let {
                    this.crime = crime
                    crimePhoto = crimeFragmentViewModel.getPhotoFile(crime)
                    photoUri = FileProvider.getUriForFile(requireActivity(),
                        "com.bignerdranch.android.criminalintent.fileprovider",
                        crimePhoto)
                    updateUI()
                }
            })
    }

    private fun updatePhotoView() {
        if(crimePhoto.exists()) {
            val bitmap = getScaledBitmap(crimePhoto.path, requireActivity())
            photoView.setImageBitmap(bitmap)
        }
        else photoView.setImageDrawable(null)
    }

    private fun updateUI() {
        titleEditText.setText(crime.title)
        dateButton.text = DateFormat.getDateInstance(DateFormat.MEDIUM).format(crime.date)
        timeButton.text = SimpleDateFormat("hh:mm a", Locale.US).format(crime.date)

        //skip checkbox animation
        solvedCheckBox.apply {
            isChecked = crime.isSolved
            jumpDrawablesToCurrentState()
        }
        requiresPoliceCheckBox.apply {
            isChecked = crime.requiresPolice
            jumpDrawablesToCurrentState()
        }

        if(crime.suspect.isNotEmpty()) {
            suspectButton.text = crime.suspect
            callSuspectButton.isEnabled = true
        }

        updatePhotoView()
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
        requiresPoliceCheckBox.apply {
            setOnCheckedChangeListener { _, isChecked -> crime.requiresPolice = isChecked }
        }

        dateButton.setOnClickListener {
            DatePickerFragment.newInstance(crime.date).apply {
                setTargetFragment(this@CrimeFragment, REQUEST_DATE)
                show(this@CrimeFragment.requireFragmentManager(), DIALOG_DATE)
            }
        }
        timeButton.setOnClickListener {
            TimePickerFragment.newInstance(crime.date).apply {
                setTargetFragment(this@CrimeFragment, REQUEST_TIME)
                show(this@CrimeFragment.requireFragmentManager(), DIALOG_TIME)
            }
        }

        reportButton.setOnClickListener {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, getCrimeReport())
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.crime_report_suspect)).also {
                    val chooserIntent = Intent.createChooser(it, getString(R.string.send_report))
                    startActivity(chooserIntent)
                }
            }
        }

        suspectButton.apply {
            val pickContactIntent = Intent(Intent.ACTION_PICK)
            pickContactIntent.type = ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE

            setOnClickListener {
                startActivityForResult(pickContactIntent, REQUEST_CONTACT)
            }

            //check that contacts app exists, disable this button if not
            val packageManager: PackageManager = requireActivity().packageManager
            val resolvedActivity: ResolveInfo? =
                packageManager.resolveActivity(pickContactIntent,
                    PackageManager.MATCH_DEFAULT_ONLY)
            if (resolvedActivity == null) {
                isEnabled = false
            }

        }

        callSuspectButton.apply {
            setOnClickListener {
                //format umber to URI
                val phoneURI = "tel:${PhoneNumberUtils.normalizeNumber(crime.number)}"
                startActivity(Intent(Intent.ACTION_DIAL, Uri.parse(phoneURI)))
            }

            isEnabled = crime.number.isNotBlank()
        }

        photoButton.apply {
            val packageManager: PackageManager = requireActivity().packageManager

            val captureImage = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val resolvedActivity: ResolveInfo? = packageManager.resolveActivity(captureImage,
                PackageManager.MATCH_DEFAULT_ONLY)

            if (resolvedActivity == null) {
                isEnabled = false
            }

            setOnClickListener {
                captureImage.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)

                val cameraActivities: List<ResolveInfo> =
                    packageManager.queryIntentActivities(captureImage,
                        PackageManager.MATCH_DEFAULT_ONLY)

                for (cameraActivity in cameraActivities) {
                    requireActivity().grantUriPermission(
                        cameraActivity.activityInfo.packageName,
                        photoUri,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                }
                startActivityForResult(captureImage, REQUEST_PHOTO)
            }

        }
    }

    //save the crime when leaving focus
    override fun onStop() {
        super.onStop()
        crimeFragmentViewModel.saveCrime(crime)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when {
            resultCode != Activity.RESULT_OK -> return

            requestCode == REQUEST_CONTACT && data != null -> {
                val contactUri: Uri? = data.data
                val queryFields = arrayOf(ContactsContract.Contacts.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER)
                if(contactUri != null) {
                    val cursor = requireActivity().contentResolver
                        .query(contactUri, queryFields, null,null,null)

                    cursor?.use {
                        if (it.count == 0) return

                        it.moveToFirst()
                        val suspect = it.getString(0)
                        val number = it.getString(1)

                        crime.suspect = suspect
                        crime.number = number

                        crimeFragmentViewModel.saveCrime(crime)

                        suspectButton.text = suspect

                        //enable call button if number is entered
                        callSuspectButton.isEnabled = number.isNotBlank()
                    }
                }
            }

            //from implicit camera intent
            requestCode == REQUEST_PHOTO -> {
                //remove write permission when photo is received
                requireActivity().revokeUriPermission(photoUri,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                //show the received photo
                updatePhotoView()
            }
        }
    }

    //generate text version of crime report1
    private fun getCrimeReport(): String {
        val solvedString = if (crime.isSolved) {
            getString(R.string.crime_report_solved)
        } else {
            getString(R.string.crime_report_unsolved)
        }
        val dateString = android.text.format.DateFormat.format(DATE_FORMAT, crime.date).toString()
        val suspect = if (crime.suspect.isBlank()) {
            getString(R.string.crime_report_no_suspect)
        } else {
            getString(R.string.crime_report_suspect, crime.suspect)
        }
        return getString(R.string.crime_report,
            crime.title, dateString, solvedString, suspect)
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

    override fun onDateSelected(date: Date) {
        crime.date = date
        updateUI()
    }

    override fun onTimeSelected(date: Date) {
        crime.date = date
        updateUI()
    }
}

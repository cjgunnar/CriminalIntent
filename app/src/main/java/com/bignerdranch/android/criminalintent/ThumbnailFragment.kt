package com.bignerdranch.android.criminalintent

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import java.io.File

private const val ARG_PHOTO_FILE_PATH = "photoFilePath"

class ThumbnailFragment : DialogFragment() {

    private lateinit var imageView: ImageView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_thumbnail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        imageView = view.findViewById(R.id.thumbnail_enlarged_iv)

        val filePath = arguments?.getString(ARG_PHOTO_FILE_PATH)
        if(filePath != null && File(filePath).exists()) {
            val bitmap = getScaledBitmap(filePath, requireActivity())
            imageView.setImageBitmap(bitmap)
        }
    }

    companion object {
        fun newInstance(photoFilePath: String): ThumbnailFragment {
            val args = Bundle().apply {
                putString(ARG_PHOTO_FILE_PATH, photoFilePath)
            }

            return ThumbnailFragment().apply {
                arguments = args
            }
        }
    }
}
package com.example.goldgallery

import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class PhotosFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_photos, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.rvPhotos)

        // Grid with 3 columns
        recyclerView.layoutManager = GridLayoutManager(context, 3)

        // Fetch real paths from the phone
        val realPhotoPaths = getLocalPhotos()

        // Send the real paths to your PhotoAdapter
        recyclerView.adapter = PhotoAdapter(realPhotoPaths)
    }

    // This function talks to the phone's database to find image files
    private fun getLocalPhotos(): List<String> {
        val photoPaths = mutableListOf<String>()
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.MediaColumns.DATA)

        // Sort to show newest photos first
        val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"

        val cursor = requireContext().contentResolver.query(uri, projection, null, null, sortOrder)

        cursor?.use {
            val columnIndex = it.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
            while (it.moveToNext()) {
                val path = it.getString(columnIndex)
                photoPaths.add(path)
            }
        }
        return photoPaths
    }
}
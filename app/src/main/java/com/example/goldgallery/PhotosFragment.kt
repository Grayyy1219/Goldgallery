package com.example.goldgallery

import android.os.Bundle
import android.provider.MediaStore
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class PhotosFragment : Fragment() {

    private val favoritePhotos = mutableSetOf<String>()
    private lateinit var photoAdapter: PhotoAdapter

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

        photoAdapter = PhotoAdapter(
            photos = realPhotoPaths,
            onPhotoClick = { photoPath ->
                val intent = android.content.Intent(requireContext(), FullImageActivity::class.java)
                intent.putExtra("IMAGE_PATH", photoPath)
                startActivity(intent)
            },
            onPhotoLongClick = { photoPath, anchorView ->
                anchorView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                showPhotoActions(photoPath)
            }
        )

        recyclerView.adapter = photoAdapter
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

    private fun showPhotoActions(photoPath: String) {
        PhotoActionsOverlay.show(
            host = requireActivity(),
            photoPath = photoPath,
            isFavorite = favoritePhotos.contains(photoPath),
            onFavoriteClick = {
                val isFavorite = if (favoritePhotos.contains(photoPath)) {
                    favoritePhotos.remove(photoPath)
                    false
                } else {
                    favoritePhotos.add(photoPath)
                    true
                }
                val message = if (isFavorite) {
                    getString(R.string.marked_favorite)
                } else {
                    getString(R.string.removed_favorite)
                }
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            },
            onPrivateClick = {
                Toast.makeText(requireContext(), getString(R.string.set_private_message), Toast.LENGTH_SHORT).show()
            },
            onDeleteClick = {
                Toast.makeText(requireContext(), getString(R.string.delete_message), Toast.LENGTH_SHORT).show()
            }
        )
    }
}

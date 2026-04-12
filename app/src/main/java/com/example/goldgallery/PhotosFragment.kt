package com.example.goldgallery

import android.os.Bundle
import android.content.ContentUris
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

        // Fetch image content URIs from MediaStore
        val photoUris = getLocalPhotos()

        photoAdapter = PhotoAdapter(
            photos = photoUris,
            onPhotoClick = { photoUri ->
                val intent = android.content.Intent(requireContext(), FullImageActivity::class.java)
                intent.putExtra("IMAGE_PATH", photoUri)
                startActivity(intent)
            },
            onPhotoLongClick = { photoUri, anchorView ->
                anchorView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                showPhotoActions(photoUri)
            }
        )

        recyclerView.adapter = photoAdapter
    }

    // This function talks to the phone's database to find image files
    private fun getLocalPhotos(): List<String> {
        val photoUris = mutableListOf<String>()
        val collectionUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Images.Media._ID)

        // Sort to show newest photos first
        val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"

        val cursor = requireContext().contentResolver.query(
            collectionUri,
            projection,
            null,
            null,
            sortOrder
        )

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            while (it.moveToNext()) {
                val imageId = it.getLong(idColumn)
                val imageUri = ContentUris.withAppendedId(collectionUri, imageId)
                photoUris.add(imageUri.toString())
            }
        }
        return photoUris
    }

    private fun showPhotoActions(photoUri: String) {
        PhotoActionsOverlay.show(
            host = requireActivity(),
            photoPath = photoUri,
            isFavorite = favoritePhotos.contains(photoUri),
            onFavoriteClick = {
                val isFavorite = if (favoritePhotos.contains(photoUri)) {
                    favoritePhotos.remove(photoUri)
                    false
                } else {
                    favoritePhotos.add(photoUri)
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

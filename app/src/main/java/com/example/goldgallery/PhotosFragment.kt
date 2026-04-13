package com.example.goldgallery

import android.content.ContentUris
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
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class PhotosFragment : Fragment() {

    private val favoritePhotos = mutableSetOf<String>()
    private lateinit var photoAdapter: MonthlyPhotoAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_photos, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.rvPhotos)
        val layoutManager = GridLayoutManager(context, 3)
        recyclerView.layoutManager = layoutManager

        photoAdapter = MonthlyPhotoAdapter(
            photos = getPhotoListItems(),
            onPhotoClick = { _, position ->
                startActivity(FullImageActivity.createIntent(requireContext(), getVisiblePhotos(), position))
            },
            onPhotoLongClick = { photoUri, anchorView ->
                anchorView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                showPhotoActions(photoUri)
            }
        )

        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (photoAdapter.getItemViewType(position) == MonthlyPhotoAdapter.VIEW_TYPE_HEADER) 3 else 1
            }
        }

        recyclerView.adapter = photoAdapter
    }

    override fun onResume() {
        super.onResume()
        if (::photoAdapter.isInitialized) {
            photoAdapter.updatePhotos(getPhotoListItems())
        }
    }

    private fun getPhotoListItems(): List<PhotoListItem> {
        val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
        val zoneId = ZoneId.systemDefault()
        val grouped = getVisiblePhotoEntries().groupBy {
            Instant.ofEpochMilli(it.dateTaken).atZone(zoneId).format(formatter)
        }

        val items = mutableListOf<PhotoListItem>()
        grouped.forEach { (month, entries) ->
            items.add(PhotoListItem.MonthHeader(month))
            entries.forEach { entry ->
                items.add(PhotoListItem.Photo(entry.uri, entry.dateTaken))
            }
        }
        return items
    }

    private fun getVisiblePhotos(): List<String> {
        return getVisiblePhotoEntries().map { it.uri }
    }

    private fun getVisiblePhotoEntries(): List<PhotoEntry> {
        return getLocalPhotos().filterNot { DeletedPhotosStore.contains(it.uri) || PrivatePhotosStore.contains(it.uri) }
    }

    private fun getLocalPhotos(): List<PhotoEntry> {
        val photoEntries = mutableListOf<PhotoEntry>()
        val collectionUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DATE_TAKEN)
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
            val dateTakenColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            while (it.moveToNext()) {
                val imageId = it.getLong(idColumn)
                val imageUri = ContentUris.withAppendedId(collectionUri, imageId)
                val dateTaken = it.getLong(dateTakenColumn)
                photoEntries.add(PhotoEntry(uri = imageUri.toString(), dateTaken = dateTaken))
            }
        }
        return photoEntries
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
                PrivatePhotosStore.add(photoUri)
                photoAdapter.removePhoto(photoUri)
                Toast.makeText(requireContext(), getString(R.string.set_private_message), Toast.LENGTH_SHORT).show()
            },
            onDeleteClick = {
                DeletedPhotosStore.add(photoUri)
                photoAdapter.removePhoto(photoUri)
                Toast.makeText(requireContext(), getString(R.string.deleted_message), Toast.LENGTH_SHORT).show()
            }
        )
    }

    private data class PhotoEntry(
        val uri: String,
        val dateTaken: Long
    )
}

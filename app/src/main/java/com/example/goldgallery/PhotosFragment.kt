package com.example.goldgallery

import android.content.ContentUris
import android.os.Bundle
import android.provider.MediaStore
import android.provider.MediaStore.Files.FileColumns
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class PhotosFragment : Fragment() {

    private val favoritePhotos = mutableSetOf<String>()
    private lateinit var photoAdapter: MonthlyPhotoAdapter
    private lateinit var multiSelectActions: LinearLayout
    private lateinit var selectionCountText: TextView
    private lateinit var toggleMultiSelectButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_photos, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.rvPhotos)
        val swipeRefreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.swipeRefreshPhotos)
        multiSelectActions = view.findViewById(R.id.layoutMultiSelectActions)
        selectionCountText = view.findViewById(R.id.tvSelectionCount)
        toggleMultiSelectButton = view.findViewById(R.id.btnToggleMultiSelect)
        val privateButton = view.findViewById<Button>(R.id.btnSelectionPrivate)
        val deleteButton = view.findViewById<Button>(R.id.btnSelectionDelete)
        val cancelButton = view.findViewById<Button>(R.id.btnSelectionCancel)
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
            },
            onPhotoSelectionChange = { updateMultiSelectUi() }
        )

        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (photoAdapter.getItemViewType(position) == MonthlyPhotoAdapter.VIEW_TYPE_HEADER) 3 else 1
            }
        }

        recyclerView.adapter = photoAdapter
        swipeRefreshLayout.setOnRefreshListener {
            val previousVisiblePhotos = getVisiblePhotos()
            val previousVisibleCount = previousVisiblePhotos.size
            val refreshedItems = getPhotoListItems()
            val latestVisiblePhotos = getVisiblePhotos()
            val latestVisibleCount = latestVisiblePhotos.size
            val newlyAddedUris = latestVisiblePhotos.toSet() - previousVisiblePhotos.toSet()
            photoAdapter.updatePhotos(refreshedItems, newlyAddedUris)

            if (latestVisibleCount > previousVisibleCount) {
                val addedCount = latestVisibleCount - previousVisibleCount
                Toast.makeText(
                    requireContext(),
                    getString(R.string.new_images_found_message, addedCount),
                    Toast.LENGTH_SHORT
                ).show()
                recyclerView.smoothScrollToPosition(0)
            } else {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.no_new_images_message),
                    Toast.LENGTH_SHORT
                ).show()
            }
            swipeRefreshLayout.isRefreshing = false
        }

        privateButton.setOnClickListener { moveSelectedToPrivate() }
        deleteButton.setOnClickListener { moveSelectedToDeleted() }
        cancelButton.setOnClickListener { clearSelection() }
        toggleMultiSelectButton.setOnClickListener { toggleMultiSelectMode() }
        updateMultiSelectUi()
    }

    override fun onResume() {
        super.onResume()
        if (::photoAdapter.isInitialized) {
            photoAdapter.updatePhotos(getPhotoListItems())
            updateMultiSelectUi()
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
                items.add(PhotoListItem.Photo(entry.uri, entry.dateTaken, entry.isVideo))
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
        val collectionUri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        val projection = arrayOf(
            FileColumns._ID,
            FileColumns.MEDIA_TYPE,
            FileColumns.DATE_TAKEN
        )
        val selection = "${FileColumns.MEDIA_TYPE} = ? OR ${FileColumns.MEDIA_TYPE} = ?"
        val selectionArgs = arrayOf(
            FileColumns.MEDIA_TYPE_IMAGE.toString(),
            FileColumns.MEDIA_TYPE_VIDEO.toString()
        )
        val sortOrder = "${FileColumns.DATE_TAKEN} DESC"

        val cursor = requireContext().contentResolver.query(
            collectionUri,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(FileColumns._ID)
            val mediaTypeColumn = it.getColumnIndexOrThrow(FileColumns.MEDIA_TYPE)
            val dateTakenColumn = it.getColumnIndexOrThrow(FileColumns.DATE_TAKEN)
            while (it.moveToNext()) {
                val mediaId = it.getLong(idColumn)
                val mediaType = it.getInt(mediaTypeColumn)
                val mediaCollectionUri = when (mediaType) {
                    FileColumns.MEDIA_TYPE_VIDEO -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    else -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                }
                val imageUri = ContentUris.withAppendedId(mediaCollectionUri, mediaId)
                val dateTaken = it.getLong(dateTakenColumn)
                photoEntries.add(
                    PhotoEntry(
                        uri = imageUri.toString(),
                        dateTaken = dateTaken,
                        isVideo = mediaType == FileColumns.MEDIA_TYPE_VIDEO
                    )
                )
            }
        }
        return photoEntries
    }

    private fun updateMultiSelectUi() {
        val selectedCount = photoAdapter.getSelectedPhotos().size
        val isSelectionMode = photoAdapter.isSelectionModeEnabled()
        multiSelectActions.visibility = if (isSelectionMode) View.VISIBLE else View.GONE
        if (isSelectionMode) {
            selectionCountText.text = getString(R.string.selected_count, selectedCount)
        }
        toggleMultiSelectButton.text = if (isSelectionMode) {
            getString(R.string.done)
        } else {
            getString(R.string.multi_select)
        }
    }

    private fun moveSelectedToPrivate() {
        val selectedPhotos = photoAdapter.getSelectedPhotos()
        if (selectedPhotos.isEmpty()) return

        selectedPhotos.forEach { photoUri ->
            PrivatePhotosStore.add(photoUri)
            photoAdapter.removePhoto(photoUri)
        }
        clearSelection()
        Toast.makeText(requireContext(), getString(R.string.set_private_message), Toast.LENGTH_SHORT).show()
    }

    private fun moveSelectedToDeleted() {
        val selectedPhotos = photoAdapter.getSelectedPhotos()
        if (selectedPhotos.isEmpty()) return

        selectedPhotos.forEach { photoUri ->
            DeletedPhotosStore.add(photoUri)
            photoAdapter.removePhoto(photoUri)
        }
        clearSelection()
        Toast.makeText(requireContext(), getString(R.string.deleted_message), Toast.LENGTH_SHORT).show()
    }

    private fun clearSelection() {
        photoAdapter.setSelectionMode(false)
        updateMultiSelectUi()
    }

    private fun toggleMultiSelectMode() {
        photoAdapter.setSelectionMode(!photoAdapter.isSelectionModeEnabled())
        updateMultiSelectUi()
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
        val dateTaken: Long,
        val isVideo: Boolean
    )
}

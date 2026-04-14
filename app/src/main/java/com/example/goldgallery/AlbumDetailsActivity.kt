package com.example.goldgallery

import android.os.Bundle
import android.content.ContentUris
import android.provider.MediaStore
import android.provider.MediaStore.Files.FileColumns
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AlbumDetailsActivity : AppCompatActivity() {
    private val favoritePhotos = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_album_details)

        val albumName = intent.getStringExtra("ALBUM_NAME") ?: ""
        title = albumName // Sets the top bar to the folder name

        val recyclerView = findViewById<RecyclerView>(R.id.rvAlbumPhotos)
        recyclerView.layoutManager = GridLayoutManager(this, 3)

        // Get only photos belonging to this folder
        val filteredPhotos = getPhotosByAlbum(albumName)
        recyclerView.adapter = PhotoAdapter(
            photos = filteredPhotos,
            onPhotoClick = { photoUri, position ->
                startActivity(FullImageActivity.createIntent(this, filteredPhotos, position))
            },
            onPhotoLongClick = { photoUri, anchorView ->
                anchorView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                showPhotoActions(photoUri)
            }
        )
    }

    private fun getPhotosByAlbum(targetBucket: String): List<String> {
        val photoUris = mutableListOf<String>()
        val collectionUri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        val projection = arrayOf(
            FileColumns._ID,
            FileColumns.MEDIA_TYPE,
            "bucket_display_name"
        )
        val selection = "(${FileColumns.MEDIA_TYPE} = ? OR ${FileColumns.MEDIA_TYPE} = ?) AND bucket_display_name = ?"
        val selectionArgs = arrayOf(
            FileColumns.MEDIA_TYPE_IMAGE.toString(),
            FileColumns.MEDIA_TYPE_VIDEO.toString(),
            targetBucket
        )
        val sortOrder = "${FileColumns.DATE_TAKEN} DESC"

        val cursor = contentResolver.query(collectionUri, projection, selection, selectionArgs, sortOrder)


        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(FileColumns._ID)
            val mediaTypeColumn = it.getColumnIndexOrThrow(FileColumns.MEDIA_TYPE)
            while (it.moveToNext()) {
                val mediaId = it.getLong(idColumn)
                val mediaType = it.getInt(mediaTypeColumn)
                val mediaCollectionUri = when (mediaType) {
                    FileColumns.MEDIA_TYPE_VIDEO -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    else -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                }
                val mediaUri = ContentUris.withAppendedId(mediaCollectionUri, mediaId)
                photoUris.add(mediaUri.toString())
            }
        }
        return photoUris
    }

    private fun showPhotoActions(photoUri: String) {
        PhotoActionsOverlay.show(
            host = this,
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
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            },
            onPrivateClick = {
                Toast.makeText(this, getString(R.string.set_private_message), Toast.LENGTH_SHORT).show()
            },
            onDeleteClick = {
                Toast.makeText(this, getString(R.string.delete_message), Toast.LENGTH_SHORT).show()
            }
        )
    }
}

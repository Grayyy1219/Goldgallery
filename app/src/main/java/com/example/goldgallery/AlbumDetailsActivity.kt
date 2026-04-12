package com.example.goldgallery

import android.os.Bundle
import android.content.ContentUris
import android.provider.MediaStore
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
            onPhotoClick = { photoUri ->
                val intent = android.content.Intent(this, FullImageActivity::class.java)
                intent.putExtra("IMAGE_PATH", photoUri)
                startActivity(intent)
            },
            onPhotoLongClick = { photoUri, anchorView ->
                anchorView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                showPhotoActions(photoUri)
            }
        )
    }

    private fun getPhotosByAlbum(targetBucket: String): List<String> {
        val photoUris = mutableListOf<String>()
        val collectionUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        )
        val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"

       val cursor = contentResolver.query(collectionUri, projection, null, null, sortOrder)


        cursor?.use {
           val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val bucketColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            while (it.moveToNext()) {
              val imageId = it.getLong(idColumn)
                val folderName = it.getString(bucketColumn)

                // Only add the photo if it's in the clicked folder
                if (folderName == targetBucket) {
                    val imageUri = ContentUris.withAppendedId(collectionUri, imageId)
                    photoUris.add(imageUri.toString())
                }
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

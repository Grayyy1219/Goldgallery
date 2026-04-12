package com.example.goldgallery

import android.os.Bundle
import android.provider.MediaStore
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

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
            onPhotoClick = { photoPath ->
                val intent = android.content.Intent(this, FullImageActivity::class.java)
                intent.putExtra("IMAGE_PATH", photoPath)
                startActivity(intent)
            },
            onPhotoLongClick = { photoPath, anchorView ->
                anchorView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                showPhotoActions(photoPath)
            }
        )
    }

    private fun getPhotosByAlbum(targetBucket: String): List<String> {
        val photoPaths = mutableListOf<String>()
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.MediaColumns.DATA)
        val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"

        val cursor = contentResolver.query(uri, projection, null, null, sortOrder)

        cursor?.use {
            val columnIndex = it.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
            while (it.moveToNext()) {
                val path = it.getString(columnIndex)
                val folderName = File(path).parentFile?.name

                // Only add the photo if it's in the clicked folder
                if (folderName == targetBucket) {
                    photoPaths.add(path)
                }
            }
        }
        return photoPaths
    }

    private fun showPhotoActions(photoPath: String) {
        PhotoActionsOverlay.show(
            host = this,
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

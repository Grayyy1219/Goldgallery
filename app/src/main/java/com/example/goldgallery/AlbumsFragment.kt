package com.example.goldgallery

import android.os.Bundle
import android.provider.MediaStore
import android.provider.MediaStore.Files.FileColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AlbumsFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_albums, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.rvAlbums)
        recyclerView.layoutManager = GridLayoutManager(context, 2)

        val albumList = getPhoneAlbums()

        // THE FINAL LINK:
        recyclerView.adapter = AlbumAdapter(albumList)
    }

    private fun getPhoneAlbums(): List<AlbumItem> {
        val albumEntries = linkedMapOf<String, AlbumSummary>()
        val uri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        val projection = arrayOf(
            FileColumns._ID,
            FileColumns.MEDIA_TYPE,
            "bucket_id",
            "bucket_display_name"
        )
        val selection = "${FileColumns.MEDIA_TYPE} = ? OR ${FileColumns.MEDIA_TYPE} = ?"
        val selectionArgs = arrayOf(
            FileColumns.MEDIA_TYPE_IMAGE.toString(),
            FileColumns.MEDIA_TYPE_VIDEO.toString()
        )
        val sortOrder = "${FileColumns.DATE_TAKEN} DESC"

        val cursor = requireContext().contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)

        cursor?.use {
            val idIndex = it.getColumnIndexOrThrow(FileColumns._ID)
            val mediaTypeIndex = it.getColumnIndexOrThrow(FileColumns.MEDIA_TYPE)
            val bucketIdIndex = it.getColumnIndexOrThrow("bucket_id")
            val bucketNameIndex = it.getColumnIndexOrThrow("bucket_display_name")
            while (it.moveToNext()) {
                val bucketId = it.getString(bucketIdIndex) ?: continue
                val folderName = it.getString(bucketNameIndex)?.takeIf { name -> name.isNotBlank() } ?: "Unknown"
                val mediaId = it.getLong(idIndex)
                val mediaType = it.getInt(mediaTypeIndex)
                val mediaUri = when (mediaType) {
                    FileColumns.MEDIA_TYPE_VIDEO -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    else -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                }
                val coverUri = android.content.ContentUris.withAppendedId(mediaUri, mediaId).toString()

                val current = albumEntries[bucketId]
                if (current == null) {
                    albumEntries[bucketId] = AlbumSummary(folderName, coverUri, 1)
                } else {
                    albumEntries[bucketId] = current.copy(count = current.count + 1)
                }
            }
        }

        return albumEntries.values.map { album ->
            AlbumItem(album.name, album.coverUri, album.count)
        }.sortedBy { it.name }
    }

    private data class AlbumSummary(
        val name: String,
        val coverUri: String,
        val count: Int
    )
}

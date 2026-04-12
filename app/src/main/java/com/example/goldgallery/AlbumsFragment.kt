package com.example.goldgallery

import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

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
        val albumsMap = mutableMapOf<String, MutableList<String>>()
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Images.Media.DATA)

        val cursor = requireContext().contentResolver.query(uri, projection, null, null, null)

        cursor?.use {
            val dataIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            while (it.moveToNext()) {
                val path = it.getString(dataIndex)
                val file = File(path)
                val folderName = file.parentFile?.name ?: "Unknown"

                if (!albumsMap.containsKey(folderName)) {
                    albumsMap[folderName] = mutableListOf()
                }
                albumsMap[folderName]?.add(path)
            }
        }

        return albumsMap.map { (name, photos) ->
            AlbumItem(name, photos[0], photos.size)
        }.sortedBy { it.name }
    }
}
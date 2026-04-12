package com.example.goldgallery

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class DeletedFragment : Fragment() {

    private lateinit var deletedAdapter: PhotoAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_deleted, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.rvDeletedPhotos)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)

        deletedAdapter = PhotoAdapter(
            photos = DeletedPhotosStore.getAll(),
            onPhotoClick = { photoUri ->
                val intent = Intent(requireContext(), FullImageActivity::class.java)
                intent.putExtra("IMAGE_PATH", photoUri)
                startActivity(intent)
            },
            onPhotoLongClick = { _, _ ->
                // No long-click actions in Deleted.
            }
        )

        recyclerView.adapter = deletedAdapter
    }

    override fun onResume() {
        super.onResume()
        if (::deletedAdapter.isInitialized) {
            deletedAdapter.updatePhotos(DeletedPhotosStore.getAll())
        }
    }
}

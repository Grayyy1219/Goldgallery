package com.example.goldgallery

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class PhotoAdapter(
    private val photos: List<String>,
    private val onPhotoClick: (String) -> Unit,
    private val onPhotoLongClick: (String, View) -> Unit
) : RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder>() {

    class PhotoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.ivPhoto)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_photo, parent, false)
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val photoPath = photos[position]

        // Load the thumbnail
        Glide.with(holder.itemView.context)
            .load(photoPath)
            .into(holder.imageView)

        holder.itemView.setOnClickListener { onPhotoClick(photoPath) }

        holder.itemView.setOnLongClickListener {
            onPhotoLongClick(photoPath, holder.itemView)
            true
        }
    }

    override fun getItemCount() = photos.size
}

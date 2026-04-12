package com.example.goldgallery

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class PhotoAdapter(private val photos: List<String>) : RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder>() {

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

        // HANDLE THE CLICK HERE
        holder.itemView.setOnClickListener {
            val intent = android.content.Intent(holder.itemView.context, FullImageActivity::class.java)
            intent.putExtra("IMAGE_PATH", photoPath)
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount() = photos.size
}
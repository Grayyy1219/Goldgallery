package com.example.goldgallery

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import jp.wasabeef.glide.transformations.BlurTransformation

class PhotoAdapter(
    photos: List<String>,
    private val onPhotoClick: (String) -> Unit,
    private val onPhotoLongClick: (String, View) -> Unit
) : RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder>() {

    private val photos = photos.toMutableList()
    private var shouldBlurPhotos = false

    class PhotoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.ivPhoto)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_photo, parent, false)
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val photoPath = photos[position]
        val request = Glide.with(holder.itemView.context).load(photoPath)
        if (shouldBlurPhotos) {
            request.apply(RequestOptions.bitmapTransform(BlurTransformation(20, 3)))
        }
        request.into(holder.imageView)

        if (shouldBlurPhotos) {
            holder.itemView.setOnClickListener(null)
            holder.itemView.setOnLongClickListener(null)
        } else {
            holder.itemView.setOnClickListener { onPhotoClick(photoPath) }
            holder.itemView.setOnLongClickListener {
                onPhotoLongClick(photoPath, holder.itemView)
                true
            }
        }
    }

    override fun getItemCount() = photos.size

    fun updatePhotos(newPhotos: List<String>) {
        photos.clear()
        photos.addAll(newPhotos)
        notifyDataSetChanged()
    }

    fun removePhoto(photoUri: String): Boolean {
        val index = photos.indexOf(photoUri)
        if (index == -1) return false
        photos.removeAt(index)
        notifyItemRemoved(index)
        return true
    }

    fun setBlurred(blurred: Boolean) {
        if (shouldBlurPhotos == blurred) return
        shouldBlurPhotos = blurred
        notifyDataSetChanged()
    }
}

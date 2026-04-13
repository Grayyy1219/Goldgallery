package com.example.goldgallery

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import jp.wasabeef.glide.transformations.BlurTransformation

class MonthlyPhotoAdapter(
    photos: List<PhotoListItem>,
    private val onPhotoClick: (String, Int) -> Unit,
    private val onPhotoLongClick: (String, View) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val VIEW_TYPE_HEADER = 0
        const val VIEW_TYPE_PHOTO = 1
    }

    private val items = photos.toMutableList()
    private var shouldBlurPhotos = false

    class PhotoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.ivPhoto)
    }

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val monthText: TextView = view.findViewById(R.id.tvMonthHeader)
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is PhotoListItem.MonthHeader -> VIEW_TYPE_HEADER
            is PhotoListItem.Photo -> VIEW_TYPE_PHOTO
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_photo_month_header, parent, false)
                HeaderViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_photo, parent, false)
                PhotoViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is PhotoListItem.MonthHeader -> bindHeader(holder as HeaderViewHolder, item)
            is PhotoListItem.Photo -> bindPhoto(holder as PhotoViewHolder, item, position)
        }
    }

    private fun bindHeader(holder: HeaderViewHolder, item: PhotoListItem.MonthHeader) {
        holder.monthText.text = item.title
        holder.itemView.setOnClickListener(null)
        holder.itemView.setOnLongClickListener(null)
    }

    private fun bindPhoto(holder: PhotoViewHolder, item: PhotoListItem.Photo, adapterPosition: Int) {
        val request = Glide.with(holder.itemView.context).load(item.uri)
        if (shouldBlurPhotos) {
            request.apply(RequestOptions.bitmapTransform(BlurTransformation(20, 3)))
        }
        request.into(holder.imageView)

        if (shouldBlurPhotos) {
            holder.itemView.setOnClickListener(null)
            holder.itemView.setOnLongClickListener(null)
        } else {
            holder.itemView.setOnClickListener {
                onPhotoClick(item.uri, getPhotoIndex(adapterPosition))
            }
            holder.itemView.setOnLongClickListener {
                onPhotoLongClick(item.uri, holder.itemView)
                true
            }
        }
    }

    override fun getItemCount() = items.size

    fun updatePhotos(newItems: List<PhotoListItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun removePhoto(photoUri: String): Boolean {
        val index = items.indexOfFirst { it is PhotoListItem.Photo && it.uri == photoUri }
        if (index == -1) return false

        items.removeAt(index)

        if (index > 0 && index < items.size) {
            val previous = items[index - 1]
            val current = items[index]
            if (previous is PhotoListItem.MonthHeader && current is PhotoListItem.MonthHeader) {
                items.removeAt(index)
            }
        } else if (index > 0 && index == items.size) {
            val previous = items[index - 1]
            if (previous is PhotoListItem.MonthHeader) {
                items.removeAt(index - 1)
            }
        }

        notifyDataSetChanged()
        return true
    }

    fun setBlurred(blurred: Boolean) {
        if (shouldBlurPhotos == blurred) return
        shouldBlurPhotos = blurred
        notifyDataSetChanged()
    }

    private fun getPhotoIndex(adapterPosition: Int): Int {
        var count = 0
        for (i in 0..adapterPosition) {
            if (items[i] is PhotoListItem.Photo) {
                count++
            }
        }
        return count - 1
    }
}

sealed class PhotoListItem {
    data class MonthHeader(val title: String) : PhotoListItem()
    data class Photo(val uri: String, val dateTaken: Long) : PhotoListItem()
}

package com.example.goldgallery

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import jp.wasabeef.glide.transformations.BlurTransformation

class MonthlyPhotoAdapter(
    photos: List<PhotoListItem>,
    private val onPhotoClick: (String, Int) -> Unit,
    private val onPhotoLongClick: (String, View) -> Unit,
    private val onPhotoSelectionChange: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val VIEW_TYPE_HEADER = 0
        const val VIEW_TYPE_PHOTO = 1
    }

    private val items = photos.toMutableList()
    private var shouldBlurPhotos = false
    private val selectedPhotos = mutableSetOf<String>()
    private var isSelectionMode = false
    private val recentlyAddedPhotos = mutableSetOf<String>()

    class PhotoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.ivPhoto)
        val videoIndicator: View = view.findViewById(R.id.tvVideoIndicator)
        val selectionOverlay: View = view.findViewById(R.id.viewSelectionOverlay)
        val selectionCheck: View = view.findViewById(R.id.tvSelectionCheck)
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
        holder.videoIndicator.visibility = if (item.isVideo) View.VISIBLE else View.GONE
        val isSelected = selectedPhotos.contains(item.uri)
        holder.selectionOverlay.visibility = if (isSelected) View.VISIBLE else View.GONE
        holder.selectionCheck.visibility = if (isSelectionMode) View.VISIBLE else View.GONE
        applyNewItemAnimationIfNeeded(holder, item.uri)

        if (shouldBlurPhotos) {
            holder.itemView.setOnClickListener(null)
            holder.itemView.setOnLongClickListener(null)
        } else {
            holder.itemView.setOnClickListener {
                if (isSelectionMode) {
                    toggleSelection(item.uri)
                    onPhotoSelectionChange()
                } else {
                    onPhotoClick(item.uri, getPhotoIndex(adapterPosition))
                }
            }
            holder.itemView.setOnLongClickListener {
                onPhotoLongClick(item.uri, holder.itemView)
                true
            }
        }
    }

    override fun getItemCount() = items.size

    fun updatePhotos(newItems: List<PhotoListItem>, newlyAddedUris: Set<String> = emptySet()) {
        recentlyAddedPhotos.clear()
        recentlyAddedPhotos.addAll(newlyAddedUris)
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun removePhoto(photoUri: String): Boolean {
        val index = items.indexOfFirst { it is PhotoListItem.Photo && it.uri == photoUri }
        if (index == -1) return false

        selectedPhotos.remove(photoUri)
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

    fun setSelectionMode(enabled: Boolean) {
        if (isSelectionMode == enabled) return
        isSelectionMode = enabled
        if (!enabled) {
            selectedPhotos.clear()
        }
        notifyDataSetChanged()
    }

    fun isSelectionModeEnabled(): Boolean = isSelectionMode

    fun getSelectedPhotos(): Set<String> = selectedPhotos.toSet()

    private fun toggleSelection(photoUri: String) {
        if (selectedPhotos.contains(photoUri)) {
            selectedPhotos.remove(photoUri)
        } else {
            selectedPhotos.add(photoUri)
        }
        if (selectedPhotos.isEmpty()) {
            isSelectionMode = false
        }
        onPhotoSelectionChange()
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

    private fun applyNewItemAnimationIfNeeded(holder: PhotoViewHolder, photoUri: String) {
        if (!recentlyAddedPhotos.remove(photoUri)) {
            holder.itemView.alpha = 1f
            holder.itemView.scaleX = 1f
            holder.itemView.scaleY = 1f
            return
        }

        holder.itemView.alpha = 0.35f
        holder.itemView.scaleX = 0.9f
        holder.itemView.scaleY = 0.9f
        holder.itemView.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(280L)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }
}

sealed class PhotoListItem {
    data class MonthHeader(val title: String) : PhotoListItem()
    data class Photo(val uri: String, val dateTaken: Long, val isVideo: Boolean) : PhotoListItem()
}

package com.example.goldgallery

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class AlbumAdapter(private val albums: List<AlbumItem>) : RecyclerView.Adapter<AlbumAdapter.AlbumViewHolder>() {

    class AlbumViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivCover: ImageView = view.findViewById(R.id.ivAlbumCover)
        val tvName: TextView = view.findViewById(R.id.tvAlbumName)
        val tvCount: TextView = view.findViewById(R.id.tvPhotoCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_album, parent, false)
        return AlbumViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlbumViewHolder, position: Int) {
        val album = albums[position]
        holder.tvName.text = album.name
        holder.tvCount.text = "${album.photoCount} Items"

        Glide.with(holder.itemView.context)
            .load(album.coverPath)
            .into(holder.ivCover)

        // Handlers for clicks
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, AlbumDetailsActivity::class.java)
            intent.putExtra("ALBUM_NAME", album.name)
            context.startActivity(intent)
        }
    }

    override fun getItemCount() = albums.size
}

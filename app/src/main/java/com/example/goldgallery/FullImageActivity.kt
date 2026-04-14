package com.example.goldgallery

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.VideoView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide

class FullImageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_full_image)

        val rootView = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val imagePaths = intent.getStringArrayListExtra(EXTRA_IMAGE_PATHS)
            ?.takeIf { it.isNotEmpty() }
            ?: intent.getStringExtra(EXTRA_IMAGE_PATH)
                ?.let { arrayListOf(it) }
            ?: arrayListOf()

        if (imagePaths.isEmpty()) {
            finish()
            return
        }

        val startIndex = intent.getIntExtra(EXTRA_START_INDEX, 0)
            .coerceIn(0, imagePaths.lastIndex)

        val viewPager = findViewById<ViewPager2>(R.id.vpFullImage)
        viewPager.adapter = FullImagePagerAdapter(imagePaths) { finish() }
        viewPager.setCurrentItem(startIndex, false)
    }

    private class FullImagePagerAdapter(
        private val photos: List<String>,
        private val onPhotoTap: () -> Unit
    ) : RecyclerView.Adapter<FullImagePagerAdapter.FullImageViewHolder>() {

        class FullImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val imageView: ImageView = view.findViewById(R.id.ivFullImage)
            val videoView: VideoView = view.findViewById(R.id.vvFullVideo)
            val videoHint: TextView = view.findViewById(R.id.tvVideoHint)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FullImageViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_full_image_page, parent, false)
            return FullImageViewHolder(view)
        }

        override fun onBindViewHolder(holder: FullImageViewHolder, position: Int) {
            val mediaUri = photos[position]
            if (isVideoUri(holder.itemView.context, mediaUri)) {
                holder.imageView.visibility = View.GONE
                holder.videoView.visibility = View.VISIBLE
                holder.videoHint.visibility = View.VISIBLE

                holder.videoView.setVideoURI(Uri.parse(mediaUri))
                holder.videoView.setOnPreparedListener { mediaPlayer ->
                    mediaPlayer.isLooping = true
                    holder.videoView.start()
                }
                holder.videoView.setOnClickListener {
                    if (holder.videoView.isPlaying) {
                        holder.videoView.pause()
                    } else {
                        holder.videoView.start()
                    }
                }
                holder.imageView.setOnClickListener(null)
            } else {
                holder.videoView.stopPlayback()
                holder.videoView.visibility = View.GONE
                holder.videoHint.visibility = View.GONE
                holder.imageView.visibility = View.VISIBLE

                Glide.with(holder.itemView.context)
                    .load(mediaUri)
                    .into(holder.imageView)

                holder.imageView.setOnClickListener { onPhotoTap() }
            }
        }

        override fun onViewRecycled(holder: FullImageViewHolder) {
            holder.videoView.stopPlayback()
            holder.videoView.setOnPreparedListener(null)
            holder.videoView.setOnClickListener(null)
            holder.imageView.setOnClickListener(null)
            super.onViewRecycled(holder)
        }

        private fun isVideoUri(context: Context, mediaUri: String): Boolean {
            val uri = Uri.parse(mediaUri)
            val mimeType = context.contentResolver.getType(uri)
            if (mimeType != null) {
                return mimeType.startsWith("video/")
            }
            val normalized = mediaUri.lowercase()
            return normalized.endsWith(".mp4") || normalized.endsWith(".mkv") || normalized.endsWith(".webm") || normalized.endsWith(".3gp")
        }

        override fun getItemCount() = photos.size
    }

    companion object {
        private const val EXTRA_IMAGE_PATH = "IMAGE_PATH"
        private const val EXTRA_IMAGE_PATHS = "IMAGE_PATHS"
        private const val EXTRA_START_INDEX = "START_INDEX"

        fun createIntent(context: Context, imagePaths: List<String>, startIndex: Int): Intent {
            return Intent(context, FullImageActivity::class.java).apply {
                putStringArrayListExtra(EXTRA_IMAGE_PATHS, ArrayList(imagePaths))
                putExtra(EXTRA_START_INDEX, startIndex)
            }
        }
    }
}

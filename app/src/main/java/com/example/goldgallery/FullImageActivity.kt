package com.example.goldgallery

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
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
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FullImageViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_full_image_page, parent, false)
            return FullImageViewHolder(view)
        }

        override fun onBindViewHolder(holder: FullImageViewHolder, position: Int) {
            Glide.with(holder.itemView.context)
                .load(photos[position])
                .into(holder.imageView)

            holder.imageView.setOnClickListener { onPhotoTap() }
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

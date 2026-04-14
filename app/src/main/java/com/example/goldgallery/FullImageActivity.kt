package com.example.goldgallery

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.VideoView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import kotlin.math.max
import kotlin.math.min

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
        viewPager.adapter = FullImagePagerAdapter(
            photos = imagePaths,
            onPhotoTap = { finish() },
            onScaleStateChange = { isZooming -> viewPager.isUserInputEnabled = !isZooming }
        )
        viewPager.setCurrentItem(startIndex, false)
    }

    private class FullImagePagerAdapter(
        private val photos: List<String>,
        private val onPhotoTap: () -> Unit,
        private val onScaleStateChange: (Boolean) -> Unit
    ) : RecyclerView.Adapter<FullImagePagerAdapter.FullImageViewHolder>() {

        class FullImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val imageView: ImageView = view.findViewById(R.id.ivFullImage)
            val videoView: VideoView = view.findViewById(R.id.vvFullVideo)
            val videoHint: TextView = view.findViewById(R.id.tvVideoHint)
            val videoControls: LinearLayout = view.findViewById(R.id.videoControls)
            val rewindButton: ImageButton = view.findViewById(R.id.btnRewind)
            val playPauseButton: ImageButton = view.findViewById(R.id.btnPlayPause)
            val forwardButton: ImageButton = view.findViewById(R.id.btnForward)
            val seekBar: SeekBar = view.findViewById(R.id.seekVideo)
            val timeLabel: TextView = view.findViewById(R.id.tvVideoTime)
            var progressUpdater: Runnable? = null
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FullImageViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_full_image_page, parent, false)
            return FullImageViewHolder(view)
        }

        override fun onBindViewHolder(holder: FullImageViewHolder, position: Int) {
            val mediaUri = photos[position]
            if (isVideoUri(holder.itemView.context, mediaUri)) {
                bindVideo(holder, mediaUri)
            } else {
                bindImage(holder, mediaUri)
            }
        }

        private fun bindVideo(holder: FullImageViewHolder, mediaUri: String) {
            holder.imageView.visibility = View.GONE
            holder.videoView.visibility = View.VISIBLE
            holder.videoHint.visibility = View.VISIBLE
            holder.videoControls.visibility = View.VISIBLE

            resetTransform(holder.videoView)

            holder.videoView.setVideoURI(Uri.parse(mediaUri))
            holder.videoView.setOnPreparedListener { mediaPlayer ->
                mediaPlayer.isLooping = true
                holder.seekBar.progress = 0
                holder.timeLabel.text = formatTime(0) + " / " + formatTime(holder.videoView.duration)
                holder.videoView.start()
                updatePlayPauseIcon(holder)
                startProgressUpdates(holder)
            }
            holder.videoView.setOnCompletionListener {
                updatePlayPauseIcon(holder)
            }

            holder.rewindButton.setOnClickListener {
                val seekPosition = max(holder.videoView.currentPosition - SEEK_INTERVAL_MS, 0)
                holder.videoView.seekTo(seekPosition)
                syncSeekBarPosition(holder)
            }

            holder.forwardButton.setOnClickListener {
                val seekPosition = min(holder.videoView.currentPosition + SEEK_INTERVAL_MS, holder.videoView.duration)
                holder.videoView.seekTo(seekPosition)
                syncSeekBarPosition(holder)
            }

            holder.playPauseButton.setOnClickListener {
                if (holder.videoView.isPlaying) {
                    holder.videoView.pause()
                } else {
                    holder.videoView.start()
                }
                updatePlayPauseIcon(holder)
            }
            holder.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    if (!fromUser) return
                    val duration = holder.videoView.duration
                    if (duration > 0) {
                        val seekToPosition = (duration * (progress / SEEK_BAR_MAX.toFloat())).toInt()
                        holder.videoView.seekTo(seekToPosition)
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) = Unit

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    syncSeekBarPosition(holder)
                }
            })

            attachZoomSupport(
                targetView = holder.videoView,
                onSingleTap = {
                    if (holder.videoView.isPlaying) holder.videoView.pause() else holder.videoView.start()
                    updatePlayPauseIcon(holder)
                }
            )
            holder.imageView.setOnTouchListener(null)
            holder.imageView.setOnClickListener(null)
        }

        private fun bindImage(holder: FullImageViewHolder, mediaUri: String) {
            holder.videoView.stopPlayback()
            holder.videoView.visibility = View.GONE
            holder.videoHint.visibility = View.GONE
            holder.videoControls.visibility = View.GONE
            holder.imageView.visibility = View.VISIBLE

            resetTransform(holder.imageView)

            Glide.with(holder.itemView.context)
                .load(mediaUri)
                .into(holder.imageView)

            attachZoomSupport(
                targetView = holder.imageView,
                onSingleTap = { onPhotoTap() }
            )
            holder.videoView.setOnTouchListener(null)
            holder.seekBar.setOnSeekBarChangeListener(null)
            stopProgressUpdates(holder)
            holder.timeLabel.text = "00:00 / 00:00"
        }

        private fun updatePlayPauseIcon(holder: FullImageViewHolder) {
            holder.playPauseButton.setImageResource(
                if (holder.videoView.isPlaying) android.R.drawable.ic_media_pause
                else android.R.drawable.ic_media_play
            )
        }

        private fun startProgressUpdates(holder: FullImageViewHolder) {
            stopProgressUpdates(holder)
            val updateRunnable = object : Runnable {
                override fun run() {
                    syncSeekBarPosition(holder)
                    holder.videoView.postDelayed(this, PROGRESS_UPDATE_INTERVAL_MS)
                }
            }
            holder.progressUpdater = updateRunnable
            holder.videoView.post(updateRunnable)
        }

        private fun stopProgressUpdates(holder: FullImageViewHolder) {
            holder.progressUpdater?.let { holder.videoView.removeCallbacks(it) }
            holder.progressUpdater = null
        }

        private fun syncSeekBarPosition(holder: FullImageViewHolder) {
            val duration = holder.videoView.duration
            if (duration <= 0) {
                holder.seekBar.progress = 0
                holder.timeLabel.text = "00:00 / 00:00"
                return
            }
            val currentPosition = holder.videoView.currentPosition
            val progress = ((currentPosition / duration.toFloat()) * SEEK_BAR_MAX).toInt()
            holder.seekBar.progress = progress.coerceIn(0, SEEK_BAR_MAX)
            holder.timeLabel.text = "${formatTime(currentPosition)} / ${formatTime(duration)}"
        }

        private fun formatTime(timeMs: Int): String {
            val totalSeconds = timeMs.coerceAtLeast(0) / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%02d:%02d".format(minutes, seconds)
        }

        private fun attachZoomSupport(targetView: View, onSingleTap: () -> Unit) {
            val zoomState = ZoomState()
            val scaleDetector = ScaleGestureDetector(targetView.context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    val newScale = (zoomState.scale * detector.scaleFactor).coerceIn(MIN_SCALE, MAX_SCALE)
                    zoomState.scale = newScale
                    applyTransform(targetView, zoomState)
                    onScaleStateChange(zoomState.scale > MIN_SCALE)
                    return true
                }
            })

            val gestureDetector = GestureDetector(targetView.context, object : GestureDetector.SimpleOnGestureListener() {
                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    onSingleTap()
                    return true
                }

                override fun onDoubleTap(e: MotionEvent): Boolean {
                    zoomState.scale = if (zoomState.scale > MIN_SCALE) MIN_SCALE else 2f
                    zoomState.translationX = 0f
                    zoomState.translationY = 0f
                    applyTransform(targetView, zoomState)
                    onScaleStateChange(zoomState.scale > MIN_SCALE)
                    return true
                }

                override fun onScroll(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    distanceX: Float,
                    distanceY: Float
                ): Boolean {
                    if (zoomState.scale <= MIN_SCALE) return false
                    zoomState.translationX -= distanceX
                    zoomState.translationY -= distanceY
                    applyTransform(targetView, zoomState)
                    return true
                }
            })

            targetView.setOnTouchListener { _, event ->
                val scaleHandled = scaleDetector.onTouchEvent(event)
                val gestureHandled = gestureDetector.onTouchEvent(event)
                if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
                    if (zoomState.scale <= MIN_SCALE) {
                        zoomState.translationX = 0f
                        zoomState.translationY = 0f
                        applyTransform(targetView, zoomState)
                        onScaleStateChange(false)
                    }
                }
                scaleHandled || gestureHandled || zoomState.scale > MIN_SCALE
            }
        }

        private fun applyTransform(targetView: View, zoomState: ZoomState) {
            targetView.scaleX = zoomState.scale
            targetView.scaleY = zoomState.scale
            targetView.translationX = zoomState.translationX
            targetView.translationY = zoomState.translationY
        }

        private fun resetTransform(targetView: View) {
            targetView.scaleX = MIN_SCALE
            targetView.scaleY = MIN_SCALE
            targetView.translationX = 0f
            targetView.translationY = 0f
        }

        override fun onViewRecycled(holder: FullImageViewHolder) {
            holder.videoView.stopPlayback()
            holder.videoView.setOnPreparedListener(null)
            holder.videoView.setOnCompletionListener(null)
            holder.videoView.setOnTouchListener(null)
            holder.imageView.setOnTouchListener(null)
            holder.rewindButton.setOnClickListener(null)
            holder.playPauseButton.setOnClickListener(null)
            holder.forwardButton.setOnClickListener(null)
            holder.seekBar.setOnSeekBarChangeListener(null)
            stopProgressUpdates(holder)
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

        private data class ZoomState(
            var scale: Float = MIN_SCALE,
            var translationX: Float = 0f,
            var translationY: Float = 0f
        )

        companion object {
            private const val SEEK_INTERVAL_MS = 10_000
            private const val PROGRESS_UPDATE_INTERVAL_MS = 300L
            private const val SEEK_BAR_MAX = 1000
            private const val MIN_SCALE = 1f
            private const val MAX_SCALE = 4f
        }
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

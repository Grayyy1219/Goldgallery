package com.example.goldgallery

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.core.net.toUri
import com.bumptech.glide.Glide
import java.io.File

object PhotoActionsOverlay {

    fun show(
        host: androidx.fragment.app.FragmentActivity,
        photoPath: String,
        isFavorite: Boolean,
        onFavoriteClick: () -> Unit,
        onPrivateClick: () -> Unit,
        onDeleteClick: () -> Unit
    ) {
        val dialog = Dialog(host, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setContentView(R.layout.dialog_photo_actions_overlay)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )
        dialog.window?.setDimAmount(0.18f)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            dialog.window?.attributes = dialog.window?.attributes?.apply {
                blurBehindRadius = 40
            }
        }

        val photoFile = File(photoPath)
        val sizeKb = if (photoFile.exists()) photoFile.length() / 1024 else 0L

        val preview = dialog.findViewById<ImageView>(R.id.ivOverlayPreview)
        val tvName = dialog.findViewById<TextView>(R.id.tvOverlayPhotoName)
        val tvDetails = dialog.findViewById<TextView>(R.id.tvOverlayPhotoDetails)
        val rowFavorite = dialog.findViewById<TextView>(R.id.rowFavorite)
        val rowPrivate = dialog.findViewById<TextView>(R.id.rowSetPrivate)
        val rowDelete = dialog.findViewById<TextView>(R.id.rowDelete)
        val backdrop = dialog.findViewById<android.view.View>(R.id.overlayBackdrop)

        Glide.with(host)
            .load(photoPath.toUri())
            .into(preview)

        tvName.text = photoFile.name
        tvDetails.text = host.getString(R.string.photo_details_template, sizeKb, photoPath)
        rowFavorite.text = if (isFavorite) {
            host.getString(R.string.unfavorite)
        } else {
            host.getString(R.string.favorite)
        }

        backdrop.setOnClickListener { dialog.dismiss() }

        rowFavorite.setOnClickListener {
            onFavoriteClick()
            dialog.dismiss()
        }
        rowPrivate.setOnClickListener {
            onPrivateClick()
            dialog.dismiss()
        }
        rowDelete.setOnClickListener {
            onDeleteClick()
            dialog.dismiss()
        }

        dialog.show()
    }
}

package com.example.goldgallery

import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class DeletedFragment : Fragment() {

    private lateinit var deletedAdapter: PhotoAdapter
    private var pendingPermanentDeleteUri: String? = null

    private val recoverableDeleteLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            val photoUri = pendingPermanentDeleteUri
            pendingPermanentDeleteUri = null

            if (result.resultCode == Activity.RESULT_OK && photoUri != null) {
                permanentlyDeletePhoto(photoUri, allowRecovery = false)
            } else {
                Toast.makeText(requireContext(), getString(R.string.photo_permanent_delete_failed_message), Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_deleted, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.rvDeletedPhotos)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)

        deletedAdapter = PhotoAdapter(
            photos = DeletedPhotosStore.getAll(),
            onPhotoClick = { photoUri ->
                val intent = Intent(requireContext(), FullImageActivity::class.java)
                intent.putExtra("IMAGE_PATH", photoUri)
                startActivity(intent)
            },
            onPhotoLongClick = { photoUri, _ ->
                showDeletedPhotoActions(photoUri)
            }
        )

        recyclerView.adapter = deletedAdapter
    }

    override fun onResume() {
        super.onResume()
        if (::deletedAdapter.isInitialized) {
            deletedAdapter.updatePhotos(DeletedPhotosStore.getAll())
        }
    }

    private fun showDeletedPhotoActions(photoUri: String) {
        val options = arrayOf(
            getString(R.string.restore_photo),
            getString(R.string.permanently_delete_photo)
        )

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.deleted_photo_actions_title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> restorePhoto(photoUri)
                    1 -> permanentlyDeletePhoto(photoUri)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun restorePhoto(photoUri: String) {
        if (DeletedPhotosStore.remove(photoUri)) {
            deletedAdapter.removePhoto(photoUri)
            Toast.makeText(requireContext(), getString(R.string.photo_restored_message), Toast.LENGTH_SHORT).show()
        }
    }

    private fun permanentlyDeletePhoto(photoUri: String, allowRecovery: Boolean = true) {
        val rowsDeleted = try {
            requireContext().contentResolver.delete(photoUri.toUri(), null, null)
        } catch (securityException: SecurityException) {
            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                allowRecovery &&
                securityException is RecoverableSecurityException
            ) {
                pendingPermanentDeleteUri = photoUri
                val request = IntentSenderRequest.Builder(
                    securityException.userAction.actionIntent.intentSender
                ).build()
                recoverableDeleteLauncher.launch(request)
            } else {
                Toast.makeText(requireContext(), getString(R.string.photo_permanent_delete_failed_message), Toast.LENGTH_SHORT).show()
            }
            return
        }

        if (rowsDeleted > 0) {
            DeletedPhotosStore.remove(photoUri)
            deletedAdapter.removePhoto(photoUri)
            Toast.makeText(requireContext(), getString(R.string.photo_permanently_deleted_message), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), getString(R.string.photo_permanent_delete_failed_message), Toast.LENGTH_SHORT).show()
        }
    }
}

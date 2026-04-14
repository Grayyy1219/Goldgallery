package com.example.goldgallery

import android.os.Bundle
import android.os.StatFs
import android.text.format.Formatter
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Theme now follows the device setting (DayNight default).

        bindStorageIndicator(view)
    }

    private fun bindStorageIndicator(view: View) {
        val summaryView = view.findViewById<TextView>(R.id.textStorageSummary)
        val progressView = view.findViewById<ProgressBar>(R.id.progressStorage)

        val statFs = StatFs(requireContext().filesDir.absolutePath)
        val totalBytes = statFs.totalBytes.coerceAtLeast(1L)
        val availableBytes = statFs.availableBytes.coerceAtLeast(0L)
        val usedBytes = (totalBytes - availableBytes).coerceAtLeast(0L)
        val usedPercent = ((usedBytes * 100L) / totalBytes).toInt().coerceIn(0, 100)

        summaryView.text = getString(
            R.string.storage_limit_summary,
            Formatter.formatFileSize(requireContext(), usedBytes),
            Formatter.formatFileSize(requireContext(), totalBytes),
            usedPercent
        )
        progressView.progress = usedPercent
    }
}

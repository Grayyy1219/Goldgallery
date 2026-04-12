package com.example.goldgallery

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Dark mode logic has been removed to restore app stability
        // and ensure click listeners in other fragments remain active.
    }
}
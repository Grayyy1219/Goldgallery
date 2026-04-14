package com.example.goldgallery


import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AppContextProvider.init(applicationContext)
        setContentView(R.layout.activity_main)

        // 1. Fix System Bar Padding
        findViewById<android.view.View>(R.id.main)?.let { view ->
            ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        }

        // 2. Permissions
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(
                arrayOf(
                    android.Manifest.permission.READ_MEDIA_IMAGES,
                    android.Manifest.permission.READ_MEDIA_VIDEO
                ),
                101
            )
        } else {
            requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), 101)
        }

        // 3. Navigation
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // Load Photos by default
        if (savedInstanceState == null) {
            loadFragment(R.id.nav_photos)
        }

        bottomNav.setOnItemSelectedListener { item ->
            loadFragment(item.itemId)
            true
        }
    }

    private fun loadFragment(itemId: Int) {
        val fragment = when (itemId) {
            R.id.nav_photos -> PhotosFragment()
            R.id.nav_albums -> AlbumsFragment()
            R.id.nav_private -> PrivateFragment()
            R.id.nav_deleted -> DeletedFragment()
            R.id.nav_settings -> SettingsFragment()
            else -> PhotosFragment()
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .commit()
    }
}

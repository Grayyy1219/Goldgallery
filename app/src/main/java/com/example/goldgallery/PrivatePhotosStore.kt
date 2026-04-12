package com.example.goldgallery

import android.content.Context

object PrivatePhotosStore {
    private const val PREFS_NAME = "private_photos_store"
    private const val KEY_PRIVATE_SET = "key_private_set"

    private val privatePhotoUris = linkedSetOf<String>()
    private var isLoaded = false

    fun add(uri: String) {
        ensureLoaded()
        privatePhotoUris.add(uri)
        persist()
    }

    fun remove(uri: String): Boolean {
        ensureLoaded()
        val removed = privatePhotoUris.remove(uri)
        if (removed) {
            persist()
        }
        return removed
    }

    fun contains(uri: String): Boolean {
        ensureLoaded()
        return privatePhotoUris.contains(uri)
    }

    fun getAll(): List<String> {
        ensureLoaded()
        return privatePhotoUris.toList()
    }

    private fun ensureLoaded() {
        if (isLoaded) return
        val stored = prefs().getStringSet(KEY_PRIVATE_SET, emptySet()) ?: emptySet()
        privatePhotoUris.clear()
        privatePhotoUris.addAll(stored)
        isLoaded = true
    }

    private fun persist() {
        prefs().edit()
            .putStringSet(KEY_PRIVATE_SET, privatePhotoUris.toSet())
            .apply()
    }

    private fun prefs() =
        AppContextProvider.get().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}

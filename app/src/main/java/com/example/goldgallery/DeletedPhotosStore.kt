package com.example.goldgallery

object DeletedPhotosStore {
    private val deletedPhotoUris = linkedSetOf<String>()

    fun add(uri: String) {
        deletedPhotoUris.add(uri)
    }

    fun contains(uri: String): Boolean = deletedPhotoUris.contains(uri)

    fun remove(uri: String): Boolean = deletedPhotoUris.remove(uri)

    fun getAll(): List<String> = deletedPhotoUris.toList()
}

package com.example.goldgallery

import android.content.Context

object AppContextProvider {
    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun get(): Context {
        check(::appContext.isInitialized) { "AppContextProvider is not initialized." }
        return appContext
    }
}

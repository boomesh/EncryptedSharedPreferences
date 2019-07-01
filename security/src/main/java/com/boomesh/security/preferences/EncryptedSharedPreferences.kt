package com.boomesh.security.preferences

import android.content.Context
import android.content.SharedPreferences
import com.boomesh.security.di.MainModule

/**
 * THIS SERVES AS THE ENTRY POINT FOR THE LIBRARY.
 * IT IS BEING IGNORED FOR UNIT TEST COVERAGE.
 * DO NOT ADD ANYMORE METHODS.
 */
object EncryptedSharedPreferences {
    private const val DEFAULT_FILE = "default"
    fun create(
        context: Context,
        fileName: String = DEFAULT_FILE
    ): SharedPreferences {
        val mainModule = MainModule(context)
        return mainModule.providesEncryptedSharedPreferences(fileName)
    }
}
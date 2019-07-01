package com.boomesh.security.di

import android.content.Context
import android.content.SharedPreferences
import com.boomesh.security.preferences.SecurePrefs

internal class MainModule(private val context: Context) {
    private val commonModule: CommonModule = CommonModule(context)
    private val encryptionModule: EncryptionModule

    init {
        encryptionModule = EncryptionModule(context, commonModule)
    }

    fun providesEncryptedSharedPreferences(fileName: String): SharedPreferences {
        return SecurePrefs(
            context.getSharedPreferences(fileName, Context.MODE_PRIVATE),
            encryptionModule.providesSecurable(),
            commonModule.provideBase64Decoder(),
            commonModule.provideBase64Encoder()
        )
    }
}
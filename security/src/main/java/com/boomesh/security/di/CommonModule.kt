package com.boomesh.security.di

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Base64
import com.boomesh.security.common.BuildConfigUtil
import com.boomesh.security.common.DateUtil
import java.util.*

internal class CommonModule(private val context: Context) {
    fun provideBuildConfigUtil(): BuildConfigUtil {
        return BuildConfigUtil(Build.VERSION.SDK_INT)
    }

    fun provideDateUtil() : DateUtil {
        return DateUtil { Calendar.getInstance() }
    }

    fun provideBase64Encoder(): (ByteArray) -> String {
        return {
            Base64.encodeToString(it, Base64.DEFAULT)
        }
    }

    fun provideBase64Decoder(): (String) -> ByteArray {
        return {
            Base64.decode(it, Base64.DEFAULT)
        }
    }

    fun providesSecurityPrefs(): SharedPreferences {
        return context.getSharedPreferences("SECURITY_PREFS", Context.MODE_PRIVATE)
    }
}
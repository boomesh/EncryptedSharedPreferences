package com.boomesh.security.preferences.editor

import android.content.SharedPreferences
import android.support.annotation.VisibleForTesting
import com.boomesh.security.common.DataSerializeUtil.booleanToString
import com.boomesh.security.common.DataSerializeUtil.stringSetToString
import com.boomesh.security.encrypt.securable.base.Securable

internal class Editor(
    private val editor: SharedPreferences.Editor,
    private val securable: Securable,
    private val encoder: (ByteArray) -> String,
    private val listener: EditorListener,
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    private val changedKeys: MutableList<String> = mutableListOf()
) : SharedPreferences.Editor {

    override fun putLong(key: String, value: Long): SharedPreferences.Editor {
        return putString(key, value.toString())
    }

    override fun putInt(key: String, value: Int): SharedPreferences.Editor {
        return putString(key, value.toString())
    }

    override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor {
        return putString(key, booleanToString(value))
    }

    override fun putStringSet(key: String, values: MutableSet<String>?): SharedPreferences.Editor {
        val string = if (values == null) {
            null
        } else {
            stringSetToString(values)
        }
        return putString(key, string)
    }

    override fun putFloat(key: String, value: Float): SharedPreferences.Editor {
        return putString(key, value.toString())
    }

    override fun putString(key: String, value: String?): SharedPreferences.Editor {
        val string =
            if (value == null) {
                value
            } else {
                val ciphered = securable.encrypt(value)
                encoder(ciphered)
            }
        editor.putString(key, string)
        changedKeys.add(key)
        return this
    }

    override fun clear(): SharedPreferences.Editor {
        return editor.clear()
    }

    override fun remove(key: String): SharedPreferences.Editor {
        return editor.remove(key)
    }

    override fun commit(): Boolean {
        val isSuccessful = editor.commit()
        notifyListener()
        return isSuccessful
    }

    override fun apply() {
        notifyListener()
        editor.apply()
    }

    private fun notifyListener() {
        if (changedKeys.isNotEmpty()) {
            listener.onSave(changedKeys.toList())
            changedKeys.clear()
        }
    }
}
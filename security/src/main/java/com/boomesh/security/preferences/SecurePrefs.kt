package com.boomesh.security.preferences

import android.content.SharedPreferences
import android.support.annotation.VisibleForTesting
import com.boomesh.security.common.DataSerializeUtil.isBoolean
import com.boomesh.security.common.DataSerializeUtil.isStringSet
import com.boomesh.security.common.DataSerializeUtil.stringToBoolean
import com.boomesh.security.common.DataSerializeUtil.stringToStringSet
import com.boomesh.security.encrypt.securable.base.Securable
import com.boomesh.security.preferences.editor.Editor
import com.boomesh.security.preferences.editor.EditorListener
import java.nio.BufferUnderflowException
import java.security.InvalidKeyException

internal class SecurePrefs internal constructor(
    private val prefs: SharedPreferences,
    private val securable: Securable,
    private val base64Decoder: (String) -> ByteArray,
    private val base64Encoder: (ByteArray) -> String
) : SharedPreferences, EditorListener {

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal val listeners: MutableList<SharedPreferences.OnSharedPreferenceChangeListener> = arrayListOf()

    override fun contains(key: String?): Boolean {
        return prefs.contains(key)
    }

    override fun getBoolean(key: String?, defValue: Boolean): Boolean {
        val decrypted = getString(key, null)
        return if (decrypted == null || !isBoolean(decrypted)) {
            defValue
        } else {
            stringToBoolean(decrypted)
        }
    }

    private fun <T> getParsedNumber(key: String?, defValue: T, parser: ((String?) -> T?)): T {
        return try {
            val decrypted = parser(getString(key, null))
            decrypted ?: defValue
        } catch (nfe: NumberFormatException) {
            defValue
        }
    }

    override fun getInt(key: String?, defValue: Int): Int {
        return getParsedNumber(key, defValue, { value -> value?.toInt() })
    }

    override fun getAll(): MutableMap<String, *> {
        val encryptedMap = prefs.all
        val decryptedMap = mutableMapOf<String, Any>()
        for (pair in encryptedMap) {
            val key = pair.key
            // expecting a string always, otherwise it is malformed (i.e. ignore it)
            val encodedValue = pair.value as? String ?: continue
            val decoded = base64Decoder(encodedValue)
            val decrypted = securable.decrypt(decoded)
            val stringValue = decrypted.toString(Charsets.UTF_8)

            var value: Any
            try {
                // int
                value = stringValue.toInt()
            } catch (nfe: NumberFormatException) {
                try {
                    // long
                    value = stringValue.toLong()
                } catch (nfe: NumberFormatException) {
                    try {
                        // float
                        value = stringValue.toFloat()
                    } catch (nfe: NumberFormatException) {
                        when {
                            isStringSet(stringValue) -> // string set
                                value = stringToStringSet(stringValue)
                            isBoolean(stringValue) -> // boolean
                                value = stringToBoolean(stringValue)
                            else -> // string
                                value = stringValue
                        }
                    }
                }
            }
            decryptedMap[key] = value
        }
        return decryptedMap
    }

    override fun edit(): SharedPreferences.Editor {
        return Editor(prefs.edit(), securable, base64Encoder, this)
    }

    override fun getLong(key: String?, defValue: Long): Long {
        return getParsedNumber(key, defValue, { value -> value?.toLong() })
    }

    override fun getFloat(key: String?, defValue: Float): Float {
        return getParsedNumber(key, defValue, { value -> value?.toFloat() })
    }

    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? {
        val decrypted = getString(key, null)
        return if (decrypted == null || !isStringSet(decrypted)) {
            defValues
        } else {
            stringToStringSet(decrypted)
        }
    }

    override fun getString(key: String?, defValue: String?): String? {
        val encodedString: String? = prefs.getString(key, null)
        return if (encodedString == null) {
            defValue
        } else {
            try {
                val decoded = base64Decoder(encodedString)
                securable.decrypt(decoded).toString(Charsets.UTF_8)
            } catch (bue: BufferUnderflowException) {
                defValue
            } catch (ike: InvalidKeyException) {
                defValue
            }
        }
    }

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        if (listeners.contains(listener)) {
            return
        }
        listeners.add(listener)
    }

    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        for (l in listeners) {
            if (l == listener) {
                listeners.remove(l)
                return
            }
        }
    }

    override fun onSave(changedKeys: List<String>) {
        for (listener in listeners) {
            for (key in changedKeys) {
                listener.onSharedPreferenceChanged(this, key)
            }
        }
    }
}
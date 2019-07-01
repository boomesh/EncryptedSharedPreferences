package com.boomesh.security.preferences.editor

import android.content.Context
import android.content.SharedPreferences
import com.boomesh.security.encrypt.securable.base.Securable
import com.boomesh.security.preferences.SecurePrefs
import com.boomesh.security.testhelpers.MockMatchers
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import java.nio.BufferUnderflowException
import java.security.InvalidKeyException
import java.util.*

class SecurePrefsTest {

    private lateinit var securePrefs: SharedPreferences

    private val base64Encoder = { bytes: ByteArray -> Base64.getMimeEncoder().encodeToString(bytes) }
    private val base64Decoder = { encoded: String -> Base64.getMimeDecoder().decode(encoded) }

    @Mock
    private lateinit var securable: Securable

    @Mock
    private lateinit var plainTextPrefs: SharedPreferences

    @Mock
    private lateinit var context: Context

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        `when`(context.getSharedPreferences(anyString(), anyInt())).thenReturn(plainTextPrefs)
        securePrefs =
            SecurePrefs(plainTextPrefs, securable, base64Decoder, base64Encoder)
    }

    @After
    fun tearDown() {
        reset(context, plainTextPrefs, securable)
        (securePrefs as SecurePrefs).listeners.clear()
    }

    @Test
    fun `contains a key`() {
        val key = "hello"
        `when`(plainTextPrefs.contains(key)).thenReturn(true)
        assertThat(securePrefs.contains(key)).isEqualTo(true)
    }

    @Test
    fun `does not contain a key`() {
        val key = "hello"
        `when`(plainTextPrefs.contains(key)).thenReturn(false)
        assertThat(securePrefs.contains(key)).isEqualTo(false)
    }

    private fun <T> getTest(key: String, stringOfValue: () -> String, getFromEncPrefs: (() -> T), expected: T) {
        val decoded: ByteArray = stringOfValue().toByteArray(Charsets.UTF_8)
        val encoded: String = base64Encoder(decoded)
        `when`(plainTextPrefs.getString(key, null)).thenReturn(encoded)
        `when`(securable.decrypt(decoded)).thenReturn(decoded)

        assertThat(getFromEncPrefs()).isEqualTo(expected)
        verify(securable).decrypt(decoded)
    }

    private fun <T> getNullTest(key: String, getFromEncPrefs: (() -> T), expected: T) {
        `when`(plainTextPrefs.getString(key, null)).thenReturn(null)
        assertThat(getFromEncPrefs()).isEqualTo(expected)
    }

    @Test
    fun `get a true boolean`() {
        val key = "hello"
        val value = true
        getTest(key, { "boolean($value)" }, { securePrefs.getBoolean(key, false) }, value)
    }

    @Test
    fun `get a false boolean`() {
        val key = "hello"
        val value = false
        getTest(key, { "boolean($value)" }, { securePrefs.getBoolean(key, true) }, value)
    }

    @Test
    fun `get an invalid boolean`() {
        val key = "hello"
        val value = "ball"
        val default = false
        getTest(key, { "boolean($value)" }, { securePrefs.getBoolean(key, default) }, default)
    }

    @Test
    fun `get bad format boolean (no suffix)`() {
        val key = "hello"
        val value = "true"
        val default = false
        getTest(key, { "boolean($value" }, { securePrefs.getBoolean(key, default) }, default)
    }

    @Test
    fun `get boolean null in prefs`() {
        val key = "hello"
        val default = false
        getNullTest(key, { securePrefs.getBoolean(key, default) }, default)
    }

    @Test
    fun `get valid int`() {
        val key = "hello"
        val value = 123123
        val default = -1
        getTest(key, { value.toString() }, { securePrefs.getInt(key, default) }, value)
    }

    @Test
    fun `get invalid int`() {
        val key = "hello"
        val value = "ball"
        val default = -1
        getTest(key, { value }, { securePrefs.getInt(key, default) }, default)
    }

    @Test
    fun `get null int`() {
        val key = "hello"
        val default = -1
        getNullTest(key, { securePrefs.getInt(key, default) }, default)
    }

    @Test
    fun `get all returns a populated non-null map`() {
        val expected = mutableMapOf(
            Pair("b", "four"),
            Pair("e", Long.MAX_VALUE),
            Pair("a", 2),
            Pair("c", 6.0f),
            Pair("f", setOf("twelve", "fourteen", "sixteen")),
            Pair("g", true)
        )

        val saved = mutableMapOf<String, String>()
        val decodedValues = mutableMapOf<String, ByteArray>()

        for (pair in expected) {
            val key = pair.key
            val value = when {
                pair.value is Iterable<*> -> (pair.value as Iterable<*>).joinToString(
                    ",",
                    prefix = "string_set[",
                    postfix = "]"
                )
                pair.value is Boolean -> "boolean(${pair.value})"
                else -> pair.value
            }
            val decoded: ByteArray = value.toString().toByteArray(Charsets.UTF_8)
            val encoded: String = base64Encoder(decoded)
            `when`(securable.decrypt(decoded)).thenReturn(decoded)
            saved[key] = encoded
            decodedValues[key] = decoded
        }

        `when`(plainTextPrefs.all).thenReturn(saved)

        assertThat(securePrefs.all).isEqualTo(expected)
        for (pair in decodedValues) {
            verify(securable).decrypt(pair.value)
        }
    }

    @Test
    fun `get all returns an empty non-null map`() {
        val expected = mutableMapOf<String, Any>()
        `when`(plainTextPrefs.all).thenReturn(expected)
        assertThat(securePrefs.all).isEqualTo(expected)
    }

    @Test
    fun `get all, some values are not string`() {
        val expected = "hello world"
        val encoded: String = base64Encoder(expected.toByteArray(Charsets.UTF_8))
        val input = mutableMapOf(Pair("a", 1), Pair("b", 1), Pair("c", encoded))

        for (pair in input) {
            @Suppress("CAST_NEVER_SUCCEEDS") val text = pair.value as? String ?: continue
            val decoded = base64Decoder(text)
            `when`(securable.decrypt(decoded)).thenReturn(decoded)
        }

        `when`(plainTextPrefs.all).thenReturn(input)
        assertThat(securePrefs.all).isEqualTo(mutableMapOf(Pair("c", expected)))
    }

    @Test
    fun `always return a non null editor`() {
        `when`(plainTextPrefs.edit()).thenReturn(mock(SharedPreferences.Editor::class.java))
        assertThat(securePrefs.edit()::class.java).isEqualTo(Editor::class.java)
    }

    @Test
    fun `get valid long`() {
        val key = "hello"
        val value = 456456L
        val default = -9L
        getTest(key, { value.toString() }, { securePrefs.getLong(key, default) }, value)
    }

    @Test
    fun `get invalid long`() {
        val key = "hello"
        val value = "ball"
        val default = -9L
        getTest(key, { value }, { securePrefs.getLong(key, default) }, default)
    }

    @Test
    fun `get null long`() {
        val key = "hello"
        val default = -9L
        getNullTest(key, { securePrefs.getLong(key, default) }, default)
    }

    @Test
    fun `get valid float`() {
        val key = "hello"
        val value = 789789.0f
        val default = -.9f
        getTest(key, { value.toString() }, { securePrefs.getFloat(key, default) }, value)
    }

    @Test
    fun `get invalid float`() {
        val key = "hello"
        val value = "ball"
        val default = -.9f
        getTest(key, { value }, { securePrefs.getFloat(key, default) }, default)
    }

    @Test
    fun `get null float`() {
        val key = "hello"
        val default = -.9f
        getNullTest(key, { securePrefs.getFloat(key, default) }, default)
    }

    @Test
    fun `get non-null string set, default is null`() {
        val key = "hello"
        val expected = mutableSetOf("a", "b", "c")
        val default = null

        val representation = expected.joinToString(separator = ",", prefix = "string_set[", postfix = "]")
        val decoded: ByteArray = representation.toByteArray(Charsets.UTF_8)
        val encoded: String = base64Encoder(decoded)
        `when`(securable.decrypt(decoded)).thenReturn(decoded)
        `when`(plainTextPrefs.getString(key, null)).thenReturn(encoded)

        assertThat(securePrefs.getStringSet(key, default)).isEqualTo(expected)
        verify(securable).decrypt(decoded)
    }

    @Test
    fun `get bad format non-null string set, (missing suffix)`() {
        val key = "hello"
        val expected = mutableSetOf("a", "b", "c")
        val default = null

        val representation = expected.joinToString(separator = ",", prefix = "string_set[")
        val decoded: ByteArray = representation.toByteArray(Charsets.UTF_8)
        val encoded: String = base64Encoder(decoded)
        `when`(securable.decrypt(decoded)).thenReturn(decoded)
        `when`(plainTextPrefs.getString(key, null)).thenReturn(encoded)

        assertThat(securePrefs.getStringSet(key, default)).isEqualTo(default)
        verify(securable).decrypt(decoded)
    }

    @Test
    fun `get null string set, default is null`() {
        val key = "hello"
        val expected = null
        val default = null

        `when`(plainTextPrefs.getString(key, null)).thenReturn(null)
        assertThat(securePrefs.getStringSet(key, default)).isEqualTo(expected)
    }

    @Test
    fun `get non-null string set, default is non-null`() {
        val key = "hello"
        val default = mutableSetOf("a", "b", "c")

        `when`(plainTextPrefs.getString(key, null)).thenReturn(null)
        assertThat(securePrefs.getStringSet(key, default)).isEqualTo(default)
    }

    @Test
    fun `get string returns non-null`() {
        val key = "hello"
        val value = "world"
        val default = null
        getTest(key, { value }, { securePrefs.getString(key, default) }, value)

    }

    @Test
    fun `get string returns null`() {
        val key = "hello"
        val default = null
        getNullTest(key, { securePrefs.getString(key, default) }, default)
    }

    @Test
    fun `get string returns default if null`() {
        val key = "hello"
        val default = "this is the default"
        getNullTest(key, { securePrefs.getString(key, default) }, default)
    }

    @Test
    fun `get string returns default if the contents being decrypted is in an invalid payload format`() {
        val key = "hello"
        val value = base64Encoder("world".toByteArray(Charsets.UTF_8))
        val default = "default"

        `when`(plainTextPrefs.getString(key, null)).thenReturn(value)
        `when`(securable.decrypt(MockMatchers.any())).thenThrow(BufferUnderflowException())
        assertThat(securePrefs.getString(key, default)).isEqualTo(default)
    }

    @Test
    fun `get string returns default if key cannot be located in keystore`() {
        val key = "hello"
        val value = base64Encoder("world".toByteArray(Charsets.UTF_8))
        val default = "default"

        `when`(plainTextPrefs.getString(key, null)).thenReturn(value)
        `when`(securable.decrypt(MockMatchers.any())).thenThrow(InvalidKeyException())
        assertThat(securePrefs.getString(key, default)).isEqualTo(default)
    }

    @Test
    fun `register multiple non-null listeners`() {
        val first = mock(SharedPreferences.OnSharedPreferenceChangeListener::class.java)
        val second = mock(SharedPreferences.OnSharedPreferenceChangeListener::class.java)
        securePrefs.registerOnSharedPreferenceChangeListener(first)
        securePrefs.registerOnSharedPreferenceChangeListener(second)

        val prefsListeners = (securePrefs as SecurePrefs).listeners
        assertThat(prefsListeners).containsExactly(first, second)
        verify(plainTextPrefs, never()).registerOnSharedPreferenceChangeListener(first)
        verify(plainTextPrefs, never()).registerOnSharedPreferenceChangeListener(second)
    }

    @Test
    fun `registering the same listener multiple times adds it once`() {
        val first = mock(SharedPreferences.OnSharedPreferenceChangeListener::class.java)
        securePrefs.registerOnSharedPreferenceChangeListener(first)
        securePrefs.registerOnSharedPreferenceChangeListener(first)

        val prefsListeners = (securePrefs as SecurePrefs).listeners
        assertThat(prefsListeners).containsExactly(first)
        verify(plainTextPrefs, never()).registerOnSharedPreferenceChangeListener(first)
    }

    @Test
    fun `unregister multiple non-null listeners`() {
        val first = mock(SharedPreferences.OnSharedPreferenceChangeListener::class.java)
        val second = mock(SharedPreferences.OnSharedPreferenceChangeListener::class.java)

        val prefsListeners = (securePrefs as SecurePrefs).listeners
        prefsListeners.addAll(listOf(first, second))

        securePrefs.unregisterOnSharedPreferenceChangeListener(first)
        securePrefs.unregisterOnSharedPreferenceChangeListener(second)

        assertThat(prefsListeners).isEmpty()
        verify(plainTextPrefs, never()).unregisterOnSharedPreferenceChangeListener(first)
        verify(plainTextPrefs, never()).unregisterOnSharedPreferenceChangeListener(second)
    }

    @Test
    fun `unregister the same listener multiple times will unregister it once`() {
        val first = mock(SharedPreferences.OnSharedPreferenceChangeListener::class.java)

        val prefsListeners = (securePrefs as SecurePrefs).listeners
        prefsListeners.add(first)

        securePrefs.unregisterOnSharedPreferenceChangeListener(first)
        securePrefs.unregisterOnSharedPreferenceChangeListener(first)

        assertThat(prefsListeners).isEmpty()
        verify(plainTextPrefs, never()).unregisterOnSharedPreferenceChangeListener(first)
    }

    @Test
    fun `unregister a listener that is not registered will not unregister`() {
        val first = mock(SharedPreferences.OnSharedPreferenceChangeListener::class.java)
        val prefsListeners = (securePrefs as SecurePrefs).listeners
        prefsListeners.add(first)

        val second = mock(SharedPreferences.OnSharedPreferenceChangeListener::class.java)
        securePrefs.unregisterOnSharedPreferenceChangeListener(second)

        assertThat(prefsListeners).containsExactly(first)
        assertThat(prefsListeners).isNotEmpty
        verify(plainTextPrefs, never()).unregisterOnSharedPreferenceChangeListener(MockMatchers.any())
    }

    @Test
    fun `non-empty saved keys, notify listeners`() {
        val changedKeys = listOf("a", "b", "c")
        val listener1 = mock(SharedPreferences.OnSharedPreferenceChangeListener::class.java)
        val listener2 = mock(SharedPreferences.OnSharedPreferenceChangeListener::class.java)
        securePrefs.registerOnSharedPreferenceChangeListener(listener1)
        securePrefs.registerOnSharedPreferenceChangeListener(listener2)

        (securePrefs as SecurePrefs).onSave(changedKeys)

        for (key in changedKeys) {
            verify(listener1).onSharedPreferenceChanged(securePrefs, key)
            verify(listener2).onSharedPreferenceChanged(securePrefs, key)
        }
    }
}
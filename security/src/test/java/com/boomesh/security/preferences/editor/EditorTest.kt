package com.boomesh.security.preferences.editor

import android.content.Context
import android.content.SharedPreferences
import com.boomesh.security.encrypt.securable.base.Securable
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import java.util.*

class EditorTest {

    private lateinit var editor: SharedPreferences.Editor
    private val base64Encoder = { bytes: ByteArray -> Base64.getMimeEncoder().encodeToString(bytes) }
    private val changedKeys: MutableList<String> = mutableListOf()

    @Mock
    private lateinit var securable: Securable

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var internalEditor: SharedPreferences.Editor

    @Mock
    private lateinit var listener: EditorListener


    @Suppress("UNCHECKED_CAST")
    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        editor = Editor(internalEditor, securable, base64Encoder, listener, changedKeys)
    }

    @After
    fun tearDown() {
        reset(context, securable, internalEditor, listener)
        changedKeys.clear()
    }

    @Test
    fun `set non-null string`() {
        val key = "hello"
        val value = "world"
        setPrefs(key, value, editor::putString)
    }

    @Test
    fun `set null string`() {
        val key = "hello"
        val value = null

        editor.putString(key, value)

        verify(internalEditor, never()).putString(eq(key), anyString())
        verify(internalEditor).putString(key, null)
    }

    @Test
    fun `set long`() {
        val key = "hello"
        val value = 123L
        setPrefs(key, value, editor::putLong)
    }

    @Test
    fun `set int`() {
        val key = "hello"
        val value = 123
        setPrefs(key, value, editor::putInt)
    }

    @Test
    fun `set float`() {
        val key = "hello"
        val value = 123.0f
        setPrefs(key, value, editor::putFloat)
    }

    @Test
    fun `set boolean`() {
        val key = "hello"
        val value = true
        setPrefs(key, value, editor::putBoolean, converter = { "boolean($it)" })
    }

    @Test
    fun `set non-null string set`() {
        val key = "hello"
        val value = mutableSetOf("hello", "and", "welcome", "to", "world")
        setPrefs(key, value, editor::putStringSet, {
            it.joinToString(
                separator = ",",
                prefix = "string_set[",
                postfix = "]"
            )
        })
    }

    @Test
    fun `set null string set`() {
        val key = "hello"
        val value = null

        editor.putStringSet(key, value)

        verify(internalEditor).putString(key, value)
        verify(internalEditor, never()).putStringSet(eq(key), anySet<String>())
    }

    @Test
    fun `remove key from prefs`() {
        val key = "hello"
        `when`(internalEditor.remove(key)).thenReturn(editor)
        editor.remove(key)
        verify(internalEditor).remove(key)
    }

    @Test
    fun `clear prefs`() {
        `when`(internalEditor.clear()).thenReturn(editor)
        editor.clear()
        verify(internalEditor).clear()
    }

    @Test
    fun `commit prefs`() {
        `when`(internalEditor.commit()).thenReturn(true)
        editor.commit()
        verify(internalEditor).commit()
    }

    @Test
    fun `commit prefs notifies listeners if keys were changed`() {
        val list = listOf("a", "b", "c")
        `when`(internalEditor.commit()).thenReturn(true)
        changedKeys.addAll(list)

        editor.commit()
        verify(listener).onSave(list)
        assertThat(changedKeys).isEmpty()
    }

    @Test
    fun `commit prefs does not notify listeners if no keys were changed`() {
        `when`(internalEditor.commit()).thenReturn(true)

        editor.commit()
        verify(listener, never()).onSave(anyList())
    }

    @Test
    fun `apply prefs`() {
        editor.apply()
        verify(internalEditor).apply()
    }

    @Test
    fun `apply prefs notifies listeners if keys were changed`() {
        val list = listOf("a", "b", "c")
        changedKeys.addAll(list)

        editor.apply()
        verify(listener).onSave(list)
        assertThat(changedKeys).isEmpty()
    }

    @Test
    fun `apply prefs does not notify listeners if no keys were changed`() {
        `when`(internalEditor.commit()).thenReturn(true)

        editor.commit()
        verify(listener, never()).onSave(anyList())
    }

    private fun <T> setPrefs(
        key: String,
        value: T,
        setter: ((String, T) -> SharedPreferences.Editor),
        converter: ((T) -> (String)) = { v: T -> "$v" }
    ) {
        val string = converter(value)
        val decoded = string.toByteArray(Charsets.UTF_8)
        `when`(securable.encrypt(string)).thenReturn(decoded)

        setter(key, value)

        val encoded = base64Encoder(decoded)
        verify(internalEditor).putString(key, encoded)
        verify(securable).encrypt(string)
        assertThat(changedKeys).contains(key)
    }
}
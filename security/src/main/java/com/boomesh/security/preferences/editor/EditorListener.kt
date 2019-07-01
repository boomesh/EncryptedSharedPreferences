package com.boomesh.security.preferences.editor

internal interface EditorListener {
    fun onSave(changedKeys: List<String>)
}
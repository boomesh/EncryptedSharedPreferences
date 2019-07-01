package com.boomesh.espsample

import android.content.SharedPreferences
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.boomesh.security.preferences.EncryptedSharedPreferences

class MainActivity : AppCompatActivity() {

    private lateinit var encryptText: EditText
    private lateinit var decryptText: TextView
    private lateinit var encryptInt: EditText
    private lateinit var decryptInt: TextView
    private lateinit var encryptLong: EditText
    private lateinit var decryptLong: TextView
    private lateinit var encryptFloat: EditText
    private lateinit var decryptFloat: TextView
    private lateinit var encryptStringSet: EditText
    private lateinit var decryptStringSet: TextView

    private lateinit var prefs: SharedPreferences

    companion object {
        private const val PREFS_KEY_TEXT = ".str"
        private const val PREFS_KEY_INT = ".int"
        private const val PREFS_KEY_LONG = ".long"
        private const val PREFS_KEY_FLOAT = ".float"
        private const val PREFS_KEY_STRSET = ".strset"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        encryptText = findViewById(R.id.encrypt_str_et)
        decryptText = findViewById(R.id.decrypt_str_tv)

        encryptInt = findViewById(R.id.encrypt_int_et)
        decryptInt = findViewById(R.id.decrypt_int_tv)

        encryptFloat = findViewById(R.id.encrypt_float_et)
        decryptFloat = findViewById(R.id.decrypt_float_tv)

        encryptLong = findViewById(R.id.encrypt_long_et)
        decryptLong = findViewById(R.id.decrypt_long_tv)

        encryptStringSet = findViewById(R.id.encrypt_strset_et)
        decryptStringSet = findViewById(R.id.decrypt_strset_tv)

        prefs = EncryptedSharedPreferences.create(applicationContext)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        findViewById<Button>(R.id.encrypt_btn).setOnClickListener {
            val editor = prefs.edit()

            val str = encryptText.text.toString()
            editor.putString(PREFS_KEY_TEXT, str)

            val int = encryptInt.text.toString().toIntOrNull() ?: 0
            editor.putInt(PREFS_KEY_INT, int)

            val lng = encryptLong.text.toString().toLongOrNull() ?: 0L
            editor.putLong(PREFS_KEY_LONG, lng)

            val flt = encryptFloat.text.toString().toFloatOrNull() ?: 0f
            editor.putFloat(PREFS_KEY_FLOAT, flt)

            val strset = encryptStringSet.text.toString().split(",").toSet()
            editor.putStringSet(PREFS_KEY_STRSET, strset)

            editor.apply()
            loadFromPrefs()
        }
        loadFromPrefs()
    }

    private fun loadFromPrefs() {
        decryptText.text = prefs.getString(PREFS_KEY_TEXT, "NOTHING")
        decryptInt.text = prefs.getInt(PREFS_KEY_INT, 0).toString()
        decryptLong.text = prefs.getLong(PREFS_KEY_LONG, 0L).toString()
        decryptFloat.text = prefs.getFloat(PREFS_KEY_FLOAT, 0f).toString()
        decryptStringSet.text = prefs.getStringSet(PREFS_KEY_STRSET, mutableSetOf())?.toString()
    }
}

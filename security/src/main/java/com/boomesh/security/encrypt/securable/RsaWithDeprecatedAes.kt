package com.boomesh.security.encrypt.securable

import android.content.SharedPreferences
import com.boomesh.security.encrypt.securable.base.Securable
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

/**
 *
 * Sources:
 *  - https://medium.com/@ericfu/securely-storing-secrets-in-an-android-application-501f030ae5a3
 *  - https://stackoverflow.com/questions/18228579/how-to-create-a-secure-random-aes-key-in-java
 *  - https://proandroiddev.com/security-best-practices-symmetric-encryption-with-aes-in-java-7616beaaade9
 */
internal class RsaWithDeprecatedAes(
    private val prefs: SharedPreferences,
    private val rsa: Securable,
    private val keyStoreFactory: (() -> KeyStore),
    private val cipherFactory: ((String) -> Cipher),
    base64Encoder: (ByteArray) -> String,
    private val base64Decoder: (String) -> ByteArray
) : Securable {

    companion object {
        private const val PREFS_MASTER_KEY = ".security_master_prefs_key"
        private const val SYMMETRIC_KEY_LENGTH = 16
        private const val ALGORITHM = "AES"
        @Suppress("SpellCheckingInspection")
        private const val ALGORITHM_MODE_PADDING = "AES/ECB/PKCS7Padding"
    }

    private val masterKey: SecretKey?
        get() {
            // fetch private key from prefs
            prefs.getString(PREFS_MASTER_KEY, null)?.let {
                val decoded = base64Decoder(it)

                // decrypt with rsa
                val decryptedKey = rsa.decrypt(decoded)

                // return (do not store in heap)
                return SecretKeySpec(decryptedKey, ALGORITHM)
            }
            return null
        }

    init {
        if (!prefs.contains(PREFS_MASTER_KEY)) {
            /*
            ** NOT OPTING FOR THIS BECAUSE THERE'S A CHANCE THIS WON'T WORK ALL THE TIME
            ** e.g. https://stackoverflow.com/a/32146078
            *
            val keyGenerator = keyGeneratorFactory()
            keyGenerator.init(256)
            keyGenerator.generateKey().encoded
            */

            val secureRandom = SecureRandom()
            val symmetricKey = ByteArray(SYMMETRIC_KEY_LENGTH)
            secureRandom.nextBytes(symmetricKey)

            val encrypted = rsa.encrypt(symmetricKey)
            val encoded = base64Encoder(encrypted)

            val editor = prefs.edit()
            editor.putString(PREFS_MASTER_KEY, encoded)
            editor.apply()
        }
    }

    override fun encrypt(bytes: ByteArray): ByteArray {
        // load keystore
        val keyStore = keyStoreFactory()
        keyStore.load(null)

        // cipher with master key
        val cipher = cipherFactory(ALGORITHM_MODE_PADDING)

        cipher.init(Cipher.ENCRYPT_MODE, masterKey)
        // not supplying IV, because some providers make them null on devices below M
        return cipher.doFinal(bytes)
    }

    override fun encrypt(text: String): ByteArray {
        return encrypt(text.toByteArray(Charsets.UTF_8))
    }

    override fun decrypt(payload: ByteArray): ByteArray {
        val keystore = keyStoreFactory()
        keystore.load(null)
        val cipher = cipherFactory(ALGORITHM_MODE_PADDING)

        cipher.init(Cipher.DECRYPT_MODE, masterKey)
        return cipher.doFinal(payload)
    }
}
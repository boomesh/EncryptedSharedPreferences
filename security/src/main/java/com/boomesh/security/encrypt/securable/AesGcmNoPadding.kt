package com.boomesh.security.encrypt.securable

import android.annotation.TargetApi
import android.os.Build
import com.boomesh.security.common.DateUtil
import com.boomesh.security.di.EncryptionModule
import com.boomesh.security.encrypt.KeyGeneratorSpecBuilder
import com.boomesh.security.encrypt.securable.base.Securable
import java.nio.ByteBuffer
import java.security.KeyStore
import java.security.spec.AlgorithmParameterSpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec

/**
 * Recommended encryption mechanisms for M and above.
 *
 * https://medium.com/@josiassena/using-the-android-keystore-system-to-store-sensitive-information-3a56175a454b
 *
 * Resources:
 *   - https://developer.android.com/guide/topics/security/cryptography#choose-algorithm
 *   - https://developer.android.com/training/articles/keystore#SupportedCiphers
 *   - https://proandroiddev.com/secure-data-in-android-initialization-vector-6ca1c659762c
 *   - https://developer.android.com/reference/android/security/keystore/KeyGenParameterSpec.Builder.html#setRandomizedEncryptionRequired(boolean)
 */
@TargetApi(Build.VERSION_CODES.M)
internal class AesGcmNoPadding(
    private val alias: String,
    private val keyStoreFactory: (() -> KeyStore),
    private val cipherFactory: ((String) -> Cipher),
    keyGeneratorFactory: (() -> KeyGenerator),
    dateUtil: DateUtil,
    keyGeneratorSpecBuilder: KeyGeneratorSpecBuilder
) : Securable {

    companion object {
        private const val ALGORITHM_MODE_PADDING = "AES/GCM/NoPadding"
        private const val TAG_LENGTH_BIT = 128
    }

    init {
        val keyStore = keyStoreFactory()
        keyStore.load(null)
        if (!keyStore.containsAlias(alias)) {
            val notBefore = dateUtil.now
            val notAfter = dateUtil.addYearsTo(notBefore, 40)
            val serialNumber = EncryptionModule.SERIAL_NUMBER
            val generator = keyGeneratorFactory()

            val spec: AlgorithmParameterSpec =
                keyGeneratorSpecBuilder.build(alias, serialNumber, notBefore.time, notAfter)
            generator.init(spec)
            generator.generateKey()
        }
    }

    override fun encrypt(bytes: ByteArray): ByteArray {
        val cipher = cipherFactory(ALGORITHM_MODE_PADDING)
        val keyStore = keyStoreFactory()
        keyStore.load(null)

        val keyEntry = keyStore.getEntry(alias, null)
        val secretKeyEntry = keyEntry as? KeyStore.SecretKeyEntry
        val secretKey = secretKeyEntry?.secretKey

        // Choosing to rely on the security provider for the cipher to generate the IV for me
        // because of documentation: https://developer.android.com/reference/android/security/keystore/KeyGenParameterSpec.Builder.html#setRandomizedEncryptionRequired(boolean)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv

        val encrypted = cipher.doFinal(bytes)
        return generatePayloadFormat(iv, encrypted)
    }

    private fun generatePayloadFormat(iv: ByteArray, encrypted: ByteArray): ByteArray {
        // following payload format: https://github.com/patrickfav/armadillo/blob/d47aca0d0ae4907f6c34dc9d3f5242bf3e32dfd0/armadillo/src/main/java/at/favre/lib/armadillo/AesGcmEncryption.java
        val payload = ByteBuffer.allocate(1 + iv.size + encrypted.size)
        payload.put(iv.size.toByte())
        payload.put(iv)
        payload.put(encrypted)
        return payload.array()
    }

    override fun encrypt(text: String): ByteArray {
        return encrypt(text.toByteArray(Charsets.UTF_8))
    }

    override fun decrypt(payload: ByteArray): ByteArray {
        val cipher = cipherFactory(ALGORITHM_MODE_PADDING)
        val keyStore = keyStoreFactory()
        keyStore.load(null)

        val keyEntry = keyStore.getEntry(alias, null)
        val secretKeyEntry = keyEntry as? KeyStore.SecretKeyEntry
        val secretKey = secretKeyEntry?.secretKey

        // following payload format in generatePayloadFormat
        val byteBuffer = ByteBuffer.wrap(payload)
        val ivSize = byteBuffer.get().toInt()
        val iv = ByteArray(ivSize)
        byteBuffer.get(iv)
        val encrypted = ByteArray(byteBuffer.remaining())
        byteBuffer.get(encrypted)

        val spec = GCMParameterSpec(TAG_LENGTH_BIT, iv)

        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

        return cipher.doFinal(encrypted)
    }
}
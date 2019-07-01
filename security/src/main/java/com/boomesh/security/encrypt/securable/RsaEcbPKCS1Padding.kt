package com.boomesh.security.encrypt.securable

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import com.boomesh.security.common.DateUtil
import com.boomesh.security.di.EncryptionModule
import com.boomesh.security.encrypt.KeyGeneratorSpecBuilder
import com.boomesh.security.encrypt.securable.base.Securable
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.interfaces.RSAPublicKey
import java.security.spec.AlgorithmParameterSpec
import javax.crypto.Cipher

/**
 * Uses the deprecated mechanisms for OSes below marshmallow
 *
 * Resources
 *  - https://developer.android.com/training/articles/keystore#SupportedCiphers
 *  - https://github.com/nelenkov/android-keystore/blob/master/src/org/nick/androidkeystore/AndroidRsaEngine.java
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
internal class RsaEcbPKCS1Padding(
    private val alias: String,
    context: Context,
    dateUtil: DateUtil,
    private val keyStoreFactory: (() -> KeyStore),
    private val cipherFactory: ((String) -> Cipher),
    keyPairGeneratorFactory: (() -> KeyPairGenerator),
    keyGeneratorSpecBuilder: KeyGeneratorSpecBuilder
) : Securable {

    companion object {
        @Suppress("SpellCheckingInspection")
        private const val ALGORITHM_MODE_PADDING = "RSA/ECB/PKCS1Padding"
    }

    init {
        val keyStore = keyStoreFactory()
        keyStore.load(null)
        if (!keyStore.containsAlias(alias)) {
            val notBefore = dateUtil.now
            val notAfter = dateUtil.addYearsTo(notBefore, 40)
            val serialNumber = EncryptionModule.SERIAL_NUMBER
            val generator = keyPairGeneratorFactory()

            val spec: AlgorithmParameterSpec =
                keyGeneratorSpecBuilder.build(context, alias, serialNumber, notBefore.time, notAfter)

            generator.initialize(spec)
            generator.genKeyPair()
        }
    }

    override fun encrypt(bytes: ByteArray): ByteArray {
        val keyStore = keyStoreFactory()
        val cipher = cipherFactory(ALGORITHM_MODE_PADDING)
        keyStore.load(null)

        val keyEntry = keyStore.getEntry(alias, null)
        val privateKey = keyEntry as KeyStore.PrivateKeyEntry
        val publicKey = privateKey.certificate.publicKey as RSAPublicKey

        cipher.init(Cipher.ENCRYPT_MODE, publicKey)

        return cipher.doFinal(bytes)
    }

    override fun encrypt(text: String): ByteArray {
        return encrypt(text.toByteArray(Charsets.UTF_8))
    }

    override fun decrypt(payload: ByteArray): ByteArray {
        val keyStore = keyStoreFactory()
        val cipher = cipherFactory(ALGORITHM_MODE_PADDING)
        keyStore.load(null)

        val keyEntry = keyStore.getEntry(alias, null)
        val privateKey = (keyEntry as KeyStore.PrivateKeyEntry).privateKey

        cipher.init(Cipher.DECRYPT_MODE, privateKey)

        return cipher.doFinal(payload)
    }
}
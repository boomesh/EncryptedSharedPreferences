package com.boomesh.security.di

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.security.KeyPairGeneratorSpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.boomesh.security.encrypt.KeyGeneratorSpecBuilder
import com.boomesh.security.encrypt.securable.AesGcmNoPadding
import com.boomesh.security.encrypt.securable.RsaEcbPKCS1Padding
import com.boomesh.security.encrypt.securable.RsaWithDeprecatedAes
import com.boomesh.security.encrypt.securable.base.Securable
import java.security.KeyPairGenerator
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator

internal class EncryptionModule(private val context: Context, private val commonModule: CommonModule) {

    companion object {
        private const val ANDROID_KEY_STORE_PROVIDER = "AndroidKeyStore"
        private const val ALIAS = "SecurityModuleAlias"
        const val SERIAL_NUMBER = 1L
    }

    fun providesSecurable(): Securable {
        if (commonModule.provideBuildConfigUtil().isOSBelow(Build.VERSION_CODES.M)) {
            return RsaWithDeprecatedAes(
                commonModule.providesSecurityPrefs(),
                providesRsaSecurable(),
                providesKeyStoreFactory(),
                providesCipherFactory(),
                commonModule.provideBase64Encoder(),
                commonModule.provideBase64Decoder()
            )
        } else {
            return AesGcmNoPadding(
                ALIAS,
                providesKeyStoreFactory(),
                providesCipherFactory(),
                providesKeyGeneratorFactory(),
                commonModule.provideDateUtil(),
                providesKeyGenSpecBuilder()
            )
        }
    }

    private fun providesKeyStoreFactory(): () -> KeyStore {
        return { KeyStore.getInstance(ANDROID_KEY_STORE_PROVIDER) }
    }

    @SuppressLint("NewApi")
    private fun providesKeyGenSpecBuilder(): KeyGeneratorSpecBuilder {
        return KeyGeneratorSpecBuilder({ context ->
            KeyPairGeneratorSpec.Builder(context)
        }, { alias, purposes ->
            KeyGenParameterSpec.Builder(alias, purposes)
        })
    }

    @SuppressLint("InlinedApi")
    private fun providesKeyGeneratorFactory(): () -> KeyGenerator {
        return { KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES) }
    }

    private fun providesKeyPairGeneratorFactory(): () -> KeyPairGenerator {
        return { KeyPairGenerator.getInstance("RSA", ANDROID_KEY_STORE_PROVIDER) }
    }

    private fun providesCipherFactory(): (String) -> Cipher {
        return {
            Cipher.getInstance(it)
        }
    }

    private fun providesRsaSecurable(): Securable {
        return RsaEcbPKCS1Padding(
            ALIAS,
            context,
            commonModule.provideDateUtil(),
            providesKeyStoreFactory(),
            providesCipherFactory(),
            providesKeyPairGeneratorFactory(),
            providesKeyGenSpecBuilder()
        )
    }
}
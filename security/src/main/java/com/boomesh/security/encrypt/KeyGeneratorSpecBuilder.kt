package com.boomesh.security.encrypt

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.security.KeyPairGeneratorSpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.math.BigInteger
import java.util.*
import javax.security.auth.x500.X500Principal

/**
 * Class serves as a wrapper around the android implementations of generating key specs.  This should help with:
 * - localizing an area to introduce changes to the key spec (not sure if this is useful)
 * - making it easier to mock for testing
 *
 * Sources:
 * - https://github.com/googlesamples/android-BasicAndroidKeyStore/blob/master/Application/src/main/java/com/example/android/basicandroidkeystore/BasicAndroidKeyStoreFragment.java
 * - https://medium.com/@josiassena/using-the-android-keystore-system-to-store-sensitive-information-3a56175a454b
 * - https://proandroiddev.com/secure-data-in-android-initialization-vector-6ca1c659762c
 */
internal class KeyGeneratorSpecBuilder(
    private val keyPairGeneratorSpecBuilderFactory: ((Context) -> KeyPairGeneratorSpec.Builder),
    private val keyGenParameterSpecBuilderFactory: ((String, Int) -> KeyGenParameterSpec.Builder)
) {

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    fun build(
        context: Context,
        alias: String,
        serialNumber: Long,
        startDate: Date,
        endDate: Date
    ): KeyPairGeneratorSpec {
        // Below Android M, use the KeyPairGeneratorSpec.Builder.
        return keyPairGeneratorSpecBuilderFactory(context).run {
            // You'll use the alias later to retrieve the key.  It's a key for the key!
            setAlias(alias)
            // The subject used for the self-signed certificate of the generated pair
            setSubject(X500Principal("CN=$alias"))
            // The serial number used for the self-signed certificate of the
            // generated pair.
            setSerialNumber(BigInteger.valueOf(serialNumber))
            // Date range of validity for the generated pair.
            setStartDate(startDate)
            setEndDate(endDate)
            build()
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    fun build(alias: String, serialNumber: Long, startDate: Date, endDate: Date): KeyGenParameterSpec {
        // On Android M or above, use the KeyGenParameterSpec.Builder and specify permitted
        // properties  and restrictions of the key.
        return keyGenParameterSpecBuilderFactory(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .run {
                setCertificateSubject(X500Principal("CN=$alias"))
                setCertificateSerialNumber(BigInteger.valueOf(serialNumber))
                setCertificateNotBefore(startDate)
                setCertificateNotAfter(endDate)
                setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                build()
            }
    }
}
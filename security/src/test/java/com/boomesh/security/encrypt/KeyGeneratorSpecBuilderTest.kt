@file:Suppress("DEPRECATION")

package com.boomesh.security.encrypt

import android.content.Context
import android.security.KeyPairGeneratorSpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import java.math.BigInteger
import java.util.*

class KeyGeneratorSpecBuilderTest {

    companion object {
        private const val ALIAS = "hello_world_alias"
        private const val SERIAL_NUMBER = 1L
    }

    @Mock
    private lateinit var context: Context
    @Mock
    private lateinit var deprecatedSpec: KeyPairGeneratorSpec.Builder
    @Mock
    private lateinit var standardSpec: KeyGenParameterSpec.Builder

    private val standardSpecFactory: (String, Int) -> KeyGenParameterSpec.Builder = { _, _ -> standardSpec }
    private lateinit var keyGeneratorSpecBuilder: KeyGeneratorSpecBuilder

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        keyGeneratorSpecBuilder =
            KeyGeneratorSpecBuilder({ deprecatedSpec }, standardSpecFactory)
    }

    @After
    fun tearDown() {
        reset(context, deprecatedSpec, standardSpec)
    }

    @Test
    fun `KeyPairGeneratorSpec returned in the builder`() {
        val startDate = Date()
        val endDate = Date()
        `when`(deprecatedSpec.build()).thenReturn(mock(KeyPairGeneratorSpec::class.java))
        keyGeneratorSpecBuilder.build(
            context,
            ALIAS,
            SERIAL_NUMBER,
            startDate,
            endDate
        )
        verify(deprecatedSpec).setAlias(ALIAS)
        verify(deprecatedSpec).setSerialNumber(BigInteger.valueOf(SERIAL_NUMBER))
        verify(deprecatedSpec).setStartDate(startDate)
        verify(deprecatedSpec).setEndDate(startDate)
    }

    @Test
    fun `KeyGenParameterSpec returned in the builder`() {
        val startDate = Date()
        val endDate = Date()
        `when`(standardSpec.build()).thenReturn(mock(KeyGenParameterSpec::class.java))
        keyGeneratorSpecBuilder.build(
            ALIAS,
            SERIAL_NUMBER,
            startDate,
            endDate
        )
        verify(standardSpec).setCertificateSerialNumber(BigInteger.valueOf(SERIAL_NUMBER))
        verify(standardSpec).setCertificateNotBefore(startDate)
        verify(standardSpec).setCertificateNotAfter(startDate)
        verify(standardSpec).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
        verify(standardSpec).setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
        verify(standardSpec).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
    }
}


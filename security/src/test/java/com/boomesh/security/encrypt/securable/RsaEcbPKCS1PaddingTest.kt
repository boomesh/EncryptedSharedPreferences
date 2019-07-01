@file:Suppress("DEPRECATION")

package com.boomesh.security.encrypt.securable

import android.content.Context
import android.security.KeyPairGeneratorSpec
import com.boomesh.security.common.DateUtil
import com.boomesh.security.encrypt.KeyGeneratorSpecBuilder
import com.boomesh.security.encrypt.securable.base.Securable
import com.boomesh.security.testhelpers.MockMatchers
import org.assertj.core.api.Assertions
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.interfaces.RSAPublicKey
import java.util.*
import javax.crypto.Cipher

class RsaEcbPKCS1PaddingTest {

    companion object {
        private const val ALIAS = "SecurityModuleAlias"
    }

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var keyStore: KeyStore

    @Mock
    private lateinit var dateUtil: DateUtil

    @Mock
    private lateinit var kpg: KeyPairGenerator

    @Mock
    private lateinit var cipher: Cipher

    @Mock
    private lateinit var keyGeneratorSpecBuilder: KeyGeneratorSpecBuilder

    private lateinit var securable: Securable

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        val calendar = Calendar.getInstance()
        Mockito.`when`(dateUtil.now).thenReturn(calendar)
        Mockito.`when`(dateUtil.addYearsTo(calendar, 40)).thenReturn(Date())

        securable =
            RsaEcbPKCS1Padding(ALIAS, context, dateUtil, { keyStore }, { cipher }, { kpg }, keyGeneratorSpecBuilder)
    }

    @After
    fun tearDown() {
        reset(keyStore, dateUtil, kpg, cipher, keyGeneratorSpecBuilder)
    }

    @Test
    fun `initialize keys, first time`() {
        val builder = mock(KeyGeneratorSpecBuilder::class.java)
        `when`(
            builder.build(
                MockMatchers.any(),
                anyString(),
                anyLong(),
                MockMatchers.any(),
                MockMatchers.any()
            )
        ).thenReturn(
            mock(KeyPairGeneratorSpec::class.java)
        )

        `when`(keyStore.containsAlias(ALIAS)).thenReturn(false)
        val rsaEcbNoPadding =
            RsaEcbPKCS1Padding(ALIAS, context, dateUtil, { keyStore }, { cipher }, { kpg }, builder)
        Mockito.verify(builder).build(
            MockMatchers.any(),
            anyString(),
            anyLong(),
            MockMatchers.any(),
            MockMatchers.any()
        )
        Assertions.assertThat(rsaEcbNoPadding).isNotNull
    }

    @Test
    fun `initialize keys, time after time `() {
        val kpg = mock(KeyPairGenerator::class.java)
        val spec = mock(KeyPairGeneratorSpec::class.java)
        val builder = mock(KeyGeneratorSpecBuilder::class.java)
        `when`(
            builder.build(
                MockMatchers.any(),
                anyString(),
                anyLong(),
                MockMatchers.any(),
                MockMatchers.any()
            )
        ).thenReturn(
            spec
        )

        `when`(keyStore.containsAlias(ALIAS)).thenReturn(true)
        val rsaEcbNoPadding =
            RsaEcbPKCS1Padding(ALIAS, context, dateUtil, { keyStore }, { cipher }, { kpg }, builder)
        Mockito.verify(builder, never()).build(
            MockMatchers.any(),
            anyString(),
            anyLong(),
            MockMatchers.any(),
            MockMatchers.any()
        )
        verify(kpg, never()).initialize(spec)
        verify(kpg, never()).genKeyPair()
        Assertions.assertThat(rsaEcbNoPadding).isNotNull
    }

    @Test
    fun `encrypt non-null text`() {
        val content = "hello world"
        //expected acquired from device testing
        @Suppress("SpellCheckingInspection") val expected: ByteArray = ("O8vhPOZFE3glp/W9I2BpZti3cUCTf/pLnIybts3ehexYoak8TXX3oyh25CdrXxov4U5s+e+1u9mM\n" +
                "mux4X5oNBjS3SZcInWdcrGa+Y4yFq8XrJTL47fpp576VrQP7eyzro1z6kUi1BpNN7A+ykKsq3xfi\n" +
                "kIMpQzN07UGuNbukzNA8e5GU5BkEIyk9qNUUuXFO2INvwBd2ugnlIaZ0ucqDofmk7U01gslZ/k2c\n" +
                "6QIAReiEIww1JEThBBWhND0pSWDvNBg+ctle5S9NzQULe14+MzgLSjgrFU0a92IPY1OUhHM8ENHw\n" +
                "NofLSmkzu/c4yh3NWT4p+0X4MFiP+sFYkdJkeg==\n").toByteArray(Charsets.UTF_8)

        val keyEntry = mock(KeyStore.PrivateKeyEntry::class.java, RETURNS_DEEP_STUBS)
        Mockito.`when`(keyStore.getEntry(ALIAS, null)).thenReturn(keyEntry)
        Mockito.`when`(keyEntry.certificate.publicKey).thenReturn(mock(RSAPublicKey::class.java))
        Mockito.`when`(cipher.doFinal(any())).thenReturn(expected)

        val output = securable.encrypt(content)
        Assertions.assertThat(output).isEqualTo(expected)
    }

    @Test
    fun `encrypt, private key is not valid`() {
        val content = "hello world"
        Mockito.`when`(keyStore.getEntry(ALIAS, null)).thenReturn(null)
        Assertions.assertThatExceptionOfType(ClassCastException::class.java).isThrownBy { securable.encrypt(content) }
    }

    @Test
    fun `encrypt, public key is not valid`() {
        val content = "hello world"
        val keyEntry = mock(KeyStore.PrivateKeyEntry::class.java, RETURNS_DEEP_STUBS)
        Mockito.`when`(keyStore.getEntry(ALIAS, null)).thenReturn(keyEntry)
        Mockito.`when`(keyEntry.certificate.publicKey).thenReturn(null)
        Assertions.assertThatExceptionOfType(TypeCastException::class.java).isThrownBy { securable.encrypt(content) }
    }

    @Test
    fun `decrypt non-null text`() {
        val content = "hello world".toByteArray(Charsets.UTF_8)
        //expected acquired from device testing
        @Suppress("SpellCheckingInspection") val encodedPayload: ByteArray =
            ("VMMYMKIZTIPrxJV8pzPzd3ecuYsL5vb4XjrwHmTn10cyznPvraQFwzVryDZGQB31EMLGuPBullzv\n" +
                    "57hd1oIZwgwzloHy4QkW+p49Si+vdiz/V7lXZ5UTCcJPEOYjuQVimut0H6KnCj/Okp9YeBBrM+ld\n" +
                    "l4SmFEo1OrMAQoTYQzk5rbIKIGKgVVgmLE38JbsKFdSlSONnKZYNFHJQRrVOcDXBc9saFqW/CqDy\n" +
                    "hHC84dwVMof7XTNA8dMPcDUd0Z1njgrpt6KXc/01p/56WlYC0jvIhPzZdfa8wWs4gOrBafcf+bRa\n" +
                    "qP5J5MnTuq39GSVsLa6fEfQSIsn6f3NWIO0Xtw==\n").toByteArray(Charsets.UTF_8)
        val payload: ByteArray = Base64.getMimeDecoder().decode(encodedPayload)

        val keyEntry = mock(KeyStore.PrivateKeyEntry::class.java, RETURNS_DEEP_STUBS)
        Mockito.`when`(keyStore.getEntry(ALIAS, null)).thenReturn(keyEntry)
        Mockito.`when`(keyEntry.certificate.publicKey).thenReturn(mock(RSAPublicKey::class.java))
        Mockito.`when`(cipher.doFinal(any())).thenReturn(content)

        val output = securable.decrypt(payload)
        Assertions.assertThat(output).isEqualTo(content)
    }

    @Test
    fun `decrypt, private key is not valid`() {
        //expected acquired from device testing
        @Suppress("SpellCheckingInspection") val encodedPayload: ByteArray =
            ("VMMYMKIZTIPrxJV8pzPzd3ecuYsL5vb4XjrwHmTn10cyznPvraQFwzVryDZGQB31EMLGuPBullzv\n" +
                    "57hd1oIZwgwzloHy4QkW+p49Si+vdiz/V7lXZ5UTCcJPEOYjuQVimut0H6KnCj/Okp9YeBBrM+ld\n" +
                    "l4SmFEo1OrMAQoTYQzk5rbIKIGKgVVgmLE38JbsKFdSlSONnKZYNFHJQRrVOcDXBc9saFqW/CqDy\n" +
                    "hHC84dwVMof7XTNA8dMPcDUd0Z1njgrpt6KXc/01p/56WlYC0jvIhPzZdfa8wWs4gOrBafcf+bRa\n" +
                    "qP5J5MnTuq39GSVsLa6fEfQSIsn6f3NWIO0Xtw==\n").toByteArray(Charsets.UTF_8)
        val payload: ByteArray = Base64.getMimeDecoder().decode(encodedPayload)
        Mockito.`when`(keyStore.getEntry(ALIAS, null)).thenReturn(null)
        Assertions.assertThatExceptionOfType(ClassCastException::class.java).isThrownBy { securable.decrypt(payload) }
    }

    // cases to handle:
    // maximum payload is 256 bytes only
    // failing key creation
    // failing keystore fetch
    // failing ciphers
}
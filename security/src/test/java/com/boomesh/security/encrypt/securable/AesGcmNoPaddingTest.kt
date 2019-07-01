package com.boomesh.security.encrypt.securable

import android.security.keystore.KeyGenParameterSpec
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
import java.nio.ByteBuffer
import java.security.InvalidKeyException
import java.security.Key
import java.security.KeyStore
import java.util.*
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class AesGcmNoPaddingTest {

    companion object {
        private const val ALIAS = "SecurityModuleAlias"
    }

    @Mock
    private lateinit var keyStore: KeyStore

    @Mock
    private lateinit var dateUtil: DateUtil

    @Mock
    private lateinit var keyGenerator: KeyGenerator

    @Mock
    private lateinit var cipher: Cipher

    @Mock
    private lateinit var keyGeneratorSpecBuilder: KeyGeneratorSpecBuilder

    private lateinit var securable: Securable

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        val calendar = Calendar.getInstance()
        `when`(dateUtil.now).thenReturn(calendar)
        `when`(dateUtil.addYearsTo(calendar, 40)).thenReturn(Date())

        securable =
            AesGcmNoPadding(ALIAS, { keyStore }, { cipher }, { keyGenerator }, dateUtil, keyGeneratorSpecBuilder)
    }

    @After
    fun tearDown() {
        reset(keyStore, dateUtil, keyGenerator, cipher, keyGeneratorSpecBuilder)
    }

    @Test
    fun `initialize keys, first time`() {
        val spec = mock(KeyGenParameterSpec::class.java)
        val builder = mock(KeyGeneratorSpecBuilder::class.java)
        `when`((builder).build(
            anyString(),
            anyLong(),
            MockMatchers.any(),
            MockMatchers.any()
        )).thenReturn(spec)
        `when`(keyStore.containsAlias(ALIAS)).thenReturn(false)
        val aesGcmNoPadding =
            AesGcmNoPadding(ALIAS, { keyStore }, { cipher }, { keyGenerator }, dateUtil, builder)
        Mockito.verify(builder).build(
            anyString(),
            anyLong(),
            MockMatchers.any(),
            MockMatchers.any()
        )
        Assertions.assertThat(aesGcmNoPadding).isNotNull
    }

    @Test
    fun `initialize keys, time after time`() {
        val keyGenerator = mock(KeyGenerator::class.java)
        val spec = mock(KeyGenParameterSpec::class.java)
        val builder = mock(KeyGeneratorSpecBuilder::class.java)

        `when`((builder).build(
            anyString(),
            anyLong(),
            MockMatchers.any(),
            MockMatchers.any()
        )).thenReturn(spec)
        `when`(keyStore.containsAlias(ALIAS)).thenReturn(true)

        val aesGcmNoPadding =
            AesGcmNoPadding(ALIAS, { keyStore }, { cipher }, { keyGenerator }, dateUtil, builder)
        verify(builder, never()).build(
            anyString(),
            anyLong(),
            MockMatchers.any(),
            MockMatchers.any()
        )
        verify(keyGenerator, never()).init(spec)
        verify(keyGenerator, never()).generateKey()
        Assertions.assertThat(aesGcmNoPadding).isNotNull
    }

    @Test
    fun `encrypt non-null text`() {
        val content = "hello world"
        val iv = byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        @Suppress("SpellCheckingInspection") val encrypted: ByteArray =
            ("DAhGl+vGx4ZYq/K5CTsfedQ/+8+larAp7nS145aHfoejGyoDi6RU/Q==\n").toByteArray(Charsets.UTF_8)

        val expected = ByteBuffer.allocate(1 + iv.size + encrypted.size)
        expected.put(iv.size.toByte())
        expected.put(iv)
        expected.put(encrypted)

        val keyEntry = mock(KeyStore.SecretKeyEntry::class.java, RETURNS_DEEP_STUBS)
        `when`(keyStore.getEntry(ALIAS, null)).thenReturn(keyEntry)
        `when`(keyEntry.secretKey).thenReturn(mock(SecretKey::class.java))
        `when`(cipher.iv).thenReturn(iv)
        `when`(cipher.doFinal(any())).thenReturn(encrypted)

        val output = securable.encrypt(content)
        Assertions.assertThat(output).isEqualTo(expected.array())
    }

    @Test
    fun `encrypt, invalid key`() {
        val content = "hello world"
        `when`(keyStore.getEntry(ALIAS, null)).thenReturn(null)
        `when`(cipher.init(eq(Cipher.ENCRYPT_MODE), MockMatchers.any<Key>())).thenThrow(InvalidKeyException())
        Assertions.assertThatExceptionOfType(InvalidKeyException::class.java).isThrownBy { securable.encrypt(content) }
    }

    @Test
    fun `decrypt non-null text`() {
        val expected = ("hello world").toByteArray(Charsets.UTF_8)
        //acquired from device testing
        @Suppress("SpellCheckingInspection") val encodedPayload = "DHKzxurXQSD7wdKqrgCgZJt0IbOMeolMnmb1EFA1dxkXnZO2usaMHw==\n".toByteArray(Charsets.UTF_8)
        val payload: ByteArray = Base64.getMimeDecoder().decode(encodedPayload)
        val secretKey = mock(SecretKey::class.java)
        val keyEntry = mock(KeyStore.SecretKeyEntry::class.java, RETURNS_DEEP_STUBS)
        `when`(keyStore.getEntry(ALIAS, null)).thenReturn(keyEntry)
        `when`(keyEntry.secretKey).thenReturn(secretKey)
        `when`(cipher.doFinal(any())).thenReturn(expected)

        val output = securable.decrypt(payload)
        Assertions.assertThat(output).isEqualTo(expected)
    }

    @Test
    fun `decrypt, invalid secret key`() {
        //acquired from device testing
        @Suppress("SpellCheckingInspection") val encodedPayload =
            "DHKzxurXQSD7wdKqrgCgZJt0IbOMeolMnmb1EFA1dxkXnZO2usaMHw==\n".toByteArray(Charsets.UTF_8)
        val payload: ByteArray = Base64.getMimeDecoder().decode(encodedPayload)
        `when`(keyStore.getEntry(ALIAS, null)).thenReturn(null)
        `when`(cipher.init(eq(Cipher.DECRYPT_MODE), MockMatchers.any<Key>(), any<GCMParameterSpec>())).thenThrow(
            InvalidKeyException()
        )
        Assertions.assertThatExceptionOfType(InvalidKeyException::class.java).isThrownBy { securable.decrypt(payload) }
    }
}
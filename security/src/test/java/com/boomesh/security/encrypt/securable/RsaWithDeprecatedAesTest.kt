package com.boomesh.security.encrypt.securable

import android.content.SharedPreferences
import com.boomesh.security.common.DateUtil
import com.boomesh.security.encrypt.KeyGeneratorSpecBuilder
import com.boomesh.security.encrypt.securable.base.Securable
import com.boomesh.security.testhelpers.MockMatchers
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import java.security.InvalidKeyException
import java.security.KeyStore
import java.util.*
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class RsaWithDeprecatedAesTest {

    companion object {
        private const val PREFS_MASTER_KEY = ".security_master_prefs_key"
    }

    @Mock
    private lateinit var prefs: SharedPreferences
    @Mock
    private lateinit var editor: SharedPreferences.Editor
    @Mock
    private lateinit var rsa: Securable
    @Mock
    private lateinit var keyStore: KeyStore
    @Mock
    private lateinit var cipher: Cipher
    @Mock
    private lateinit var keyGenerator: KeyGenerator
    @Mock
    private lateinit var dateUtil: DateUtil
    @Mock
    private lateinit var keyGeneratorSpecBuilder: KeyGeneratorSpecBuilder
    @Mock
    private lateinit var base64Encoder: (ByteArray) -> String
    @Mock
    private lateinit var base64Decoder: (String) -> ByteArray

    private lateinit var securable: Securable

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        `when`(prefs.edit()).thenReturn(editor)
        securable = RsaWithDeprecatedAes(
            prefs,
            rsa,
            { keyStore },
            { cipher },
            base64Encoder,
            base64Decoder
        )
    }

    @After
    fun tearDown() {
        reset(
            prefs,
            editor,
            rsa,
            keyStore,
            cipher,
            keyGenerator,
            dateUtil,
            keyGeneratorSpecBuilder,
            base64Encoder,
            base64Decoder
        )
    }

    @Test
    fun `initialize keys, first time`() {
        val key = "12345"
        val encryptedKey = key.toByteArray(Charsets.UTF_8)
        val encoded = Base64.getMimeEncoder().encode(encryptedKey).toString()
        val editor = mock(SharedPreferences.Editor::class.java)
        `when`(prefs.contains(PREFS_MASTER_KEY)).thenReturn(false)
        `when`(rsa.encrypt(MockMatchers.any<ByteArray>())).thenReturn(encryptedKey)
        `when`(base64Encoder(encryptedKey)).thenReturn(encoded)
        `when`(prefs.edit()).thenReturn(editor)

        RsaWithDeprecatedAes(
            prefs,
            rsa,
            { keyStore },
            { cipher },
            base64Encoder,
            base64Decoder
        )

        verify(editor).putString(PREFS_MASTER_KEY, encoded)
        verify(editor).apply()
    }

    @Test
    fun `initialize, symmetric key exists`() {
        val editor = mock(SharedPreferences.Editor::class.java)
        `when`(prefs.contains(PREFS_MASTER_KEY)).thenReturn(true)
        `when`(prefs.edit()).thenReturn(editor)

        RsaWithDeprecatedAes(
            prefs,
            rsa,
            { keyStore },
            { cipher },
            base64Encoder,
            base64Decoder
        )

        verify(rsa, never()).encrypt(ArgumentMatchers.anyString())
        verify(editor, never()).apply()
    }

    @Test
    fun `encrypt, non-null text`() {
        val content = "hello world"
        @Suppress("SpellCheckingInspection") val encrypted: ByteArray =
            ("DAhGl+vGx4ZYq/K5CTsfedQ/+8+larAp7nS145aHfoejGyoDi6RU/Q==\n").toByteArray(Charsets.UTF_8)

        //set the symmetric key in prefs
        prepareDecryptSymmetricKey("123")

        `when`(cipher.doFinal(any())).thenReturn(encrypted)

        val output = securable.encrypt(content)
        Assertions.assertThat(output).isEqualTo(encrypted)
    }

    @Test
    fun `encrypt, symmetric key missing`() {
        val content = "hello world"
        `when`(prefs.contains(PREFS_MASTER_KEY)).thenReturn(false)
        `when`(
            cipher.init(
                ArgumentMatchers.anyInt(),
                MockMatchers.any<SecretKey>()
            )
        ).thenThrow(InvalidKeyException::class.java)

        assertThatExceptionOfType(InvalidKeyException::class.java).isThrownBy { securable.encrypt(content) }
    }

    @Test
    fun `decrypt non-null text`() {
        val expected = ("hello world").toByteArray(Charsets.UTF_8)
        //acquired from device testing
        @Suppress("SpellCheckingInspection") val encodedPayload =
            "DHKzxurXQSD7wdKqrgCgZJt0IbOMeolMnmb1EFA1dxkXnZO2usaMHw==\n".toByteArray(Charsets.UTF_8)
        val payload: ByteArray = Base64.getMimeDecoder().decode(encodedPayload)

        //set the symmetric key in prefs
        prepareDecryptSymmetricKey("123")

        `when`(cipher.doFinal(any())).thenReturn(expected)

        val output = securable.decrypt(payload)
        assertThat(output).isEqualTo(expected)
    }

    private fun prepareDecryptSymmetricKey(symmetricKey: String) {
        val symmetricKeyDecoded = symmetricKey.toByteArray(Charsets.UTF_8)

        //set the symmetric key in prefs
        val symmetricKeyEncoded = Base64.getMimeEncoder().encodeToString(symmetricKeyDecoded)
        `when`(prefs.getString(PREFS_MASTER_KEY, null)).thenReturn(symmetricKeyEncoded)
        `when`(base64Decoder(symmetricKeyEncoded)).thenReturn(symmetricKeyDecoded)

        // simulate that we're passing in an rsa-encrypted base64 decoded symmetric key
        `when`(rsa.decrypt(symmetricKeyDecoded)).thenReturn(symmetricKeyDecoded)
    }

    // cases to handle
    // symmetric key is missing
    // rsa fails encryption
    // the payload for decryption is empty
    // the content is empty for encryption
}
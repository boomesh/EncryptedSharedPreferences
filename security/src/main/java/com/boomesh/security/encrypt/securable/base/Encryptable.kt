package com.boomesh.security.encrypt.securable.base

import java.nio.BufferUnderflowException
import java.security.InvalidKeyException

internal interface Encryptable {
    @Throws(BufferUnderflowException::class, InvalidKeyException::class)
    fun encrypt(bytes: ByteArray) : ByteArray

    @Throws(BufferUnderflowException::class, InvalidKeyException::class)
    fun encrypt(text: String) : ByteArray
}
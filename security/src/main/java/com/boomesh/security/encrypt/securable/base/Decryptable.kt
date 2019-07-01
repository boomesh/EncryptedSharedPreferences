package com.boomesh.security.encrypt.securable.base

import java.nio.BufferUnderflowException
import java.security.InvalidKeyException

interface Decryptable {
    @Throws(BufferUnderflowException::class, InvalidKeyException::class)
    fun decrypt(payload: ByteArray): ByteArray
}
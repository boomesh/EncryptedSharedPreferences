package com.boomesh.security.testhelpers

import org.mockito.Mockito


object MockMatchers {
    fun <T> any(): T = Mockito.any()
}
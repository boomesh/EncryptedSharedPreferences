package com.boomesh.security.common

import org.assertj.core.api.Assertions
import org.junit.Test

@Suppress("UsePropertyAccessSyntax")
class BuildConfigUtilTest {

    companion object {
        private const val M = 23
    }

    @Test
    fun `true if below M`() {
        val currentOS = 21
        val util = BuildConfigUtil(currentOS)
        Assertions.assertThat(util.isOSBelow(M)).isTrue()
    }

    @Test
    fun `false if below M`() {
        val currentOS = 99
        val util = BuildConfigUtil(currentOS)
        Assertions.assertThat(util.isOSBelow(M)).isFalse()
    }
}
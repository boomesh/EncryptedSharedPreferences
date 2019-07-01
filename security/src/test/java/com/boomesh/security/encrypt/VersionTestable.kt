@file:Suppress("unused")

package com.boomesh.security.encrypt

import com.boomesh.security.common.BuildConfigUtil
import org.mockito.Mockito

internal interface VersionTestable {

    fun setToBelowM(buildConfigUtil: BuildConfigUtil) {
        Mockito.`when`(buildConfigUtil.isOSBelow(Mockito.anyInt())).thenReturn(true)
    }

    fun setToAboveM(buildConfigUtil: BuildConfigUtil) {
        Mockito.`when`(buildConfigUtil.isOSBelow(Mockito.anyInt())).thenReturn(false)
    }
}
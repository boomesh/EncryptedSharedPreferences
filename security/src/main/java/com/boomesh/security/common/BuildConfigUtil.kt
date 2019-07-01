package com.boomesh.security.common

internal class BuildConfigUtil(private val androidOS: Int) {
    fun isOSBelow(sdk: Int): Boolean {
        return androidOS < sdk
    }
}
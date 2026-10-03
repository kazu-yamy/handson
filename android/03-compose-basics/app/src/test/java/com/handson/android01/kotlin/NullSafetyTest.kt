package com.handson.android01.kotlin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class NullSafetyTest {
    private val users: Map<String, String?> = mapOf("u1" to "taro", "u2" to null)

    @Test
    fun findNickname_returnsNullableValue() {
        assertEquals("taro", findNickname(users, "u1"))
        assertNull(findNickname(users, "u2"))
        assertNull(findNickname(users, "unknown"))
    }

    @Test
    fun safeCall_returnsNullWhenReceiverIsNull() {
        assertEquals(4, nicknameLength("taro"))
        assertNull(nicknameLength(null))
    }

    @Test
    fun elvis_providesFallback() {
        assertEquals("taro", displayName("taro"))
        assertEquals("名無し", displayName(null))
    }

    @Test
    fun forceUnwrap_throwsNullPointerException_whenNull() {
        assertEquals(4, forceLength("taro"))
        val e = assertThrows(NullPointerException::class.java) { forceLength(null) }
        println("NPE message: ${e.message}")
    }

    @Test
    fun let_runsOnlyWhenNotNull() {
        assertEquals("こんにちは、taro", greetIfPresent("taro"))
        assertNull(greetIfPresent(null))
    }

    @Test
    fun smartCast_afterNullCheck() {
        assertEquals(4, lengthOrZero("taro"))
        assertEquals(0, lengthOrZero(null))
    }

    @Test
    fun readTitle_handlesMissingExtras() {
        assertEquals("Lesson", readTitle(mapOf("title" to "Lesson")))
        assertEquals("untitled", readTitle(null))
        assertEquals("untitled", readTitle(mapOf("title" to 42)))
    }

    @Test
    fun lateinit_throwsUntilAssigned() {
        val profile = Profile()
        val e = assertThrows(UninitializedPropertyAccessException::class.java) { profile.name }
        println("lateinit message: ${e.message}")
        profile.name = "taro"
        assertEquals("taro", profile.name)
    }

    @Test
    fun byLazy_computesOnce() {
        val profile = Profile()
        profile.name = "taro"
        assertEquals("profile:taro", profile.summary)
        profile.name = "hanako"
        // 初回読み取りの値がキャッシュされる
        assertEquals("profile:taro", profile.summary)
    }
}

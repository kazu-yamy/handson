package com.handson.android01.kotlin

import org.junit.Assert.assertEquals
import org.junit.Test

class BasicsTest {
    @Test
    fun greet_usesDefaultArguments() {
        assertEquals("こんにちは、Android！", greet("Android"))
    }

    @Test
    fun greet_acceptsNamedArguments() {
        // 名前付き引数は順番を変えて渡せる。省略した greeting は既定値のまま
        assertEquals("Hello、Kotlin!", greet(punctuation = "!", name = "Kotlin", greeting = "Hello"))
        assertEquals("やあ、Compose！", greet("Compose", greeting = "やあ"))
    }

    @Test
    fun add_isSingleExpressionFunction() {
        assertEquals(5, add(2, 3))
    }

    @Test
    fun describeNumber_usesWhenExpression() {
        assertEquals("負の数", describeNumber(-1))
        assertEquals("ゼロ", describeNumber(0))
        assertEquals("正の偶数", describeNumber(4))
        assertEquals("正の奇数", describeNumber(7))
    }

    @Test
    fun dayType_matchesMultipleValues() {
        assertEquals("休日", dayType("土"))
        assertEquals("休日", dayType("日"))
        assertEquals("平日", dayType("月"))
    }

    @Test
    fun formatSum_usesStringTemplate() {
        assertEquals("2 + 3 = 5", formatSum(2, 3))
    }

    @Test
    fun var_canBeReassigned_andValIsTopLevelConstant() {
        counter = 0
        assertEquals(1, incrementCounter())
        assertEquals(2, incrementCounter())
        assertEquals("HandsonAndroid13", appName)
    }
}

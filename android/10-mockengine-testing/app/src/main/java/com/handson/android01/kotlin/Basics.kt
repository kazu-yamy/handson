package com.handson.android01.kotlin

// Step 1: 変数・関数・文字列テンプレート・when 式

// 再代入できない（Java の final 相当）。型は右辺から推論される（String）
val appName = "HandsonAndroid10"

// 再代入できる。型は推論されるが、ここでは Int のまま
var counter = 0

// デフォルト引数: 省略すると既定値が使われる
fun greet(name: String, greeting: String = "こんにちは", punctuation: String = "！"): String =
    "$greeting、$name$punctuation"

// 単一式関数: 戻り値の型を省略でき、= の右辺がそのまま戻り値になる
fun add(a: Int, b: Int): Int = a + b

// when は式として値を返せる。上から順に評価され、最初に一致した分岐が使われる
fun describeNumber(n: Int): String = when {
    n < 0 -> "負の数"
    n == 0 -> "ゼロ"
    n % 2 == 0 -> "正の偶数"
    else -> "正の奇数"
}

// 値そのものと比較する when（複数の値を , で並べられる）
fun dayType(day: String): String = when (day) {
    "土", "日" -> "休日"
    else -> "平日"
}

// 文字列テンプレート: $変数名、または ${式}
fun formatSum(a: Int, b: Int): String = "${a} + ${b} = ${add(a, b)}"

fun incrementCounter(): Int {
    counter++
    return counter
}

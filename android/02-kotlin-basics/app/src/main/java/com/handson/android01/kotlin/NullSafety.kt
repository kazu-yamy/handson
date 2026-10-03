package com.handson.android01.kotlin

// Step 2: null 安全

// 型に ? を付けると null を入れられる。String は null を入れられない
fun findNickname(users: Map<String, String?>, id: String): String? = users[id]

// ?. 安全呼び出し: レシーバーが null なら式全体が null になる
fun nicknameLength(nickname: String?): Int? = nickname?.length

// ?: エルビス演算子: 左辺が null のときだけ右辺を使う
fun displayName(nickname: String?): String = nickname ?: "名無し"

// !! 非 null アサーション: null なら NullPointerException を投げる
fun forceLength(nickname: String?): Int = nickname!!.length

// let: null でないときだけブロックを実行する（it は非 null の値）
fun greetIfPresent(nickname: String?): String? = nickname?.let { "こんにちは、$it" }

// スマートキャスト: null チェックの後は String として扱える（!! は不要）
fun lengthOrZero(text: String?): Int {
    if (text != null) {
        return text.length
    }
    return 0
}

// Android では Intent.extras（Bundle）のように、値が無いかもしれない入力が多い。
// Bundle の代わりに Map で模擬する
fun readTitle(extras: Map<String, Any?>?): String {
    val title = extras?.get("title") as? String
    return title ?: "untitled"
}

class Profile {
    // lateinit: 後から代入する前提の var。代入前に読むと例外になる。
    // プリミティブ型（Int など）と null 許容型（String?）には付けられない
    lateinit var name: String

    // by lazy: 初めて読んだときに一度だけ計算される val
    val summary: String by lazy { "profile:$name" }
}

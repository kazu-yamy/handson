package com.handson.android01.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

// 画面を表す「キー」。Navigation 3 では、バックスタックの中身はこのキーが並んだただのリスト。
// @Serializable: キーを保存できる形（Bundle に入る値）に変換するための印。rememberNavBackStack は、画面回転やプロセスの再生成のときにバックスタックをこの形で保存する。
// NavKey は印を付けるだけのインターフェース（メソッドは無い）。object は引数の無い画面、data class は引数付きの画面に使う
@Serializable
data object TodoList : NavKey

// 詳細画面は、どの Todo を見せるかを id で持つ。キーが画面の「引数」になる
@Serializable
data class TodoDetail(val id: Int) : NavKey

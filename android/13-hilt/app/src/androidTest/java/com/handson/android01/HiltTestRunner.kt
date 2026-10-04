package com.handson.android01

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

// UI テストでは、TodoApplication の代わりに HiltTestApplication を起動する。
// HiltTestApplication は、テストごとに Hilt のコンポーネントを作り直す（@HiltAndroidTest を付けたテストで有効）
class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader?, className: String?, context: Context?): Application =
        super.newApplication(cl, HiltTestApplication::class.java.name, context)
}

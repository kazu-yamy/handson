package com.handson.android01

import android.app.Application
import com.handson.android01.data.AppContainer

// アプリのプロセスが作られたときに、Activity より先に 1 度だけ作られる。
// DB を作るには Context が要るので、ここで AppContainer に渡す
class TodoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContainer.init(this)
    }
}

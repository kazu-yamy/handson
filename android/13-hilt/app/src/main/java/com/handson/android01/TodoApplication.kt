package com.handson.android01

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

// Hilt の入口。この注釈を付けると、アプリ全体の部品の置き場所（コンポーネント）がここで作られる
@HiltAndroidApp
class TodoApplication : Application()

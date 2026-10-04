package com.handson.android01.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.handson.android01.ui.screens.FakeTodoDao
import org.junit.runner.RunWith

// 偽物も、本物と同じ約束を守っているかを確かめる
@RunWith(AndroidJUnit4::class)
class FakeTodoDaoTest : TodoDaoContract() {

    override val dao: TodoDao = FakeTodoDao()
}

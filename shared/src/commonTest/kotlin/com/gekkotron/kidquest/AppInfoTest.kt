package com.gekkotron.kidquest

import kotlin.test.Test
import kotlin.test.assertEquals

class AppInfoTest {
    @Test
    fun appNameIsKidQuest() {
        assertEquals("KidQuest", AppInfo.NAME)
    }
}

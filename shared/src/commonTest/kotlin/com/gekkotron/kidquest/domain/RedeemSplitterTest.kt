package com.gekkotron.kidquest.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RedeemSplitterTest {
    @Test fun soloTakesAll() = assertEquals(mapOf(1L to 1000), RedeemSplitter.equal(1000, listOf(1L)))
    @Test fun evenSplit() = assertEquals(mapOf(1L to 500, 2L to 500), RedeemSplitter.equal(1000, listOf(1L, 2L)))

    @Test
    fun remainderGoesToEarliestMembers() {
        assertEquals(mapOf(7L to 501, 3L to 500), RedeemSplitter.equal(1001, listOf(7L, 3L)))
        assertEquals(mapOf(1L to 34, 2L to 33, 3L to 33), RedeemSplitter.equal(100, listOf(1L, 2L, 3L)))
    }

    @Test
    fun validSplit() {
        assertTrue(RedeemSplitter.isValid(1000, setOf(1L, 2L), mapOf(1L to 700, 2L to 300)))
        assertTrue(RedeemSplitter.isValid(1000, setOf(1L, 2L), mapOf(1L to 1000, 2L to 0)))
    }

    @Test
    fun invalidSplits() {
        assertFalse(RedeemSplitter.isValid(1000, setOf(1L, 2L), mapOf(1L to 700, 2L to 200)), "sum too low")
        assertFalse(RedeemSplitter.isValid(1000, setOf(1L, 2L), mapOf(1L to 1100, 2L to -100)), "negative share")
        assertFalse(RedeemSplitter.isValid(1000, setOf(1L, 2L), mapOf(1L to 1000)), "missing member")
        assertFalse(RedeemSplitter.isValid(1000, setOf(1L), mapOf(1L to 500, 9L to 500)), "foreign member")
    }
}

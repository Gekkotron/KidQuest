package com.gekkotron.kidquest.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals

class ColorWheelTest {
    private val eps = 0.001f

    @Test
    fun rightOfCentreIsHueZero() {
        val (h, s) = wheelPositionToHueSat(50f, 0f, 100f)
        assertEquals(0f, h, eps)
        assertEquals(0.5f, s, eps)
    }

    @Test
    fun belowCentreIsHue90AndLeftIsHue180() {
        assertEquals(90f, wheelPositionToHueSat(0f, 100f, 100f).first, eps)
        assertEquals(180f, wheelPositionToHueSat(-100f, 0f, 100f).first, eps)
        assertEquals(270f, wheelPositionToHueSat(0f, -100f, 100f).first, eps)
    }

    @Test
    fun centreHasZeroSaturation() {
        assertEquals(0f, wheelPositionToHueSat(0f, 0f, 100f).second, eps)
    }

    @Test
    fun beyondRadiusClampsToOne() {
        assertEquals(1f, wheelPositionToHueSat(300f, 400f, 100f).second, eps)
    }
}

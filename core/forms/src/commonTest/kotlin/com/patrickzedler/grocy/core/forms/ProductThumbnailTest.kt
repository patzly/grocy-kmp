package com.patrickzedler.grocy.core.forms

import kotlin.test.Test
import kotlin.test.assertEquals

class ProductThumbnailTest {

    @Test
    fun thereAreTwentyFourDifferentShapes() {
        assertEquals(24, ThumbnailShapes.distinct().size)
    }

    @Test
    fun aPictureAlwaysGetsTheSameShape() {
        // A fixed literal, so a picture keeps its shape on every platform and after every start
        assertEquals(11, thumbnailShapeIndex("pizza.jpg"))
    }

    @Test
    fun picturesGetDifferentShapes() {
        val names = (1..100).map { "picture$it.jpg" }

        val shapes = names.map(::thumbnailShapeIndex).toSet()

        assertEquals(24, shapes.size)
    }

    @Test
    fun negativeHashesStillPickAShape() {
        // Its hash overflows into a negative number
        val index = thumbnailShapeIndex("chocolate sauce.jpg")

        assertEquals(16, index)
    }
}

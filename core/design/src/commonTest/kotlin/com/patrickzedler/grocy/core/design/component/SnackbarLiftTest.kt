package com.patrickzedler.grocy.core.design.component

import kotlin.test.Test
import kotlin.test.assertEquals

class SnackbarLiftTest {

    @Test
    fun snackbarsLieAboveAShownToolbar() {
        assertEquals(200, snackbarLift(200f, 0f, -200f))
    }

    @Test
    fun snackbarsFollowTheToolbarWhileItSlidesOut() {
        assertEquals(50, snackbarLift(200f, -150f, -200f))
    }

    @Test
    fun snackbarsReturnToTheBottomOnceTheToolbarIsGone() {
        assertEquals(0, snackbarLift(200f, -200f, -200f))
    }

    @Test
    fun withoutToolbarSnackbarsStayAtTheBottom() {
        assertEquals(0, snackbarLift(0f, 0f, 0f))
    }
}

package com.patrickzedler.grocy.core.design.component

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isUnspecified
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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

    @Test
    fun snackbarsKeepTheScreenOffsetAboveTheToolbar() {
        // 16 offset + 64 bar + 4, Material's snackbar brings the other 12
        assertEquals(84.dp, snackbarSpace(hasToolbar = true, isPane = false))
    }

    @Test
    fun aPanesToolbarLiesTheMarginHigher() {
        assertEquals(100.dp, snackbarSpace(hasToolbar = true, isPane = true))
    }

    @Test
    fun withoutToolbarSnackbarsNeedNoSpace() {
        assertEquals(0.dp, snackbarSpace(hasToolbar = false, isPane = true))
    }

    @Test
    fun oneOfTwoPanesHasNoWidthLimit() {
        assertTrue(snackbarMaxWidth(ScreenPane.List).isUnspecified)
        assertTrue(snackbarMaxWidth(ScreenPane.Detail).isUnspecified)
    }

    @Test
    fun otherScreensKeepMaterialsWidthLimit() {
        assertEquals(624.dp, snackbarMaxWidth(ScreenPane.Single))
        assertEquals(624.dp, snackbarMaxWidth(null))
    }
}

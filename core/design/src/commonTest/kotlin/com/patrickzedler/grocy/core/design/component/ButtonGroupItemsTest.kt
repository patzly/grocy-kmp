package com.patrickzedler.grocy.core.design.component

import androidx.compose.material3.IconButtonDefaults.IconButtonWidthOption
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class ButtonGroupItemsTest {

    @Test
    fun groupEdgesKeepTheRoomOfSingleButtonsTouchTargets() {
        // 32.dp narrow and 40.dp uniform containers in 48.dp touch targets
        assertEquals(8.dp, touchTargetRoom(IconButtonWidthOption.Narrow))
        assertEquals(4.dp, touchTargetRoom(IconButtonWidthOption.Uniform))
    }

    @Test
    fun wideButtonsNeedNoRoomBesideThem() {
        // 52.dp wide, wider than the touch target
        assertEquals(0.dp, touchTargetRoom(IconButtonWidthOption.Wide))
    }
}

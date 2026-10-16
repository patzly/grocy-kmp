package com.patrickzedler.grocy.core.update.play

import com.google.android.play.core.install.model.UpdateAvailability
import com.patrickzedler.grocy.core.model.UpdateCheck
import kotlin.test.Test
import kotlin.test.assertEquals

class PlayUpdateCheckTest {

    @Test
    fun updateOnPlayIsAvailableWithoutVersionName() {
        assertEquals(
            UpdateCheck.Available(versionName = null),
            updateCheckOf(UpdateAvailability.UPDATE_AVAILABLE),
        )
    }

    @Test
    fun interruptedUpdateIsOfferedAgain() {
        assertEquals(
            UpdateCheck.Available(versionName = null),
            updateCheckOf(UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS),
        )
    }

    @Test
    fun noUpdateIsUpToDate() {
        assertEquals(UpdateCheck.UpToDate, updateCheckOf(UpdateAvailability.UPDATE_NOT_AVAILABLE))
    }

    @Test
    fun unknownAvailabilityFails() {
        assertEquals(UpdateCheck.Failed, updateCheckOf(UpdateAvailability.UNKNOWN))
    }
}

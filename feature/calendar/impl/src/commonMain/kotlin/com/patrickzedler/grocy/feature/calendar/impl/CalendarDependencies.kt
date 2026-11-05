package com.patrickzedler.grocy.feature.calendar.impl

import com.patrickzedler.grocy.core.data.calendar.CalendarRepository
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository

/** What Grocy's calendar needs from the app. */
interface CalendarDependencies {
    val masterDataRepository: MasterDataRepository

    val calendarRepository: CalendarRepository
}

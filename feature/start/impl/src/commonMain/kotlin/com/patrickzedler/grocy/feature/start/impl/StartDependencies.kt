package com.patrickzedler.grocy.feature.start.impl

import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository

/** What the start screen needs from the app. */
interface StartDependencies {
    val masterDataRepository: MasterDataRepository
}

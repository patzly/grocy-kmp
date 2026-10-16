package com.patrickzedler.grocy.feature.about.impl

import com.patrickzedler.grocy.core.data.unlock.UnlockRepository
import com.patrickzedler.grocy.core.data.update.UpdateRepository

interface AboutDependencies {
    val unlockRepository: UnlockRepository
    val updateRepository: UpdateRepository
    val versionName: String
}

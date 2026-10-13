package com.patrickzedler.grocy.feature.about.impl

import com.patrickzedler.grocy.core.data.unlock.UnlockRepository

interface AboutDependencies {
    val unlockRepository: UnlockRepository
    val versionName: String
}

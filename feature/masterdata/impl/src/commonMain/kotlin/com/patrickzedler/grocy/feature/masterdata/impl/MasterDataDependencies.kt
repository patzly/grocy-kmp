package com.patrickzedler.grocy.feature.masterdata.impl

import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository

/** What the master data screens need from the app. */
interface MasterDataDependencies {
    val masterDataRepository: MasterDataRepository
}

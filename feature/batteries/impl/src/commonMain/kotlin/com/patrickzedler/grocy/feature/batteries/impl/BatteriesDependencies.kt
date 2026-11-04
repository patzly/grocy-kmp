package com.patrickzedler.grocy.feature.batteries.impl

import com.patrickzedler.grocy.core.data.batteries.BatteriesRepository
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.settings.BatteriesJournalOptionsRepository
import com.patrickzedler.grocy.core.data.settings.BatteriesOverviewOptionsRepository
import com.patrickzedler.grocy.core.data.settings.ScannerFormsRepository

/** What Grocy's batteries need from the app. */
interface BatteriesDependencies {
    val masterDataRepository: MasterDataRepository

    /** The batteries overview, its journal and tracking. */
    val batteriesRepository: BatteriesRepository

    /** Fields, grouping and order of the batteries overview, read at app start. */
    val batteriesOverviewOptions: BatteriesOverviewOptionsRepository

    /** Fields, grouping and order of the batteries journal, read at app start. */
    val batteriesJournalOptions: BatteriesJournalOptionsRepository

    /** The forms that open with their scanner, read at app start. */
    val scannerForms: ScannerFormsRepository
}

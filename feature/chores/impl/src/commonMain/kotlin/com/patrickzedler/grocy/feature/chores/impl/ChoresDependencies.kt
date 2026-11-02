package com.patrickzedler.grocy.feature.chores.impl

import com.patrickzedler.grocy.core.data.chores.ChoresRepository
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.settings.ChoresJournalOptionsRepository
import com.patrickzedler.grocy.core.data.settings.ChoresOverviewOptionsRepository
import com.patrickzedler.grocy.core.data.settings.ScannerFormsRepository

/** What Grocy's chores need from the app. */
interface ChoresDependencies {
    val masterDataRepository: MasterDataRepository

    /** The chores overview, its journal and tracking. */
    val choresRepository: ChoresRepository

    /** Fields, grouping and order of the chores overview, read at app start. */
    val choresOverviewOptions: ChoresOverviewOptionsRepository

    /** Fields, grouping and order of the chores journal, read at app start. */
    val choresJournalOptions: ChoresJournalOptionsRepository

    /** The forms that open with their scanner, read at app start. */
    val scannerForms: ScannerFormsRepository
}

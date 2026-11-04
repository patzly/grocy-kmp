package com.patrickzedler.grocy.feature.equipment.impl

import com.patrickzedler.grocy.core.data.equipment.EquipmentRepository
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.settings.EquipmentOptionsRepository

/** What Grocy's equipment needs from the app. */
interface EquipmentDependencies {
    val masterDataRepository: MasterDataRepository
    val equipmentRepository: EquipmentRepository

    /** Fields, grouping and order of the equipment list, read at app start. */
    val equipmentOptions: EquipmentOptionsRepository
}

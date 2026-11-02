package com.patrickzedler.grocy.feature.tasks.impl

import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.settings.TasksOverviewOptionsRepository
import com.patrickzedler.grocy.core.data.tasks.TasksRepository

/** What Grocy's tasks need from the app. */
interface TasksDependencies {
    val masterDataRepository: MasterDataRepository

    val tasksRepository: TasksRepository

    /** Fields, grouping and order of the tasks page, read at app start. */
    val tasksOverviewOptions: TasksOverviewOptionsRepository
}

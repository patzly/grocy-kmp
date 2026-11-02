package com.patrickzedler.grocy.feature.tasks.impl

import com.patrickzedler.grocy.feature.masterdata.api.TaskCategoriesRoute
import com.patrickzedler.grocy.feature.tasks.api.TaskRoute
import com.patrickzedler.grocy.feature.tasks.api.TasksRoute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TasksEntriesTest {

    @Test
    fun aTasksFormMarksItInTheList() {
        assertEquals(3, selectedTaskOf(TaskRoute(3)))
        assertNull(selectedTaskOf(TaskRoute()))
        assertNull(selectedTaskOf(TasksRoute))
    }

    @Test
    fun aFormBesideTheListKeepsItShownOthersDoNot() {
        assertTrue(isInTasksScene(TasksRoute))
        assertTrue(isInTasksScene(TaskRoute(3)))
        assertFalse(isInTasksScene(TaskRoute(inList = false)))
        assertFalse(isInTasksScene(TaskCategoriesRoute))
    }
}

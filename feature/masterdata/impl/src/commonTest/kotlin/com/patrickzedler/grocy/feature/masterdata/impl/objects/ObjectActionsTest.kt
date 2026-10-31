package com.patrickzedler.grocy.feature.masterdata.impl.objects

import com.patrickzedler.grocy.core.forms.ObjectAction
import com.patrickzedler.grocy.core.forms.objectActionsOf
import kotlin.test.Test
import kotlin.test.assertEquals

class ObjectActionsTest {

    @Test
    fun everyOtherListCanOnlyDelete() {
        val others = ObjectListKind.entries.filter { it != ObjectListKind.CHORES }

        for (kind in others) {
            assertEquals(listOf(ObjectAction.DELETE), objectActionsOf(kind.entity), kind.name)
        }
    }

    @Test
    fun everyListHasItsOwnEntity() {
        val entities = ObjectListKind.entries.map { it.entity }

        assertEquals(entities.size, entities.toSet().size)
    }
}

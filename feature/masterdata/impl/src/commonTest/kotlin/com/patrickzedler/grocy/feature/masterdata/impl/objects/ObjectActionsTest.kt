package com.patrickzedler.grocy.feature.masterdata.impl.objects

import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ObjectActionsTest {

    @Test
    fun productsCanBeCopiedMergedAndDeletedLikeInGrocy() {
        assertEquals(
            listOf(ObjectAction.COPY, ObjectAction.MERGE, ObjectAction.DELETE),
            objectActionsOf(MasterDataRepository.PRODUCTS),
        )
    }

    @Test
    fun choresCanBeMergedAndDeleted() {
        assertEquals(
            listOf(ObjectAction.MERGE, ObjectAction.DELETE),
            objectActionsOf(MasterDataRepository.CHORES),
        )
    }

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

    @Test
    fun mergeNeedsTwoDifferentObjects() {
        assertEquals(MergeError.REQUIRED, mergeErrorOf(keepId = 1, removeId = null))
        assertEquals(MergeError.REQUIRED, mergeErrorOf(keepId = null, removeId = 2))
        assertEquals(MergeError.SAME, mergeErrorOf(keepId = 1, removeId = 1))
        assertNull(mergeErrorOf(keepId = 1, removeId = 2))
    }
}

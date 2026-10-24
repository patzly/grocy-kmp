package com.patrickzedler.grocy.feature.masterdata.impl.objects

import kotlin.test.Test
import kotlin.test.assertEquals

class ObjectListTest {

    private fun row(
        id: Int,
        name: String,
        description: String? = null,
        active: Boolean = true,
        count: Int? = null,
        entity: String? = null,
    ) = ObjectRow(
        id = id,
        active = active,
        texts = listOfNotNull(
            ObjectColumn.NAME to name,
            description?.let { ObjectColumn.DESCRIPTION to it },
            count?.let { ObjectColumn.PRODUCT_COUNT to "$it products" },
        ).toMap(),
        numbers = count?.let { mapOf(ObjectColumn.PRODUCT_COUNT to it.toDouble()) }.orEmpty(),
        entity = entity,
    )

    private val rows = listOf(
        row(1, "pantry", "Dry food", count = 10),
        row(2, "Fridge", count = 2),
        row(3, "Old shelf", "Removed", active = false, count = 0),
        row(4, "Ärmel", "Clothes", count = 3),
    )

    private fun ids(
        filter: ObjectFilter = ObjectFilter(),
        sort: ObjectSort = ObjectSort(ObjectColumn.NAME),
        query: String = "",
        list: List<ObjectRow> = rows,
    ) = buildObjectList(list, filter, sort, query).map { it.id }

    @Test
    fun activeObjectsAreSortedByNameIgnoringCaseAndAccents() {
        assertEquals(listOf(4, 2, 1), ids())
    }

    @Test
    fun disabledObjectsOnlyShowWithTheirFilter() {
        assertEquals(listOf(4, 2, 3, 1), ids(ObjectFilter(showDisabled = true)))
    }

    @Test
    fun namesSortDescending() {
        assertEquals(listOf(1, 2, 4), ids(sort = ObjectSort(ObjectColumn.NAME, ascending = false)))
    }

    @Test
    fun numbersSortByValueNotText() {
        val sort = ObjectSort(ObjectColumn.PRODUCT_COUNT)

        assertEquals(listOf(2, 4, 1), ids(sort = sort))
        assertEquals(listOf(1, 4, 2), ids(sort = sort.copy(ascending = false)))
    }

    @Test
    fun emptyValuesComeLastInBothDirections() {
        val sort = ObjectSort(ObjectColumn.DESCRIPTION)

        assertEquals(listOf(4, 1, 2), ids(sort = sort))
        assertEquals(listOf(1, 4, 2), ids(sort = sort.copy(ascending = false)))
    }

    @Test
    fun searchLooksAtEveryColumn() {
        assertEquals(listOf(1), ids(query = "dry"))
        assertEquals(listOf(4), ids(query = "clothes"))
    }

    @Test
    fun searchKeepsTheDisabledFilter() {
        assertEquals(emptyList(), ids(query = "removed"))
        assertEquals(listOf(3), ids(ObjectFilter(showDisabled = true), query = "removed"))
    }

    @Test
    fun userfieldsAreFilteredByEntity() {
        val fields = listOf(row(1, "brand", entity = "products"), row(2, "room", entity = "chores"))

        assertEquals(listOf(2), ids(ObjectFilter(entity = "chores"), list = fields))
    }

    @Test
    fun aHeadlineOtherThanTheNameDecidesTies() {
        val fields = listOf(
            ObjectRow(1, texts = mapOf(ObjectColumn.ENTITY to "a", ObjectColumn.CAPTION to "Z")),
            ObjectRow(2, texts = mapOf(ObjectColumn.ENTITY to "a", ObjectColumn.CAPTION to "B")),
        )

        val sorted = buildObjectList(
            fields,
            ObjectFilter(),
            ObjectSort(ObjectColumn.ENTITY),
            query = "",
            nameColumn = ObjectColumn.CAPTION,
        )

        assertEquals(listOf(2, 1), sorted.map { it.id })
    }

    @Test
    fun theFilterIsActiveOnlyAwayFromItsDefaults() {
        assertEquals(false, ObjectFilter().isActive)
        assertEquals(true, ObjectFilter(showDisabled = true).isActive)
        assertEquals(true, ObjectFilter(entity = "chores").isActive)
    }

    @Test
    fun userfieldEntitiesFollowGrocysListSorted() {
        val entities = userfieldEntities(listOf("books"))

        assertEquals(entities.sorted(), entities)
        assertEquals(true, "userentity-books" in entities)
        assertEquals(true, "users" in entities)
        assertEquals(true, "shopping_locations" in entities)
    }
}

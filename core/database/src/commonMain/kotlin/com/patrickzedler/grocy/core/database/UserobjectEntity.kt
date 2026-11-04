package com.patrickzedler.grocy.core.database

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/** An object of a userentity with the values of its fields as JSON. */
@Entity(tableName = "userobjects")
data class UserobjectEntity(
    @PrimaryKey val id: Int,
    val userentityId: Int,
    val userfields: String,
)

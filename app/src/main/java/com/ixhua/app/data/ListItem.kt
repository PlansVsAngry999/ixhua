
package com.ixhua.app.data

data class ListItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val color: String = "PURPLE",
    val createdAt: Long = System.currentTimeMillis()
)

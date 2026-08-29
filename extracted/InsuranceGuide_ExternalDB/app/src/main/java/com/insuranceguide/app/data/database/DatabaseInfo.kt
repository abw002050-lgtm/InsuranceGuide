package com.insuranceguide.app.data.database

data class DatabaseInfo(
    val name: String,
    val sizeBytes: Long,
    val isValid: Boolean,
    val isCompatible: Boolean,
    val missingTables: List<String> = emptyList(),
    val tableCounts: Map<String, Long> = emptyMap(),
    val error: String? = null
)

package com.insuranceguide.app.data.database

data class Employee(
    val id: Long,
    val employeeNo: Long?,
    val name: String,
    val birthDate: String?,
    val joinDate: String?,
    val outDate: String?,
    val duration: String?,
    val mark: String?,
    val salary: Long?,
    val location: String?
)

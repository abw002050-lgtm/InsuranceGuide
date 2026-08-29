package com.insuranceguide.app.data.database

data class Pension(
    val id: Long, val name: String, val linkNo: Long?, val insuranceNo: Long?,
    val fullAccount: String?, val typeCode: String?, val officeName: String?,
    val kindType: String?, val reasonType: String?, val wakeelName: String?,
    val loan: Long?, val total: Long?, val net: Long?, val company: String?,
    val birthDate: String?, val joinDate: String?, val outDate: String?, val linkDate: String?,
    val branchNo: Long?, val branchName: String?, val fullMark: String?
)

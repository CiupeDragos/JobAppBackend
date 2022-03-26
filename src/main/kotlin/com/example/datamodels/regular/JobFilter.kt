package com.example.datamodels.regular

data class JobFilter(
    val jobType: String?,
    val jobMinSalary: Int?,
    val jobLocation: String?,
    val jobRemote: String?
)

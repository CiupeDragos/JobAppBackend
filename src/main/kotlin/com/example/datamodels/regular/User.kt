package com.example.datamodels.regular

import org.bson.codecs.pojo.annotations.BsonId

data class User(
    @BsonId
    val userID: String,
    val username: String,
    val password: String,
    val postedJobsIDs: List<String>,
    val savedJobsIDs: List<String>,
    val jobApplicationsIDs: List<String>,
    var realName: String = "",
    var phoneNumber: String = "",
    var email: String = "",
    var profilePicUrl: String = ""
)

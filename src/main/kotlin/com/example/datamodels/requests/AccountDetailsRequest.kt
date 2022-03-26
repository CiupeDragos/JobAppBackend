package com.example.datamodels.requests

data class AccountDetailsRequest(
    val profilePicUrl: String,
    val username: String,
    val realName: String,
    val phoneNumber: String,
    val email: String
)

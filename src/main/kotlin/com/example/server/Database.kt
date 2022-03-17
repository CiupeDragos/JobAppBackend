package com.example.server

import com.example.datamodels.regular.JobPost
import com.example.datamodels.regular.User
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo

val client = KMongo.createClient().coroutine
val database = client.getDatabase("JobAppDatabase")

val users = database.getCollection<User>("users")
val jobPosts = database.getCollection<JobPost>("job_posts")
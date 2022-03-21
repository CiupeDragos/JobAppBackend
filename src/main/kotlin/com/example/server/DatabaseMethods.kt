package com.example.server

import com.example.datamodels.regular.JobFilter
import com.example.datamodels.regular.JobPost
import com.example.datamodels.regular.User
import com.example.security.checkHashForPassword
import org.litote.kmongo.*
import java.util.*

suspend fun findUser(username: String): User? {
    return users.findOne(User::username eq username)
}

suspend fun findJobPost(jobID: String): JobPost? {
    return jobPosts.findOneById(jobID)
}

suspend fun checkIfUserExists(username: String): Boolean {
    val userToFind = users.findOne(User::username eq username)
    return userToFind != null
}

suspend fun checkIfJobExists(jobId: String): Boolean {
    val jobToFind = jobPosts.findOneById(jobId)
    return jobToFind != null
}

suspend fun addUser(
    username: String,
    password: String,
    postedJobsIDs: List<String> = listOf(),
    savedJobIDs: List<String> = listOf(),
    jobApplicationsIDs: List<String> = listOf()
): Boolean {
    val userID = UUID.randomUUID().toString()
    val userToAdd = User(userID, username, password, postedJobsIDs, savedJobIDs, jobApplicationsIDs)

    return users.insertOne(userToAdd).wasAcknowledged()
}

suspend fun addJobPost(jobPost: JobPost, jobLogoImageName: String?): Boolean {

    val jobPostToAdd: JobPost = if(jobLogoImageName != null) {
        jobPost.copy(
            jobTimestamp = System.currentTimeMillis(),
            jobImageUrl = "/assets/$jobLogoImageName"
        )
    } else {
        jobPost.copy(
            jobTimestamp = System.currentTimeMillis()
        )
    }

    val user = findUser(jobPost.jobCreatorUsername) ?: return false
    val modifiedPostedJobs = user.postedJobsIDs.toMutableList().apply { add(jobPostToAdd.jobID) }.toList()
    val modifiedUser = user.copy(postedJobsIDs = modifiedPostedJobs)
    users.replaceOneById(user.userID, modifiedUser)

    return jobPosts.insertOne(jobPostToAdd).wasAcknowledged()
}

suspend fun saveJobPost(jobID: String, username: String) {
    val user = findUser(username) ?: return
    val modifiedSavedJobs = user.savedJobsIDs.toMutableList().apply { add(jobID) }.toList()
    users.replaceOneById(user.userID, user.copy(savedJobsIDs = modifiedSavedJobs))
}

suspend fun deleteJobPost(jobID: String, jobCreator: String): Boolean {
    val allUsers = users.find().toList()
    val jobOwner = findUser(jobCreator) ?: return false
    val modifiedJobPosts = jobOwner.postedJobsIDs.toMutableList().apply { remove(jobID) }.toList()

    users.replaceOneById(jobOwner.userID, jobOwner.copy(postedJobsIDs = modifiedJobPosts))

    allUsers.forEach { curUser ->
        if(jobID in curUser.savedJobsIDs) {
            val savedJobs = curUser.savedJobsIDs.toMutableList().apply { remove(jobID) }
            val modifiedUser = curUser.copy(savedJobsIDs = savedJobs.toList())
            users.replaceOneById(curUser.userID, modifiedUser)
        }
    }

    return jobPosts.deleteOneById(jobID).wasAcknowledged()
}

suspend fun loginUser(username: String, password: String): Boolean {
    val userToFind = users.findOne(User::username eq username)!!
    val actualUserPassword = userToFind.password
    return checkHashForPassword(actualUserPassword, password)
}

suspend fun findFilteredJobs(jobFilter: JobFilter, searchQuery: String, requesterUsername: String): List<JobPost> {

    val savedJobs = findUser(requesterUsername)!!.savedJobsIDs
    //val lowercaseQuery = searchQuery.lowercase()

    return jobPosts.find(
        JobPost::jobTitle regex (if(searchQuery.isEmpty()) "\\w*" else "\\w*$searchQuery\\w*"),
        JobPost::jobType regex (if(jobFilter.jobType != null) "${jobFilter.jobType}" else "\\w+"),
        JobPost::jobSalary gte (jobFilter.jobMinSalary ?: 0),
        JobPost::jobLocation regex (if(jobFilter.jobLocation != null) "\\w*${jobFilter.jobLocation}\\w*" else "\\w+")
    ).toList().apply {
        map { jobPost ->
            jobPost.isAddedToFavourites = jobPost.jobID in savedJobs
        }
    }
    /*
    val jobPostList = jobPosts.find().toList()
    jobPostList.filter { curJobPost ->
        ((curJobPost.jobTitle.lowercase()).contains(lowercaseQuery)) &&
        (curJobPost.jobType.contains((jobFilter.jobType ?: ""))) &&
        (curJobPost.jobSalary >= (jobFilter.jobMinSalary ?: 0)) &&
        (curJobPost.jobLocation.lowercase().contains((jobFilter.jobLocation?.lowercase() ?: "")))
    }.map { curJobPost ->
        curJobPost.isAddedToFavourites = curJobPost.jobID in savedJobs
    }

    return jobPostList
    */
}

suspend fun getSavedJobs(accountUsername: String): List<JobPost> {
    val savedJobsIDs =  findUser(accountUsername)?.savedJobsIDs ?: return listOf()
    val savedJobsList = mutableListOf<JobPost>()
    savedJobsIDs.forEach { jobID ->
        findJobPost(jobID)?.also { savedJobsList.add(it) }
    }
    return savedJobsList
}
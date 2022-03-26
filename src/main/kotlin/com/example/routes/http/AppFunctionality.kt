package com.example.routes.http

import com.example.datamodels.regular.JobFilter
import com.example.datamodels.regular.JobPost
import com.example.datamodels.requests.AccountDetailsRequest
import com.example.datamodels.requests.FavouriteJobRequest
import com.example.datamodels.responses.BasicApiResponse
import com.example.other.Constants.ASSETS_FOLDER
import com.example.server.*
import com.google.gson.Gson
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.InternalServerError
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.File

fun Route.createJobPost() {
    post("/createJobPost") {
        var jobPost: JobPost? = null
        var fileName: String? = null
        val multipartData = call.receiveMultipart()
        multipartData.forEachPart { part ->
            when (part) {
                is PartData.FormItem -> {
                    jobPost = Gson().fromJson(part.value, JobPost::class.java)
                }
                is PartData.FileItem -> {
                    fileName = part.originalFileName
                    val fileBytes = part.streamProvider().readBytes()
                    File("uploads/$fileName").writeBytes(fileBytes)
                }
                else -> {}
            }
        }
        if(jobPost == null) {
            call.respond(BadRequest, BasicApiResponse(false, "Bad request format"))
            return@post
        }
        if(!addJobPost(jobPost!!, fileName)) {
            call.respond(OK, BasicApiResponse(false, "Could not add the job"))
            return@post
        }
        call.respond(OK, BasicApiResponse(true, "Job uploaded successfully"))
    }
}

fun Route.deleteJobPost() {
    post("/deleteJobPost") {
        val jobID = call.parameters["jobID"]
        val username = call.parameters["username"]
        if(jobID == null || username == null) {
            call.respond(BadRequest, BasicApiResponse(false, "Bad request format"))
            return@post
        }
        if(!checkIfUserExists(username)) {
            call.respond(BadRequest, BasicApiResponse(false, "The username trying to delete this job doesn't exist"))
            return@post
        }
        val job = findJobPost(jobID)
        if(job == null) {
            call.respond(BadRequest, BasicApiResponse(false, "The job you are trying to delete does not exist"))
            return@post
        }
        if(job.jobCreatorUsername != username) {
            call.respond(BadRequest, BasicApiResponse(false, "You did not create this job"))
            return@post
        }
        if(deleteJobPost(jobID, username)) {
            call.respond(OK, BasicApiResponse(true, "The job was deleted successfully"))
        } else {
            call.respond(InternalServerError, BasicApiResponse(false, "An unknown error has occurred"))
        }
    }
}



fun Route.addJobToFavourites() {
    post("/addJobToFavourites") {
        val request = call.receiveOrNull<FavouriteJobRequest>()
        if(request == null) {
            call.respond(BadRequest, BasicApiResponse(false, "Bad request format"))
            return@post
        }
        if(!checkIfUserExists(request.accountUsername)) {
            call.respond(BadRequest, BasicApiResponse(false, "The username trying to add this job to favourites does not exist"))
            return@post
        }
        if(!checkIfJobExists(request.jobID)) {
            call.respond(BadRequest, BasicApiResponse(false, "The job you are trying to add to favourites,does not exist"))
            return@post
        }
        if(request.jobID in findUser(request.accountUsername)!!.savedJobsIDs) {
            call.respond(OK, BasicApiResponse(false, "You already saved this job"))
        } else {
            saveJobPost(request.jobID, request.accountUsername)
            call.respond(OK, BasicApiResponse(true, "The job has been saved"))
        }
    }
}

fun Route.deleteJobFromFavourites() {
    post("/deleteJobFromFavourites") {
        val request = call.receiveOrNull<FavouriteJobRequest>()
        if(request == null) {
            call.respond(BadRequest, BasicApiResponse(false, "Bad request format"))
            return@post
        }
        if(!checkIfUserExists(request.accountUsername)) {
            call.respond(BadRequest, BasicApiResponse(false, "The username trying to remove this job from favourites does not exist"))
            return@post
        }
        if(!checkIfJobExists(request.jobID)) {
            call.respond(BadRequest, BasicApiResponse(false, "The job you are trying to remove from favourites does not exist"))
            return@post
        }
        val user = findUser(request.accountUsername)!!
        if(request.jobID !in user.savedJobsIDs) {
            call.respond(OK, BasicApiResponse(false, "You don't have this job saved"))
        } else {
            val modifiedSaveJobsList = user.savedJobsIDs.toMutableList().apply { remove(request.jobID) }.toList()
            users.replaceOneById(user.userID, user.copy(savedJobsIDs = modifiedSaveJobsList))
            call.respond(OK, BasicApiResponse(true, "The job has been deleted from favourites"))
        }
    }
}

fun Route.getJobPosts() {
    get("/getJobPosts") {
        val filters = call.parameters["jobFilter"]
        val searchQuery = call.parameters["searchQuery"]
        val requesterUsername = call.parameters["requesterUsername"]

        if(filters == null || searchQuery == null) {
            call.respond(BadRequest, BasicApiResponse(false, "Bad request format"))
            return@get
        }

        if(requesterUsername == null) {
            call.respond(BadRequest, BasicApiResponse(false, "The user trying to get the jobs does not exist"))
            return@get
        }

        val jobFilter = Gson().fromJson(filters, JobFilter::class.java)
        val jobs = findFilteredJobs(jobFilter, searchQuery, requesterUsername)
        call.respond(OK, jobs)
    }
}

fun Route.getPostedJobs() {
    get("/getPostedJobs") {
        val username = call.parameters["accountUsername"]
        if(username == null) {
            call.respond(BadRequest, BasicApiResponse(false, "Bad request format"))
            return@get
        }
        if(!checkIfUserExists(username)) {
            call.respond(BadRequest, BasicApiResponse(false, "The user does not exist"))
            return@get
        }

        call.respond(OK, getPostedJobs(username))
    }
}

fun Route.getSavedJobs() {
    get("/getSavedJobs") {
        val username = call.parameters["accountUsername"]
        if(username == null){
            call.respond(BadRequest, BasicApiResponse(false, "Bad request format"))
            return@get
        }
        if(!checkIfUserExists(username)) {
            call.respond(BadRequest, BasicApiResponse(false, "The user does not exist"))
            return@get
        }
        call.respond(OK, getSavedJobs(username))
    }
}

fun Route.getAccountDetails() {
    get("/getAccountDetails") {
        val username = call.parameters["username"]
        if(username == null) {
            call.respond(BadRequest, BasicApiResponse(false, "Bad request format"))
            return@get
        }
        if(!checkIfUserExists(username)) {
            call.respond(NotFound, BasicApiResponse(false, "Username does not exist"))
            return@get
        }
        val user = findUser(username)!!
        val accountDetailsResponse = AccountDetailsRequest(
            user.profilePicUrl,
            username,
            user.realName,
            user.phoneNumber,
            user.email
        )
        call.respond(OK, accountDetailsResponse)
    }
}

fun Route.updateAccountDetails() {
    post("/updateAccountDetails") {
        val multipartRequest = call.receiveMultipart()
        var accountRequest: AccountDetailsRequest? = null
        var profilePicName: String? = null

        multipartRequest.forEachPart { part ->
            when(part)
            {
                is PartData.FormItem -> {
                    accountRequest = Gson().fromJson(part.value, AccountDetailsRequest::class.java)
                }
                is PartData.FileItem -> {
                    profilePicName = part.originalFileName
                    val fileBytes = part.streamProvider().readBytes()
                    File("uploads/$profilePicName").writeBytes(fileBytes)
                }
                else -> {}
            }
        }

        if(accountRequest == null) {
            call.respond(BadRequest, BasicApiResponse(false, "Bad request format"))
            return@post
        }
        if(!checkIfUserExists(accountRequest!!.username)) {
            call.respond(BadRequest, BasicApiResponse(false, "The username does not exist"))
            return@post
        }
        if(profilePicName != null) {
            val user = findUser(accountRequest!!.username)!!
            val newUser = user.copy(profilePicUrl = "/$ASSETS_FOLDER/$profilePicName")
            users.replaceOneById(user.userID, newUser)
        }

        val user = findUser(accountRequest!!.username)!!
        val newUser = user.copy(
            realName = accountRequest!!.realName,
            email = accountRequest!!.email,
            phoneNumber = accountRequest!!.phoneNumber
        )
        users.replaceOneById(user.userID, newUser)

        call.respond(OK, BasicApiResponse(true, "Account details updated successfully"))
    }
}

fun Route.getStaticContent() {
    static(ASSETS_FOLDER) {
        files("uploads")
    }
}
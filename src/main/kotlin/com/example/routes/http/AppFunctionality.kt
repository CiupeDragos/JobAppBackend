package com.example.routes.http

import com.example.datamodels.regular.JobFilter
import com.example.datamodels.regular.JobPost
import com.example.datamodels.requests.FavouriteJobRequest
import com.example.datamodels.responses.BasicApiResponse
import com.example.server.*
import com.google.gson.Gson
import io.ktor.http.*
import io.ktor.http.ContentType.Application.Json
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.InternalServerError
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.litote.kmongo.json

fun Route.createJobPost() {
    post("/createJobPost") {
        val request = call.receiveOrNull<JobPost>()
        if(request == null) {
            call.respond(HttpStatusCode.BadRequest, BasicApiResponse(false, "Bad request format"))
            return@post
        }
        if(!checkIfUserExists(request.jobCreatorUsername)) {
            call.respond(HttpStatusCode.BadRequest, BasicApiResponse(false, "The username trying to create this job doesn't exist"))
            return@post
        }
        if(addJobPost(request)) {
            call.respond(OK, BasicApiResponse(true, "The job post was created successfully"))
        } else {
            call.respond(InternalServerError, BasicApiResponse(false, "An unknown error occurred"))
        }
    }
}

fun Route.deleteJobPost() {
    post("/deleteJobPost") {
        val jobID = call.parameters["jobID"]
        val username = call.parameters["username"]
        if(jobID == null || username == null) {
            call.respond(HttpStatusCode.BadRequest, BasicApiResponse(false, "Bad request format"))
            return@post
        }
        if(!checkIfUserExists(username)) {
            call.respond(HttpStatusCode.BadRequest, BasicApiResponse(false, "The username trying to delete this job doesn't exist"))
            return@post
        }
        val job = findJobPost(jobID)
        if(job == null) {
            call.respond(HttpStatusCode.BadRequest, BasicApiResponse(false, "The job you are trying to delete does not exist"))
            return@post
        }
        if(job.jobCreatorUsername != username) {
            call.respond(HttpStatusCode.BadRequest, BasicApiResponse(false, "You did not create this job"))
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
            call.respond(HttpStatusCode.BadRequest, BasicApiResponse(false, "Bad request format"))
            return@post
        }
        if(!checkIfUserExists(request.accountUsername)) {
            call.respond(HttpStatusCode.BadRequest, BasicApiResponse(false, "The username trying to add this job to favourites does not exist"))
            return@post
        }
        if(!checkIfJobExists(request.jobID)) {
            call.respond(HttpStatusCode.BadRequest, BasicApiResponse(false, "The job you are trying to add to favourites,does not exist"))
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
            call.respond(HttpStatusCode.BadRequest, BasicApiResponse(false, "Bad request format"))
            return@post
        }
        if(!checkIfUserExists(request.accountUsername)) {
            call.respond(HttpStatusCode.BadRequest, BasicApiResponse(false, "The username trying to remove this job from favourites does not exist"))
            return@post
        }
        if(!checkIfJobExists(request.jobID)) {
            call.respond(HttpStatusCode.BadRequest, BasicApiResponse(false, "The job you are trying to remove from favourites does not exist"))
            return@post
        }
        val user = findUser(request.accountUsername)!!
        if(request.jobID !in user.savedJobsIDs) {
            call.respond(OK, BasicApiResponse(false, "You don't have this job saved"))
        } else {
            val modifiedSaveJobsList = user.savedJobsIDs.toMutableList().apply { remove(request.jobID) }.toList()
            users.replaceOneById(user.userID, user.copy(savedJobsIDs = modifiedSaveJobsList))
            call.respond(OK, BasicApiResponse(true, "The job has been delete from favourites"))
        }
    }
}

fun Route.getJobPosts() {
    get("/getJobPosts") {
        val request = call.parameters["jobFilter"]

        if(request == null) {
            call.respond(BadRequest, BasicApiResponse(false, "Bad request format"))
            return@get
        }
        val jobFilter = Gson().fromJson(request, JobFilter::class.java)
        val jobs = findFilteredJobs(jobFilter)
        call.respond(OK, jobs)
    }
}
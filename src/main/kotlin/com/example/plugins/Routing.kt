package com.example.plugins

import com.example.routes.http.*
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    install(Routing) {
        registerAccount()
        loginAccount()
        createJobPost()
        deleteJobPost()
        addJobToFavourites()
        deleteJobFromFavourites()
        getJobPosts()
    }
}

package com.example.routes.http

import com.example.datamodels.requests.AccountRequest
import com.example.datamodels.responses.BasicApiResponse
import com.example.other.Constants.MAX_USERNAME_LENGTH
import com.example.other.Constants.MIN_PASSWORD_LENGTH
import com.example.other.Constants.MIN_USERNAME_LENGTH
import com.example.security.encodePasswordWithSalt
import com.example.server.addUser
import com.example.server.checkIfUserExists
import com.example.server.loginUser
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.registerAccount() {
    post("/registerAccount") {
        val request = call.receiveOrNull<AccountRequest>()
        if (request == null) {
            call.respond(HttpStatusCode.BadRequest, BasicApiResponse(false, "Bad request format"))
            return@post
        }

        val username = request.username
        val password = request.password

        if (checkIfUserExists(username)) {
            call.respond(
                HttpStatusCode.OK,
                BasicApiResponse(false, "An account is already registered with this username")
            )
            return@post
        }
        if (username.length < MIN_USERNAME_LENGTH || username.length > MAX_USERNAME_LENGTH) {
            call.respond(
                HttpStatusCode.OK,
                BasicApiResponse(false, "The username must be between 4 and 12 characters long")
            )
            return@post
        }
        if (password.length < MIN_PASSWORD_LENGTH) {
            call.respond(HttpStatusCode.OK, BasicApiResponse(false, "The password must be at least 8 characters long"))
            return@post
        }

        val encodedPassword = encodePasswordWithSalt(password)
        if (addUser(username, encodedPassword)) {
            call.respond(HttpStatusCode.OK, BasicApiResponse(true, "The account has been created successfully"))
        }
    }
}

fun Route.loginAccount() {
    post("/loginAccount") {
        val request = call.receiveOrNull<AccountRequest>()

        if (request == null) {
            call.respond(HttpStatusCode.BadRequest, BasicApiResponse(false, "Bad request format"))
            return@post
        }

        val username = request.username
        val password = request.password

        if (!checkIfUserExists(username)) {
            call.respond(HttpStatusCode.OK, BasicApiResponse(false, "No account with this username exists"))
            return@post
        }
        if (loginUser(username, password)) {
            call.respond(HttpStatusCode.OK, BasicApiResponse(true, "Account successfully logged in"))
        } else {
            call.respond(HttpStatusCode.OK, BasicApiResponse(false, "Username or password do not match"))
        }
    }
}
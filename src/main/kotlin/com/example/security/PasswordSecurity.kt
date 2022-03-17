package com.example.security

import org.apache.commons.codec.binary.Hex
import org.apache.commons.codec.digest.DigestUtils
import java.security.SecureRandom

fun encodePasswordWithSalt(passwordToEncode: String, saltLength: Int = 32): String {
    val salt = SecureRandom.getInstance("SHA1PRNG").generateSeed(saltLength)
    val hexSalt = Hex.encodeHexString(salt)
    val hash = DigestUtils.sha256Hex("$hexSalt$passwordToEncode")
    return "$hexSalt:$hash"
}

fun checkHashForPassword(hashedPassword: String, unhashedPassword: String): Boolean {
    val splitHash = hashedPassword.split(":")
    val salt = splitHash[0]
    val actualHashedPassword = splitHash[1]
    val passToCheck = DigestUtils.sha256Hex("$salt$unhashedPassword")

    return passToCheck == actualHashedPassword
}
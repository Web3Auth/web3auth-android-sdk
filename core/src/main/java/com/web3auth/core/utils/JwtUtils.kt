package com.web3auth.core.utils

import com.auth0.jwt.JWT
import java.io.UnsupportedEncodingException
import java.util.*

object JwtUtils {

    @Throws(Exception::class)
    fun isTokenExpired(token: String): Boolean {
        var isExpired = false
        try {
            val jwt = JWT.decode(token)
            isExpired = jwt.expiresAt.before(Date())
        } catch (ex: UnsupportedEncodingException) {
            ex.printStackTrace()
        }
        return isExpired
    }
}
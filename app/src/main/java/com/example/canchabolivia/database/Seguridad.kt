package com.example.canchabolivia.database

import java.security.MessageDigest

object Seguridad {
    fun hash(clave: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(clave.toByteArray())
            .joinToString("") { "%02x".format(it) }
}

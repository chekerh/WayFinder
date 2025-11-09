package tn.esprit.wayfinder.models

import kotlinx.serialization.Serializable

@Serializable
data class SignUpRequest(
    val email: String,
    val password: String,
    val username: String,
    val first_name: String,
    val last_name: String
)

package tn.esprit.wayfinder.models

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val username: String, // Corrected to match backend DTO
    val password: String 
)

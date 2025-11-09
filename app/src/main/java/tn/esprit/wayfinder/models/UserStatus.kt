package tn.esprit.wayfinder.models

import kotlinx.serialization.Serializable

@Serializable
enum class UserStatus {
    ACTIVE, INACTIVE, SUSPENDED
}

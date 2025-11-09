package tn.esprit.wayfinder.presentation.auth

import tn.esprit.wayfinder.models.LoginRequest
import tn.esprit.wayfinder.models.SignUpRequest
import tn.esprit.wayfinder.network.ApiService

// FIX: AuthRepository now correctly defined
class AuthRepository(private val apiService: ApiService) {

    suspend fun login(request: LoginRequest) = apiService.login(request)

    suspend fun register(request: SignUpRequest) = apiService.register(request)
}

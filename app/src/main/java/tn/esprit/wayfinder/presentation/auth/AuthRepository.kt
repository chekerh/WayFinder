package tn.esprit.wayfinder.presentation.auth

import tn.esprit.wayfinder.models.*
import tn.esprit.wayfinder.network.ApiService

// FIX: AuthRepository now correctly defined
class AuthRepository(private val apiService: ApiService) {

    suspend fun login(request: LoginRequest) = apiService.login(request)

    suspend fun register(request: SignUpRequest) = apiService.register(request)

    suspend fun sendOTPForRegistration(email: String) = apiService.sendOTPForRegistration(
        SendOTPForRegistrationRequest(email = email)
    )

    suspend fun registerWithOTP(request: RegisterWithOTPRequest) = apiService.registerWithOTP(request)

    suspend fun googleSignIn(idToken: String) = apiService.googleSignIn(
        GoogleSignInRequest(idToken = idToken, clientType = "android")
    )

    suspend fun verifyEmail(token: String) = apiService.verifyEmail(VerifyEmailRequest(token))

    suspend fun verifyEmailGet(token: String) = apiService.verifyEmailGet(token)

    suspend fun resendVerificationEmail(email: String) = apiService.resendVerificationEmail(
        ResendVerificationRequest(email)
    )
}

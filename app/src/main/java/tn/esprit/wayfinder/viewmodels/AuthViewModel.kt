package tn.esprit.wayfinder.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import tn.esprit.wayfinder.manager.TokenManager
import tn.esprit.wayfinder.models.LoginRequest
import tn.esprit.wayfinder.models.SignUpRequest
import tn.esprit.wayfinder.models.RegisterWithOTPRequest
import tn.esprit.wayfinder.presentation.auth.AuthRepository

sealed class LoginResult {
    object Idle : LoginResult()
    object Loading : LoginResult()
    data class Success(val navigateTo: String) : LoginResult()
    data class Error(val message: String) : LoginResult()
}

sealed class SignUpResult {
    object Idle : SignUpResult()
    object Loading : SignUpResult()
    data class Success(val message: String) : SignUpResult()
    data class Error(val message: String) : SignUpResult()
    data class OTPSent(val email: String) : SignUpResult() // OTP sent successfully
}

sealed class GoogleSignInResult {
    object Idle : GoogleSignInResult()
    object Loading : GoogleSignInResult()
    data class Success(val navigateTo: String, val emailVerified: Boolean) : GoogleSignInResult()
    data class Error(val message: String) : GoogleSignInResult()
}

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _loginResult = MutableStateFlow<LoginResult>(LoginResult.Idle)
    val loginResult: StateFlow<LoginResult> = _loginResult

    private val _signUpResult = MutableStateFlow<SignUpResult>(SignUpResult.Idle)
    val signUpResult: StateFlow<SignUpResult> = _signUpResult

    private val _googleSignInResult = MutableStateFlow<GoogleSignInResult>(GoogleSignInResult.Idle)
    val googleSignInResult: StateFlow<GoogleSignInResult> = _googleSignInResult

    fun clearMessages() {
        _loginResult.value = LoginResult.Idle
        _signUpResult.value = SignUpResult.Idle
        _googleSignInResult.value = GoogleSignInResult.Idle
    }

    fun login(context: Context, request: LoginRequest) {
        viewModelScope.launch {
            _loginResult.value = LoginResult.Loading
            try {
                val response = authRepository.login(request)
                val tokenManager = TokenManager(context)
                tokenManager.saveToken(response.accessToken)
                tokenManager.saveUser(response.user)

                if (response.onboardingCompleted) {
                    _loginResult.value = LoginResult.Success("home")
                } else {
                    _loginResult.value = LoginResult.Success("onboarding")
                }
            } catch (e: Exception) {
                _loginResult.value = LoginResult.Error(parseError(e))
            }
        }
    }

    fun sendOTPForRegistration(email: String) {
        viewModelScope.launch {
            _signUpResult.value = SignUpResult.Loading
            try {
                val response = authRepository.sendOTPForRegistration(email)
                _signUpResult.value = SignUpResult.OTPSent(email)
            } catch (e: Exception) {
                _signUpResult.value = SignUpResult.Error(parseError(e))
            }
        }
    }

    fun registerWithOTP(
        context: Context,
        email: String,
        firstName: String,
        lastName: String,
        password: String,
        otpCode: String
    ) {
        viewModelScope.launch {
            _signUpResult.value = SignUpResult.Loading
            try {
                // Generate username from email (take part before @ and add random number)
                val emailPrefix = email.substringBefore("@")
                val randomSuffix = (1000..9999).random()
                val username = "${emailPrefix}_$randomSuffix"
                
                val request = RegisterWithOTPRequest(
                    username = username,
                    email = email,
                    first_name = firstName,
                    last_name = lastName,
                    password = password,
                    otp_code = otpCode
                )
                val response = authRepository.registerWithOTP(request)
                
                // Check if this is an auto-login response (user already existed)
                if (response.accessToken != null && response.onboardingCompleted != null) {
                    // User already exists and was auto-logged in - save token and user
                    android.util.Log.d("AuthViewModel", "User already exists, auto-logged in: ${response.user.email}")
                    val tokenManager = TokenManager(context)
                    tokenManager.saveToken(response.accessToken)
                    tokenManager.saveUser(response.user)
                    _signUpResult.value = SignUpResult.Success("Connexion réussie")
                } else {
                    // New user created successfully
                    _signUpResult.value = SignUpResult.Success(response.message)
                }
            } catch (e: Exception) {
                val errorMessage = parseError(e)
                android.util.Log.e("AuthViewModel", "Registration error: $errorMessage", e)
                
                // If it's a 409 Conflict and message suggests user exists, try to login
                if (e is HttpException && e.code() == 409 && 
                    (errorMessage.contains("existe déjà", ignoreCase = true) || 
                     errorMessage.contains("already exists", ignoreCase = true))) {
                    android.util.Log.d("AuthViewModel", "User exists, attempting auto-login with password")
                    // Try to login with the provided credentials
                    try {
                        val loginRequest = LoginRequest(email = email, password = password)
                        val loginResponse = authRepository.login(loginRequest)
                        
                        // Save token and user data
                        val tokenManager = TokenManager(context)
                        tokenManager.saveToken(loginResponse.accessToken)
                        tokenManager.saveUser(loginResponse.user)
                        android.util.Log.d("AuthViewModel", "Auto-login successful for existing user")
                        _signUpResult.value = SignUpResult.Success("Connexion réussie avec votre compte existant")
                    } catch (loginError: Exception) {
                        android.util.Log.e("AuthViewModel", "Auto-login failed", loginError)
                        // If login fails, show the original error but suggest login
                        _signUpResult.value = SignUpResult.Error(
                            "Un compte existe déjà avec cet email. Veuillez vous connecter avec votre mot de passe."
                        )
                    }
                } else {
                    _signUpResult.value = SignUpResult.Error(errorMessage)
                }
            }
        }
    }

    fun logout(context: Context) {
        val tokenManager = TokenManager(context)
        tokenManager.deleteToken()
    }

    fun googleSignIn(context: Context, idToken: String) {
        viewModelScope.launch {
            _googleSignInResult.value = GoogleSignInResult.Loading
            try {
                android.util.Log.d("AuthViewModel", "Google Sign-In: Starting authentication with ID token")
                val response = authRepository.googleSignIn(idToken)
                android.util.Log.d("AuthViewModel", "Google Sign-In: Success - User ID: ${response.user.id}, Email: ${response.user.email}, Email verified: ${response.emailVerified}, Onboarding: ${response.onboardingCompleted}")
                
                val tokenManager = TokenManager(context)
                tokenManager.saveToken(response.accessToken)
                tokenManager.saveUser(response.user)

                // Navigate based on onboarding and email verification status
                val navigateTo = when {
                    !response.emailVerified -> {
                        android.util.Log.d("AuthViewModel", "Google Sign-In: Email not verified, navigating to email_verification")
                        "email_verification"
                    }
                    response.onboardingCompleted -> {
                        android.util.Log.d("AuthViewModel", "Google Sign-In: Onboarding completed, navigating to home")
                        "home"
                    }
                    else -> {
                        android.util.Log.d("AuthViewModel", "Google Sign-In: Onboarding not completed, navigating to onboarding")
                        "onboarding"
                    }
                }
                _googleSignInResult.value = GoogleSignInResult.Success(navigateTo, response.emailVerified)
            } catch (e: Exception) {
                android.util.Log.e("AuthViewModel", "Google Sign-In: Error occurred - ${e.javaClass.simpleName}", e)
                android.util.Log.e("AuthViewModel", "Google Sign-In: Error message: ${e.message}")
                
                val errorMessage = parseError(e)
                android.util.Log.e("AuthViewModel", "Google Sign-In: Parsed error message: $errorMessage")
                
                // Provide more specific error messages based on the error type
                val finalErrorMessage = when {
                    // Check for specific conflict scenarios
                    errorMessage.contains("exists", ignoreCase = true) || 
                    errorMessage.contains("déjà", ignoreCase = true) ||
                    errorMessage.contains("already", ignoreCase = true) -> {
                        // Check if it's a Google ID conflict or email conflict
                        if (errorMessage.contains("google", ignoreCase = true) || 
                            errorMessage.contains("google_id", ignoreCase = true)) {
                            "Ce compte Google est déjà lié à un autre compte. Veuillez utiliser le compte associé ou contactez le support."
                        } else {
                            "Un compte existe déjà avec cet email. Le compte Google a été lié à votre compte existant. Veuillez réessayer."
                        }
                    }
                    errorMessage.contains("invalid", ignoreCase = true) || 
                    errorMessage.contains("token", ignoreCase = true) -> {
                        "Le jeton Google est invalide ou a expiré. Veuillez réessayer."
                    }
                    errorMessage.contains("network", ignoreCase = true) || 
                    errorMessage.contains("connection", ignoreCase = true) -> {
                        "Erreur de connexion. Vérifiez votre connexion Internet et réessayez."
                    }
                    else -> errorMessage
                }
                
                _googleSignInResult.value = GoogleSignInResult.Error(finalErrorMessage)
            }
        }
    }

    suspend fun verifyEmail(token: String): Boolean {
        return try {
            authRepository.verifyEmail(token)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun resendVerificationEmail(email: String): Boolean {
        return try {
            authRepository.resendVerificationEmail(email)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun parseError(throwable: Throwable): String {
        return when (throwable) {
            is HttpException -> {
                val statusCode = throwable.code()
                val errorBody = throwable.response()?.errorBody()?.string()
                android.util.Log.e("AuthViewModel", "HTTP Error: Status $statusCode")
                android.util.Log.e("AuthViewModel", "HTTP Error Body: $errorBody")
                android.util.Log.e("AuthViewModel", "HTTP Error Headers: ${throwable.response()?.headers()}")
                
                if (!errorBody.isNullOrBlank()) {
                    val parsedMessage = try {
                        val element = Json.parseToJsonElement(errorBody)
                        val messageElement = element.jsonObject["message"]
                        when {
                            messageElement is JsonArray -> {
                                // Handle array of error messages
                                val messages = messageElement.mapNotNull { 
                                    if (it is JsonPrimitive) {
                                        it.content
                                    } else null
                                }
                                messages.joinToString(". ")
                            }
                            messageElement is JsonPrimitive -> {
                                messageElement.content
                            }
                            else -> null
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("AuthViewModel", "Error parsing error body", e)
                        null
                    }
                    
                    when {
                        parsedMessage != null -> {
                            android.util.Log.d("AuthViewModel", "Using parsed message: $parsedMessage")
                            parsedMessage
                        }
                        statusCode == 409 -> {
                            android.util.Log.w("AuthViewModel", "409 Conflict - User may already exist. Backend should handle account linking.")
                            "Un compte existe déjà avec cet email. Le système devrait lier automatiquement votre compte Google. Si le problème persiste, veuillez vous connecter avec votre mot de passe."
                        }
                        statusCode == 400 -> {
                            android.util.Log.w("AuthViewModel", "400 Bad Request")
                            "Requête invalide. Veuillez réessayer."
                        }
                        statusCode == 401 -> {
                            android.util.Log.w("AuthViewModel", "401 Unauthorized")
                            "Non autorisé. Veuillez vérifier vos identifiants."
                        }
                        statusCode == 404 -> {
                            android.util.Log.w("AuthViewModel", "404 Not Found")
                            "Ressource non trouvée."
                        }
                        statusCode >= 500 -> {
                            android.util.Log.e("AuthViewModel", "Server error: $statusCode")
                            "Erreur serveur. Veuillez réessayer plus tard."
                        }
                        else -> {
                            android.util.Log.w("AuthViewModel", "Unhandled status code: $statusCode")
                            errorBody
                        }
                    }
                } else {
                    when (statusCode) {
                        409 -> {
                            android.util.Log.w("AuthViewModel", "409 Conflict - No error body provided")
                            "Un compte existe déjà avec cet email. Le système devrait lier automatiquement votre compte Google."
                        }
                        400 -> {
                            android.util.Log.w("AuthViewModel", "400 Bad Request - No error body")
                            "Requête invalide."
                        }
                        401 -> {
                            android.util.Log.w("AuthViewModel", "401 Unauthorized - No error body")
                            "Non autorisé."
                        }
                        404 -> {
                            android.util.Log.w("AuthViewModel", "404 Not Found - No error body")
                            "Ressource non trouvée."
                        }
                        in 500..599 -> {
                            android.util.Log.e("AuthViewModel", "Server error: $statusCode - No error body")
                            "Erreur serveur. Veuillez réessayer plus tard."
                        }
                        else -> {
                            android.util.Log.w("AuthViewModel", "Unhandled status code: $statusCode - No error body")
                            throwable.message() ?: "Erreur HTTP $statusCode"
                        }
                    }
                }
            }
            is UnknownHostException -> {
                android.util.Log.e("AuthViewModel", "Network error: UnknownHostException - ${throwable.message}")
                "Impossible de se connecter au serveur. Vérifiez votre connexion Internet."
            }
            is SocketTimeoutException -> {
                android.util.Log.e("AuthViewModel", "Network error: SocketTimeoutException - ${throwable.message}")
                "Le serveur met trop de temps à répondre. Réessayez dans un instant."
            }
            else -> {
                android.util.Log.e("AuthViewModel", "Unexpected error: ${throwable.javaClass.simpleName}", throwable)
                android.util.Log.e("AuthViewModel", "Error message: ${throwable.message}")
                android.util.Log.e("AuthViewModel", "Error stack trace: ${throwable.stackTraceToString()}")
                throwable.message ?: "Une erreur inattendue est survenue"
            }
        }
    }
}

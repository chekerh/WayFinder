package tn.esprit.wayfinder.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import tn.esprit.wayfinder.manager.TokenManager
import tn.esprit.wayfinder.models.LoginRequest
import tn.esprit.wayfinder.models.SignUpRequest
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
                _signUpResult.value = SignUpResult.Success(response.message)
            } catch (e: Exception) {
                _signUpResult.value = SignUpResult.Error(parseError(e))
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
                val response = authRepository.googleSignIn(idToken)
                val tokenManager = TokenManager(context)
                tokenManager.saveToken(response.accessToken)
                tokenManager.saveUser(response.user)

                // Navigate based on onboarding and email verification status
                val navigateTo = when {
                    !response.emailVerified -> "email_verification"
                    response.onboardingCompleted -> "home"
                    else -> "onboarding"
                }
                _googleSignInResult.value = GoogleSignInResult.Success(navigateTo, response.emailVerified)
            } catch (e: Exception) {
                _googleSignInResult.value = GoogleSignInResult.Error(parseError(e))
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
                val errorBody = throwable.response()?.errorBody()?.string()
                if (!errorBody.isNullOrBlank()) {
                    val parsedMessage = try {
                        val element = Json.parseToJsonElement(errorBody)
                        element.jsonObject["message"]?.jsonPrimitive?.content
                    } catch (_: Exception) {
                        null
                    }
                    parsedMessage ?: errorBody
                } else {
                    throwable.message()
                }
            }
            is UnknownHostException -> "Impossible de se connecter au serveur. Vérifiez votre connexion Internet."
            is SocketTimeoutException -> "Le serveur met trop de temps à répondre. Réessayez dans un instant."
            else -> throwable.message ?: "Une erreur inattendue est survenue"
        }
    }
}

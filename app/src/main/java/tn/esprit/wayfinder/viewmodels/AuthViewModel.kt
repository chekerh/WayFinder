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
}

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _loginResult = MutableStateFlow<LoginResult>(LoginResult.Idle)
    val loginResult: StateFlow<LoginResult> = _loginResult

    private val _signUpResult = MutableStateFlow<SignUpResult>(SignUpResult.Idle)
    val signUpResult: StateFlow<SignUpResult> = _signUpResult

    fun clearMessages() {
        _loginResult.value = LoginResult.Idle
        _signUpResult.value = SignUpResult.Idle
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

    fun signup(request: SignUpRequest) {
        viewModelScope.launch {
            _signUpResult.value = SignUpResult.Loading
            try {
                val response = authRepository.register(request)
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

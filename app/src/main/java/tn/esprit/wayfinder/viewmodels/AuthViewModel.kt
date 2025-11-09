package tn.esprit.wayfinder.viewmodels

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import tn.esprit.wayfinder.manager.TokenManager
import tn.esprit.wayfinder.models.LoginRequest
import tn.esprit.wayfinder.models.LoginResponse
import tn.esprit.wayfinder.models.SignUpRequest
import tn.esprit.wayfinder.presentation.auth.AuthRepository

// FIX: ViewModel now takes repository in constructor for testability and DI
class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    val loading = mutableStateOf(false)
    val successMessage = mutableStateOf<String?>(null)
    val errorMessage = mutableStateOf<String?>(null)
    
    val loginResult = mutableStateOf<Result<LoginResponse>?>(null)

    fun clearMessages() {
        successMessage.value = null
        errorMessage.value = null
        loginResult.value = null
    }

    fun login(context: Context, request: LoginRequest) {
        viewModelScope.launch {
            loading.value = true
            try {
                val response = authRepository.login(request)
                val tokenManager = TokenManager(context)
                tokenManager.saveToken(response.accessToken)
                loading.value = false
                loginResult.value = Result.success(response)
            } catch (e: Exception) {
                loading.value = false
                errorMessage.value = e.message ?: "An unexpected error occurred"
                loginResult.value = Result.failure(e)
            }
        }
    }

    fun signup(request: SignUpRequest, onSignUpSuccess: () -> Unit) {
        viewModelScope.launch {
            loading.value = true
            try {
                val response = authRepository.register(request)
                loading.value = false
                successMessage.value = response.message
                onSignUpSuccess()
            } catch (e: Exception) {
                loading.value = false
                errorMessage.value = e.message ?: "An unexpected error occurred"
            }
        }
    }

    fun logout(context: Context) {
        val tokenManager = TokenManager(context)
        tokenManager.deleteToken()
    }
}

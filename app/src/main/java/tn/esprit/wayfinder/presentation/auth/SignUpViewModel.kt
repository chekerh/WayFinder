package tn.esprit.wayfinder.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tn.esprit.wayfinder.models.SignUpRequest
import tn.esprit.wayfinder.models.SignUpResponse

class SignUpViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _signUpResult = MutableStateFlow<Result<SignUpResponse>?>(null)
    val signUpResult = _signUpResult.asStateFlow()

    fun signUp(request: SignUpRequest) {
        viewModelScope.launch {
            try {
                // FIX: Changed from .signup to .register to match the repository
                val response = repository.register(request)
                _signUpResult.value = Result.success(response)
            } catch (e: Exception) {
                _signUpResult.value = Result.failure(e)
            }
        }
    }
}

package tn.esprit.wayfinder.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tn.esprit.wayfinder.models.*
import tn.esprit.wayfinder.presentation.groupflight.GroupFlightRepository

sealed class GroupFlightUiState {
    object Idle : GroupFlightUiState()
    object Loading : GroupFlightUiState()
    data class Success(val groupFlights: List<GroupFlight>) : GroupFlightUiState()
    data class Error(val message: String) : GroupFlightUiState()
}

sealed class GroupFlightDetailUiState {
    object Idle : GroupFlightDetailUiState()
    object Loading : GroupFlightDetailUiState()
    data class Success(val groupFlight: GroupFlight) : GroupFlightDetailUiState()
    data class Error(val message: String) : GroupFlightDetailUiState()
}

sealed class CreateGroupFlightUiState {
    object Idle : CreateGroupFlightUiState()
    object Loading : CreateGroupFlightUiState()
    data class Success(val groupFlight: GroupFlight) : CreateGroupFlightUiState()
    data class Error(val message: String) : CreateGroupFlightUiState()
}

class GroupFlightViewModel(
    private val groupFlightRepository: GroupFlightRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<GroupFlightUiState>(GroupFlightUiState.Idle)
    val uiState: StateFlow<GroupFlightUiState> = _uiState.asStateFlow()
    
    private val _detailUiState = MutableStateFlow<GroupFlightDetailUiState>(GroupFlightDetailUiState.Idle)
    val detailUiState: StateFlow<GroupFlightDetailUiState> = _detailUiState.asStateFlow()
    
    private val _createUiState = MutableStateFlow<CreateGroupFlightUiState>(CreateGroupFlightUiState.Idle)
    val createUiState: StateFlow<CreateGroupFlightUiState> = _createUiState.asStateFlow()
    
    private val _invitations = MutableStateFlow<List<GroupFlightInvitation>>(emptyList())
    val invitations: StateFlow<List<GroupFlightInvitation>> = _invitations.asStateFlow()
    
    fun loadGroupFlights() {
        viewModelScope.launch {
            try {
                _uiState.value = GroupFlightUiState.Loading
                val groupFlights = groupFlightRepository.getGroupFlights()
                _uiState.value = GroupFlightUiState.Success(groupFlights)
            } catch (e: Exception) {
                _uiState.value = GroupFlightUiState.Error(
                    e.message ?: "Failed to load group flights"
                )
            }
        }
    }
    
    fun loadMyGroupFlights() {
        viewModelScope.launch {
            try {
                _uiState.value = GroupFlightUiState.Loading
                val groupFlights = groupFlightRepository.getMyGroupFlights()
                _uiState.value = GroupFlightUiState.Success(groupFlights)
            } catch (e: Exception) {
                _uiState.value = GroupFlightUiState.Error(
                    e.message ?: "Failed to load your group flights"
                )
            }
        }
    }
    
    fun loadGroupFlightDetail(id: String) {
        viewModelScope.launch {
            try {
                _detailUiState.value = GroupFlightDetailUiState.Loading
                val groupFlight = groupFlightRepository.getGroupFlightById(id)
                _detailUiState.value = GroupFlightDetailUiState.Success(groupFlight)
            } catch (e: Exception) {
                _detailUiState.value = GroupFlightDetailUiState.Error(
                    e.message ?: "Failed to load group flight details"
                )
            }
        }
    }
    
    fun createGroupFlight(request: CreateGroupFlightRequest) {
        viewModelScope.launch {
            try {
                _createUiState.value = CreateGroupFlightUiState.Loading
                val groupFlight = groupFlightRepository.createGroupFlight(request)
                _createUiState.value = CreateGroupFlightUiState.Success(groupFlight)
                // Refresh list
                loadMyGroupFlights()
            } catch (e: Exception) {
                _createUiState.value = CreateGroupFlightUiState.Error(
                    e.message ?: "Failed to create group flight"
                )
            }
        }
    }
    
    fun joinGroupFlight(id: String) {
        viewModelScope.launch {
            try {
                _detailUiState.value = GroupFlightDetailUiState.Loading
                val groupFlight = groupFlightRepository.joinGroupFlight(id)
                _detailUiState.value = GroupFlightDetailUiState.Success(groupFlight)
            } catch (e: Exception) {
                _detailUiState.value = GroupFlightDetailUiState.Error(
                    e.message ?: "Failed to join group flight"
                )
            }
        }
    }
    
    fun inviteUsers(groupFlightId: String, userIds: List<String>) {
        viewModelScope.launch {
            try {
                groupFlightRepository.inviteToGroupFlight(groupFlightId, userIds)
                loadGroupFlightDetail(groupFlightId)
            } catch (e: Exception) {
                android.util.Log.e("GroupFlightViewModel", "Error inviting users: ${e.message}", e)
            }
        }
    }
    
    fun loadInvitations() {
        viewModelScope.launch {
            try {
                val invitations = groupFlightRepository.getInvitations()
                _invitations.value = invitations
            } catch (e: Exception) {
                android.util.Log.e("GroupFlightViewModel", "Error loading invitations: ${e.message}", e)
            }
        }
    }
    
    fun acceptInvitation(invitationId: String) {
        viewModelScope.launch {
            try {
                val groupFlight = groupFlightRepository.acceptInvitation(invitationId)
                _detailUiState.value = GroupFlightDetailUiState.Success(groupFlight)
                loadInvitations()
            } catch (e: Exception) {
                android.util.Log.e("GroupFlightViewModel", "Error accepting invitation: ${e.message}", e)
            }
        }
    }
    
    fun declineInvitation(invitationId: String) {
        viewModelScope.launch {
            try {
                groupFlightRepository.declineInvitation(invitationId)
                loadInvitations()
            } catch (e: Exception) {
                android.util.Log.e("GroupFlightViewModel", "Error declining invitation: ${e.message}", e)
            }
        }
    }
    
    fun cancelGroupFlight(id: String) {
        viewModelScope.launch {
            try {
                groupFlightRepository.cancelGroupFlight(id)
                loadMyGroupFlights()
            } catch (e: Exception) {
                android.util.Log.e("GroupFlightViewModel", "Error cancelling group flight: ${e.message}", e)
            }
        }
    }
}


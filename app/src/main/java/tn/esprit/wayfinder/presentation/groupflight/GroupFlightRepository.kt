package tn.esprit.wayfinder.presentation.groupflight

import tn.esprit.wayfinder.models.*
import tn.esprit.wayfinder.network.ApiService

class GroupFlightRepository(private val apiService: ApiService) {
    
    suspend fun createGroupFlight(request: CreateGroupFlightRequest): GroupFlight {
        return apiService.createGroupFlight(request)
    }
    
    suspend fun getGroupFlights(): List<GroupFlight> {
        return apiService.getGroupFlights()
    }
    
    suspend fun getMyGroupFlights(): List<GroupFlight> {
        return apiService.getMyGroupFlights()
    }
    
    suspend fun getGroupFlightById(id: String): GroupFlight {
        return apiService.getGroupFlightById(id)
    }
    
    suspend fun joinGroupFlight(id: String): GroupFlight {
        return apiService.joinGroupFlight(id, JoinGroupFlightRequest(id))
    }
    
    suspend fun inviteToGroupFlight(id: String, userIds: List<String>): GroupFlight {
        return apiService.inviteToGroupFlight(id, mapOf("user_ids" to userIds))
    }
    
    suspend fun cancelGroupFlight(id: String) {
        apiService.cancelGroupFlight(id)
    }
    
    suspend fun getInvitations(): List<GroupFlightInvitation> {
        return apiService.getGroupFlightInvitations()
    }
    
    suspend fun acceptInvitation(invitationId: String): GroupFlight {
        return apiService.acceptGroupFlightInvitation(invitationId)
    }
    
    suspend fun declineInvitation(invitationId: String) {
        apiService.declineGroupFlightInvitation(invitationId)
    }
    
    suspend fun getCostBreakdown(id: String): GroupFlightCostBreakdown {
        return apiService.getGroupFlightCostBreakdown(id)
    }
}


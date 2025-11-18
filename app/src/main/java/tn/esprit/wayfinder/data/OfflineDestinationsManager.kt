package tn.esprit.wayfinder.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import tn.esprit.wayfinder.models.FlightDestination

private val Context.offlineDataStore by preferencesDataStore(name = "offline_destinations")

class OfflineDestinationsManager(private val context: Context) {
    private val dataStore = context.offlineDataStore
    private val flightsCache = FlightsCache(context)
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private val OFFLINE_DESTINATION_IDS_KEY = stringSetPreferencesKey("offline_destination_ids")
        private val OFFLINE_DESTINATIONS_KEY = stringPreferencesKey("offline_destinations_json")
    }

    suspend fun downloadForOffline(destination: FlightDestination): Boolean {
        return try {
            val currentOfflineIds = getOfflineDestinationIds().toMutableSet()
            currentOfflineIds.add(destination.id)
            
            val currentOffline = getOfflineDestinations().toMutableList()
            val existingIndex = currentOffline.indexOfFirst { it.id == destination.id }
            if (existingIndex >= 0) {
                currentOffline[existingIndex] = destination
            } else {
                currentOffline.add(destination)
            }

            val payload = json.encodeToString(ListSerializer(FlightDestination.serializer()), currentOffline)
            
            dataStore.edit { prefs ->
                prefs[OFFLINE_DESTINATION_IDS_KEY] = currentOfflineIds
                prefs[OFFLINE_DESTINATIONS_KEY] = payload
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun removeFromOffline(destinationId: String): Boolean {
        return try {
            val currentOfflineIds = getOfflineDestinationIds().toMutableSet()
            currentOfflineIds.remove(destinationId)
            
            val currentOffline = getOfflineDestinations().toMutableList()
            currentOffline.removeAll { it.id == destinationId }

            val payload = json.encodeToString(ListSerializer(FlightDestination.serializer()), currentOffline)
            
            dataStore.edit { prefs ->
                prefs[OFFLINE_DESTINATION_IDS_KEY] = currentOfflineIds
                prefs[OFFLINE_DESTINATIONS_KEY] = payload
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun isDownloaded(destinationId: String): Boolean {
        return getOfflineDestinationIds().contains(destinationId)
    }

    suspend fun getOfflineDestinations(): List<FlightDestination> {
        val prefs = dataStore.data.firstOrNull() ?: return emptyList()
        val payload = prefs[OFFLINE_DESTINATIONS_KEY] ?: return emptyList()
        return try {
            json.decodeFromString(ListSerializer(FlightDestination.serializer()), payload)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun getOfflineDestinationIds(): Set<String> {
        val prefs = dataStore.data.firstOrNull() ?: return emptySet()
        return prefs[OFFLINE_DESTINATION_IDS_KEY] ?: emptySet()
    }

    suspend fun clearAllOffline() {
        dataStore.edit { it.clear() }
    }
}


package tn.esprit.wayfinder.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import tn.esprit.wayfinder.models.FlightDestination

private val Context.flightsDataStore by preferencesDataStore(name = "flights_cache")

data class CachedFlights(
    val destinations: List<FlightDestination>,
    val updatedAt: Long,
    val source: String?
)

class FlightsCache(private val context: Context) {

    private val dataStore = context.flightsDataStore
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private val DESTINATIONS_KEY = stringPreferencesKey("destinations_json")
        private val UPDATED_AT_KEY = longPreferencesKey("updated_at")
        private val SOURCE_KEY = stringPreferencesKey("source")
    }

    suspend fun store(destinations: List<FlightDestination>, source: String? = null) {
        if (destinations.isEmpty()) return
        val payload = json.encodeToString(ListSerializer(FlightDestination.serializer()), destinations)
        val timestamp = System.currentTimeMillis()
        dataStore.edit { prefs ->
            prefs[DESTINATIONS_KEY] = payload
            prefs[UPDATED_AT_KEY] = timestamp
            source?.let { prefs[SOURCE_KEY] = it }
        }
    }

    suspend fun read(): CachedFlights? {
        val prefs = dataStore.data.firstOrNull() ?: return null
        val payload = prefs[DESTINATIONS_KEY] ?: return null
        return try {
            val destinations = json.decodeFromString(ListSerializer(FlightDestination.serializer()), payload)
            val updatedAt = prefs[UPDATED_AT_KEY] ?: 0L
            val source = prefs[SOURCE_KEY]
            CachedFlights(destinations, updatedAt, source)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }
}


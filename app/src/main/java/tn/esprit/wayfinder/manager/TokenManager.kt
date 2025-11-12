package tn.esprit.wayfinder.manager

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import tn.esprit.wayfinder.models.User

class TokenManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secret_shared_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveToken(token: String) {
        sharedPreferences.edit().putString("jwt_token", token).apply()
    }

    fun getToken(): String? {
        return sharedPreferences.getString("jwt_token", null)
    }

    fun deleteToken() {
        sharedPreferences.edit()
            .remove("jwt_token")
            .remove("user_data")
            .apply()
    }

    fun saveUser(user: User) {
        val json = Json.encodeToString(user)
        sharedPreferences.edit().putString("user_data", json).apply()
    }

    fun getUser(): User? {
        val json = sharedPreferences.getString("user_data", null)
        return json?.let {
            try {
                Json.decodeFromString<User>(it)
            } catch (e: Exception) {
                // Handle possible deserialization errors
                null
            }
        }
    }
}

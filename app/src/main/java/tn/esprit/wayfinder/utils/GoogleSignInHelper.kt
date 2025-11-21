package tn.esprit.wayfinder.utils

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task

object GoogleSignInHelper {
    private const val TAG = "GoogleSignInHelper"

    /**
     * Get Google Sign-In Client
     * @param context Android context
     * @param serverClientId Google OAuth Client ID for Android (from Google Cloud Console)
     * @return GoogleSignInClient instance
     */
    fun getGoogleSignInClient(context: Context, serverClientId: String): GoogleSignInClient {
        // Minimal configuration - exactly like iOS
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(serverClientId)
            .requestEmail()
            .build()

        return GoogleSignIn.getClient(context, gso)
    }

    /**
     * Get Google Sign-In Intent
     * @param context Android context
     * @param serverClientId Google OAuth Client ID for Android
     * @return Intent for Google Sign-In
     */
    fun getSignInIntent(context: Context, serverClientId: String) =
        getGoogleSignInClient(context, serverClientId).signInIntent

    /**
     * Handle Google Sign-In result and extract ID token from Intent
     * @param intent Intent from Google Sign-In result
     * @return ID token string or null if failed
     */
    fun handleSignInIntent(intent: android.content.Intent?): String? {
        if (intent == null) {
            Log.e(TAG, "Google Sign-In failed: Intent is null")
            return null
        }
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(intent)
            handleSignInResult(task)
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-In failed: ${e.message}", e)
            null
        }
    }

    /**
     * Handle Google Sign-In result and extract ID token
     * @param task Task from Google Sign-In result
     * @return ID token string or null if failed
     */
    fun handleSignInResult(task: Task<GoogleSignInAccount>): String? {
        return try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken != null) {
                Log.d(TAG, "Google Sign-In successful. ID Token obtained.")
                idToken
            } else {
                Log.e(TAG, "Google Sign-In failed: ID token is null")
                null
            }
        } catch (e: ApiException) {
            Log.e(TAG, "Google Sign-In failed: ${e.statusCode}", e)
            null
        }
    }

    /**
     * Get current signed-in account (if any)
     * @param context Android context
     * @return GoogleSignInAccount or null
     */
    fun getSignedInAccount(context: Context): GoogleSignInAccount? {
        return try {
            GoogleSignIn.getLastSignedInAccount(context)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting signed-in account", e)
            null
        }
    }

    /**
     * Sign out from Google
     * @param context Android context
     * @param serverClientId Google OAuth Client ID for Android
     */
    suspend fun signOut(context: Context, serverClientId: String) {
        try {
            getGoogleSignInClient(context, serverClientId).signOut()
            Log.d(TAG, "Google Sign-Out successful")
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-Out failed", e)
        }
    }
}


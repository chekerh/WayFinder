package tn.esprit.wayfinder.network

import android.content.Context
import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import tn.esprit.wayfinder.manager.TokenManager

class AuthInterceptor(context: Context) : Interceptor {

    private val tokenManager = TokenManager(context)
    private val TAG = "AuthInterceptor"

    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request().newBuilder()

        val token = tokenManager.getToken()
        if (token != null) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
            Log.d(TAG, "Added Authorization header to request: ${chain.request().url}")
        } else {
            Log.w(TAG, "No token found for authenticated request: ${chain.request().url}")
        }

        val response = chain.proceed(requestBuilder.build())
        
        // Handle 401 Unauthorized errors - token is expired or invalid
        if (response.code == 401) {
            Log.w(TAG, "401 Unauthorized for ${chain.request().url} - Token may be expired or invalid")
            // Clear the expired token to force re-authentication
            tokenManager.deleteToken()
            Log.d(TAG, "Expired token cleared - user will need to re-authenticate")
        }
        
        return response
    }
}

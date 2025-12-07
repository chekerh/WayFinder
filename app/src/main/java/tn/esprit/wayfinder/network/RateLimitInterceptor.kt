package tn.esprit.wayfinder.network

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Interceptor that handles HTTP 429 (Too Many Requests) errors
 * by respecting the retry-after header and implementing exponential backoff
 */
class RateLimitInterceptor : Interceptor {
    
    private val TAG = "RateLimitInterceptor"
    private val MAX_RETRIES = 3
    private val INITIAL_RETRY_DELAY_MS = 1000L // 1 second
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var response = chain.proceed(request)
        var retryCount = 0
        
        // Handle 429 Too Many Requests
        while (response.code == 429 && retryCount < MAX_RETRIES) {
            val retryAfterHeader = response.header("retry-after-strict") 
                ?: response.header("retry-after")
                ?: response.header("Retry-After")
            
            val waitTime = if (retryAfterHeader != null) {
                try {
                    // Parse retry-after header (can be seconds or HTTP date)
                    retryAfterHeader.toLongOrNull()?.let { seconds ->
                        TimeUnit.SECONDS.toMillis(seconds)
                    } ?: run {
                        // If it's not a number, use exponential backoff
                        calculateExponentialBackoff(retryCount)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse retry-after header: $retryAfterHeader", e)
                    calculateExponentialBackoff(retryCount)
                }
            } else {
                // No retry-after header, use exponential backoff
                calculateExponentialBackoff(retryCount)
            }
            
            // Ensure wait time is reasonable (max 60 seconds)
            val finalWaitTime = waitTime.coerceIn(1000L, 60000L)
            
            Log.w(TAG, "Rate limited (429) for ${request.url}. Waiting ${finalWaitTime}ms before retry ${retryCount + 1}/$MAX_RETRIES")
            
            // Close the previous response body to avoid leaks
            response.close()
            
            // Wait before retrying
            try {
                Thread.sleep(finalWaitTime)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                throw IOException("Interrupted while waiting for rate limit", e)
            }
            
            // Retry the request
            response = chain.proceed(request)
            retryCount++
        }
        
        // If still rate limited after max retries, return the error
        if (response.code == 429) {
            Log.e(TAG, "Still rate limited after $MAX_RETRIES retries for ${request.url}")
        }
        
        return response
    }
    
    /**
     * Calculate exponential backoff delay
     * @param retryCount Current retry attempt (0-indexed)
     * @return Delay in milliseconds
     */
    private fun calculateExponentialBackoff(retryCount: Int): Long {
        // Exponential backoff: 1s, 2s, 4s, 8s...
        return INITIAL_RETRY_DELAY_MS * (1L shl retryCount)
    }
}


package tn.esprit.wayfinder.network

import android.content.Context
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object RetrofitInstance {

    // FIX: Re-added /api/ to the base URL. The backend prompt specifies all routes are under /api/.
    // This ensures the final URL matches the Vercel deployment (e.g., ...vercel.app/api/auth/login)
    private const val BASE_URL = "https://wayfinder-api-w92x.onrender.com/api/"


    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false  // Don't encode default values (null, empty, etc.)
        coerceInputValues = true  // Coerce null/unknown values to defaults
        isLenient = true  // Allow lenient parsing
    }

    fun create(context: Context): ApiService {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // FIX: Reordered interceptors. The logging interceptor should be last to log the final request.
        // This prevents issues where the request method is inadvertently changed from POST to GET.
        // Add HTTP cache for faster response times - increased size for better offline support
        val cacheSize = 50 * 1024 * 1024L // 50 MB cache (increased from 10 MB)
        val cache = okhttp3.Cache(context.cacheDir, cacheSize)
        
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS) // 2 minutes for image uploads
            .writeTimeout(120, TimeUnit.SECONDS) // 2 minutes for image uploads
            .callTimeout(150, TimeUnit.SECONDS) // 2.5 minutes total timeout
            .cache(cache) // Enable HTTP response caching
            .addInterceptor(AuthInterceptor(context))
            .addInterceptor(RateLimitInterceptor()) // Handle rate limiting with retry logic
            .addInterceptor(loggingInterceptor)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ApiService::class.java)
    }
}

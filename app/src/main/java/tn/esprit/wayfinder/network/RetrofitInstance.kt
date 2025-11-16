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
    }

    fun create(context: Context): ApiService {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // FIX: Reordered interceptors. The logging interceptor should be last to log the final request.
        // This prevents issues where the request method is inadvertently changed from POST to GET.
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .callTimeout(90, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor(context))
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

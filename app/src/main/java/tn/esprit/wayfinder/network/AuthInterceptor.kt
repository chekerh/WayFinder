package tn.esprit.wayfinder.network

import android.content.Context
import okhttp3.Interceptor
import okhttp3.Response
import tn.esprit.wayfinder.manager.TokenManager

class AuthInterceptor(context: Context) : Interceptor {

    private val tokenManager = TokenManager(context)

    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request().newBuilder()

        tokenManager.getToken()?.let {
            requestBuilder.addHeader("Authorization", "Bearer $it")
        }

        return chain.proceed(requestBuilder.build())
    }
}

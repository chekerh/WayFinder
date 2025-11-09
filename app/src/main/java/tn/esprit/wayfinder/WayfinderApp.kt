package tn.esprit.wayfinder

import android.app.Application
import tn.esprit.wayfinder.network.ApiService
import tn.esprit.wayfinder.network.RetrofitInstance

/**
 * Custom Application class to hold the singleton instance of our ApiService.
 * This is created only once when the app starts.
 */
class WayfinderApp : Application() {

    // Lazily initialized ApiService
    val apiService: ApiService by lazy {
        RetrofitInstance.create(this)
    }

    override fun onCreate() {
        super.onCreate()
        // You can add any other app-wide initializations here in the future
    }
}

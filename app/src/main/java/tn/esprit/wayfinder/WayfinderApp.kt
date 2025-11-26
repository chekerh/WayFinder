package tn.esprit.wayfinder

import android.app.Application
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
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
        
        // Configure Coil for optimal image loading and caching
        // Coil automatically uses the default ImageLoader, but we can configure it here
        // The default ImageLoader already has good caching, but we ensure it's optimized
    }
}

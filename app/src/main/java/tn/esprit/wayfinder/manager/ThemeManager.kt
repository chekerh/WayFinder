package tn.esprit.wayfinder.manager

import android.content.Context
import android.content.SharedPreferences

class ThemeManager(context: Context) {
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("wayfinder_theme_prefs", Context.MODE_PRIVATE)
    
    companion object {
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_FOLLOW_SYSTEM = "follow_system"
    }
    
    /**
     * Active ou désactive le dark mode
     * @param enabled true pour activer, false pour désactiver
     */
    fun setDarkModeEnabled(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_DARK_MODE, enabled)
            .putBoolean(KEY_FOLLOW_SYSTEM, false)
            .apply()
    }
    
    /**
     * Active le mode "suivre les préférences système"
     */
    fun setFollowSystemTheme(follow: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_FOLLOW_SYSTEM, follow)
            .apply()
    }
    
    /**
     * Vérifie si le dark mode est activé manuellement
     */
    fun isDarkModeEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_DARK_MODE, false)
    }
    
    /**
     * Vérifie si l'app suit les préférences système
     */
    fun isFollowingSystem(): Boolean {
        return sharedPreferences.getBoolean(KEY_FOLLOW_SYSTEM, true)
    }
    
    /**
     * Obtient l'état du dark mode (en tenant compte du mode système si activé)
     */
    fun getDarkModeState(useSystemTheme: Boolean = isFollowingSystem()): Boolean {
        return if (useSystemTheme) {
            // Retourne null pour indiquer qu'on suit le système
            // L'appelant devra utiliser isSystemInDarkTheme()
            false // Valeur par défaut, mais ne sera pas utilisée
        } else {
            isDarkModeEnabled()
        }
    }
}


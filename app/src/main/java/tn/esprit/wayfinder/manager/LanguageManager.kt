package tn.esprit.wayfinder.manager

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class LanguageManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "language_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val KEY_LANGUAGE = "selected_language"

    companion object {
        const val LANGUAGE_FRENCH = "fr"
        const val LANGUAGE_ENGLISH = "en"
        const val LANGUAGE_DEFAULT = LANGUAGE_FRENCH
    }

    /**
     * Définit la langue de l'application
     * @param languageCode Code de la langue (ex: "fr", "en")
     */
    fun setLanguage(languageCode: String) {
        sharedPreferences.edit().putString(KEY_LANGUAGE, languageCode).apply()
    }

    /**
     * Obtient la langue actuellement sélectionnée
     * @return Code de la langue (par défaut: "fr")
     */
    fun getLanguage(): String {
        return sharedPreferences.getString(KEY_LANGUAGE, LANGUAGE_DEFAULT) ?: LANGUAGE_DEFAULT
    }

    /**
     * Vérifie si la langue française est sélectionnée
     */
    fun isFrench(): Boolean {
        return getLanguage() == LANGUAGE_FRENCH
    }

    /**
     * Vérifie si la langue anglaise est sélectionnée
     */
    fun isEnglish(): Boolean {
        return getLanguage() == LANGUAGE_ENGLISH
    }
}

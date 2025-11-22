package tn.esprit.wayfinder.utils

import android.content.Context
import tn.esprit.wayfinder.data.LanguageDictionary
import tn.esprit.wayfinder.manager.LanguageManager

/**
 * Utilitaire pour traduire dynamiquement les chaînes de caractères
 * en fonction de la langue sélectionnée par l'utilisateur
 */
object StringTranslator {
    
    /**
     * Traduit un texte en fonction de la langue actuelle de l'application
     * @param context Le contexte de l'application
     * @param text Le texte à traduire (supposé être en français par défaut)
     * @return Le texte traduit dans la langue sélectionnée
     */
    fun translate(context: Context, text: String): String {
        val languageManager = LanguageManager(context)
        val currentLanguage = languageManager.getLanguage()
        
        // Si la langue est française, retourner le texte tel quel
        if (currentLanguage == LanguageManager.LANGUAGE_FRENCH) {
            return text
        }
        
        // Si la langue est anglaise, traduire le texte
        if (currentLanguage == LanguageManager.LANGUAGE_ENGLISH) {
            return LanguageDictionary.toEnglish(text)
        }
        
        // Par défaut, retourner le texte original
        return text
    }
    
    /**
     * Extension function pour traduire facilement une String
     */
    fun String.translate(context: Context): String {
        return StringTranslator.translate(context, this)
    }
}

/**
 * Extension function pour traduire une String directement
 * Usage: "Bonjour".translate(context)
 */
fun String.translate(context: Context): String {
    return StringTranslator.translate(context, this)
}

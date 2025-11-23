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
            // D'abord, essayer de traduire le texte complet
            val directTranslation = LanguageDictionary.toEnglish(text)
            if (directTranslation != text) {
                return directTranslation
            }
            
            // Si pas de traduction directe, essayer de traduire les parties du message
            // pour les messages composés comme "Votre réservation pour X a été confirmée. Numéro de confirmation: Y"
            val compositeTranslation = translateCompositeMessage(text)
            if (compositeTranslation != text) {
                return compositeTranslation
            }
            
            // Si aucune traduction trouvée, retourner le texte original
            return text
        }
        
        // Par défaut, retourner le texte original
        return text
    }
    
    /**
     * Traduit un message composé en traduisant ses parties individuelles
     */
    private fun translateCompositeMessage(text: String): String {
        var translated = text
        
        // Liste des patterns à traduire (ordre important - du plus long au plus court)
        val patterns = mapOf(
            "Votre réservation pour" to "Your reservation for",
            "a été effectué avec succès" to "has been processed successfully",
            "a été confirmée" to "has been confirmed",
            "a été annulée" to "has been cancelled",
            "a été mise à jour" to "has been updated",
            "Numéro de confirmation:" to "Confirmation number:",
            "Numéro de confirmation" to "Confirmation number",
            "Votre paiement de" to "Your payment of",
            "Le paiement de" to "The payment of",
            "a échoué" to "failed",
            "Le prix pour" to "The price for",
            "a baissé de" to "has dropped by",
            "Nouveau prix" to "New price",
            "Veuillez réessayer" to "Please try again"
        )
        
        // Remplacer chaque pattern par sa traduction
        patterns.forEach { (french, english) ->
            translated = translated.replace(french, english, ignoreCase = true)
        }
        
        return translated
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

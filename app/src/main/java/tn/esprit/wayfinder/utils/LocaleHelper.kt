package tn.esprit.wayfinder.utils

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.Build
import java.util.Locale

class LocaleHelper(base: Context) : ContextWrapper(base) {

    companion object {
        /**
         * Crée un nouveau contexte avec la locale spécifiée
         */
        fun wrap(context: Context, language: String): ContextWrapper {
            var ctx = context
            val config = context.resources.configuration
            val locale = Locale(language)
            Locale.setDefault(locale)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                config.setLocale(locale)
                ctx = context.createConfigurationContext(config)
            } else {
                @Suppress("DEPRECATION")
                config.locale = locale
                @Suppress("DEPRECATION")
                context.resources.updateConfiguration(config, context.resources.displayMetrics)
                ctx = context
            }

            return LocaleHelper(ctx)
        }

        /**
         * Applique la locale au contexte de l'application
         */
        fun setLocale(context: Context, language: String): Context {
            return wrap(context, language)
        }
    }
}

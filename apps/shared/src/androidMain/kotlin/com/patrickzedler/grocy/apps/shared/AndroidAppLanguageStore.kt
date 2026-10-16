package com.patrickzedler.grocy.apps.shared

import android.app.LocaleManager
import android.content.ComponentCallbacks
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import com.patrickzedler.grocy.core.data.language.AppLanguageStore
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Android's per-app language. From Android 13 on the system keeps it ([LocaleManager]), shows it
 * in the app's system settings and recreates the activities when it changes. Before, the app
 * stores it itself and applies it with [wrap] in `attachBaseContext`; activities recreate
 * themselves when it changes (see [MainActivity]).
 */
internal class AndroidAppLanguageStore(context: Context) : AppLanguageStore {

    private val appContext = context.applicationContext

    private val languageState = MutableStateFlow(currentLanguage())

    override val language: StateFlow<String?> = languageState.asStateFlow()

    init {
        // From Android 13 on the language can also change in the system settings
        appContext.registerComponentCallbacks(
            object : ComponentCallbacks {
                override fun onConfigurationChanged(newConfig: Configuration) {
                    languageState.value = currentLanguage()
                }

                @Deprecated("Deprecated in Java")
                override fun onLowMemory() = Unit
            },
        )
    }

    override fun setLanguage(tag: String?) {
        // Synchronously, the recreated activity reads it in attachBaseContext right away
        appContext.languagePreferences().edit().apply {
            if (tag == null) remove(KEY_LANGUAGE) else putString(KEY_LANGUAGE, tag)
        }.commit()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            appContext.getSystemService(LocaleManager::class.java)?.applicationLocales =
                if (tag == null) {
                    LocaleList.getEmptyLocaleList()
                } else {
                    LocaleList.forLanguageTags(tag)
                }
        }
        languageState.value = tag
    }

    private fun currentLanguage(): String? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            appContext.getSystemService(LocaleManager::class.java)
                ?.applicationLocales
                ?.takeUnless { it.isEmpty }
                ?.get(0)
                ?.toLanguageTag()
        } else {
            storedLanguage(appContext)
        }

    companion object {
        // A plain preference: attachBaseContext needs it synchronously, before any coroutine
        private const val PREFERENCES = "language"
        private const val KEY_LANGUAGE = "language"

        private fun Context.languagePreferences() =
            getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

        private fun storedLanguage(context: Context): String? =
            context.languagePreferences().getString(KEY_LANGUAGE, null)

        /** The locale of [tag], the system's for null. */
        fun localeOf(tag: String?): Locale =
            tag?.let(Locale::forLanguageTag) ?: Resources.getSystem().configuration.locales[0]

        /**
         * Before Android 13: [base] in the stored language, which also becomes the default
         * locale, where Compose Multiplatform reads the language of its texts.
         */
        fun wrap(base: Context): Context {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return base
            val locale = localeOf(storedLanguage(base.applicationContext ?: base))
            Locale.setDefault(locale)
            val configuration = Configuration(base.resources.configuration).apply {
                setLocales(LocaleList(locale))
            }
            return base.createConfigurationContext(configuration)
        }
    }
}

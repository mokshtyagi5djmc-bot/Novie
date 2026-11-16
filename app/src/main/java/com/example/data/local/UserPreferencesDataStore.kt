package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AppCurrency
import com.example.model.AppLanguage
import com.example.model.StorageMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.noviePrefsDataStore: DataStore<Preferences> by preferencesDataStore(name = "novie_preferences")

data class UserPreferences(
    val storageMode: StorageMode = StorageMode.PRIVATE_LOCAL,
    val currency: AppCurrency = AppCurrency.USD,
    val language: AppLanguage = AppLanguage.EN,
    val convertCurrencyUsingRates: Boolean = true,
    val onboardingCompleted: Boolean = false
)

class UserPreferencesDataStore(private val context: Context) {

    val preferencesFlow: Flow<UserPreferences> = context.noviePrefsDataStore.data.map { prefs ->
        UserPreferences(
            storageMode = StorageMode.fromName(prefs[KEY_STORAGE_MODE]),
            currency = AppCurrency.fromCode(prefs[KEY_CURRENCY]),
            language = AppLanguage.fromCode(prefs[KEY_LANGUAGE]),
            convertCurrencyUsingRates = prefs[KEY_CONVERT_RATES] ?: true,
            onboardingCompleted = prefs[KEY_ONBOARDING_DONE] ?: false
        )
    }

    suspend fun setStorageMode(mode: StorageMode) {
        context.noviePrefsDataStore.edit { prefs ->
            prefs[KEY_STORAGE_MODE] = mode.name
            prefs[KEY_ONBOARDING_DONE] = true
        }
    }

    suspend fun setCurrency(currency: AppCurrency) {
        context.noviePrefsDataStore.edit { prefs ->
            prefs[KEY_CURRENCY] = currency.code
        }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.noviePrefsDataStore.edit { prefs ->
            prefs[KEY_LANGUAGE] = language.code
        }
    }

    suspend fun setConvertCurrencyUsingRates(enabled: Boolean) {
        context.noviePrefsDataStore.edit { prefs ->
            prefs[KEY_CONVERT_RATES] = enabled
        }
    }

    suspend fun markOnboardingCompleted() {
        context.noviePrefsDataStore.edit { prefs ->
            prefs[KEY_ONBOARDING_DONE] = true
        }
    }

    private companion object {
        val KEY_STORAGE_MODE = stringPreferencesKey("storage_mode")
        val KEY_CURRENCY = stringPreferencesKey("currency_code")
        val KEY_LANGUAGE = stringPreferencesKey("language_code")
        val KEY_CONVERT_RATES = booleanPreferencesKey("convert_currency_rates")
        val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_completed")
    }
}

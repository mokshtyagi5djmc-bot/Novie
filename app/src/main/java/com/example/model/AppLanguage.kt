package com.example.model

import java.util.Locale

enum class AppLanguage(
    val code: String,
    val nativeLabel: String,
    val englishName: String,
    val shortCode: String,
    val isRtl: Boolean = false
) {
    EN("en", "English", "English", "EN", false),
    FR("fr", "Français", "French", "FR", false),
    DE("de", "Deutsch", "German", "DE", false),
    AR("ar", "العربية", "Arabic", "AR", true),
    HI("hi", "हिन्दी", "Hindi", "HI", false);

    fun toLocale(): Locale = Locale.forLanguageTag(code)

    companion object {
        fun fromCode(code: String?): AppLanguage {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: EN
        }
    }
}

enum class StorageMode {
    PRIVATE_LOCAL,
    GOOGLE_SYNC;

    companion object {
        fun fromName(name: String?): StorageMode {
            return entries.firstOrNull { it.name == name } ?: PRIVATE_LOCAL
        }
    }
}

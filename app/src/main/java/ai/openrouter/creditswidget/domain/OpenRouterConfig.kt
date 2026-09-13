package ai.openrouter.creditswidget.domain

import androidx.annotation.StringRes
import ai.openrouter.creditswidget.R

enum class StartPage(val url: String, @StringRes val labelRes: Int) {
    CREDITS("https://openrouter.ai/settings/credits", R.string.start_page_credits), LOGS("https://openrouter.ai/logs", R.string.start_page_logs), ACTIVITY("https://openrouter.ai/activity", R.string.start_page_activity)
}
enum class RefreshInterval(val minutes: Long, @StringRes val labelRes: Int) {
    DISABLED(0, R.string.interval_disabled), MINUTES_15(15, R.string.interval_15), MINUTES_30(30, R.string.interval_30), HOUR_1(60, R.string.interval_1h), HOURS_3(180, R.string.interval_3h), HOURS_6(360, R.string.interval_6h), HOURS_12(720, R.string.interval_12h), HOURS_24(1440, R.string.interval_24h)
}
object ApiKeyNormalizer {
    private val pattern = Regex("^sk-or-[A-Za-z0-9_-]+$")
    fun normalize(input: String?): String? {
        var value = input?.trim()?.trim('"', '\'')?.trim() ?: return null
        if (value.startsWith("authorization:", true)) value = value.substringAfter(':').trim()
        if (value.startsWith("bearer ", true)) value = value.substring(7).trim()
        value = value.trimEnd(';').trim().trim('"', '\'').trim()
        return value.takeIf { it.length >= 30 && !it.any(Char::isWhitespace) && pattern.matches(it) }
    }
}

object ApiKeyMasker {
    fun mask(value: String): String {
        val trimmed = value.trim()
        val prefix = when {
            trimmed.startsWith("sk-or-mgmt-") -> "sk-or-mgmt-"
            trimmed.startsWith("sk-or-v1-") -> "sk-or-v1-"
            trimmed.startsWith("sk-or-") -> "sk-or-"
            else -> ""
        }
        return prefix + "••••••••" + trimmed.takeLast(4)
    }
}

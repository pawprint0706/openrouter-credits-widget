package ai.openrouter.creditswidget.domain

enum class StartPage(val url: String, val label: String) {
    CREDITS("https://openrouter.ai/settings/credits", "크레딧"), LOGS("https://openrouter.ai/logs", "로그"), ACTIVITY("https://openrouter.ai/activity", "활동")
}
enum class RefreshInterval(val minutes: Long, val label: String) {
    DISABLED(0, "사용 안 함"), MINUTES_15(15, "15분"), MINUTES_30(30, "30분"), HOUR_1(60, "1시간"), HOURS_3(180, "3시간"), HOURS_6(360, "6시간"), HOURS_12(720, "12시간"), HOURS_24(1440, "24시간")
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

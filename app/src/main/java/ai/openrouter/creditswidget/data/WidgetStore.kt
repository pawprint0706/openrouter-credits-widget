package ai.openrouter.creditswidget.data

import android.content.Context
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import ai.openrouter.creditswidget.domain.CreditsSnapshot
import ai.openrouter.creditswidget.domain.RefreshInterval
import ai.openrouter.creditswidget.domain.StartPage
import kotlinx.coroutines.flow.first

private val Context.widgetData by preferencesDataStore("widget_data")
data class WidgetState(val snapshot: CreditsSnapshot?, val message: String?, val interval: RefreshInterval, val startPage: StartPage)
class WidgetStore(private val context: Context) {
    private val credits = stringPreferencesKey("credits"); private val usage = stringPreferencesKey("usage"); private val fetched = longPreferencesKey("fetched"); private val status = stringPreferencesKey("status"); private val interval = stringPreferencesKey("interval"); private val page = stringPreferencesKey("page")
    suspend fun state(): WidgetState {
        val p = context.widgetData.data.first()
        val totalCredits = p[credits]?.toBigDecimalOrNull()
        val totalUsage = p[usage]?.toBigDecimalOrNull()
        val fetchedAt = p[fetched]
        val snapshot = if (totalCredits != null && totalUsage != null && fetchedAt != null) CreditsSnapshot(totalCredits, totalUsage, fetchedAt) else null
        return WidgetState(snapshot, p[status], enumOrDefault(p[interval], RefreshInterval.HOUR_1), enumOrDefault(p[page], StartPage.CREDITS))
    }
    suspend fun saveSnapshot(s: CreditsSnapshot) { context.widgetData.edit { it[credits] = s.totalCredits.toPlainString(); it[usage] = s.totalUsage.toPlainString(); it[fetched] = s.fetchedAtMillis; it.remove(status) } }
    suspend fun clearSnapshot() { context.widgetData.edit { it.remove(credits); it.remove(usage); it.remove(fetched); it[status] = "API 키 설정 필요" } }
    suspend fun setStatus(message: String) { context.widgetData.edit { it[status] = message } }
    suspend fun saveSettings(newInterval: RefreshInterval, newPage: StartPage) { context.widgetData.edit { it[interval] = newInterval.name; it[page] = newPage.name } }
    private inline fun <reified T : Enum<T>> enumOrDefault(value: String?, fallback: T): T = value?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: fallback
}

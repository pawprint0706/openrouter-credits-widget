package ai.openrouter.creditswidget.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import ai.openrouter.creditswidget.domain.CreditsSnapshot
import ai.openrouter.creditswidget.domain.RefreshInterval
import ai.openrouter.creditswidget.domain.StartPage
import ai.openrouter.creditswidget.domain.WidgetStatus
import kotlinx.coroutines.flow.first

private val Context.widgetData by preferencesDataStore("widget_data")

/** A last known balance always outranks a status label, so a stale render can never hide the amount. */
internal fun widgetPrimaryText(snapshot: CreditsSnapshot?, status: WidgetStatus?, label: (WidgetStatus) -> String): String =
    snapshot?.let { it.dollars(it.remainingCredits) } ?: label(status ?: WidgetStatus.NEEDS_KEY)

data class WidgetState(val snapshot: CreditsSnapshot?, val status: WidgetStatus?, val interval: RefreshInterval, val startPage: StartPage, val notifyOnRefresh: Boolean)
class WidgetStore(private val context: Context) {
    private val credits = stringPreferencesKey("credits"); private val usage = stringPreferencesKey("usage"); private val fetched = longPreferencesKey("fetched"); private val status = stringPreferencesKey("status"); private val interval = stringPreferencesKey("interval"); private val page = stringPreferencesKey("page"); private val notify = booleanPreferencesKey("notify")
    suspend fun state(): WidgetState {
        val p = context.widgetData.data.first()
        val totalCredits = p[credits]?.toBigDecimalOrNull()
        val totalUsage = p[usage]?.toBigDecimalOrNull()
        val fetchedAt = p[fetched]
        val snapshot = if (totalCredits != null && totalUsage != null && fetchedAt != null) CreditsSnapshot(totalCredits, totalUsage, fetchedAt) else null
        val widgetStatus = p[status]?.let { stored -> runCatching { WidgetStatus.valueOf(stored) }.getOrNull() }
        return WidgetState(snapshot, widgetStatus, enumOrDefault(p[interval], RefreshInterval.HOUR_1), enumOrDefault(p[page], StartPage.CREDITS), p[notify] ?: false)
    }
    suspend fun saveSnapshot(s: CreditsSnapshot) { context.widgetData.edit { it[credits] = s.totalCredits.toPlainString(); it[usage] = s.totalUsage.toPlainString(); it[fetched] = s.fetchedAtMillis; it.remove(status) } }
    suspend fun clearSnapshot() { context.widgetData.edit { it.remove(credits); it.remove(usage); it.remove(fetched); it[status] = WidgetStatus.NEEDS_KEY.name } }
    suspend fun setStatus(value: WidgetStatus) { context.widgetData.edit { it[status] = value.name } }
    suspend fun saveNotifyOnRefresh(enabled: Boolean) { context.widgetData.edit { it[notify] = enabled } }
    suspend fun saveSettings(newInterval: RefreshInterval, newPage: StartPage) { context.widgetData.edit { it[interval] = newInterval.name; it[page] = newPage.name } }
    private inline fun <reified T : Enum<T>> enumOrDefault(value: String?, fallback: T): T = value?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: fallback
}

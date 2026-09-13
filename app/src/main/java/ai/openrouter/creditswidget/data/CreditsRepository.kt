package ai.openrouter.creditswidget.data

import android.content.Context
import androidx.glance.appwidget.updateAll
import ai.openrouter.creditswidget.domain.RefreshResult
import ai.openrouter.creditswidget.domain.WidgetStatus
import ai.openrouter.creditswidget.widget.CreditsWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CreditsRepository(private val context: Context) {
    private val keys = EncryptedKeyStore(context); private val store = WidgetStore(context); private val api = OpenRouterApi()
    suspend fun refresh(): RefreshResult = withContext(Dispatchers.IO) {
        val key = keys.read() ?: run {
            store.clearSnapshot()
            CreditsWidget().updateAll(context)
            return@withContext RefreshResult.NoKey
        }
        when (val result = api.credits(key)) {
            is ApiOutcome.Success -> { store.saveSnapshot(result.snapshot); RefreshResult.Success }
            ApiOutcome.Unauthorized -> { keys.delete(); store.clearSnapshot(); RefreshResult.Unauthorized }
            ApiOutcome.Forbidden -> { store.setStatus(WidgetStatus.DIFFERENT_KEY); RefreshResult.Forbidden }
            ApiOutcome.Retryable -> { store.setStatus(WidgetStatus.OFFLINE); RefreshResult.Retryable }
            ApiOutcome.Invalid -> { store.setStatus(WidgetStatus.RESPONSE_ERROR); RefreshResult.InvalidResponse }
        }.also { CreditsWidget().updateAll(context) }
    }
}

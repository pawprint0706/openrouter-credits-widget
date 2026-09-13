package ai.openrouter.creditswidget.worker

import android.content.Context
import android.widget.Toast
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.Constraints
import androidx.work.workDataOf
import androidx.glance.appwidget.updateAll
import ai.openrouter.creditswidget.data.CreditsRepository
import ai.openrouter.creditswidget.data.WidgetStore
import ai.openrouter.creditswidget.domain.RefreshInterval
import ai.openrouter.creditswidget.domain.RefreshResult
import ai.openrouter.creditswidget.widget.CreditsWidget
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

private const val NOTIFY_USER = "notify_user"

class CreditsRefreshWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val store = WidgetStore(applicationContext)
        return try {
            // The repository renders the widget once, after the refresh finishes, so every render carries
            // final state. Rendering a "refreshing" state first would leave the widget stuck on it: Glance
            // only recomposes when it starts a session, so a later update cannot replace an earlier render.
            val result = CreditsRepository(applicationContext).refresh()
            if (inputData.getBoolean(NOTIFY_USER, false) && runAttemptCount == 0 && store.state().notifyOnRefresh) showToast(result)
            when (result) { RefreshResult.Retryable -> Result.retry(); else -> Result.success() }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            try {
                store.setStatus("오프라인 또는 오류")
                CreditsWidget().updateAll(applicationContext)
            } catch (_: Exception) {
                // WorkManager records the failure; the next successful refresh re-renders the widget.
            }
            Result.failure()
        }
    }

    private suspend fun showToast(result: RefreshResult) {
        val message = when (result) {
            RefreshResult.Success -> WidgetStore(applicationContext).state().snapshot?.let { "갱신 완료  " + it.dollars(it.remainingCredits) } ?: "갱신 완료"
            RefreshResult.NoKey -> "API 키를 설정하세요"
            RefreshResult.Unauthorized -> "API 키가 거부되어 삭제되었습니다"
            RefreshResult.Forbidden -> "이 키로는 크레딧을 읽을 수 없습니다"
            RefreshResult.Retryable -> "네트워크 오류로 갱신하지 못했습니다"
            RefreshResult.InvalidResponse -> "응답 오류로 갱신하지 못했습니다"
        }
        withContext(Dispatchers.Main) { Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show() }
    }
}
object CreditsScheduler {
    private const val PERIODIC = "openrouter-credits-periodic"; private const val NOW = "openrouter-credits-now"
    private val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
    fun schedule(context: Context, interval: RefreshInterval) { val manager = WorkManager.getInstance(context); if (interval == RefreshInterval.DISABLED) { manager.cancelUniqueWork(PERIODIC); return }; val request = PeriodicWorkRequestBuilder<CreditsRefreshWorker>(interval.minutes, TimeUnit.MINUTES).setConstraints(constraints).setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES).build(); manager.enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.UPDATE, request) }
    fun refreshNow(context: Context) { val request = OneTimeWorkRequestBuilder<CreditsRefreshWorker>().setInputData(workDataOf(NOTIFY_USER to true)).setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES).build(); WorkManager.getInstance(context).enqueueUniqueWork(NOW, ExistingWorkPolicy.REPLACE, request) }
}

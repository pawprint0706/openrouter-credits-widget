package ai.openrouter.creditswidget.worker

import android.content.Context
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
import ai.openrouter.creditswidget.data.CreditsRepository
import ai.openrouter.creditswidget.domain.RefreshInterval
import ai.openrouter.creditswidget.domain.RefreshResult
import java.util.concurrent.TimeUnit

class CreditsRefreshWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = when (CreditsRepository(applicationContext).refresh()) { RefreshResult.Retryable -> Result.retry(); else -> Result.success() }
}
object CreditsScheduler {
    private const val PERIODIC = "openrouter-credits-periodic"; private const val NOW = "openrouter-credits-now"
    private val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
    fun schedule(context: Context, interval: RefreshInterval) { val manager = WorkManager.getInstance(context); if (interval == RefreshInterval.DISABLED) { manager.cancelUniqueWork(PERIODIC); return }; val request = PeriodicWorkRequestBuilder<CreditsRefreshWorker>(interval.minutes, TimeUnit.MINUTES).setConstraints(constraints).setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES).build(); manager.enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.UPDATE, request) }
    fun refreshNow(context: Context) { val request = OneTimeWorkRequestBuilder<CreditsRefreshWorker>().setConstraints(constraints).setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES).build(); WorkManager.getInstance(context).enqueueUniqueWork(NOW, ExistingWorkPolicy.KEEP, request) }
}

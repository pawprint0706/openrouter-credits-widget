package ai.openrouter.creditswidget.worker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.glance.appwidget.updateAll
import ai.openrouter.creditswidget.data.REFRESHING_STATUS_TIMEOUT_MILLIS
import ai.openrouter.creditswidget.data.WidgetStore
import ai.openrouter.creditswidget.widget.CreditsWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class RefreshTimeoutReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                if (WidgetStore(appContext).expireRefreshingIfStale()) {
                    CreditsWidget().updateAll(appContext)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

object RefreshTimeoutScheduler {
    private const val REQUEST_CODE = 701

    fun schedule(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + REFRESHING_STATUS_TIMEOUT_MILLIS,
            PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                Intent(context, RefreshTimeoutReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ),
        )
    }
}

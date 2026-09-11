package ai.openrouter.creditswidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.os.Bundle
import android.os.Build
import android.widget.RemoteViews
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import ai.openrouter.creditswidget.data.WidgetStore
import ai.openrouter.creditswidget.ui.BrowserLauncher
import ai.openrouter.creditswidget.widget.CreditsWidgetReceiver
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        publishWidgetPreview()
        lifecycleScope.launch {
            BrowserLauncher.open(this@MainActivity, WidgetStore(this@MainActivity).state().startPage)
            finish()
        }
    }

    private fun publishWidgetPreview() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return
        runCatching {
            AppWidgetManager.getInstance(this).setWidgetPreview(
                ComponentName(this, CreditsWidgetReceiver::class.java),
                AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN,
                RemoteViews(packageName, R.layout.widget_preview),
            )
        }
    }
}

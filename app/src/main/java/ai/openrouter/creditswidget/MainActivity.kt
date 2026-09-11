package ai.openrouter.creditswidget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import ai.openrouter.creditswidget.data.WidgetStore
import ai.openrouter.creditswidget.ui.BrowserLauncher
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() { override fun onCreate(state: Bundle?) { super.onCreate(state); lifecycleScope.launch { BrowserLauncher.open(this@MainActivity, WidgetStore(this@MainActivity).state().startPage); finish() } } }

package ai.openrouter.creditswidget.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.ActionCallback
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionRunCallback
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.TextStyle
import ai.openrouter.creditswidget.MainActivity
import ai.openrouter.creditswidget.SettingsActivity
import ai.openrouter.creditswidget.data.EncryptedKeyStore
import ai.openrouter.creditswidget.data.WidgetStore
import ai.openrouter.creditswidget.worker.CreditsScheduler

class CreditsWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = WidgetStore(context).state()
        val hasKey = EncryptedKeyStore(context).exists()
        provideContent {
            val bodyAction = if (hasKey) actionRunCallback<RefreshAction>() else actionStartActivity<SettingsActivity>()
            val main = ColorProvider(Color(0xFF1D1B20), Color(0xFFE7E0EC)); val muted = ColorProvider(Color(0xFF6D6875), Color(0xFFCAC4D0))
            Column(GlanceModifier.fillMaxSize().background(imageProvider = ImageProvider(ai.openrouter.creditswidget.R.drawable.widget_background)).appWidgetBackground().clickable(bodyAction).padding(horizontal = 12.dp, vertical = 6.dp)) {
                Spacer(GlanceModifier.defaultWeight())
                Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.Bottom) {
                    Image(provider = ImageProvider(ai.openrouter.creditswidget.R.drawable.openrouter_logo), contentDescription = "OpenRouter", modifier = GlanceModifier.size(32.dp).clickable(actionStartActivity<MainActivity>()))
                    Spacer(GlanceModifier.width(12.dp))
                    if (state.snapshot == null) Text(state.message ?: "API 키 설정 필요", style = TextStyle(color = main, fontWeight = FontWeight.Bold, fontSize = 26.sp), modifier = GlanceModifier.defaultWeight().clickable(bodyAction), maxLines = 1)
                    else {
                        Text(state.snapshot.dollars(state.snapshot.remainingCredits), style = TextStyle(color = main, fontWeight = FontWeight.Bold, fontSize = 26.sp), modifier = GlanceModifier.defaultWeight().clickable(bodyAction), maxLines = 1)
                        Spacer(GlanceModifier.width(10.dp))
                        Text("누적 충전  " + state.snapshot.dollars(state.snapshot.totalCredits), style = TextStyle(color = muted, fontSize = 13.sp), modifier = GlanceModifier.clickable(bodyAction), maxLines = 1)
                    }
                }
            }
        }
    }
}
class CreditsWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = CreditsWidget() }
class RefreshAction : ActionCallback { override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) { CreditsScheduler.refreshNow(context) } }

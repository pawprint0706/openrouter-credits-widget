package ai.openrouter.creditswidget.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
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
            val main = ColorProvider(ai.openrouter.creditswidget.R.color.widget_primary)
            val muted = ColorProvider(ai.openrouter.creditswidget.R.color.widget_muted)
            val divider = ColorProvider(ai.openrouter.creditswidget.R.color.widget_divider)
            Column(GlanceModifier.fillMaxSize().background(imageProvider = ImageProvider(ai.openrouter.creditswidget.R.drawable.widget_background)).appWidgetBackground().clickable(bodyAction).padding(horizontal = 10.dp, vertical = 4.dp)) {
                Spacer(GlanceModifier.defaultWeight())
                Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.Bottom) {
                    Image(provider = ImageProvider(ai.openrouter.creditswidget.R.drawable.openrouter_logo), contentDescription = "OpenRouter", modifier = GlanceModifier.size(36.dp).clickable(actionStartActivity<MainActivity>()))
                    Spacer(GlanceModifier.width(10.dp))
                    Box(GlanceModifier.width(1.dp).height(36.dp).background(divider)) {}
                    Spacer(GlanceModifier.width(10.dp))
                    if (state.snapshot == null) Text(state.message ?: "API 키 설정 필요", style = TextStyle(color = main, fontWeight = FontWeight.Bold, fontSize = 28.sp), modifier = GlanceModifier.defaultWeight().clickable(bodyAction), maxLines = 1)
                    else {
                        Text(state.snapshot.dollars(state.snapshot.remainingCredits), style = TextStyle(color = main, fontWeight = FontWeight.Bold, fontSize = 28.sp), modifier = GlanceModifier.defaultWeight().clickable(bodyAction), maxLines = 1)
                        Spacer(GlanceModifier.width(8.dp))
                        Text("누적 충전  " + state.snapshot.dollars(state.snapshot.totalCredits), style = TextStyle(color = muted, fontSize = 14.sp), modifier = GlanceModifier.clickable(bodyAction), maxLines = 1)
                    }
                }
            }
        }
    }
}
class CreditsWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = CreditsWidget() }
class RefreshAction : ActionCallback { override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) { CreditsScheduler.refreshNow(context) } }

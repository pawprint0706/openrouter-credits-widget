package ai.openrouter.creditswidget.ui

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import ai.openrouter.creditswidget.SettingsActivity
import ai.openrouter.creditswidget.domain.StartPage

object BrowserLauncher {
    fun open(context: Context, page: StartPage) {
        val settings = PendingIntent.getActivity(context, 91, Intent(context, SettingsActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val purple = android.graphics.Color.rgb(118, 36, 244)
        val tabs = CustomTabsIntent.Builder().setShowTitle(true).setDefaultColorSchemeParams(CustomTabColorSchemeParams.Builder().setToolbarColor(purple).build()).setColorSchemeParams(CustomTabsIntent.COLOR_SCHEME_DARK, CustomTabColorSchemeParams.Builder().setToolbarColor(android.graphics.Color.rgb(35, 24, 53)).build()).addMenuItem("앱 설정", settings).build()
        try { tabs.launchUrl(context, Uri.parse(page.url)) } catch (_: Exception) { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(page.url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
}

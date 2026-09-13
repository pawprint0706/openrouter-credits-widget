package ai.openrouter.creditswidget.domain

import androidx.annotation.StringRes
import ai.openrouter.creditswidget.R

/** Persisted as its name so the stored value stays locale independent. */
enum class WidgetStatus(@StringRes val labelRes: Int) {
    NEEDS_KEY(R.string.widget_status_needs_key),
    OFFLINE(R.string.widget_status_offline),
    DIFFERENT_KEY(R.string.widget_status_different_key),
    RESPONSE_ERROR(R.string.widget_status_response_error),
}

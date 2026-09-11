package ai.openrouter.creditswidget.domain

import java.math.BigDecimal
import java.math.RoundingMode

data class CreditsSnapshot(val totalCredits: BigDecimal, val totalUsage: BigDecimal, val fetchedAtMillis: Long) {
    val remainingCredits: BigDecimal get() = totalCredits.subtract(totalUsage).max(BigDecimal.ZERO)
    fun dollars(value: BigDecimal) = "$" + value.setScale(2, RoundingMode.HALF_UP).toPlainString()
}

sealed interface RefreshResult { data object Success : RefreshResult; data object NoKey : RefreshResult; data object Unauthorized : RefreshResult; data object Forbidden : RefreshResult; data object Retryable : RefreshResult; data object InvalidResponse : RefreshResult }

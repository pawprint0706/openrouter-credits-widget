package ai.openrouter.creditswidget

import ai.openrouter.creditswidget.data.CreditsParser
import ai.openrouter.creditswidget.data.widgetPrimaryText
import ai.openrouter.creditswidget.domain.ApiKeyMasker
import ai.openrouter.creditswidget.domain.ApiKeyNormalizer
import ai.openrouter.creditswidget.domain.CreditsSnapshot
import ai.openrouter.creditswidget.domain.RefreshInterval
import ai.openrouter.creditswidget.domain.StartPage
import ai.openrouter.creditswidget.domain.WidgetStatus
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class OpenRouterUnitTest {
    @Test fun parserAcceptsNumbersAndDecimalStringsAndClampsBalance() { val normal = CreditsParser.parse("{\"data\":{\"total_credits\":\"100.50\",\"total_usage\":25.75}}", 1)!!; assertEquals(BigDecimal("74.75"), normal.remainingCredits); val over = CreditsParser.parse("{\"data\":{\"total_credits\":10,\"total_usage\":12}}", 1)!!; assertEquals(BigDecimal.ZERO, over.remainingCredits) }
    @Test fun parserRejectsMalformedPayloads() { assertNull(CreditsParser.parse("{}")); assertNull(CreditsParser.parse("{\"data\":{\"total_credits\":true,\"total_usage\":1}}")) }
    @Test fun pastedAuthorizationKeyIsNormalized() { val key = "sk-or-abcdefghijklmnopqrstuvwxyz123456"; assertEquals(key, ApiKeyNormalizer.normalize(" Authorization: Bearer '$key'; ")); assertNull(ApiKeyNormalizer.normalize("bearer not-a-key")) }
    @Test fun savedApiKeyIsMaskedWithoutExposingItsBody() { assertEquals("sk-or-v1-••••••••3456", ApiKeyMasker.mask("sk-or-v1-abcdefghijklmnopqrstuvwxyz123456")); assertEquals("sk-or-mgmt-••••••••WXYZ", ApiKeyMasker.mask("sk-or-mgmt-abcdefghijklmnopqrstuvWXYZ")) }
    @Test fun urlsAndPeriodicMinimumAreStable() { assertEquals("https://openrouter.ai/settings/credits", StartPage.CREDITS.url); assertEquals("https://openrouter.ai/logs", StartPage.LOGS.url); assertEquals(15, RefreshInterval.MINUTES_15.minutes); assertEquals(0, RefreshInterval.DISABLED.minutes) }
    @Test fun lastKnownBalanceOutranksEveryStatusLabel() {
        val snapshot = CreditsSnapshot(BigDecimal("100.50"), BigDecimal("25.75"), 1)
        val label: (WidgetStatus) -> String = { it.name }
        assertEquals("$74.75", widgetPrimaryText(snapshot, WidgetStatus.OFFLINE, label))
        assertEquals("$74.75", widgetPrimaryText(snapshot, null, label))
        assertEquals("DIFFERENT_KEY", widgetPrimaryText(null, WidgetStatus.DIFFERENT_KEY, label))
        assertEquals("NEEDS_KEY", widgetPrimaryText(null, null, label))
    }
}

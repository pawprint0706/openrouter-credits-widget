package ai.openrouter.creditswidget

import ai.openrouter.creditswidget.data.CreditsParser
import ai.openrouter.creditswidget.data.REFRESHING_STATUS
import ai.openrouter.creditswidget.data.REFRESHING_STATUS_TIMEOUT_MILLIS
import ai.openrouter.creditswidget.data.visibleWidgetStatus
import ai.openrouter.creditswidget.domain.ApiKeyMasker
import ai.openrouter.creditswidget.domain.ApiKeyNormalizer
import ai.openrouter.creditswidget.domain.RefreshInterval
import ai.openrouter.creditswidget.domain.StartPage
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class OpenRouterUnitTest {
    @Test fun parserAcceptsNumbersAndDecimalStringsAndClampsBalance() { val normal = CreditsParser.parse("{\"data\":{\"total_credits\":\"100.50\",\"total_usage\":25.75}}", 1)!!; assertEquals(BigDecimal("74.75"), normal.remainingCredits); val over = CreditsParser.parse("{\"data\":{\"total_credits\":10,\"total_usage\":12}}", 1)!!; assertEquals(BigDecimal.ZERO, over.remainingCredits) }
    @Test fun parserRejectsMalformedPayloads() { assertNull(CreditsParser.parse("{}")); assertNull(CreditsParser.parse("{\"data\":{\"total_credits\":true,\"total_usage\":1}}")) }
    @Test fun pastedAuthorizationKeyIsNormalized() { val key = "sk-or-abcdefghijklmnopqrstuvwxyz123456"; assertEquals(key, ApiKeyNormalizer.normalize(" Authorization: Bearer '$key'; ")); assertNull(ApiKeyNormalizer.normalize("bearer not-a-key")) }
    @Test fun savedApiKeyIsMaskedWithoutExposingItsBody() { assertEquals("sk-or-v1-••••••••3456", ApiKeyMasker.mask("sk-or-v1-abcdefghijklmnopqrstuvwxyz123456")); assertEquals("sk-or-mgmt-••••••••WXYZ", ApiKeyMasker.mask("sk-or-mgmt-abcdefghijklmnopqrstuvWXYZ")) }
    @Test fun urlsAndPeriodicMinimumAreStable() { assertEquals("https://openrouter.ai/settings/credits", StartPage.CREDITS.url); assertEquals("https://openrouter.ai/logs", StartPage.LOGS.url); assertEquals(15, RefreshInterval.MINUTES_15.minutes); assertEquals(0, RefreshInterval.DISABLED.minutes) }
    @Test fun staleOrUnownedRefreshingStatusIsHidden() {
        val now = 100_000L
        assertEquals(REFRESHING_STATUS, visibleWidgetStatus(REFRESHING_STATUS, now - 1, now))
        assertNull(visibleWidgetStatus(REFRESHING_STATUS, null, now))
        assertNull(visibleWidgetStatus(REFRESHING_STATUS, now - REFRESHING_STATUS_TIMEOUT_MILLIS, now))
        assertNull(visibleWidgetStatus(REFRESHING_STATUS, now + 1, now))
        assertEquals("오프라인 또는 오류", visibleWidgetStatus("오프라인 또는 오류", null, now))
    }
}

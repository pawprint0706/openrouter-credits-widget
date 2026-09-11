package ai.openrouter.creditswidget.data

import ai.openrouter.creditswidget.domain.CreditsSnapshot
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.math.BigDecimal
import java.io.IOException
import java.util.concurrent.TimeUnit

sealed interface ApiOutcome { data class Success(val snapshot: CreditsSnapshot) : ApiOutcome; data object Unauthorized : ApiOutcome; data object Forbidden : ApiOutcome; data object Retryable : ApiOutcome; data object Invalid : ApiOutcome }
object CreditsParser {
    fun parse(body: String, nowMillis: Long = System.currentTimeMillis()): CreditsSnapshot? = try {
        val data = Json.parseToJsonElement(body).jsonObject["data"]?.jsonObject ?: return null
        val credits = data["total_credits"]?.jsonPrimitive?.content?.toBigDecimalOrNull() ?: return null
        val usage = data["total_usage"]?.jsonPrimitive?.content?.toBigDecimalOrNull() ?: return null
        CreditsSnapshot(credits, usage, nowMillis)
    } catch (_: Exception) { null }
}
class OpenRouterApi(private val client: OkHttpClient = OkHttpClient.Builder().callTimeout(15, TimeUnit.SECONDS).followRedirects(false).build()) {
    fun credits(apiKey: String): ApiOutcome = try {
        val request = Request.Builder().url("https://openrouter.ai/api/v1/credits").header("Authorization", "Bearer $apiKey").header("Accept", "application/json").build()
        client.newCall(request).execute().use { response -> when (response.code) {
            in 200..299 -> response.body?.string()?.let { CreditsParser.parse(it) }?.let(ApiOutcome::Success) ?: ApiOutcome.Invalid
            401 -> ApiOutcome.Unauthorized; 403 -> ApiOutcome.Forbidden; 429, in 500..599 -> ApiOutcome.Retryable; else -> ApiOutcome.Invalid
        } }
    } catch (_: IOException) { ApiOutcome.Retryable }
}

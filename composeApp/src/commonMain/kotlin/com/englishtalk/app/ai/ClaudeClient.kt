package com.englishtalk.app.ai

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/** One message in a Messages API conversation. `content` is kept as raw JSON so
 *  assistant turns (including any thinking blocks) can be sent back unchanged. */
data class ApiMessage(val role: String, val content: JsonElement) {
    companion object {
        fun user(text: String) = ApiMessage("user", JsonPrimitive(text))
    }
}

class ClaudeResult(
    /** Concatenated text blocks of the response. */
    val text: String,
    /** The assistant message to append to the conversation history. */
    val assistantMessage: ApiMessage,
)

class ClaudeException(message: String) : Exception(message)

/**
 * Minimal Claude Messages API client over Ktor.
 * There is no official Anthropic SDK for Kotlin/Native (iOS), so the app talks to
 * the REST API directly from common code.
 */
class ClaudeClient(
    private val httpClient: HttpClient = createHttpClient(),
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun createMessage(
        config: AiConfig,
        system: String,
        messages: List<ApiMessage>,
        outputSchema: JsonObject? = null,
        effort: String = "low",
        maxTokens: Int = 16000,
    ): ClaudeResult {
        if (config.apiKey.isBlank()) {
            throw ClaudeException("Chưa có API key. Vào Cài đặt để nhập Claude API key.")
        }
        val useFallbacks = config.model in SERVER_FALLBACK_MODELS
        val body = buildJsonObject {
            put("model", config.model)
            put("max_tokens", maxTokens)
            put("system", system)
            put("messages", buildJsonArray {
                messages.forEach { m ->
                    add(buildJsonObject {
                        put("role", m.role)
                        put("content", m.content)
                    })
                }
            })
            put("output_config", buildJsonObject {
                put("effort", effort)
                if (outputSchema != null) {
                    put("format", buildJsonObject {
                        put("type", "json_schema")
                        put("schema", outputSchema)
                    })
                }
            })
            // Server-side refusal fallback: a classifier false positive is retried
            // on Anthropic's recommended fallback model instead of failing the turn.
            if (useFallbacks) put("fallbacks", "default")
        }

        val response = try {
            httpClient.post(config.baseUrl.trimEnd('/') + "/v1/messages") {
                contentType(ContentType.Application.Json)
                header("x-api-key", config.apiKey)
                header("anthropic-version", "2023-06-01")
                if (useFallbacks) header("anthropic-beta", "server-side-fallback-2026-07-01")
                setBody(body.toString())
            }
        } catch (e: Exception) {
            throw ClaudeException("Không kết nối được tới máy chủ AI: ${e.message}")
        }

        val raw = response.bodyAsText()
        if (!response.status.isSuccess()) {
            val apiMessage = runCatching {
                json.parseToJsonElement(raw).jsonObject["error"]?.jsonObject
                    ?.get("message")?.jsonPrimitive?.contentOrNull
            }.getOrNull()
            throw ClaudeException(
                when (response.status.value) {
                    401 -> "API key không hợp lệ (401)."
                    429 -> "Vượt giới hạn tốc độ, vui lòng thử lại sau (429)."
                    529, 503 -> "Máy chủ AI đang quá tải, thử lại sau."
                    else -> "Lỗi AI ${response.status.value}: ${apiMessage ?: raw.take(200)}"
                }
            )
        }

        val obj = json.parseToJsonElement(raw).jsonObject
        val stopReason = obj["stop_reason"]?.jsonPrimitive?.contentOrNull
        if (stopReason == "refusal") {
            throw ClaudeException("AI đã từ chối yêu cầu này. Hãy thử diễn đạt khác.")
        }
        val content = obj["content"]?.jsonArray ?: JsonArray(emptyList())
        if (stopReason == "max_tokens") {
            throw ClaudeException("Phản hồi của AI bị cắt ngắn, vui lòng thử lại.")
        }
        val text = content
            .filter { it.jsonObject["type"]?.jsonPrimitive?.contentOrNull == "text" }
            .joinToString("") { it.jsonObject["text"]?.jsonPrimitive?.contentOrNull.orEmpty() }
        return ClaudeResult(text = text, assistantMessage = ApiMessage("assistant", content))
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://api.anthropic.com"

        val MODELS = listOf(
            "claude-opus-5-5" to "Claude Opus 5.5 (thông minh nhất)",
            "claude-sonnet-5-5" to "Claude Sonnet 5.5 (cân bằng)",
            "claude-haiku-5-5" to "Claude Haiku 5.5 (nhanh, rẻ)",
        )
        const val DEFAULT_MODEL = "claude-opus-5-5"

        private val SERVER_FALLBACK_MODELS = setOf("claude-opus-5-5", "claude-sonnet-5-5")

        fun createHttpClient() = HttpClient {
            install(HttpTimeout) {
                requestTimeoutMillis = 180_000
                connectTimeoutMillis = 20_000
                socketTimeoutMillis = 180_000
            }
        }
    }
}

data class AiConfig(
    val apiKey: String,
    val model: String,
    val baseUrl: String,
)

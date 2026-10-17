package com.englishtalk.app.ai

import com.englishtalk.app.data.Scenario
import com.englishtalk.app.data.UserProfile
import com.englishtalk.app.data.VocabWord
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

@Serializable
data class TurnFeedback(
    @SerialName("has_issue") val hasIssue: Boolean = false,
    val corrected: String = "",
    val explanation: String = "",
    @SerialName("more_natural") val moreNatural: String = "",
)

@Serializable
data class TutorTurn(
    val reply: String,
    @SerialName("reply_translation") val replyTranslation: String = "",
    val feedback: TurnFeedback = TurnFeedback(),
    val suggestions: List<String> = emptyList(),
)

@Serializable
data class PronunciationTip(
    val word: String,
    val ipa: String = "",
    val issue: String = "",
    @SerialName("how_to") val howTo: String = "",
)

@Serializable
data class PronunciationAnalysis(
    @SerialName("sentence_ipa") val sentenceIpa: String = "",
    val summary: String = "",
    val tips: List<PronunciationTip> = emptyList(),
)

@Serializable
data class SessionSummary(
    val strengths: List<String> = emptyList(),
    val improvements: List<String> = emptyList(),
    @SerialName("useful_phrases") val usefulPhrases: List<String> = emptyList(),
    val score: Int = 0,
)

@Serializable
private data class WordList(val words: List<VocabWord>)

/**
 * All prompts and response schemas for the AI tutor.
 * Every call uses structured outputs so the UI can render feedback reliably.
 */
class TutorService(private val client: ClaudeClient = ClaudeClient()) {

    private val json = Json { ignoreUnknownKeys = true }

    private fun tutorPersona(profile: UserProfile) = """
        You are a friendly, encouraging native English speaking tutor inside a mobile app that helps
        learners speak English like a native. The learner's native language is ${profile.nativeLanguage};
        their CEFR level is ${profile.level}. They prefer a ${profile.accent.name} English accent and vocabulary.
        Explanations meant for the learner must be written in ${profile.nativeLanguage}, short and concrete.
        English you produce must sound natural and idiomatic for a native ${profile.accent.name} speaker,
        adjusted to the learner's level.
    """.trimIndent()

    // ---------- Role-play conversation ----------

    fun conversationSystemPrompt(profile: UserProfile, scenario: Scenario, customTopic: String?) = """
        ${tutorPersona(profile)}

        ROLE-PLAY
        Scenario: ${scenario.title} — ${scenario.description}
        ${if (!customTopic.isNullOrBlank()) "Topic chosen by the learner: $customTopic" else ""}
        Your role: ${scenario.aiRole}
        The learner's role: ${scenario.userRole}
        Goal for the learner: ${scenario.goal}

        Rules:
        - Stay in character. Reply in spoken-style English, 1-3 short sentences, and usually end with a
          question or prompt that keeps the conversation going.
        - The learner's messages come from speech recognition, so ignore missing punctuation and capitalization.
        - In "feedback", evaluate ONLY the learner's latest message: grammar, word choice, and naturalness.
          Set has_issue=false when it is already correct and natural; then corrected and explanation can be empty,
          but still offer a more_natural alternative if a native speaker would phrase it differently.
        - "suggestions": 2-3 short replies the learner could say next, at their level.
        - "reply_translation": translation of your reply into ${profile.nativeLanguage}.
        - When the learner's message is exactly "[START]", open the scene in character; feedback.has_issue=false.
    """.trimIndent()

    suspend fun conversationTurn(
        config: AiConfig,
        systemPrompt: String,
        history: List<ApiMessage>,
    ): Pair<TutorTurn, ApiMessage> {
        val result = client.createMessage(config, systemPrompt, history, TURN_SCHEMA, effort = "low")
        return json.decodeFromString<TutorTurn>(result.text) to result.assistantMessage
    }

    suspend fun summarizeSession(
        config: AiConfig,
        systemPrompt: String,
        history: List<ApiMessage>,
    ): SessionSummary {
        val messages = history + ApiMessage.user(
            "[END OF ROLE-PLAY] Step out of character. Evaluate the learner's English across the whole " +
                "conversation. Write strengths and improvements in the learner's native language; " +
                "useful_phrases are natural English phrases they should learn from this scenario; " +
                "score is 0-100 for overall speaking performance."
        )
        val result = client.createMessage(config, systemPrompt, messages, SUMMARY_SCHEMA, effort = "medium")
        return json.decodeFromString(result.text)
    }

    // ---------- Pronunciation ----------

    suspend fun analyzePronunciation(
        config: AiConfig,
        profile: UserProfile,
        target: String,
        recognized: String,
        missedWords: List<String>,
    ): PronunciationAnalysis {
        val prompt = """
            The learner tried to read this sentence aloud: "$target"
            The speech recognizer heard: "$recognized"
            Words that did not match: ${missedWords.joinToString().ifBlank { "(none)" }}

            Give the IPA (${profile.accent.name} accent) for the whole target sentence, a one-sentence summary,
            and up to 4 tips focused on the mismatched words — explain the likely mispronounced sound and
            how to place the tongue/lips, considering typical difficulties of ${profile.nativeLanguage} speakers
            (final consonants, th, r/l, v/w, s/sh, word stress, linking). If everything matched, give tips
            on intonation, linking and rhythm to sound more native.
        """.trimIndent()
        val result = client.createMessage(
            config, tutorPersona(profile), listOf(ApiMessage.user(prompt)), PRONUNCIATION_SCHEMA, effort = "low"
        )
        return json.decodeFromString(result.text)
    }

    // ---------- Vocabulary ----------

    suspend fun generateVocabulary(config: AiConfig, profile: UserProfile, topic: String): List<VocabWord> {
        val prompt = """
            Create 10 useful English words or short phrases about the topic "$topic" that native speakers
            really use in daily conversation, suitable for level ${profile.level}.
            For each: the word, IPA (${profile.accent.name}), the meaning in ${profile.nativeLanguage},
            and one natural example sentence.
        """.trimIndent()
        val result = client.createMessage(
            config, tutorPersona(profile), listOf(ApiMessage.user(prompt)), VOCAB_SCHEMA, effort = "low"
        )
        return json.decodeFromString<WordList>(result.text).words
    }

    companion object {
        private fun obj(build: JsonObjectBuilder.() -> Unit) = buildJsonObject {
            put("type", "object")
            build()
            put("additionalProperties", false)
        }

        private fun str() = buildJsonObject { put("type", "string") }
        private fun strArray() = buildJsonObject {
            put("type", "array")
            put("items", str())
        }

        private fun required(vararg names: String) = buildJsonArray { names.forEach { add(JsonPrimitive(it)) } }

        val TURN_SCHEMA: JsonObject = obj {
            putJsonObject("properties") {
                put("reply", str())
                put("reply_translation", str())
                put("feedback", obj {
                    putJsonObject("properties") {
                        putJsonObject("has_issue") { put("type", "boolean") }
                        put("corrected", str())
                        put("explanation", str())
                        put("more_natural", str())
                    }
                    put("required", required("has_issue", "corrected", "explanation", "more_natural"))
                })
                put("suggestions", strArray())
            }
            put("required", required("reply", "reply_translation", "feedback", "suggestions"))
        }

        val SUMMARY_SCHEMA: JsonObject = obj {
            putJsonObject("properties") {
                put("strengths", strArray())
                put("improvements", strArray())
                put("useful_phrases", strArray())
                putJsonObject("score") { put("type", "integer") }
            }
            put("required", required("strengths", "improvements", "useful_phrases", "score"))
        }

        val PRONUNCIATION_SCHEMA: JsonObject = obj {
            putJsonObject("properties") {
                put("sentence_ipa", str())
                put("summary", str())
                putJsonObject("tips") {
                    put("type", "array")
                    put("items", obj {
                        putJsonObject("properties") {
                            put("word", str())
                            put("ipa", str())
                            put("issue", str())
                            put("how_to", str())
                        }
                        put("required", required("word", "ipa", "issue", "how_to"))
                    })
                }
            }
            put("required", required("sentence_ipa", "summary", "tips"))
        }

        val VOCAB_SCHEMA: JsonObject =
            obj {
                putJsonObject("properties") {
                    putJsonObject("words") {
                        put("type", "array")
                        put("items", obj {
                            putJsonObject("properties") {
                                put("word", str())
                                put("ipa", str())
                                put("meaning", str())
                                put("example", str())
                            }
                            put("required", required("word", "ipa", "meaning", "example"))
                        })
                    }
                }
                put("required", required("words"))
            }
    }
}

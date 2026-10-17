package com.englishtalk.app.data

import com.englishtalk.app.ai.AiConfig
import com.englishtalk.app.ai.ClaudeClient
import com.englishtalk.app.platform.Accent
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import kotlin.time.Clock

data class AppPreferences(
    val apiKey: String = "",
    val model: String = ClaudeClient.DEFAULT_MODEL,
    val baseUrl: String = ClaudeClient.DEFAULT_BASE_URL,
    val accent: Accent = Accent.US,
    val level: CefrLevel = CefrLevel.B1,
    val speechRate: Float = 1.0f,
    val autoPlayReplies: Boolean = true,
    val showTranslation: Boolean = true,
    val dailyGoalXp: Int = 50,
) {
    val aiConfig get() = AiConfig(apiKey = apiKey.trim(), model = model, baseUrl = baseUrl.ifBlank { ClaudeClient.DEFAULT_BASE_URL })
    val profile get() = UserProfile(level = level, accent = accent)
}

data class Progress(
    val totalXp: Int = 0,
    val todayXp: Int = 0,
    val streakDays: Int = 0,
    val conversations: Int = 0,
    val spokenTurns: Int = 0,
    val pronunciationAttempts: Int = 0,
    val pronunciationScoreSum: Int = 0,
    val wordsReviewed: Int = 0,
) {
    val averagePronunciation: Int
        get() = if (pronunciationAttempts == 0) 0 else pronunciationScoreSum / pronunciationAttempts
}

/** Local persistence for preferences, progress and AI-generated decks. */
class AppRepository(private val settings: Settings = Settings()) {

    private val json = Json { ignoreUnknownKeys = true }

    private val _preferences = MutableStateFlow(loadPreferences())
    val preferences: StateFlow<AppPreferences> = _preferences.asStateFlow()

    private val _progress = MutableStateFlow(loadProgress())
    val progress: StateFlow<Progress> = _progress.asStateFlow()

    private val _customDecks = MutableStateFlow(loadDecks())
    val customDecks: StateFlow<List<VocabDeck>> = _customDecks.asStateFlow()

    // ---------- Preferences ----------

    fun updatePreferences(transform: (AppPreferences) -> AppPreferences) {
        val p = transform(_preferences.value)
        _preferences.value = p
        settings.putString(K_API_KEY, p.apiKey)
        settings.putString(K_MODEL, p.model)
        settings.putString(K_BASE_URL, p.baseUrl)
        settings.putString(K_ACCENT, p.accent.name)
        settings.putString(K_LEVEL, p.level.name)
        settings.putFloat(K_RATE, p.speechRate)
        settings.putBoolean(K_AUTOPLAY, p.autoPlayReplies)
        settings.putBoolean(K_TRANSLATION, p.showTranslation)
        settings.putInt(K_GOAL, p.dailyGoalXp)
    }

    private fun loadPreferences() = AppPreferences(
        apiKey = settings.getString(K_API_KEY, ""),
        model = settings.getString(K_MODEL, ClaudeClient.DEFAULT_MODEL),
        baseUrl = settings.getString(K_BASE_URL, ClaudeClient.DEFAULT_BASE_URL),
        accent = enumOrDefault(settings.getString(K_ACCENT, ""), Accent.US),
        level = enumOrDefault(settings.getString(K_LEVEL, ""), CefrLevel.B1),
        speechRate = settings.getFloat(K_RATE, 1.0f),
        autoPlayReplies = settings.getBoolean(K_AUTOPLAY, true),
        showTranslation = settings.getBoolean(K_TRANSLATION, true),
        dailyGoalXp = settings.getInt(K_GOAL, 50),
    )

    // ---------- Progress ----------

    fun recordConversationStarted() = addProgress(xp = 5) { it.copy(conversations = it.conversations + 1) }

    fun recordSpokenTurn() = addProgress(xp = 10) { it.copy(spokenTurns = it.spokenTurns + 1) }

    fun recordPronunciation(score: Int) = addProgress(xp = 2 + score / 10) {
        it.copy(
            pronunciationAttempts = it.pronunciationAttempts + 1,
            pronunciationScoreSum = it.pronunciationScoreSum + score,
        )
    }

    fun recordWordReviewed() = addProgress(xp = 2) { it.copy(wordsReviewed = it.wordsReviewed + 1) }

    /** Refreshes today's XP / streak when the app opens on a new day. */
    fun refreshDay() = addProgress(xp = 0) { it }

    private fun addProgress(xp: Int, transform: (Progress) -> Progress) {
        val today = today()
        val lastActive = settings.getStringOrNull(K_LAST_DAY)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        _progress.update { current ->
            var p = transform(current)
            if (lastActive != today) p = p.copy(todayXp = 0)
            if (lastActive != null && lastActive.toEpochDays() < today.toEpochDays() - 1) p = p.copy(streakDays = 0)
            if (xp > 0) {
                // The first XP of the day extends the streak.
                if (lastActive != today) p = p.copy(streakDays = p.streakDays + 1)
                p = p.copy(totalXp = p.totalXp + xp, todayXp = p.todayXp + xp)
                settings.putString(K_LAST_DAY, today.toString())
            }
            p
        }
        saveProgress(_progress.value)
    }

    private fun today(): LocalDate =
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    private fun saveProgress(p: Progress) {
        settings.putInt(K_TOTAL_XP, p.totalXp)
        settings.putInt(K_TODAY_XP, p.todayXp)
        settings.putInt(K_STREAK, p.streakDays)
        settings.putInt(K_CONVERSATIONS, p.conversations)
        settings.putInt(K_TURNS, p.spokenTurns)
        settings.putInt(K_PRON_ATTEMPTS, p.pronunciationAttempts)
        settings.putInt(K_PRON_SUM, p.pronunciationScoreSum)
        settings.putInt(K_WORDS, p.wordsReviewed)
    }

    private fun loadProgress() = Progress(
        totalXp = settings.getInt(K_TOTAL_XP, 0),
        todayXp = settings.getInt(K_TODAY_XP, 0),
        streakDays = settings.getInt(K_STREAK, 0),
        conversations = settings.getInt(K_CONVERSATIONS, 0),
        spokenTurns = settings.getInt(K_TURNS, 0),
        pronunciationAttempts = settings.getInt(K_PRON_ATTEMPTS, 0),
        pronunciationScoreSum = settings.getInt(K_PRON_SUM, 0),
        wordsReviewed = settings.getInt(K_WORDS, 0),
    )

    // ---------- AI-generated vocabulary decks ----------

    fun addDeck(deck: VocabDeck) {
        _customDecks.update { listOf(deck) + it }
        settings.putString(K_DECKS, json.encodeToString(_customDecks.value))
    }

    fun deleteDeck(id: String) {
        _customDecks.update { decks -> decks.filterNot { it.id == id } }
        settings.putString(K_DECKS, json.encodeToString(_customDecks.value))
    }

    private fun loadDecks(): List<VocabDeck> =
        settings.getStringOrNull(K_DECKS)
            ?.let { runCatching { json.decodeFromString<List<VocabDeck>>(it) }.getOrNull() }
            .orEmpty()

    private inline fun <reified E : Enum<E>> enumOrDefault(name: String, default: E): E =
        enumValues<E>().firstOrNull { it.name == name } ?: default

    private companion object {
        const val K_API_KEY = "api_key"
        const val K_MODEL = "model"
        const val K_BASE_URL = "base_url"
        const val K_ACCENT = "accent"
        const val K_LEVEL = "level"
        const val K_RATE = "speech_rate"
        const val K_AUTOPLAY = "autoplay"
        const val K_TRANSLATION = "translation"
        const val K_GOAL = "daily_goal"
        const val K_LAST_DAY = "last_active_day"
        const val K_TOTAL_XP = "total_xp"
        const val K_TODAY_XP = "today_xp"
        const val K_STREAK = "streak"
        const val K_CONVERSATIONS = "conversations"
        const val K_TURNS = "turns"
        const val K_PRON_ATTEMPTS = "pron_attempts"
        const val K_PRON_SUM = "pron_sum"
        const val K_WORDS = "words_reviewed"
        const val K_DECKS = "custom_decks"
    }
}

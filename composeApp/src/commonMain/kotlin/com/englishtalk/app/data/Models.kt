package com.englishtalk.app.data

import com.englishtalk.app.platform.Accent
import kotlinx.serialization.Serializable

data class Scenario(
    val id: String,
    val emoji: String,
    val title: String,
    val titleVi: String,
    val description: String,
    val aiRole: String,
    val userRole: String,
    val goal: String,
    val level: String,
    /** Free-talk style scenarios let the learner type their own topic. */
    val customTopic: Boolean = false,
)

data class PracticeSentence(val text: String, val meaning: String)

data class SoundLesson(
    val id: String,
    val title: String,
    val subtitle: String,
    val tip: String,
    val sentences: List<PracticeSentence>,
)

@Serializable
data class VocabWord(
    val word: String,
    val ipa: String = "",
    val meaning: String = "",
    val example: String = "",
)

@Serializable
data class VocabDeck(
    val id: String,
    val emoji: String,
    val title: String,
    val words: List<VocabWord>,
    val aiGenerated: Boolean = false,
)

enum class CefrLevel(val label: String) {
    A1("A1 – Mới bắt đầu"),
    A2("A2 – Sơ cấp"),
    B1("B1 – Trung cấp"),
    B2("B2 – Trung cao cấp"),
    C1("C1 – Cao cấp"),
    C2("C2 – Thành thạo"),
}

data class UserProfile(
    val level: CefrLevel = CefrLevel.B1,
    val accent: Accent = Accent.US,
    val nativeLanguage: String = "Vietnamese",
)

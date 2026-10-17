package com.englishtalk.app

import com.englishtalk.app.scoring.PronunciationScorer
import com.englishtalk.app.scoring.WordStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PronunciationScorerTest {

    @Test
    fun perfectMatchScores100IgnoringPunctuationAndCase() {
        val r = PronunciationScorer.score("I think this is the third Thursday.", "i think this is the third thursday")
        assertEquals(100, r.score)
        assertTrue(r.words.all { it.status == WordStatus.CORRECT })
    }

    @Test
    fun nearMissIsCloseAndMissingWordIsMissing() {
        val r = PronunciationScorer.score("She likes cats and dogs.", "she like cat dogs")
        val statuses = r.words.map { it.status }
        assertEquals(
            listOf(WordStatus.CORRECT, WordStatus.CLOSE, WordStatus.CLOSE, WordStatus.MISSING, WordStatus.CORRECT),
            statuses,
        )
        assertTrue(r.score in 40..80)
        assertEquals(listOf("likes", "cats", "and"), r.problemWords)
    }

    @Test
    fun emptyRecognitionScoresZero() {
        val r = PronunciationScorer.score("Hello world", "")
        assertEquals(0, r.score)
        assertTrue(r.words.all { it.status == WordStatus.MISSING })
    }

    @Test
    fun digitsMatchSpelledNumbers() {
        val r = PronunciationScorer.score("Thank you for those three things.", "thank you for those 3 things")
        assertEquals(100, r.score)
    }
}

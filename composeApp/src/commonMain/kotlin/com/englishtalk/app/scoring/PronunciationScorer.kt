package com.englishtalk.app.scoring

enum class WordStatus { CORRECT, CLOSE, WRONG, MISSING }

data class ScoredWord(val display: String, val status: WordStatus, val heard: String? = null)

data class PronunciationResult(
    val score: Int,
    val words: List<ScoredWord>,
    val recognized: String,
) {
    val problemWords: List<String>
        get() = words.filter { it.status != WordStatus.CORRECT }.map { it.display.normalizeWord() }
}

/**
 * Compares what the learner was asked to say with what speech recognition heard.
 * Uses a word-level alignment (edit distance) and a character similarity per word,
 * so a near miss ("tink" for "think") counts as CLOSE rather than WRONG.
 *
 * This is an approximation: a recognizer that understood the word implies it was
 * pronounced intelligibly. For phoneme-level scoring, plug in a dedicated
 * pronunciation-assessment service.
 */
object PronunciationScorer {

    fun score(target: String, recognized: String): PronunciationResult {
        val targetTokens = target.split(Regex("\\s+")).filter { it.isNotBlank() }
        val targetNorm = targetTokens.map { it.normalizeWord() }
        val heardNorm = recognized.split(Regex("\\s+")).map { it.normalizeWord() }.filter { it.isNotEmpty() }

        val n = targetNorm.size
        val m = heardNorm.size
        // dp[i][j] = cost to align first i target words with first j heard words
        val dp = Array(n + 1) { IntArray(m + 1) }
        for (i in 0..n) dp[i][0] = i * 2
        for (j in 0..m) dp[0][j] = j
        for (i in 1..n) for (j in 1..m) {
            val sub = dp[i - 1][j - 1] + substitutionCost(targetNorm[i - 1], heardNorm[j - 1])
            val del = dp[i - 1][j] + 2 // target word not said
            val ins = dp[i][j - 1] + 1 // extra word heard
            dp[i][j] = minOf(sub, del, ins)
        }

        // Backtrack to label each target word.
        val result = arrayOfNulls<ScoredWord>(n)
        var i = n
        var j = m
        while (i > 0) {
            if (j > 0 && dp[i][j] == dp[i - 1][j - 1] + substitutionCost(targetNorm[i - 1], heardNorm[j - 1])) {
                val sim = similarity(targetNorm[i - 1], heardNorm[j - 1])
                val status = when {
                    sim >= 0.999 -> WordStatus.CORRECT
                    sim >= 0.6 -> WordStatus.CLOSE
                    else -> WordStatus.WRONG
                }
                result[i - 1] = ScoredWord(targetTokens[i - 1], status, heardNorm[j - 1])
                i--; j--
            } else if (j > 0 && dp[i][j] == dp[i][j - 1] + 1) {
                j--
            } else {
                result[i - 1] = ScoredWord(targetTokens[i - 1], WordStatus.MISSING)
                i--
            }
        }

        val words = result.map { it!! }
        val points = words.sumOf {
            when (it.status) {
                WordStatus.CORRECT -> 1.0
                WordStatus.CLOSE -> 0.5
                else -> 0.0
            }
        }
        val extraPenalty = (m - words.count { it.heard != null }).coerceAtLeast(0) * 0.25
        val score = if (n == 0) 0 else (((points - extraPenalty) / n) * 100).coerceIn(0.0, 100.0).toInt()
        return PronunciationResult(score, words, recognized)
    }

    private fun substitutionCost(a: String, b: String): Int {
        val sim = similarity(a, b)
        return when {
            sim >= 0.999 -> 0
            sim >= 0.6 -> 1
            else -> 3
        }
    }

    /** Normalized Levenshtein similarity in [0, 1]. */
    fun similarity(a: String, b: String): Double {
        if (a == b) return 1.0
        if (a.isEmpty() || b.isEmpty()) return 0.0
        val prev = IntArray(b.length + 1) { it }
        val cur = IntArray(b.length + 1)
        for (i in 1..a.length) {
            cur[0] = i
            for (j in 1..b.length) {
                val c = if (a[i - 1] == b[j - 1]) 0 else 1
                cur[j] = minOf(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + c)
            }
            cur.copyInto(prev)
        }
        return 1.0 - prev[b.length].toDouble() / maxOf(a.length, b.length)
    }
}

private val numberWords = mapOf(
    "0" to "zero", "1" to "one", "2" to "two", "3" to "three", "4" to "four", "5" to "five",
    "6" to "six", "7" to "seven", "8" to "eight", "9" to "nine", "10" to "ten",
    "3rd" to "third", "1st" to "first", "2nd" to "second",
)

internal fun String.normalizeWord(): String {
    val w = lowercase()
        .replace('’', '\'')
        .filter { it.isLetterOrDigit() || it == '\'' }
        .trim('\'')
    return numberWords[w] ?: w
}

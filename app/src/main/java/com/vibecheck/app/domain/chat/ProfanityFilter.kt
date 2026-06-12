package com.vibecheck.app.domain.chat

/**
 * Lightweight client-side profanity filter for anonymous chat (SOW: "chat uses
 * profanity filter + report button"). Catches a curated base list plus common
 * obfuscations (leetspeak, repeated/spacer characters). It is deliberately
 * conservative — the report flow and server-side moderation are the backstop.
 *
 * Shared by the chat UI and the chat repository so what's shown and what's
 * sent agree.
 */
object ProfanityFilter {

    // Base lemmas, lower-case. Kept short and uncontroversial; expand server-side.
    private val blocked = setOf(
        "fuck", "shit", "bitch", "cunt", "asshole", "bastard", "dick",
        "piss", "slut", "whore", "fag", "faggot", "nigger", "nigga",
        "retard", "rape", "kys",
    )

    // Leetspeak / common substitutions normalised before matching.
    // '!' is intentionally excluded — it doubles as sentence punctuation, so
    // treating it as a letter would swallow trailing "!" into the word.
    private val substitutions = mapOf(
        '0' to 'o', '1' to 'i', '3' to 'e', '4' to 'a',
        '5' to 's', '7' to 't', '@' to 'a', '$' to 's',
    )

    private val wordRegex = Regex("[\\p{L}\\p{N}@$*]+")

    /** True when [text] contains no blocked term. Empty/blank counts as acceptable. */
    fun isAcceptable(text: String): Boolean =
        wordRegex.findAll(text).none { isBlocked(it.value) }

    /**
     * Returns [text] with any blocked word masked as asterisks of the same
     * length, preserving the original spacing and punctuation.
     */
    fun clean(text: String): String {
        if (text.isBlank()) return text
        return wordRegex.replace(text) { match ->
            if (isBlocked(match.value)) "*".repeat(match.value.length) else match.value
        }
    }

    /**
     * A word is blocked if its de-leeted form — or that form with repeated
     * letters collapsed (catching "fuuuck") — is in the list. Checking both
     * means legitimate double letters ("asshole") still match directly.
     */
    private fun isBlocked(word: String): Boolean {
        val deLeet = normalize(word)
        if (deLeet.isEmpty()) return false
        return deLeet in blocked || collapseRepeats(deLeet) in blocked
    }

    /** Lower-case, de-leet, letters only. */
    private fun normalize(word: String): String {
        val deLeet = buildString {
            for (ch in word.lowercase()) append(substitutions[ch] ?: ch)
        }
        return deLeet.filter { it.isLetter() }
    }

    private fun collapseRepeats(s: String): String {
        if (s.isEmpty()) return s
        val sb = StringBuilder()
        var prev = ' '
        for (ch in s) {
            if (ch != prev) sb.append(ch)
            prev = ch
        }
        return sb.toString()
    }
}

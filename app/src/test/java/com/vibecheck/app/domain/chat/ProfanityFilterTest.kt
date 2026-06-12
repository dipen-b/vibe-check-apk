package com.vibecheck.app.domain.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfanityFilterTest {

    @Test
    fun cleanTextIsAcceptable() {
        assertTrue(ProfanityFilter.isAcceptable("hey, rough day here too. you ok?"))
        assertTrue(ProfanityFilter.isAcceptable(""))
        assertTrue(ProfanityFilter.isAcceptable("   "))
    }

    @Test
    fun plainProfanityIsRejected() {
        assertFalse(ProfanityFilter.isAcceptable("this is shit"))
        assertFalse(ProfanityFilter.isAcceptable("FUCK this"))
    }

    @Test
    fun leetspeakIsCaught() {
        assertFalse(ProfanityFilter.isAcceptable("sh1t"))
        assertFalse(ProfanityFilter.isAcceptable("\$hit")) // $->s
        assertFalse(ProfanityFilter.isAcceptable("b1tch")) // 1->i
    }

    @Test
    fun stretchedLettersAreCaught() {
        assertFalse(ProfanityFilter.isAcceptable("fuuuuck"))
        assertFalse(ProfanityFilter.isAcceptable("shiiit"))
    }

    @Test
    fun wordsWithLegitimateDoubleLettersStillMatch() {
        assertFalse(ProfanityFilter.isAcceptable("you asshole"))
    }

    @Test
    fun substringsOfCleanWordsAreNotFlagged() {
        // "assassin", "class", "scunthorpe"-style false positives must pass.
        assertTrue(ProfanityFilter.isAcceptable("the class was great"))
        assertTrue(ProfanityFilter.isAcceptable("assassin's creed"))
        assertTrue(ProfanityFilter.isAcceptable("I live in Scunthorpe"))
    }

    @Test
    fun cleanMasksWithSameLength() {
        assertEquals("this is ****", ProfanityFilter.clean("this is shit"))
        assertEquals("you *******", ProfanityFilter.clean("you asshole"))
    }

    @Test
    fun cleanPreservesPunctuationAndSpacing() {
        assertEquals("hey, you ok?", ProfanityFilter.clean("hey, you ok?"))
        assertEquals("what the ****!", ProfanityFilter.clean("what the fuck!"))
    }
}

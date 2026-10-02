package com.example.sanathanatattva

import androidx.compose.ui.text.SpanStyle
import com.example.sanathanatattva.data.model.DrawSchedule
import com.example.sanathanatattva.ui.screens.luckydraw.*
import org.junit.Assert.*
import org.junit.Test

class HowItWorksCopyTest {
    private val strong = SpanStyle()

    @Test fun `draw day and time read naturally in English`() {
        assertEquals("12th", ordinalDay(12))
        assertEquals("1st", ordinalDay(1))
        assertEquals("22nd", ordinalDay(22))
        assertEquals("13th", ordinalDay(13))
        assertEquals("5pm", clockLabel("17:00"))
        assertEquals("4.30pm", clockLabel("16:30"))
        assertEquals("12pm", clockLabel("12:00"))
        assertEquals("not-a-time", clockLabel("not-a-time"))
    }

    @Test fun `Kannada deadline names the part of the day and carries the by`() {
        assertEquals("ತಿಂಗಳ 12ನೇ ತಾರೀಖು ಸಂಜೆ 5 ಗಂಟೆಯೊಳಗೆ", kannadaDeadline(DrawSchedule(12, "17:00")))
        assertEquals("ತಿಂಗಳ 15ನೇ ತಾರೀಖು ಸಂಜೆ 4:30ರೊಳಗೆ", kannadaDeadline(DrawSchedule(15, "16:30")))
        assertEquals("ತಿಂಗಳ 1ನೇ ತಾರೀಖು ಬೆಳಿಗ್ಗೆ 10 ಗಂಟೆಯೊಳಗೆ", kannadaDeadline(DrawSchedule(1, "10:00")))
        assertEquals("ತಿಂಗಳ 5ನೇ ತಾರೀಖು ಮಧ್ಯಾಹ್ನ 12 ಗಂಟೆಯೊಳಗೆ", kannadaDeadline(DrawSchedule(5, "12:00")))
        assertEquals("ತಿಂಗಳ 5ನೇ ತಾರೀಖು ರಾತ್ರಿ 9 ಗಂಟೆಯೊಳಗೆ", kannadaDeadline(DrawSchedule(5, "21:00")))
    }

    @Test fun `both languages give seven rules with the real fee and date`() {
        val schedule = DrawSchedule(12, "17:00")
        val english = howItWorksCopy(RulesLanguage.English, 100.0, schedule, strong)
        val kannada = howItWorksCopy(RulesLanguage.Kannada, 100.0, schedule, strong)
        assertEquals(7, english.rules.size)
        assertEquals(7, kannada.rules.size)
        assertTrue(english.rules[0].text.contains("the ₹100 once"))
        assertTrue(english.rules[1].text.contains("before the 12th, 5pm"))
        assertTrue(kannada.rules[0].text.contains("₹100 ಅನ್ನು"))
        assertTrue(kannada.rules[1].text.startsWith("ತಿಂಗಳ 12ನೇ ತಾರೀಖು ಸಂಜೆ 5 ಗಂಟೆಯೊಳಗೆ"))
        assertEquals("month 9", english.ladderLabel)
        assertEquals("9ನೇ ತಿಂಗಳು", kannada.ladderLabel)
    }

    @Test fun `unknown fee and schedule are spoken of generally, never invented`() {
        val english = howItWorksCopy(RulesLanguage.English, null, null, strong)
        val kannada = howItWorksCopy(RulesLanguage.Kannada, null, null, strong)
        assertTrue(english.rules[0].text.contains("the joining fee"))
        assertTrue(english.rules[1].text.contains("before each month's draw"))
        assertTrue(kannada.rules[0].text.contains("ಸೇರ್ಪಡೆ ಶುಲ್ಕವನ್ನು"))
        assertTrue(kannada.rules[1].text.startsWith("ಪ್ರತಿ ತಿಂಗಳ ಡ್ರಾಗೆ ಮುನ್ನ"))
        assertFalse(english.rules.any { it.text.contains("₹") })
        assertFalse(kannada.rules.any { it.text.contains("₹") })
    }
}

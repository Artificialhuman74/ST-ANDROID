package com.example.sanathanatattva

import com.example.sanathanatattva.util.isMobile
import com.example.sanathanatattva.util.mobileDigits
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneTest {
    @Test fun anyShapeOfNumberBecomesTheTenDigits() {
        assertEquals("9876543210", mobileDigits("+91 98765 43210"))
        assertEquals("9876543210", mobileDigits("919876543210"))
        assertEquals("9876543210", mobileDigits("098765 43210"))
        assertEquals("9876543210", mobileDigits("+91-98765-43210"))
        assertEquals("98765", mobileDigits("98765"))
    }

    @Test fun anExtraDigitOnAFullNumberIsIgnored() {
        assertEquals("9123456789", mobileDigits("91234567890"))
        assertEquals("9876543210", mobileDigits("98765432101"))
    }

    @Test fun onlyRealMobilesPass() {
        assertTrue(isMobile("9876543210"))
        assertFalse(isMobile("1234567890"))
        assertFalse(isMobile("98765"))
    }
}

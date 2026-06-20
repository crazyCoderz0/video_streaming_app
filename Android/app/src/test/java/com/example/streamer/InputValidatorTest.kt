package com.example.streamer

import com.example.streamer.utils.InputValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InputValidatorTest {
    @Test
    fun validTargetParses() {
        val result = InputValidator.parseTarget("192.168.1.10", "9000")
        assertTrue(result.isSuccess)
        assertEquals(9000, result.getOrThrow().port)
    }

    @Test
    fun invalidPortFails() {
        assertTrue(InputValidator.parseTarget("192.168.1.10", "80").isFailure)
    }
}


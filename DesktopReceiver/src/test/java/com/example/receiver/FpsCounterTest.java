package com.example.receiver;

import com.example.receiver.utils.FpsCounter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class FpsCounterTest {
    @Test
    void tickIsSafeForRepeatedFrames() {
        FpsCounter counter = new FpsCounter();
        assertDoesNotThrow(() -> {
            for (int i = 0; i < 120; i++) {
                counter.tick();
            }
        });
    }
}


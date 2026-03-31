package com.sift.stats;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShannonEntropyTest {

    @Test
    void uniformDistributionOverFourSymbolsHasTwoBitsOfEntropy() {
        assertEquals(2.0, ShannonEntropy.bitsPerCharacter("aabbccdd"), 0.0001);
    }

    @Test
    void singleRepeatedCharacterHasZeroEntropy() {
        assertEquals(0.0, ShannonEntropy.bitsPerCharacter("aaaaaa"), 0.0001);
    }

    @Test
    void emptyStringHasZeroEntropy() {
        assertEquals(0.0, ShannonEntropy.bitsPerCharacter(""), 0.0001);
    }

    @Test
    void randomHighEntropyStringExceedsTypicalHostnameEntropy() {
        double randomLabelEntropy = ShannonEntropy.bitsPerCharacter("q7x9zv2mkw4pldr8");
        double hostnameEntropy = ShannonEntropy.bitsPerCharacter("wwwexamplecom");

        assertTrue(randomLabelEntropy > hostnameEntropy);
    }
}
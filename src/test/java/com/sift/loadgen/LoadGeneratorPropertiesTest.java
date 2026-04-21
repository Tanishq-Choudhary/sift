package com.sift.loadgen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LoadGeneratorPropertiesTest {

    @Test
    void testValidMixedProfile() {
        LoadGeneratorProperties props = new LoadGeneratorProperties();
        props.setProfile(LoadProfile.MIXED);
        props.setNormalPercent(70);
        props.setSynFloodPercent(20);
        props.setPortScanPercent(7);
        props.setDnsTunnelPercent(3);

        assertDoesNotThrow(props::validate);
    }

    @Test
    void testInvalidMixedProfilePercentages() {
        LoadGeneratorProperties props = new LoadGeneratorProperties();
        props.setProfile(LoadProfile.MIXED);
        props.setNormalPercent(50);
        props.setSynFloodPercent(20);
        props.setPortScanPercent(10);
        props.setDnsTunnelPercent(10); // Sum = 90

        assertThrows(IllegalArgumentException.class, props::validate);
    }

    @Test
    void testNonMixedProfileIgnoresPercentageSum() {
        LoadGeneratorProperties props = new LoadGeneratorProperties();
        props.setProfile(LoadProfile.SYN_FLOOD);
        props.setNormalPercent(0);

        assertDoesNotThrow(props::validate);
    }

    @Test
    void testInvalidTotalPackets() {
        LoadGeneratorProperties props = new LoadGeneratorProperties();
        props.setTotalPackets(-10);

        assertThrows(IllegalArgumentException.class, props::validate);
    }
}
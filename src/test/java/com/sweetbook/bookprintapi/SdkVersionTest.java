package com.sweetbook.bookprintapi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SdkVersionTest {

    @Test
    void versionConstantIsPresent() {
        assertNotNull(SdkVersion.VERSION);
        assertTrue(SdkVersion.VERSION.startsWith("0."), "초기 SDK는 0.x 시리즈");
    }
}

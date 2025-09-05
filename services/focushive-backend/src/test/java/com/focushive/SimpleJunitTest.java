package com.focushive;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Simple non-Spring test to verify test infrastructure is working.
 */
class SimpleJunitTest {

    @Test
    void simpleTest() {
        // Basic test to verify JUnit and AssertJ are working
        assertThat("hello").isEqualTo("hello");
        assertThat(2 + 2).isEqualTo(4);
    }
    
    @Test
    void anotherTest() {
        // Another simple test
        assertThat(true).isTrue();
        assertThat(false).isFalse();
    }
}
package com.focushive.buddy.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WorkHoursMapConverterTest {

    private WorkHoursMapConverter converter;

    @BeforeEach
    void setUp() {
        converter = new WorkHoursMapConverter();
    }

    @Test
    void shouldConvertMapToJsonString() {
        // Given
        Map<String, BuddyPreferences.WorkHours> workHours = Map.of(
                "MONDAY", new BuddyPreferences.WorkHours(9, 17),
                "TUESDAY", new BuddyPreferences.WorkHours(10, 18)
        );

        // When
        String json = converter.convertToDatabaseColumn(workHours);

        // Then
        assertThat(json).isNotNull();
        assertThat(json).contains("MONDAY");
        assertThat(json).contains("TUESDAY");
        assertThat(json).contains("\"startHour\":9");
        assertThat(json).contains("\"endHour\":17");
        assertThat(json).contains("\"startHour\":10");
        assertThat(json).contains("\"endHour\":18");
    }

    @Test
    void shouldConvertJsonStringToMap() {
        // Given
        String json = "{\"MONDAY\":{\"startHour\":9,\"endHour\":17},\"TUESDAY\":{\"startHour\":10,\"endHour\":18}}";

        // When
        Map<String, BuddyPreferences.WorkHours> workHours = converter.convertToEntityAttribute(json);

        // Then
        assertThat(workHours).hasSize(2);
        assertThat(workHours.get("MONDAY")).isEqualTo(new BuddyPreferences.WorkHours(9, 17));
        assertThat(workHours.get("TUESDAY")).isEqualTo(new BuddyPreferences.WorkHours(10, 18));
    }

    @Test
    void shouldHandleNullValues() {
        // When
        String json = converter.convertToDatabaseColumn(null);
        Map<String, BuddyPreferences.WorkHours> workHours = converter.convertToEntityAttribute(null);

        // Then
        assertThat(json).isNull();
        assertThat(workHours).isNull();
    }

    @Test
    void shouldHandleEmptyJson() {
        // Given
        String emptyJson = "";

        // When
        Map<String, BuddyPreferences.WorkHours> workHours = converter.convertToEntityAttribute(emptyJson);

        // Then
        assertThat(workHours).isNull();
    }

    @Test
    void shouldRoundTripConversion() {
        // Given
        Map<String, BuddyPreferences.WorkHours> original = Map.of(
                "MONDAY", new BuddyPreferences.WorkHours(9, 17),
                "WEDNESDAY", new BuddyPreferences.WorkHours(8, 16),
                "FRIDAY", new BuddyPreferences.WorkHours(10, 15)
        );

        // When
        String json = converter.convertToDatabaseColumn(original);
        Map<String, BuddyPreferences.WorkHours> roundTrip = converter.convertToEntityAttribute(json);

        // Then
        assertThat(roundTrip).isEqualTo(original);
    }
}
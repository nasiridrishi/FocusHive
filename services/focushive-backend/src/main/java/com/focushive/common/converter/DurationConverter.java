package com.focushive.common.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.Duration;

/**
 * JPA Converter for Duration type to store as bigint (milliseconds) in database
 */
@Converter(autoApply = true)
public class DurationConverter implements AttributeConverter<Duration, Long> {

    @Override
    public Long convertToDatabaseColumn(Duration duration) {
        return duration == null ? null : duration.toMillis();
    }

    @Override
    public Duration convertToEntityAttribute(Long millis) {
        return millis == null ? null : Duration.ofMillis(millis);
    }
}
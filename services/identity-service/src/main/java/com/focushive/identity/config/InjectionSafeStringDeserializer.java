package com.focushive.identity.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * Custom JSON deserializer that prevents NoSQL injection attempts by
 * rejecting non-string tokens when deserializing to String fields.
 * This prevents JSON objects, arrays, booleans, and numbers from being
 * accepted as string values, which is a common NoSQL injection vector.
 */
@Slf4j
public class InjectionSafeStringDeserializer extends JsonDeserializer<String> {

    @Override
    public String deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonToken token = parser.getCurrentToken();

        // Only allow string values
        if (token == JsonToken.VALUE_STRING) {
            return parser.getValueAsString();
        }

        // Log injection attempt and reject non-string tokens
        String fieldName = parser.getCurrentName();
        String tokenType = token != null ? token.toString() : "unknown";

        log.warn("NoSQL injection attempt detected: non-string token '{}' for string field '{}' from IP: {}",
                tokenType, fieldName, getClientIP(context));

        // Create detailed error message based on token type
        String errorMessage = switch (token) {
            case START_OBJECT -> "JSON objects are not allowed in string fields. Expected a string value.";
            case START_ARRAY -> "JSON arrays are not allowed in string fields. Expected a string value.";
            case VALUE_TRUE, VALUE_FALSE -> "Boolean values are not allowed in string fields. Expected a string value.";
            case VALUE_NUMBER_INT, VALUE_NUMBER_FLOAT -> "Numeric values are not allowed in string fields. Expected a string value.";
            case VALUE_NULL -> "Null values should be sent as JSON null, not in string fields.";
            default -> "Invalid token type for string field. Expected a string value.";
        };

        throw new InvalidFormatException(
                parser,
                errorMessage,
                parser.currentValue(),
                String.class
        );
    }

    /**
     * Extract client IP from deserialization context for logging.
     */
    private String getClientIP(DeserializationContext context) {
        // Try to get IP from context attributes (set by interceptors/filters)
        Object ip = context.getAttribute("client_ip");
        return ip != null ? ip.toString() : "unknown";
    }
}
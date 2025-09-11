package com.focushive.identity.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * Comprehensive unit tests for MessageResponse DTO.
 * Tests message response creation, serialization, and data handling.
 */
@DisplayName("MessageResponse DTO Unit Tests")
class MessageResponseUnitTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create MessageResponse with constructor")
        void shouldCreateMessageResponseWithConstructor() {
            // When
            MessageResponse response = new MessageResponse("Operation completed successfully");

            // Then
            assertAll(
                    () -> assertThat(response).isNotNull(),
                    () -> assertThat(response.getMessage()).isEqualTo("Operation completed successfully")
            );
        }

        @Test
        @DisplayName("Should create MessageResponse with null message")
        void shouldCreateMessageResponseWithNullMessage() {
            // When
            MessageResponse response = new MessageResponse(null);

            // Then
            assertAll(
                    () -> assertThat(response).isNotNull(),
                    () -> assertThat(response.getMessage()).isNull()
            );
        }

        @Test
        @DisplayName("Should create MessageResponse with empty message")
        void shouldCreateMessageResponseWithEmptyMessage() {
            // When
            MessageResponse response = new MessageResponse("");

            // Then
            assertAll(
                    () -> assertThat(response).isNotNull(),
                    () -> assertThat(response.getMessage()).isEqualTo("")
            );
        }
    }

    @Nested
    @DisplayName("Message Scenarios")
    class MessageScenarioTests {

        @Test
        @DisplayName("Should handle success messages")
        void shouldHandleSuccessMessages() {
            // Given
            String[] successMessages = {
                    "User registered successfully",
                    "Password reset email sent",
                    "Profile updated successfully",
                    "Authentication successful",
                    "Token validated successfully"
            };

            // When/Then
            for (String message : successMessages) {
                MessageResponse response = new MessageResponse(message);
                assertThat(response.getMessage()).isEqualTo(message);
            }
        }

        @Test
        @DisplayName("Should handle error messages")
        void shouldHandleErrorMessages() {
            // Given
            String[] errorMessages = {
                    "Invalid credentials provided",
                    "User not found",
                    "Token has expired",
                    "Insufficient permissions",
                    "Validation failed"
            };

            // When/Then
            for (String message : errorMessages) {
                MessageResponse response = new MessageResponse(message);
                assertThat(response.getMessage()).isEqualTo(message);
            }
        }

        @Test
        @DisplayName("Should handle warning messages")
        void shouldHandleWarningMessages() {
            // Given
            String[] warningMessages = {
                    "Password will expire in 7 days",
                    "Rate limit approaching",
                    "Account locked temporarily",
                    "Session will expire soon",
                    "Service maintenance scheduled"
            };

            // When/Then
            for (String message : warningMessages) {
                MessageResponse response = new MessageResponse(message);
                assertThat(response.getMessage()).isEqualTo(message);
            }
        }
    }

    @Nested
    @DisplayName("JSON Serialization Tests")
    class JsonSerializationTests {

        @Test
        @DisplayName("Should serialize MessageResponse to JSON correctly")
        void shouldSerializeMessageResponseToJsonCorrectly() throws Exception {
            // Given
            MessageResponse response = new MessageResponse("Serialization test message");

            // When
            String json = objectMapper.writeValueAsString(response);

            // Then
            assertThat(json).contains("\"message\":\"Serialization test message\"");
        }

        @Test
        @DisplayName("Should deserialize JSON to MessageResponse correctly")
        void shouldDeserializeJsonToMessageResponseCorrectly() throws Exception {
            // Given
            String json = """
                {
                    "message": "Deserialization test message"
                }
                """;

            // When
            MessageResponse response = objectMapper.readValue(json, MessageResponse.class);

            // Then
            assertAll(
                    () -> assertThat(response).isNotNull(),
                    () -> assertThat(response.getMessage()).isEqualTo("Deserialization test message")
            );
        }

        @Test
        @DisplayName("Should handle null message in JSON serialization")
        void shouldHandleNullMessageInJsonSerialization() throws Exception {
            // Given
            MessageResponse response = new MessageResponse(null);

            // When
            String json = objectMapper.writeValueAsString(response);

            // Then
            assertThat(json).contains("\"message\":null");
        }

        @Test
        @DisplayName("Should handle null message in JSON deserialization")
        void shouldHandleNullMessageInJsonDeserialization() throws Exception {
            // Given
            String json = """
                {
                    "message": null
                }
                """;

            // When
            MessageResponse response = objectMapper.readValue(json, MessageResponse.class);

            // Then
            assertAll(
                    () -> assertThat(response).isNotNull(),
                    () -> assertThat(response.getMessage()).isNull()
            );
        }

        @Test
        @DisplayName("Should handle special characters in JSON serialization")
        void shouldHandleSpecialCharactersInJsonSerialization() throws Exception {
            // Given
            String specialMessage = "Message with special chars: !@#$%^&*()_+-=[]{}|;':\",./<>?`~ and unicode: 中文 русский 🚀";
            MessageResponse response = new MessageResponse(specialMessage);

            // When
            String json = objectMapper.writeValueAsString(response);
            MessageResponse deserialized = objectMapper.readValue(json, MessageResponse.class);

            // Then
            assertThat(deserialized.getMessage()).isEqualTo(specialMessage);
        }
    }

    @Nested
    @DisplayName("Equals and HashCode Tests")
    class EqualsAndHashCodeTests {

        @Test
        @DisplayName("Should be equal when messages are the same")
        void shouldBeEqualWhenMessagesAreTheSame() {
            // Given
            MessageResponse response1 = new MessageResponse("Same message");
            MessageResponse response2 = new MessageResponse("Same message");

            // Then
            assertAll(
                    () -> assertThat(response1).isEqualTo(response2),
                    () -> assertThat(response1.hashCode()).isEqualTo(response2.hashCode())
            );
        }

        @Test
        @DisplayName("Should not be equal when messages differ")
        void shouldNotBeEqualWhenMessagesDiffer() {
            // Given
            MessageResponse response1 = new MessageResponse("Message one");
            MessageResponse response2 = new MessageResponse("Message two");

            // Then
            assertThat(response1).isNotEqualTo(response2);
        }

        @Test
        @DisplayName("Should be equal when both messages are null")
        void shouldBeEqualWhenBothMessagesAreNull() {
            // Given
            MessageResponse response1 = new MessageResponse(null);
            MessageResponse response2 = new MessageResponse(null);

            // Then
            assertAll(
                    () -> assertThat(response1).isEqualTo(response2),
                    () -> assertThat(response1.hashCode()).isEqualTo(response2.hashCode())
            );
        }

        @Test
        @DisplayName("Should not be equal when one message is null")
        void shouldNotBeEqualWhenOneMessageIsNull() {
            // Given
            MessageResponse response1 = new MessageResponse("Not null message");
            MessageResponse response2 = new MessageResponse(null);

            // Then
            assertThat(response1).isNotEqualTo(response2);
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle very long messages")
        void shouldHandleVeryLongMessages() {
            // Given
            String longMessage = "A".repeat(10000);

            // When
            MessageResponse response = new MessageResponse(longMessage);

            // Then
            assertAll(
                    () -> assertThat(response.getMessage()).hasSize(10000),
                    () -> assertThat(response.getMessage()).isEqualTo(longMessage)
            );
        }

        @Test
        @DisplayName("Should handle multiline messages")
        void shouldHandleMultilineMessages() {
            // Given
            String multilineMessage = "Line 1\nLine 2\nLine 3\r\nLine 4\tTabbed line";

            // When
            MessageResponse response = new MessageResponse(multilineMessage);

            // Then
            assertAll(
                    () -> assertThat(response.getMessage()).isEqualTo(multilineMessage),
                    () -> assertThat(response.getMessage()).contains("\n"),
                    () -> assertThat(response.getMessage()).contains("\r\n"),
                    () -> assertThat(response.getMessage()).contains("\t")
            );
        }

        @Test
        @DisplayName("Should handle HTML content in messages")
        void shouldHandleHtmlContentInMessages() {
            // Given
            String htmlMessage = "<html><body><h1>Error</h1><p>Invalid request</p></body></html>";

            // When
            MessageResponse response = new MessageResponse(htmlMessage);

            // Then
            assertThat(response.getMessage()).isEqualTo(htmlMessage);
        }

        @Test
        @DisplayName("Should handle JSON content in messages")
        void shouldHandleJsonContentInMessages() {
            // Given
            String jsonMessage = "{\"error\":\"Invalid token\",\"code\":\"AUTH001\",\"timestamp\":\"2024-01-15T10:30:00Z\"}";

            // When
            MessageResponse response = new MessageResponse(jsonMessage);

            // Then
            assertThat(response.getMessage()).isEqualTo(jsonMessage);
        }

        @Test
        @DisplayName("Should handle whitespace-only messages")
        void shouldHandleWhitespaceOnlyMessages() {
            // Given
            String whitespaceMessage = "   \t\n\r   ";

            // When
            MessageResponse response = new MessageResponse(whitespaceMessage);

            // Then
            assertThat(response.getMessage()).isEqualTo(whitespaceMessage);
        }

        @Test
        @DisplayName("Should handle messages with escape sequences")
        void shouldHandleMessagesWithEscapeSequences() {
            // Given
            String escapedMessage = "Message with \"quotes\" and \\backslashes\\ and \nnewlines";

            // When
            MessageResponse response = new MessageResponse(escapedMessage);

            // Then
            assertThat(response.getMessage()).isEqualTo(escapedMessage);
        }
    }

    @Nested
    @DisplayName("Modification Tests")
    class ModificationTests {

        @Test
        @DisplayName("Should allow message modification after creation")
        void shouldAllowMessageModificationAfterCreation() {
            // Given
            MessageResponse response = new MessageResponse("Initial message");

            // When
            response.setMessage("Modified message");

            // Then
            assertThat(response.getMessage()).isEqualTo("Modified message");
        }

        @Test
        @DisplayName("Should allow setting message to null after creation")
        void shouldAllowSettingMessageToNullAfterCreation() {
            // Given
            MessageResponse response = new MessageResponse("Initial message");

            // When
            response.setMessage(null);

            // Then
            assertThat(response.getMessage()).isNull();
        }

        @Test
        @DisplayName("Should allow multiple message modifications")
        void shouldAllowMultipleMessageModifications() {
            // Given
            MessageResponse response = new MessageResponse("Message 1");

            // When/Then
            response.setMessage("Message 2");
            assertThat(response.getMessage()).isEqualTo("Message 2");

            response.setMessage("Message 3");
            assertThat(response.getMessage()).isEqualTo("Message 3");

            response.setMessage("");
            assertThat(response.getMessage()).isEqualTo("");

            response.setMessage(null);
            assertThat(response.getMessage()).isNull();
        }
    }

    @Nested
    @DisplayName("ToString Tests")
    class ToStringTests {

        @Test
        @DisplayName("Should provide meaningful toString representation")
        void shouldProvideMeaningfulToStringRepresentation() {
            // Given
            MessageResponse response = new MessageResponse("Test message");

            // When
            String toString = response.toString();

            // Then
            assertAll(
                    () -> assertThat(toString).contains("MessageResponse"),
                    () -> assertThat(toString).contains("message=Test message")
            );
        }

        @Test
        @DisplayName("Should handle null message in toString")
        void shouldHandleNullMessageInToString() {
            // Given
            MessageResponse response = new MessageResponse(null);

            // When
            String toString = response.toString();

            // Then
            assertAll(
                    () -> assertThat(toString).contains("MessageResponse"),
                    () -> assertThat(toString).contains("message=null")
            );
        }
    }
}
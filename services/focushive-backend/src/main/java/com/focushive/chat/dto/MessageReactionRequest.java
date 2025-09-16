package com.focushive.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for adding or removing a message reaction.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageReactionRequest {

    @NotBlank(message = "Emoji is required")
    @Size(min = 1, max = 10, message = "Emoji must be between 1 and 10 characters")
    @Pattern(regexp = "^[\\p{So}\\p{Sc}\\p{Sm}\\p{Sk}\\p{Cn}😀-🙏💀-🙈]+$",
             message = "Invalid emoji format")
    private String emoji;

    /**
     * Action to perform: ADD or REMOVE
     */
    @NotBlank(message = "Action is required")
    @Pattern(regexp = "^(ADD|REMOVE)$", message = "Action must be ADD or REMOVE")
    private String action;
}
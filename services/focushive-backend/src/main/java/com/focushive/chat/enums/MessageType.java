package com.focushive.chat.enums;

/**
 * Enum representing different types of chat messages.
 * Includes basic message types and extended functionality.
 */
public enum MessageType {
    /**
     * Regular text message from a user.
     */
    TEXT,

    /**
     * Emoji-only message or emoji reactions.
     */
    EMOJI,

    /**
     * File attachment message.
     */
    FILE,

    /**
     * System-generated message.
     */
    SYSTEM,

    /**
     * Announcement message from moderators/admin.
     */
    ANNOUNCEMENT,

    /**
     * User join notification.
     */
    JOIN,

    /**
     * User leave notification.
     */
    LEAVE,

    /**
     * Reply to another message (threaded conversation).
     */
    REPLY,

    /**
     * Message indicating typing activity.
     */
    TYPING_INDICATOR,

    /**
     * Message edit notification.
     */
    EDIT,

    /**
     * Message deletion notification.
     */
    DELETE
}
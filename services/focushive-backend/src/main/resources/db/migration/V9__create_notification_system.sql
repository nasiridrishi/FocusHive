-- Create notification system tables

-- Create notification_preferences table
CREATE TABLE notification_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    notification_type VARCHAR(50) NOT NULL CHECK (notification_type IN (
        'HIVE_INVITATION',
        'TASK_ASSIGNED',
        'TASK_COMPLETED',
        'ACHIEVEMENT_UNLOCKED',
        'BUDDY_REQUEST',
        'BUDDY_REQUEST_ACCEPTED',
        'BUDDY_REQUEST_DECLINED',
        'SESSION_REMINDER',
        'BUDDY_SESSION_STARTED',
        'BUDDY_SESSION_COMPLETED',
        'SYSTEM_ANNOUNCEMENT',
        'SYSTEM_NOTIFICATION',
        'CHAT_MENTION',
        'FORUM_REPLY',
        'WEEKLY_SUMMARY',
        'HIVE_MEMBER_JOINED',
        'HIVE_MEMBER_LEFT',
        'HIVE_SETTINGS_UPDATED'
    )),
    in_app_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    email_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    push_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    quiet_start_time TIME,
    quiet_end_time TIME,
    frequency VARCHAR(20) NOT NULL DEFAULT 'IMMEDIATE' CHECK (frequency IN (
        'IMMEDIATE',
        'DAILY_DIGEST',
        'WEEKLY_DIGEST',
        'OFF'
    )),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, notification_type)
);

-- Create indexes for notification_preferences
CREATE INDEX idx_notification_preferences_user ON notification_preferences(user_id);
CREATE INDEX idx_notification_preferences_type ON notification_preferences(notification_type);
CREATE INDEX idx_notification_preferences_frequency ON notification_preferences(frequency);
CREATE INDEX idx_notification_preferences_channels ON notification_preferences(user_id, in_app_enabled, email_enabled, push_enabled);

-- Create trigger for notification_preferences updated_at
CREATE TRIGGER update_notification_preferences_updated_at 
    BEFORE UPDATE ON notification_preferences
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Create notification_templates table
CREATE TABLE notification_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    notification_type VARCHAR(50) NOT NULL CHECK (notification_type IN (
        'HIVE_INVITATION',
        'TASK_ASSIGNED',
        'TASK_COMPLETED',
        'ACHIEVEMENT_UNLOCKED',
        'BUDDY_REQUEST',
        'BUDDY_REQUEST_ACCEPTED',
        'BUDDY_REQUEST_DECLINED',
        'SESSION_REMINDER',
        'BUDDY_SESSION_STARTED',
        'BUDDY_SESSION_COMPLETED',
        'SYSTEM_ANNOUNCEMENT',
        'SYSTEM_NOTIFICATION',
        'CHAT_MENTION',
        'FORUM_REPLY',
        'WEEKLY_SUMMARY',
        'HIVE_MEMBER_JOINED',
        'HIVE_MEMBER_LEFT',
        'HIVE_SETTINGS_UPDATED'
    )),
    language VARCHAR(5) NOT NULL CHECK (LENGTH(language) >= 2),
    subject VARCHAR(200),
    body_text TEXT NOT NULL,
    body_html TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(notification_type, language)
);

-- Create indexes for notification_templates
CREATE INDEX idx_notification_templates_type ON notification_templates(notification_type);
CREATE INDEX idx_notification_templates_lang ON notification_templates(language);

-- Create trigger for notification_templates updated_at
CREATE TRIGGER update_notification_templates_updated_at 
    BEFORE UPDATE ON notification_templates
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Note: Notifications table will be created in a later migration
-- These indexes will be added when the notifications table is created

-- Insert default notification templates for English
INSERT INTO notification_templates (notification_type, language, subject, body_text, body_html) VALUES
-- Hive Invitation
('HIVE_INVITATION', 'en', 
 'You''ve been invited to join {{hiveName}}',
 'Hi {{userName}}, you''ve been invited to join the hive "{{hiveName}}" by {{inviterName}}. Click the link below to accept the invitation.',
 '<p>Hi {{userName}},</p><p>You''ve been invited to join the hive "<strong>{{hiveName}}</strong>" by {{inviterName}}.</p><p><a href="{{invitationUrl}}">Click here to accept the invitation</a></p>'),

-- Task Assignment
('TASK_ASSIGNED', 'en',
 'New task assigned: {{taskName}}',
 'You have been assigned a new task: "{{taskName}}". Priority: {{priority}}. Due date: {{dueDate}}.',
 '<p>You have been assigned a new task:</p><h3>{{taskName}}</h3><p><strong>Priority:</strong> {{priority}}</p><p><strong>Due date:</strong> {{dueDate}}</p><p><a href="{{taskUrl}}">View task details</a></p>'),

-- Task Completion
('TASK_COMPLETED', 'en',
 'Task completed: {{taskName}}',
 'The task "{{taskName}}" has been completed by {{completedBy}}.',
 '<p>The task "<strong>{{taskName}}</strong>" has been completed by {{completedBy}}.</p><p><a href="{{taskUrl}}">View task details</a></p>'),

-- Achievement Unlocked
('ACHIEVEMENT_UNLOCKED', 'en',
 'Achievement unlocked: {{achievementName}}',
 'Congratulations! You''ve unlocked the "{{achievementName}}" achievement. {{achievementDescription}}',
 '<p>🎉 Congratulations!</p><p>You''ve unlocked the "<strong>{{achievementName}}</strong>" achievement.</p><p>{{achievementDescription}}</p>'),

-- Buddy Request
('BUDDY_REQUEST', 'en',
 'New buddy request from {{requesterName}}',
 '{{requesterName}} would like to be your study buddy. Check out their profile and decide if you''d like to accept.',
 '<p>{{requesterName}} would like to be your study buddy.</p><p>Check out their profile and decide if you''d like to accept.</p><p><a href="{{profileUrl}}">View Profile</a> | <a href="{{acceptUrl}}">Accept</a> | <a href="{{declineUrl}}">Decline</a></p>'),

-- Session Reminder
('SESSION_REMINDER', 'en',
 'Focus session reminder',
 'Don''t forget about your scheduled focus session starting in {{timeUntil}}. Title: {{sessionTitle}}',
 '<p>Don''t forget about your scheduled focus session starting in <strong>{{timeUntil}}</strong>.</p><p><strong>Title:</strong> {{sessionTitle}}</p><p><a href="{{sessionUrl}}">Join session</a></p>'),

-- System Announcement
('SYSTEM_ANNOUNCEMENT', 'en',
 'System Announcement: {{title}}',
 '{{content}}',
 '<div style="border: 2px solid #007bff; padding: 15px; border-radius: 5px;"><h3>{{title}}</h3><p>{{content}}</p></div>'),

-- Weekly Summary
('WEEKLY_SUMMARY', 'en',
 'Your weekly productivity summary',
 'Here''s your productivity summary for this week: {{totalHours}} hours focused, {{completedTasks}} tasks completed, {{achievementsUnlocked}} achievements unlocked.',
 '<h2>Your Weekly Summary</h2><ul><li><strong>{{totalHours}}</strong> hours focused</li><li><strong>{{completedTasks}}</strong> tasks completed</li><li><strong>{{achievementsUnlocked}}</strong> achievements unlocked</li></ul><p><a href="{{detailsUrl}}">View detailed report</a></p>');

-- Insert default notification preferences for common notification types
-- This would typically be done when a user registers, but we can set up system defaults

-- Add comment explaining the notification system
COMMENT ON TABLE notification_preferences IS 'User preferences for different types of notifications including delivery channels and quiet hours';
COMMENT ON TABLE notification_templates IS 'Templates for notification messages in different languages with variable substitution support';
-- COMMENT ON TABLE notifications IS 'Individual notification instances sent to users'; -- Table not created yet

-- Add column comments for better documentation
COMMENT ON COLUMN notification_preferences.quiet_start_time IS 'Start time for do-not-disturb period (e.g., 22:00)';
COMMENT ON COLUMN notification_preferences.quiet_end_time IS 'End time for do-not-disturb period (e.g., 08:00)';
COMMENT ON COLUMN notification_preferences.frequency IS 'How often notifications should be delivered: IMMEDIATE, DAILY_DIGEST, WEEKLY_DIGEST, or OFF';
COMMENT ON COLUMN notification_templates.body_text IS 'Plain text version of notification template with {{variable}} placeholders';
COMMENT ON COLUMN notification_templates.body_html IS 'HTML version of notification template with {{variable}} placeholders for rich email content';
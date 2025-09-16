-- Buddy Service Test Data Migration
-- Creates sample data for testing and development purposes
-- Note: This migration is only loaded in test profile

-- ==============================================================================
-- TEST USER PREFERENCES
-- ==============================================================================
INSERT INTO buddy_preferences (id, user_id, preferred_timezone, preferred_work_hours, focus_areas, communication_style, matching_enabled, timezone_flexibility, min_commitment_hours, max_partners) VALUES
-- Alice: Software Developer, PST, Morning person
('a1111111-1111-1111-1111-111111111111', 'a0000000-0000-0000-0000-000000000001', 'America/Los_Angeles',
 '{"MONDAY": {"startHour": 8, "endHour": 17}, "TUESDAY": {"startHour": 8, "endHour": 17}, "WEDNESDAY": {"startHour": 8, "endHour": 17}, "THURSDAY": {"startHour": 8, "endHour": 17}, "FRIDAY": {"startHour": 8, "endHour": 16}}',
 ARRAY['CODING', 'SYSTEM_DESIGN', 'LEARNING'], 'MODERATE', true, 2, 15, 2),

-- Bob: Student, EST, Afternoon person
('b1111111-1111-1111-1111-111111111111', 'b0000000-0000-0000-0000-000000000001', 'America/New_York',
 '{"MONDAY": {"startHour": 13, "endHour": 22}, "TUESDAY": {"startHour": 13, "endHour": 22}, "WEDNESDAY": {"startHour": 13, "endHour": 22}, "THURSDAY": {"startHour": 13, "endHour": 22}, "FRIDAY": {"startHour": 13, "endHour": 18}}',
 ARRAY['STUDYING', 'RESEARCH', 'WRITING'], 'FREQUENT', true, 3, 20, 3),

-- Charlie: Designer, GMT, Flexible hours
('c1111111-1111-1111-1111-111111111111', 'c0000000-0000-0000-0000-000000000001', 'Europe/London',
 '{"MONDAY": {"startHour": 9, "endHour": 18}, "TUESDAY": {"startHour": 10, "endHour": 19}, "WEDNESDAY": {"startHour": 9, "endHour": 18}, "THURSDAY": {"startHour": 9, "endHour": 18}, "FRIDAY": {"startHour": 9, "endHour": 17}}',
 ARRAY['DESIGN', 'CREATIVE_WORK', 'LEARNING'], 'MINIMAL', true, 4, 12, 2),

-- David: Researcher, JST, Early riser
('d1111111-1111-1111-1111-111111111111', 'd0000000-0000-0000-0000-000000000001', 'Asia/Tokyo',
 '{"MONDAY": {"startHour": 6, "endHour": 15}, "TUESDAY": {"startHour": 6, "endHour": 15}, "WEDNESDAY": {"startHour": 6, "endHour": 15}, "THURSDAY": {"startHour": 6, "endHour": 15}, "FRIDAY": {"startHour": 6, "endHour": 14}}',
 ARRAY['RESEARCH', 'ANALYSIS', 'WRITING'], 'MODERATE', true, 1, 25, 1),

-- Eva: Product Manager, CET, Standard hours
('e1111111-1111-1111-1111-111111111111', 'e0000000-0000-0000-0000-000000000001', 'Europe/Berlin',
 '{"MONDAY": {"startHour": 9, "endHour": 17}, "TUESDAY": {"startHour": 9, "endHour": 17}, "WEDNESDAY": {"startHour": 9, "endHour": 17}, "THURSDAY": {"startHour": 9, "endHour": 17}, "FRIDAY": {"startHour": 9, "endHour": 16}}',
 ARRAY['PLANNING', 'MEETINGS', 'STRATEGY'], 'FREQUENT', true, 2, 18, 2),

-- Frank: Writer, PST, Night owl (disabled matching)
('f1111111-1111-1111-1111-111111111111', 'f0000000-0000-0000-0000-000000000001', 'America/Los_Angeles',
 '{"MONDAY": {"startHour": 20, "endHour": 24}, "TUESDAY": {"startHour": 20, "endHour": 24}, "WEDNESDAY": {"startHour": 20, "endHour": 24}, "THURSDAY": {"startHour": 20, "endHour": 24}, "FRIDAY": {"startHour": 19, "endHour": 23}}',
 ARRAY['WRITING', 'CREATIVE_WORK'], 'MINIMAL', false, 3, 10, 1);

-- ==============================================================================
-- TEST PARTNERSHIPS
-- ==============================================================================
INSERT INTO buddy_partnerships (id, user1_id, user2_id, status, agreement_text, duration_days, compatibility_score, health_score, started_at) VALUES
-- Active partnership: Alice & Bob
('a2222222-2222-2222-2222-222222222222', 'a0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000001', 'ACTIVE',
 'We commit to daily check-ins and helping each other with our coding/study goals. Communication in the evenings EST.',
 30, 0.7850, 0.9200, CURRENT_TIMESTAMP - INTERVAL '15 days'),

-- Active partnership: Charlie & Eva
('c2222222-2222-2222-2222-222222222222', 'c0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000001', 'ACTIVE',
 'Daily standups and project accountability. Focus on creative and strategic work.',
 45, 0.8120, 0.8900, CURRENT_TIMESTAMP - INTERVAL '10 days'),

-- Pending partnership: David & Charlie
('d2222222-2222-2222-2222-222222222222', 'c0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001', 'PENDING',
 'Research collaboration and writing accountability partnership.',
 60, 0.7650, NULL, NULL),

-- Recently ended partnership: Alice & Charlie
('a3333333-3333-3333-3333-333333333333', 'a0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001', 'ENDED',
 'Coding and design collaboration - trial period.',
 14, 0.6450, 0.7500, CURRENT_TIMESTAMP - INTERVAL '45 days'),

-- Long-term active partnership: Bob & Eva
('b3333333-3333-3333-3333-333333333333', 'b0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000001', 'ACTIVE',
 'Study planning and career development accountability.',
 90, 0.8750, 0.9500, CURRENT_TIMESTAMP - INTERVAL '25 days');

-- ==============================================================================
-- TEST SHARED GOALS
-- ==============================================================================
INSERT INTO shared_goals (id, partnership_id, title, description, created_by, target_date, status, progress_percentage) VALUES
-- Goals for Alice & Bob partnership
('a5555555-5555-5555-5555-555555555555', 'a2222222-2222-2222-2222-222222222222', 'Complete JavaScript Certification',
 'Both finish the advanced JavaScript certification course by end of month',
 'a0000000-0000-0000-0000-000000000001', CURRENT_DATE + INTERVAL '15 days', 'IN_PROGRESS', 75),

('a6666666-6666-6666-6666-666666666666', 'a2222222-2222-2222-2222-222222222222', 'Daily Coding Practice',
 'Maintain daily coding practice streak - minimum 1 hour per day',
 'b0000000-0000-0000-0000-000000000001', CURRENT_DATE + INTERVAL '20 days', 'IN_PROGRESS', 60),

-- Goals for Charlie & Eva partnership
('c5555555-5555-5555-5555-555555555555', 'c2222222-2222-2222-2222-222222222222', 'Launch Portfolio Website',
 'Complete and launch updated portfolio websites for both partners',
 'c0000000-0000-0000-0000-000000000001', CURRENT_DATE + INTERVAL '25 days', 'IN_PROGRESS', 40),

('c6666666-6666-6666-6666-666666666666', 'c2222222-2222-2222-2222-222222222222', 'Weekly Strategy Sessions',
 'Conduct weekly strategic planning and review sessions',
 'e0000000-0000-0000-0000-000000000001', CURRENT_DATE + INTERVAL '30 days', 'IN_PROGRESS', 85),

-- Completed goal for Bob & Eva
('b5555555-5555-5555-5555-555555555555', 'b3333333-3333-3333-3333-333333333333', 'Create Study Schedule',
 'Develop and implement comprehensive study schedule for semester',
 'b0000000-0000-0000-0000-000000000001', CURRENT_DATE - INTERVAL '5 days', 'COMPLETED', 100),

-- Cancelled goal from ended partnership
('a7777777-7777-7777-7777-777777777777', 'a3333333-3333-3333-3333-333333333333', 'Build Side Project',
 'Collaborate on coding/design side project',
 'a0000000-0000-0000-0000-000000000001', CURRENT_DATE - INTERVAL '30 days', 'CANCELLED', 25);

-- ==============================================================================
-- TEST GOAL MILESTONES
-- ==============================================================================
INSERT INTO goal_milestones (id, goal_id, title, target_date, order_index, completed_at, completed_by) VALUES
-- Milestones for JavaScript Certification goal
('a8888888-8888-8888-8888-888888888888', 'a5555555-5555-5555-5555-555555555555', 'Complete Modules 1-3', CURRENT_DATE - INTERVAL '5 days', 1,
 CURRENT_TIMESTAMP - INTERVAL '3 days', 'a0000000-0000-0000-0000-000000000001'),
('a9999999-9999-9999-9999-999999999999', 'a5555555-5555-5555-5555-555555555555', 'Complete Modules 4-6', CURRENT_DATE + INTERVAL '5 days', 2, NULL, NULL),
('aaaa1111-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'a5555555-5555-5555-5555-555555555555', 'Pass Final Exam', CURRENT_DATE + INTERVAL '15 days', 3, NULL, NULL),

-- Milestones for Portfolio goal
('c8888888-8888-8888-8888-888888888888', 'c5555555-5555-5555-5555-555555555555', 'Design Mockups Complete', CURRENT_DATE + INTERVAL '10 days', 1, NULL, NULL),
('c9999999-9999-9999-9999-999999999999', 'c5555555-5555-5555-5555-555555555555', 'Development Phase', CURRENT_DATE + INTERVAL '20 days', 2, NULL, NULL),
('caaa1111-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'c5555555-5555-5555-5555-555555555555', 'Launch & Deploy', CURRENT_DATE + INTERVAL '25 days', 3, NULL, NULL);

-- ==============================================================================
-- TEST CHECK-INS
-- ==============================================================================
INSERT INTO buddy_checkins (id, partnership_id, user_id, checkin_type, content, mood, productivity_rating, created_at) VALUES
-- Recent check-ins for Alice & Bob
('c1111111-1111-1111-1111-111111111111', 'a2222222-2222-2222-2222-222222222222', 'a0000000-0000-0000-0000-000000000001', 'DAILY',
 'Great progress today! Completed 2 JavaScript modules and feeling confident about the upcoming exam.',
 'EXCELLENT', 9, CURRENT_TIMESTAMP - INTERVAL '1 day'),

('c2222222-1111-1111-1111-111111111111', 'a2222222-2222-2222-2222-222222222222', 'b0000000-0000-0000-0000-000000000001', 'DAILY',
 'Solid day of coding practice. Worked on the React project for 2 hours. Need to review state management tomorrow.',
 'GOOD', 7, CURRENT_TIMESTAMP - INTERVAL '1 day'),

('c3333333-1111-1111-1111-111111111111', 'a2222222-2222-2222-2222-222222222222', 'a0000000-0000-0000-0000-000000000001', 'DAILY',
 'Struggled a bit with promises and async concepts, but Bob helped me understand it better in our evening call.',
 'GOOD', 6, CURRENT_TIMESTAMP - INTERVAL '2 days'),

-- Check-ins for Charlie & Eva
('c4444444-1111-1111-1111-111111111111', 'c2222222-2222-2222-2222-222222222222', 'c0000000-0000-0000-0000-000000000001', 'DAILY',
 'Finished the portfolio mockups! Eva gave great feedback on the UX flow. Ready to start development phase.',
 'EXCELLENT', 10, CURRENT_TIMESTAMP - INTERVAL '1 day'),

('c5555555-1111-1111-1111-111111111111', 'c2222222-2222-2222-2222-222222222222', 'e0000000-0000-0000-0000-000000000001', 'WEEKLY',
 'Great week of strategic planning. Charlie and I aligned on our Q2 goals and identified key opportunities.',
 'GOOD', 8, CURRENT_TIMESTAMP - INTERVAL '2 days'),

-- Older check-ins for streak tracking
('c6666666-1111-1111-1111-111111111111', 'a2222222-2222-2222-2222-222222222222', 'b0000000-0000-0000-0000-000000000001', 'DAILY',
 'Consistent progress on coding challenges. Alice and I are keeping each other motivated!',
 'GOOD', 7, CURRENT_TIMESTAMP - INTERVAL '3 days');

-- ==============================================================================
-- TEST ACCOUNTABILITY SCORES
-- ==============================================================================
INSERT INTO accountability_scores (id, user_id, partnership_id, score, checkins_completed, goals_achieved, response_rate, streak_days) VALUES
-- Individual user scores
('a1111111-2222-2222-2222-222222222222', 'a0000000-0000-0000-0000-000000000001', NULL, 0.87, 28, 3, 0.95, 12),
('a2222222-3333-3333-3333-333333333333', 'b0000000-0000-0000-0000-000000000001', NULL, 0.82, 25, 2, 0.88, 8),
('a3333333-4444-4444-4444-444444444444', 'c0000000-0000-0000-0000-000000000001', NULL, 0.75, 18, 1, 0.92, 5),
('a4444444-5555-5555-5555-555555555555', 'e0000000-0000-0000-0000-000000000001', NULL, 0.91, 22, 4, 0.96, 10),
('a5555555-6666-6666-6666-666666666666', 'd0000000-0000-0000-0000-000000000001', NULL, 0.68, 5, 0, 0.75, 2),

-- Partnership-specific scores
('a6666666-7777-7777-7777-777777777777', 'a0000000-0000-0000-0000-000000000001', 'a2222222-2222-2222-2222-222222222222', 0.85, 15, 1, 0.93, 12),
('a7777777-8888-8888-8888-888888888888', 'b0000000-0000-0000-0000-000000000001', 'a2222222-2222-2222-2222-222222222222', 0.78, 13, 1, 0.87, 8),
('a8888888-9999-9999-9999-999999999999', 'c0000000-0000-0000-0000-000000000001', 'c2222222-2222-2222-2222-222222222222', 0.88, 10, 1, 0.95, 5),
('a9999999-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'e0000000-0000-0000-0000-000000000001', 'c2222222-2222-2222-2222-222222222222', 0.92, 10, 2, 0.98, 10);


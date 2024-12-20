-- =======================================================
-- TEST SCHEMA FOR H2 WITH POSTGRESQL COMPATIBILITY
-- =======================================================
-- This schema provides H2-compatible definitions for PostgreSQL-specific features
-- used in the application's Flyway migrations and JPA entities.

-- =======================================================
-- DOMAIN AND TYPE DEFINITIONS
-- =======================================================

-- PostgreSQL JSONB type → H2 CLOB
CREATE DOMAIN IF NOT EXISTS JSONB AS CLOB;

-- PostgreSQL UUID type → H2 VARCHAR(36) with validation
CREATE DOMAIN IF NOT EXISTS UUID AS VARCHAR(36)
  CHECK (VALUE IS NULL OR VALUE ~ '^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$');

-- PostgreSQL TEXT type → H2 CLOB
CREATE DOMAIN IF NOT EXISTS TEXT AS CLOB;

-- PostgreSQL TIMESTAMP WITH TIME ZONE → H2 TIMESTAMP WITH TIME ZONE  
CREATE DOMAIN IF NOT EXISTS TIMESTAMPTZ AS TIMESTAMP WITH TIME ZONE;

-- =======================================================
-- POSTGRESQL FUNCTION COMPATIBILITY
-- =======================================================

-- gen_random_uuid() function for H2
CREATE ALIAS IF NOT EXISTS gen_random_uuid FOR "java.util.UUID.randomUUID";

-- CURRENT_TIMESTAMP function (H2 compatible)
-- H2 already supports this, but ensure consistency
-- CREATE ALIAS IF NOT EXISTS CURRENT_TIMESTAMP FOR "java.time.Instant.now";

-- =======================================================
-- POSTGRESQL ARRAY TYPE SUPPORT (SIMPLIFIED)
-- =======================================================

-- For array types, we'll use VARCHAR with JSON-like storage
-- This is a simplified approach for testing
CREATE DOMAIN IF NOT EXISTS TEXT_ARRAY AS VARCHAR(1000);
CREATE DOMAIN IF NOT EXISTS VARCHAR_ARRAY AS VARCHAR(1000);
CREATE DOMAIN IF NOT EXISTS INT_ARRAY AS VARCHAR(500);

-- =======================================================
-- POSTGRESQL ENUM TYPE SUPPORT
-- =======================================================

-- H2 doesn't support ENUM types, so we'll create CHECK constraints
-- These will be applied in individual table creation scripts

-- Notification types (used in notification system)
-- Will be handled as VARCHAR with CHECK constraints in table definitions

-- User roles (used in user management)
-- Will be handled as VARCHAR with CHECK constraints in table definitions

-- =======================================================
-- TIME ZONE SUPPORT
-- =======================================================

-- H2 supports time zones, but ensure compatibility
-- Set default timezone for consistency
-- This is handled by H2 automatically, but we can set it if needed
-- SET TIME ZONE 'UTC';

-- =======================================================
-- POSTGRESQL SPECIFIC SQL FUNCTIONS
-- =======================================================

-- COALESCE function (H2 already supports this)
-- LENGTH function (H2 already supports this)
-- LOWER/UPPER functions (H2 already supports this)

-- =======================================================
-- INDEX SUPPORT
-- =======================================================

-- H2 supports most index types, but some PostgreSQL-specific features need adaptation:
-- - Partial indexes: H2 supports WHERE clauses in CREATE INDEX
-- - GIN indexes: Not supported in H2, will fall back to regular indexes
-- - Functional indexes: Limited support, will use regular indexes where possible

-- =======================================================
-- COMMENT SUPPORT
-- =======================================================

-- H2 supports table and column comments similar to PostgreSQL
-- No additional setup needed

-- =======================================================
-- VALIDATION NOTES
-- =======================================================

-- The following PostgreSQL features are NOT supported in H2 and will be ignored in tests:
-- 1. Advanced JSONB operators (use string operations instead)
-- 2. Array operators (use JSON/string operations instead)  
-- 3. Advanced text search (use LIKE operations instead)
-- 4. Specific PostgreSQL functions (use H2 equivalents where available)
-- 5. Custom PostgreSQL types (mapped to standard H2 types above)

-- For production deployment, ensure all PostgreSQL-specific features work correctly
-- This schema is only for testing purposes with H2

-- =======================================================
-- H2 SPECIFIC OPTIMIZATIONS FOR TESTS
-- =======================================================

-- Enable PostgreSQL compatibility mode functions
SET MODE PostgreSQL;

-- Set case sensitivity for compatibility
SET DATABASE_TO_LOWER TRUE;

-- Configure null ordering to match PostgreSQL
SET DEFAULT_NULL_ORDERING HIGH;

-- Performance settings for tests
SET CACHE_SIZE 65536;  -- 64MB cache
SET LOG 0;             -- Disable logging for performance
SET UNDO_LOG 0;        -- Disable undo log for tests (faster)

-- =======================================================
-- UTILITY FUNCTIONS FOR TESTS
-- =======================================================

-- Function to check if a string is a valid UUID
CREATE OR REPLACE FUNCTION is_valid_uuid(input VARCHAR(36))
RETURNS BOOLEAN
AS 
$$
BEGIN
  RETURN input IS NOT NULL AND 
         input ~ '^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$';
END;
$$
LANGUAGE SQL;

-- Function to simulate JSONB operations (basic)
CREATE OR REPLACE FUNCTION jsonb_extract_path_text(json_data CLOB, path VARCHAR)
RETURNS VARCHAR
AS
$$
BEGIN
  -- Simplified JSON path extraction
  -- In real tests, this would need more sophisticated JSON parsing
  RETURN NULL;
END;
$$
LANGUAGE SQL;

-- =======================================================
-- CLEANUP FUNCTIONS FOR TESTS
-- =======================================================

-- Procedure to clean up test data between test runs
CREATE OR REPLACE PROCEDURE cleanup_test_data()
AS
$$
BEGIN
  -- This would be called by test framework to clean up
  -- Table-specific cleanup will be handled by JPA/Hibernate with create-drop
  NULL;
END;
$$
LANGUAGE SQL;
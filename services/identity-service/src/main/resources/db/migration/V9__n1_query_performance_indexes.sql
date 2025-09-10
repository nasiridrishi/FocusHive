-- V9__n1_query_performance_indexes.sql
-- Additional performance optimization indexes to prevent N+1 queries
-- Specifically designed to support EntityGraph and JOIN FETCH queries

-- Personas table additional indexes for N+1 query optimization
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_personas_user_id_created_at ON personas(user_id, created_at ASC);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_personas_user_id_active_default ON personas(user_id, is_active DESC, is_default DESC, created_at ASC);

-- Persona attributes table indexes (for ElementCollection optimization)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_persona_attributes_persona_id ON persona_attributes(persona_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_persona_attributes_key_value ON persona_attributes(persona_id, attribute_key, attribute_value);

-- Multi-column indexes for common query patterns
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_personas_composite_lookup ON personas(user_id, id, is_active, is_default);

-- Indexes to support batch fetching of users with personas
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_users_deleted_at_created_at ON users(deleted_at, created_at DESC) WHERE deleted_at IS NULL;

-- Composite index for persona priority ordering query
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_personas_priority_order ON personas(
    user_id,
    (CASE WHEN is_active = true THEN 0 ELSE 1 END),
    (CASE WHEN is_default = true THEN 0 ELSE 1 END),
    created_at ASC
);

-- Analyze tables after creating indexes
ANALYZE users;
ANALYZE personas;
ANALYZE persona_attributes;
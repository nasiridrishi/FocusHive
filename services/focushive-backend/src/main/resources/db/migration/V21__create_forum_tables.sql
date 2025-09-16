-- V21__create_forum_tables.sql
-- Create Forum Service database tables

-- Forum Categories table
CREATE TABLE forum_categories (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    slug VARCHAR(100) NOT NULL UNIQUE,
    parent_id VARCHAR(36),
    hive_id VARCHAR(36),
    icon VARCHAR(50),
    sort_order INTEGER DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT true,
    post_count INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_forum_category_parent FOREIGN KEY (parent_id) REFERENCES forum_categories(id) ON DELETE CASCADE,
    CONSTRAINT fk_forum_category_hive FOREIGN KEY (hive_id) REFERENCES hives(id) ON DELETE CASCADE
);

-- Forum Posts table
CREATE TABLE forum_posts (
    id VARCHAR(36) PRIMARY KEY,
    category_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    content_html TEXT,
    slug VARCHAR(255),
    tags TEXT, -- JSON array stored as text
    view_count INTEGER NOT NULL DEFAULT 0,
    reply_count INTEGER NOT NULL DEFAULT 0,
    vote_score INTEGER NOT NULL DEFAULT 0,
    is_pinned BOOLEAN NOT NULL DEFAULT false,
    is_locked BOOLEAN NOT NULL DEFAULT false,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    edited_at TIMESTAMP,
    edited_by VARCHAR(36),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_forum_post_category FOREIGN KEY (category_id) REFERENCES forum_categories(id) ON DELETE CASCADE,
    CONSTRAINT fk_forum_post_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_forum_post_edited_by FOREIGN KEY (edited_by) REFERENCES users(id) ON DELETE SET NULL
);

-- Forum Replies table
CREATE TABLE forum_replies (
    id VARCHAR(36) PRIMARY KEY,
    post_id VARCHAR(36) NOT NULL,
    parent_reply_id VARCHAR(36),
    user_id VARCHAR(36) NOT NULL,
    content TEXT NOT NULL,
    content_html TEXT,
    vote_score INTEGER NOT NULL DEFAULT 0,
    is_accepted BOOLEAN NOT NULL DEFAULT false,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    edited_at TIMESTAMP,
    edited_by VARCHAR(36),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_forum_reply_post FOREIGN KEY (post_id) REFERENCES forum_posts(id) ON DELETE CASCADE,
    CONSTRAINT fk_forum_reply_parent FOREIGN KEY (parent_reply_id) REFERENCES forum_replies(id) ON DELETE CASCADE,
    CONSTRAINT fk_forum_reply_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_forum_reply_edited_by FOREIGN KEY (edited_by) REFERENCES users(id) ON DELETE SET NULL
);

-- Forum Votes table
CREATE TABLE forum_votes (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    post_id VARCHAR(36),
    reply_id VARCHAR(36),
    vote_type INTEGER NOT NULL CHECK (vote_type IN (-1, 1)),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_forum_vote_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_forum_vote_post FOREIGN KEY (post_id) REFERENCES forum_posts(id) ON DELETE CASCADE,
    CONSTRAINT fk_forum_vote_reply FOREIGN KEY (reply_id) REFERENCES forum_replies(id) ON DELETE CASCADE,
    CONSTRAINT uk_forum_vote_user_post UNIQUE (user_id, post_id),
    CONSTRAINT uk_forum_vote_user_reply UNIQUE (user_id, reply_id),
    CONSTRAINT ck_forum_vote_target CHECK (
        (post_id IS NOT NULL AND reply_id IS NULL) OR
        (post_id IS NULL AND reply_id IS NOT NULL)
    )
);

-- Forum Subscriptions table
CREATE TABLE forum_subscriptions (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    post_id VARCHAR(36),
    category_id VARCHAR(36),
    notification_type VARCHAR(20) NOT NULL DEFAULT 'ALL',
    is_muted BOOLEAN NOT NULL DEFAULT false,
    muted_until TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_forum_subscription_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_forum_subscription_post FOREIGN KEY (post_id) REFERENCES forum_posts(id) ON DELETE CASCADE,
    CONSTRAINT fk_forum_subscription_category FOREIGN KEY (category_id) REFERENCES forum_categories(id) ON DELETE CASCADE,
    CONSTRAINT uk_forum_subscription_user_post UNIQUE (user_id, post_id),
    CONSTRAINT uk_forum_subscription_user_category UNIQUE (user_id, category_id),
    CONSTRAINT ck_forum_subscription_target CHECK (
        (post_id IS NOT NULL AND category_id IS NULL) OR
        (post_id IS NULL AND category_id IS NOT NULL)
    )
);

-- Forum Post Tags table (for normalized tag storage)
CREATE TABLE forum_post_tags (
    post_id VARCHAR(36) NOT NULL,
    tag VARCHAR(50) NOT NULL,

    PRIMARY KEY (post_id, tag),
    CONSTRAINT fk_forum_post_tag_post FOREIGN KEY (post_id) REFERENCES forum_posts(id) ON DELETE CASCADE
);

-- Create indexes for better performance
CREATE INDEX idx_forum_category_slug ON forum_categories(slug);
CREATE INDEX idx_forum_category_hive ON forum_categories(hive_id);
CREATE INDEX idx_forum_category_parent ON forum_categories(parent_id);

CREATE INDEX idx_forum_post_category ON forum_posts(category_id);
CREATE INDEX idx_forum_post_user ON forum_posts(user_id);
CREATE INDEX idx_forum_post_created ON forum_posts(created_at DESC);
CREATE INDEX idx_forum_post_votes ON forum_posts(vote_score DESC);
CREATE INDEX idx_forum_post_slug ON forum_posts(category_id, slug);
CREATE INDEX idx_forum_post_pinned_locked ON forum_posts(is_pinned, is_locked);

CREATE INDEX idx_forum_reply_post ON forum_replies(post_id);
CREATE INDEX idx_forum_reply_user ON forum_replies(user_id);
CREATE INDEX idx_forum_reply_parent ON forum_replies(parent_reply_id);
CREATE INDEX idx_forum_reply_created ON forum_replies(created_at);

CREATE INDEX idx_forum_vote_user ON forum_votes(user_id);
CREATE INDEX idx_forum_vote_post ON forum_votes(post_id);
CREATE INDEX idx_forum_vote_reply ON forum_votes(reply_id);

CREATE INDEX idx_forum_subscription_user ON forum_subscriptions(user_id);
CREATE INDEX idx_forum_subscription_post ON forum_subscriptions(post_id);
CREATE INDEX idx_forum_subscription_category ON forum_subscriptions(category_id);

CREATE INDEX idx_forum_post_tags_tag ON forum_post_tags(tag);

-- Insert default global categories
INSERT INTO forum_categories (id, name, description, slug, sort_order, is_active) VALUES
('general-discussion', 'General Discussion', 'General topics and discussions', 'general-discussion', 1, true),
('help-support', 'Help & Support', 'Questions and technical support', 'help-support', 2, true),
('announcements', 'Announcements', 'Official announcements and updates', 'announcements', 3, true),
('feedback', 'Feedback', 'Suggestions and feedback', 'feedback', 4, true);

-- Update post_count for categories (trigger would be better in production)
UPDATE forum_categories
SET post_count = (
    SELECT COUNT(*)
    FROM forum_posts
    WHERE forum_posts.category_id = forum_categories.id
    AND forum_posts.is_deleted = false
);
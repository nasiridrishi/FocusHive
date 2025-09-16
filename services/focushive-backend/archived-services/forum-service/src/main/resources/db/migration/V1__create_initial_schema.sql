CREATE TABLE forums (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE posts (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    content TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    author VARCHAR(255) NOT NULL,
    forum_id BIGINT NOT NULL,
    FOREIGN KEY (forum_id) REFERENCES forums(id)
);

CREATE TABLE comments (
    id BIGSERIAL PRIMARY KEY,
    content TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    author VARCHAR(255) NOT NULL,
    post_id BIGINT NOT NULL,
    FOREIGN KEY (post_id) REFERENCES posts(id)
);

CREATE TABLE votes (
    id BIGSERIAL PRIMARY KEY,
    vote_type VARCHAR(255) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    post_id BIGINT,
    comment_id BIGINT,
    FOREIGN KEY (post_id) REFERENCES posts(id),
    FOREIGN KEY (comment_id) REFERENCES comments(id)
);

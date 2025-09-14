package com.focushive.forum.repository;

import com.focushive.forum.domain.Comment;
import com.focushive.forum.domain.Forum;
import com.focushive.forum.domain.Post;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
public class CommentRepositoryTest {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private ForumRepository forumRepository;

    @Test
    public void shouldSaveAndRetrieveComment() {
        // given
        Forum forum = new Forum();
        forum.setName("Test Forum");
        forum.setDescription("Test Description");
        forum.setCreatedAt(Instant.now());
        forumRepository.save(forum);

        Post post = new Post();
        post.setTitle("Test Post");
        post.setContent("Test Content");
        post.setCreatedAt(Instant.now());
        post.setAuthor("testuser");
        post.setForum(forum);
        postRepository.save(post);

        Comment comment = new Comment();
        comment.setContent("Test Comment");
        comment.setCreatedAt(Instant.now());
        comment.setAuthor("testuser");
        comment.setPost(post);

        // when
        Comment savedComment = commentRepository.save(comment);

        // then
        assertThat(savedComment).isNotNull();
        assertThat(savedComment.getId()).isNotNull();
        assertThat(savedComment.getContent()).isEqualTo("Test Comment");
        assertThat(savedComment.getPost()).isEqualTo(post);
    }
}

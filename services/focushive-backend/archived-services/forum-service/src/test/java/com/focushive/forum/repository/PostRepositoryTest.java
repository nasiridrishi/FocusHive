package com.focushive.forum.repository;

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
public class PostRepositoryTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private ForumRepository forumRepository;

    @Test
    public void shouldSaveAndRetrievePost() {
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

        // when
        Post savedPost = postRepository.save(post);

        // then
        assertThat(savedPost).isNotNull();
        assertThat(savedPost.getId()).isNotNull();
        assertThat(savedPost.getTitle()).isEqualTo("Test Post");
        assertThat(savedPost.getForum()).isEqualTo(forum);
    }
}

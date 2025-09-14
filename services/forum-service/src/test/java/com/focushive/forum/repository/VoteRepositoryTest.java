package com.focushive.forum.repository;

import com.focushive.forum.domain.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
public class VoteRepositoryTest {

    @Autowired
    private VoteRepository voteRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private ForumRepository forumRepository;

    @Test
    public void shouldSaveAndRetrieveVote() {
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

        Vote vote = new Vote();
        vote.setVoteType("UPVOTE");
        vote.setUserId("testuser");
        vote.setPost(post);

        // when
        Vote savedVote = voteRepository.save(vote);

        // then
        assertThat(savedVote).isNotNull();
        assertThat(savedVote.getId()).isNotNull();
        assertThat(savedVote.getVoteType()).isEqualTo("UPVOTE");
        assertThat(savedVote.getPost()).isEqualTo(post);
    }
}

package com.focushive.forum.repository;

import com.focushive.forum.domain.Forum;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
public class ForumRepositoryTest {

    @Autowired
    private ForumRepository forumRepository;

    @Test
    public void shouldSaveAndRetrieveForum() {
        // given
        Forum forum = new Forum();
        forum.setName("Test Forum");
        forum.setDescription("Test Description");
        forum.setCreatedAt(Instant.now());

        // when
        Forum savedForum = forumRepository.save(forum);

        // then
        assertThat(savedForum).isNotNull();
        assertThat(savedForum.getId()).isNotNull();
        assertThat(savedForum.getName()).isEqualTo("Test Forum");
    }
}

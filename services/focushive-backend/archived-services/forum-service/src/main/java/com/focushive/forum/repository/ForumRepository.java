package com.focushive.forum.repository;

import com.focushive.forum.domain.Forum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ForumRepository extends JpaRepository<Forum, Long> {
}

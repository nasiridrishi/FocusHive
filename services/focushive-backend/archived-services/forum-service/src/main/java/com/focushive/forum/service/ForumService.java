package com.focushive.forum.service;

import com.focushive.forum.domain.Forum;
import com.focushive.forum.repository.ForumRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ForumService {

    private final ForumRepository forumRepository;

    public ForumService(ForumRepository forumRepository) {
        this.forumRepository = forumRepository;
    }

    public Forum createForum(Forum forum) {
        return forumRepository.save(forum);
    }

    public List<Forum> getAllForums() {
        return forumRepository.findAll();
    }
}

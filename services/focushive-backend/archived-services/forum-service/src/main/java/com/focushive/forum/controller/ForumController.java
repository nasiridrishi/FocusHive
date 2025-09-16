package com.focushive.forum.controller;

import com.focushive.forum.domain.Forum;
import com.focushive.forum.service.ForumService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/forums")
public class ForumController {

    private final ForumService forumService;
    private static final Logger logger = LoggerFactory.getLogger(ForumController.class);

    public ForumController(ForumService forumService) {
        this.forumService = forumService;
    }

    @PostMapping
    public ResponseEntity<Forum> createForum(@RequestBody Forum forum) {
        logger.info("Creating forum: {}", forum);
        Forum savedForum = forumService.createForum(forum);
        logger.info("Saved forum: {}", savedForum);
        return new ResponseEntity<>(savedForum, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Forum>> getAllForums() {
        List<Forum> forums = forumService.getAllForums();
        return new ResponseEntity<>(forums, HttpStatus.OK);
    }
}

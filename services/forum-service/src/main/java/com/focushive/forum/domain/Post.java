package com.focushive.forum.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "posts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String content;

    private Instant createdAt;

    private String author;

    @ManyToOne
    @JoinColumn(name = "forum_id")
    private Forum forum;

    @OneToMany(mappedBy = "post")
    private List<Comment> comments;
}

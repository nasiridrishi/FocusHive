package com.focushive.forum.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "forums")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Forum {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String description;

    private Instant createdAt;

    @OneToMany(mappedBy = "forum")
    private List<Post> posts;
}

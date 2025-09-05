package com.focushive.music.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * User music preferences entity.
 */
@Entity
@Table(name = "music_preferences")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class MusicPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private String userId;

    @ElementCollection
    @CollectionTable(name = "user_preferred_genres", joinColumns = @JoinColumn(name = "preference_id"))
    @Column(name = "genre")
    private List<String> preferredGenres;

    @Column(name = "energy_level")
    @Builder.Default
    private Double energyLevel = 0.6;

    @Column(name = "tempo_preference")
    @Builder.Default
    private Double tempoPreference = 120.0;

    @Column(name = "ambient_sounds_enabled")
    @Builder.Default
    private Boolean ambientSoundsEnabled = true;

    @Column(name = "auto_start_music")
    @Builder.Default
    private Boolean autoStartMusic = true;

    @Column(name = "default_volume")
    @Builder.Default
    private Double defaultVolume = 0.5;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
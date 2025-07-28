package com.focushive.timer.repository;

import com.focushive.timer.entity.PomodoroSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PomodoroSettingsRepository extends JpaRepository<PomodoroSettings, String> {
    
    Optional<PomodoroSettings> findByUserId(String userId);
    
    boolean existsByUserId(String userId);
}
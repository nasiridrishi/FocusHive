package com.focushive.timer.repository;

import com.focushive.timer.entity.HiveTimer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HiveTimerRepository extends JpaRepository<HiveTimer, String> {
    
    // Find active timer for a hive
    Optional<HiveTimer> findByHiveIdAndIsRunningTrue(String hiveId);
    
    // Find all timers for a hive
    List<HiveTimer> findByHiveIdOrderByCreatedAtDesc(String hiveId);
    
    // Check if hive has active timer
    boolean existsByHiveIdAndIsRunningTrue(String hiveId);
    
    // Clean up old timers
    @Query("DELETE FROM HiveTimer ht WHERE ht.hiveId = :hiveId AND ht.isRunning = false")
    void deleteInactiveTimers(@Param("hiveId") String hiveId);
}
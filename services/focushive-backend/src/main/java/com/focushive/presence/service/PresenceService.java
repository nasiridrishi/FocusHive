package com.focushive.presence.service;

import com.focushive.presence.dto.PresenceUpdate;
import com.focushive.presence.dto.UserPresence;
import com.focushive.presence.dto.HivePresenceInfo;
import com.focushive.presence.dto.FocusSession;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Service interface for managing user presence and focus sessions.
 */
public interface PresenceService {
    
    /**
     * Updates a user's presence status.
     * 
     * @param userId the user ID
     * @param update the presence update
     * @return the updated user presence
     */
    UserPresence updateUserPresence(String userId, PresenceUpdate update);
    
    /**
     * Gets a user's current presence.
     * 
     * @param userId the user ID
     * @return the user's presence, or null if not found
     */
    UserPresence getUserPresence(String userId);
    
    /**
     * Adds a user to a hive's active presence list.
     * 
     * @param hiveId the hive ID
     * @param userId the user ID
     * @return the updated hive presence info
     */
    HivePresenceInfo joinHivePresence(String hiveId, String userId);
    
    /**
     * Removes a user from a hive's active presence list.
     * 
     * @param hiveId the hive ID
     * @param userId the user ID
     * @return the updated hive presence info
     */
    HivePresenceInfo leaveHivePresence(String hiveId, String userId);
    
    /**
     * Gets all active users in a hive.
     * 
     * @param hiveId the hive ID
     * @return list of active user presences
     */
    List<UserPresence> getHiveActiveUsers(String hiveId);
    
    /**
     * Gets presence info for multiple hives.
     * 
     * @param hiveIds the hive IDs
     * @return map of hive ID to presence info
     */
    Map<String, HivePresenceInfo> getHivesPresenceInfo(Set<String> hiveIds);
    
    /**
     * Starts a focus session for a user.
     * 
     * @param userId the user ID
     * @param hiveId the hive ID (optional)
     * @param durationMinutes the planned duration
     * @return the created focus session
     */
    FocusSession startFocusSession(String userId, String hiveId, int durationMinutes);
    
    /**
     * Ends a user's current focus session.
     * 
     * @param userId the user ID
     * @param sessionId the session ID
     * @return the completed focus session
     */
    FocusSession endFocusSession(String userId, String sessionId);
    
    /**
     * Gets a user's current active focus session.
     * 
     * @param userId the user ID
     * @return the active session, or null if none
     */
    FocusSession getActiveFocusSession(String userId);
    
    /**
     * Gets all active focus sessions in a hive.
     * 
     * @param hiveId the hive ID
     * @return list of active focus sessions
     */
    List<FocusSession> getHiveFocusSessions(String hiveId);
    
    /**
     * Records a heartbeat for a user to maintain presence.
     * 
     * @param userId the user ID
     */
    void recordHeartbeat(String userId);
    
    /**
     * Cleans up stale presence data.
     * Called periodically to remove inactive users.
     */
    void cleanupStalePresence();
    
    /**
     * Marks a user as offline and cleans up their presence.
     * 
     * @param userId the user ID
     */
    void markUserOffline(String userId);
}
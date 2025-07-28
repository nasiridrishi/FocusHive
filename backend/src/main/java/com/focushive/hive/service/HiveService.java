package com.focushive.hive.service;

import com.focushive.hive.dto.CreateHiveRequest;
import com.focushive.hive.dto.HiveResponse;
import com.focushive.hive.dto.UpdateHiveRequest;
import com.focushive.hive.entity.Hive;
import com.focushive.hive.entity.HiveMember;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service interface for hive management operations.
 */
public interface HiveService {
    
    /**
     * Creates a new hive.
     * 
     * @param request the hive creation request
     * @param ownerId the ID of the user creating the hive
     * @return the created hive response
     */
    HiveResponse createHive(CreateHiveRequest request, String ownerId);
    
    /**
     * Gets a hive by ID.
     * 
     * @param hiveId the hive ID
     * @param userId the requesting user ID (for permission checks)
     * @return the hive response
     */
    HiveResponse getHive(String hiveId, String userId);
    
    /**
     * Gets a hive by slug.
     * 
     * @param slug the hive slug
     * @param userId the requesting user ID (for permission checks)
     * @return the hive response
     */
    HiveResponse getHiveBySlug(String slug, String userId);
    
    /**
     * Updates a hive.
     * 
     * @param hiveId the hive ID
     * @param request the update request
     * @param userId the requesting user ID (must be owner or moderator)
     * @return the updated hive response
     */
    HiveResponse updateHive(String hiveId, UpdateHiveRequest request, String userId);
    
    /**
     * Deletes a hive (soft delete).
     * 
     * @param hiveId the hive ID
     * @param userId the requesting user ID (must be owner)
     */
    void deleteHive(String hiveId, String userId);
    
    /**
     * Lists public hives.
     * 
     * @param pageable pagination information
     * @return page of public hives
     */
    Page<HiveResponse> listPublicHives(Pageable pageable);
    
    /**
     * Lists hives for a specific user.
     * 
     * @param userId the user ID
     * @param pageable pagination information
     * @return page of user's hives
     */
    Page<HiveResponse> listUserHives(String userId, Pageable pageable);
    
    /**
     * Searches public hives.
     * 
     * @param query the search query
     * @param pageable pagination information
     * @return page of matching hives
     */
    Page<HiveResponse> searchHives(String query, Pageable pageable);
    
    /**
     * Lists hives by type.
     * 
     * @param type the hive type
     * @param pageable pagination information
     * @return page of hives of the specified type
     */
    Page<HiveResponse> listHivesByType(Hive.HiveType type, Pageable pageable);
    
    /**
     * Joins a hive.
     * 
     * @param hiveId the hive ID
     * @param userId the user ID
     * @return the member response
     */
    HiveMember joinHive(String hiveId, String userId);
    
    /**
     * Leaves a hive.
     * 
     * @param hiveId the hive ID
     * @param userId the user ID
     */
    void leaveHive(String hiveId, String userId);
    
    /**
     * Gets members of a hive.
     * 
     * @param hiveId the hive ID
     * @param userId the requesting user ID (for permission checks)
     * @param pageable pagination information
     * @return page of hive members
     */
    Page<HiveMember> getHiveMembers(String hiveId, String userId, Pageable pageable);
    
    /**
     * Updates a member's role.
     * 
     * @param hiveId the hive ID
     * @param memberId the member ID to update
     * @param newRole the new role
     * @param requesterId the requesting user ID (must be owner)
     * @return the updated member
     */
    HiveMember updateMemberRole(String hiveId, String memberId, HiveMember.MemberRole newRole, String requesterId);
    
    /**
     * Removes a member from a hive.
     * 
     * @param hiveId the hive ID
     * @param memberId the member ID to remove
     * @param requesterId the requesting user ID (must be owner or moderator)
     */
    void removeMember(String hiveId, String memberId, String requesterId);
    
    /**
     * Checks if a user is a member of a hive.
     * 
     * @param hiveId the hive ID
     * @param userId the user ID
     * @return true if the user is a member
     */
    boolean isMember(String hiveId, String userId);
    
    /**
     * Gets a user's role in a hive.
     * 
     * @param hiveId the hive ID
     * @param userId the user ID
     * @return the member role, or null if not a member
     */
    HiveMember.MemberRole getUserRole(String hiveId, String userId);
    
    /**
     * Updates hive statistics (e.g., total focus minutes).
     * 
     * @param hiveId the hive ID
     * @param additionalMinutes minutes to add
     */
    void updateHiveStatistics(String hiveId, long additionalMinutes);
}
package com.focushive.hive.service.impl;

import com.focushive.api.client.IdentityServiceClient;
import com.focushive.api.dto.identity.UserDto;
import com.focushive.common.exception.ResourceNotFoundException;
import com.focushive.common.exception.BadRequestException;
import com.focushive.common.exception.ForbiddenException;
import com.focushive.common.exception.ValidationException;
import com.focushive.config.UnifiedRedisConfig;
import com.focushive.hive.dto.CreateHiveRequest;
import com.focushive.hive.dto.HiveResponse;
import com.focushive.hive.dto.UpdateHiveRequest;
import com.focushive.hive.entity.Hive;
import com.focushive.hive.entity.HiveMember;
import com.focushive.hive.repository.HiveMemberRepository;
import com.focushive.hive.repository.HiveRepository;
import com.focushive.hive.service.HiveService;
import com.focushive.user.entity.User;
import com.focushive.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Implementation of HiveService for managing virtual co-working spaces.
 */
@Slf4j
@Service
@Transactional
public class HiveServiceImpl implements HiveService {

    private final HiveRepository hiveRepository;
    private final HiveMemberRepository hiveMemberRepository;
    private final UserRepository userRepository;
    private final IdentityServiceClient identityServiceClient;
    private final ApplicationEventPublisher eventPublisher;

    public HiveServiceImpl(HiveRepository hiveRepository,
                          HiveMemberRepository hiveMemberRepository,
                          UserRepository userRepository,
                          ApplicationEventPublisher eventPublisher,
                          @Autowired(required = false) IdentityServiceClient identityServiceClient) {
        this.hiveRepository = hiveRepository;
        this.hiveMemberRepository = hiveMemberRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
        this.identityServiceClient = identityServiceClient;

        if (identityServiceClient == null) {
            log.warn("IdentityServiceClient not available - running in degraded mode");
        }
    }

    // Constants for business rules
    private static final int MAX_HIVES_PER_USER = 10;
    
    @Override
    @Caching(evict = {
        @CacheEvict(value = UnifiedRedisConfig.HIVES_ACTIVE_CACHE, allEntries = true),
        @CacheEvict(value = UnifiedRedisConfig.HIVES_USER_CACHE, allEntries = true)
    })
    public HiveResponse createHive(CreateHiveRequest request, String ownerId) {
        log.info("Creating new hive '{}' for user {}", request.getName(), ownerId);

        // Validate request constraints
        validateHiveConstraints(request);

        // Get owner from database
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Check user hive limit
        long userHiveCount = hiveRepository.countByOwnerIdAndDeletedAtIsNull(ownerId);
        if (userHiveCount >= MAX_HIVES_PER_USER) {
            throw new BadRequestException(
                String.format("You have reached the maximum number of hives (%d). Please delete some hives first.",
                    MAX_HIVES_PER_USER));
        }

        // Check name uniqueness per user
        if (hiveRepository.existsByNameAndOwnerIdAndDeletedAtIsNull(request.getName(), ownerId)) {
            throw new BadRequestException("A hive with this name already exists in your account");
        }

        // Generate unique slug
        String slug = generateUniqueSlug(request.getName());
        
        // Create hive entity
        Hive hive = new Hive();
        hive.setName(request.getName());
        hive.setSlug(slug);
        hive.setDescription(request.getDescription());
        hive.setOwner(owner);
        hive.setMaxMembers(request.getMaxMembers());
        hive.setIsPublic(request.getIsPublic());
        hive.setType(request.getType());
        hive.setBackgroundImage(request.getBackgroundImage());
        hive.setIsActive(true);
        hive.setMemberCount(1); // Owner is the first member
        
        // Save hive
        hive = hiveRepository.save(hive);
        
        // Add owner as first member with OWNER role
        HiveMember ownerMember = new HiveMember();
        ownerMember.setHive(hive);
        ownerMember.setUser(owner);
        ownerMember.setRole(HiveMember.MemberRole.OWNER);
        ownerMember.setJoinedAt(LocalDateTime.now());
        hiveMemberRepository.save(ownerMember);
        
        // Publish hive created event
        try {
            eventPublisher.publishEvent(new com.focushive.events.HiveCreatedEvent(
                hive.getId(), hive.getName(), ownerId, hive.getIsPublic()));
        } catch (Exception e) {
            log.warn("Failed to publish HiveCreatedEvent for hive {}: {}", hive.getId(), e.getMessage());
        }

        log.info("Successfully created hive '{}' with ID {}", hive.getName(), hive.getId());
        return new HiveResponse(hive, 1);
    }
    
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = UnifiedRedisConfig.HIVE_DETAILS_CACHE, key = "#hiveId + ':' + #userId", unless = "#result == null")
    public HiveResponse getHive(String hiveId, String userId) {
        // Removed debug log to avoid logging user data frequently
        
        Hive hive = hiveRepository.findByIdAndActive(hiveId)
                .orElseThrow(() -> new ResourceNotFoundException("Hive not found or inactive"));
        
        // Check access permissions
        if (!hive.getIsPublic() && !isMember(hiveId, userId)) {
            throw new ForbiddenException("You don't have permission to view this private hive");
        }
        
        int memberCount = (int) hiveMemberRepository.countByHiveId(hiveId);
        return new HiveResponse(hive, memberCount);
    }
    
    @Override
    @Transactional(readOnly = true)
    public HiveResponse getHiveBySlug(String slug, String userId) {
        // Removed debug log to avoid logging user data frequently
        
        Hive hive = hiveRepository.findBySlugAndActive(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Hive not found or inactive"));
        
        // Check access permissions
        if (!hive.getIsPublic() && !isMember(hive.getId(), userId)) {
            throw new ForbiddenException("You don't have permission to view this private hive");
        }
        
        int memberCount = (int) hiveMemberRepository.countByHiveId(hive.getId());
        return new HiveResponse(hive, memberCount);
    }
    
    @Override
    public HiveResponse updateHive(String hiveId, UpdateHiveRequest request, String userId) {
        log.info("Updating hive {} by user {}", hiveId, userId);
        
        Hive hive = hiveRepository.findByIdAndActive(hiveId)
                .orElseThrow(() -> new ResourceNotFoundException("Hive not found or inactive"));
        
        // Check permissions - only owner or moderator can update
        HiveMember.MemberRole userRole = getUserRole(hiveId, userId);
        if (userRole == null || userRole == HiveMember.MemberRole.MEMBER) {
            throw new ForbiddenException("Only hive owner or moderators can update hive settings");
        }
        
        // Update fields if provided
        if (request.getName() != null) {
            hive.setName(request.getName());
        }
        if (request.getDescription() != null) {
            hive.setDescription(request.getDescription());
        }
        if (request.getMaxMembers() != null) {
            // Validate that new max is not less than current members
            int currentMembers = (int) hiveMemberRepository.countByHiveId(hiveId);
            if (request.getMaxMembers() < currentMembers) {
                throw new BadRequestException(
                    String.format("Cannot set max members to %d, hive currently has %d members",
                        request.getMaxMembers(), currentMembers)
                );
            }
            hive.setMaxMembers(request.getMaxMembers());
        }
        if (request.getIsPublic() != null) {
            hive.setIsPublic(request.getIsPublic());
        }
        if (request.getIsActive() != null && userRole == HiveMember.MemberRole.OWNER) {
            // Only owner can activate/deactivate
            hive.setIsActive(request.getIsActive());
        }
        if (request.getType() != null) {
            hive.setType(request.getType());
        }
        if (request.getBackgroundImage() != null) {
            hive.setBackgroundImage(request.getBackgroundImage());
        }
        if (request.getThemeColor() != null) {
            hive.setThemeColor(request.getThemeColor());
        }
        if (request.getRules() != null) {
            hive.setRules(request.getRules());
        }
        if (request.getTags() != null) {
            hive.setTags(request.getTags());
        }
        
        hive = hiveRepository.save(hive);
        
        int memberCount = (int) hiveMemberRepository.countByHiveId(hiveId);
        log.info("Successfully updated hive {}", hiveId);
        return new HiveResponse(hive, memberCount);
    }
    
    @Override
    public void deleteHive(String hiveId, String userId) {
        log.info("Deleting hive {} by user {}", hiveId, userId);
        
        Hive hive = hiveRepository.findByIdAndActive(hiveId)
                .orElseThrow(() -> new ResourceNotFoundException("Hive not found or inactive"));
        
        // Only owner can delete
        if (!hive.getOwner().getId().equals(userId)) {
            throw new ForbiddenException("Only the hive owner can delete the hive");
        }
        
        // Soft delete
        hive.setDeletedAt(LocalDateTime.now());
        hive.setIsActive(false);
        hiveRepository.save(hive);

        // Publish hive deleted event
        try {
            eventPublisher.publishEvent(new Object()); // Generic event for now
        } catch (Exception e) {
            log.warn("Failed to publish HiveDeletedEvent for hive {}: {}", hiveId, e.getMessage());
        }

        log.info("Successfully deleted hive {}", hiveId);
    }
    
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = UnifiedRedisConfig.HIVES_ACTIVE_CACHE, key = "'public:' + #pageable.pageNumber + ':' + #pageable.pageSize", unless = "#result.isEmpty()")
    public Page<HiveResponse> listPublicHives(Pageable pageable) {
        Page<Hive> hives = hiveRepository.findPublicHives(pageable);
        return hives.map(hive -> {
            int memberCount = (int) hiveMemberRepository.countByHiveId(hive.getId());
            return new HiveResponse(hive, memberCount);
        });
    }
    
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = UnifiedRedisConfig.HIVES_USER_CACHE, key = "#userId + ':' + #pageable.pageNumber + ':' + #pageable.pageSize", unless = "#result.isEmpty()")
    public Page<HiveResponse> listUserHives(String userId, Pageable pageable) {
        Page<HiveMember> memberships = hiveMemberRepository.findByUserId(userId, pageable);
        return memberships.map(member -> {
            Hive hive = member.getHive();
            int memberCount = (int) hiveMemberRepository.countByHiveId(hive.getId());
            return new HiveResponse(hive, memberCount);
        });
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<HiveResponse> searchHives(String query, Pageable pageable) {
        Page<Hive> hives = hiveRepository.searchPublicHives(query, pageable);
        return hives.map(hive -> {
            int memberCount = (int) hiveMemberRepository.countByHiveId(hive.getId());
            return new HiveResponse(hive, memberCount);
        });
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<HiveResponse> listHivesByType(Hive.HiveType type, Pageable pageable) {
        Page<Hive> hives = hiveRepository.findByType(type, pageable);
        return hives.map(hive -> {
            int memberCount = (int) hiveMemberRepository.countByHiveId(hive.getId());
            return new HiveResponse(hive, memberCount);
        });
    }
    
    @Override
    public HiveMember joinHive(String hiveId, String userId) {
        log.info("User {} joining hive {}", userId, hiveId);
        
        // Check if hive exists and is active
        Hive hive = hiveRepository.findByIdAndActive(hiveId)
                .orElseThrow(() -> new ResourceNotFoundException("Hive not found or inactive"));
        
        // Check if already a member
        if (hiveMemberRepository.existsByHiveIdAndUserId(hiveId, userId)) {
            throw new BadRequestException("You are already a member of this hive");
        }
        
        // Check if hive is full
        int currentMembers = (int) hiveMemberRepository.countByHiveId(hiveId);
        if (currentMembers >= hive.getMaxMembers()) {
            throw new BadRequestException("This hive is full");
        }
        
        // Get user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        // Create membership
        HiveMember member = new HiveMember();
        member.setHive(hive);
        member.setUser(user);
        member.setRole(HiveMember.MemberRole.MEMBER);
        member.setJoinedAt(LocalDateTime.now());
        
        member = hiveMemberRepository.save(member);
        
        log.info("User {} successfully joined hive {}", userId, hiveId);
        return member;
    }
    
    @Override
    public void leaveHive(String hiveId, String userId) {
        log.info("User {} leaving hive {}", userId, hiveId);
        
        HiveMember member = hiveMemberRepository.findByHiveIdAndUserId(hiveId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("You are not a member of this hive"));
        
        // Check if user is the owner
        if (member.getRole() == HiveMember.MemberRole.OWNER) {
            // Check if there are other members
            int memberCount = (int) hiveMemberRepository.countByHiveId(hiveId);
            if (memberCount > 1) {
                throw new BadRequestException(
                    "As the owner, you cannot leave the hive while other members are present. " +
                    "Please transfer ownership or remove all members first."
                );
            }
        }
        
        // Remove membership
        hiveMemberRepository.delete(member);
        
        log.info("User {} successfully left hive {}", userId, hiveId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<HiveMember> getHiveMembers(String hiveId, String userId, Pageable pageable) {
        // Check if hive exists
        Hive hive = hiveRepository.findByIdAndActive(hiveId)
                .orElseThrow(() -> new ResourceNotFoundException("Hive not found or inactive"));
        
        // Check access permissions
        if (!hive.getIsPublic() && !isMember(hiveId, userId)) {
            throw new ForbiddenException("You don't have permission to view members of this private hive");
        }
        
        return hiveMemberRepository.findByHiveId(hiveId, pageable);
    }
    
    @Override
    public HiveMember updateMemberRole(String hiveId, String memberId, HiveMember.MemberRole newRole, String requesterId) {
        log.info("Updating member {} role to {} in hive {} by user {}", memberId, newRole, hiveId, requesterId);
        
        // Check requester is owner
        HiveMember.MemberRole requesterRole = getUserRole(hiveId, requesterId);
        if (requesterRole != HiveMember.MemberRole.OWNER) {
            throw new ForbiddenException("Only the hive owner can change member roles");
        }
        
        // Get member to update
        HiveMember member = hiveMemberRepository.findByHiveIdAndUserId(hiveId, memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found in this hive"));
        
        // Cannot change owner's role
        if (member.getRole() == HiveMember.MemberRole.OWNER) {
            throw new BadRequestException("Cannot change the owner's role");
        }
        
        // Update role
        member.setRole(newRole);
        member = hiveMemberRepository.save(member);

        // Publish role change event
        try {
            eventPublisher.publishEvent(new Object()); // Generic event for now
        } catch (Exception e) {
            log.warn("Failed to publish RoleChangeEvent for member {} in hive {}: {}", memberId, hiveId, e.getMessage());
        }

        log.info("Successfully updated member {} role to {} in hive {}", memberId, newRole, hiveId);
        return member;
    }
    
    @Override
    public void removeMember(String hiveId, String memberId, String requesterId) {
        log.info("Removing member {} from hive {} by user {}", memberId, hiveId, requesterId);
        
        // Check requester permissions
        HiveMember.MemberRole requesterRole = getUserRole(hiveId, requesterId);
        if (requesterRole == null || requesterRole == HiveMember.MemberRole.MEMBER) {
            throw new ForbiddenException("Only hive owner or moderators can remove members");
        }
        
        // Get member to remove
        HiveMember member = hiveMemberRepository.findByHiveIdAndUserId(hiveId, memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found in this hive"));
        
        // Cannot remove owner
        if (member.getRole() == HiveMember.MemberRole.OWNER) {
            throw new BadRequestException("Cannot remove the hive owner");
        }
        
        // Moderators cannot remove other moderators or owner
        if (requesterRole == HiveMember.MemberRole.MODERATOR && 
            member.getRole() != HiveMember.MemberRole.MEMBER) {
            throw new ForbiddenException("Moderators can only remove regular members");
        }
        
        // Remove member
        hiveMemberRepository.delete(member);
        
        log.info("Successfully removed member {} from hive {}", memberId, hiveId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean isMember(String hiveId, String userId) {
        return hiveMemberRepository.existsByHiveIdAndUserId(hiveId, userId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public HiveMember.MemberRole getUserRole(String hiveId, String userId) {
        return hiveMemberRepository.findByHiveIdAndUserId(hiveId, userId)
                .map(HiveMember::getRole)
                .orElse(null);
    }
    
    @Override
    public void updateHiveStatistics(String hiveId, long additionalMinutes) {
        hiveRepository.incrementTotalFocusMinutes(hiveId, additionalMinutes);
    }
    
    /**
     * Generates a unique slug from the hive name.
     */
    private String generateUniqueSlug(String name) {
        String baseSlug = name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        
        // Ensure slug is not empty
        if (baseSlug.isEmpty()) {
            baseSlug = "hive";
        }
        
        // Make unique if necessary
        String slug = baseSlug;
        int counter = 1;
        while (hiveRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + counter++;
        }
        
        return slug;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = UnifiedRedisConfig.HIVES_STATS_CACHE, key = "'active-count'")
    public long getActiveHiveCount() {
        // For now, consider all hives as active
        // In the future, this could be enhanced to filter by recent activity
        return hiveRepository.count();
    }

    @Override
    public void validateHiveConstraints(CreateHiveRequest request) {
        log.debug("Validating hive constraints for request: {}", request.getName());

        // Validate name
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new ValidationException("Hive name is required");
        }

        if (request.getName().length() > 100) {
            throw new ValidationException("Hive name must not exceed 100 characters");
        }

        // Validate description
        if (request.getDescription() != null && request.getDescription().length() > 500) {
            throw new ValidationException("Description must not exceed 500 characters");
        }

        // Validate max members
        if (request.getMaxMembers() == null) {
            throw new ValidationException("Max members is required");
        }

        if (request.getMaxMembers() < 1) {
            throw new ValidationException("Max members must be at least 1");
        }

        if (request.getMaxMembers() > 100) {
            throw new ValidationException("Max members cannot exceed 100");
        }

        // Validate type
        if (request.getType() == null) {
            throw new ValidationException("Hive type is required");
        }

        // Validate public flag
        if (request.getIsPublic() == null) {
            throw new ValidationException("Public/private setting is required");
        }

        log.debug("Hive constraints validation passed for: {}", request.getName());
    }
}
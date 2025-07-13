package com.focushive.presence.controller;

import com.focushive.presence.dto.PresenceUpdate;
import com.focushive.presence.dto.UserPresence;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.List;

@Controller
public class PresenceController {
    
    @MessageMapping("/presence/update")
    @SendToUser("/queue/presence/confirm")
    public UserPresence updatePresence(@Payload PresenceUpdate update, Principal principal) {
        // TODO: Update user presence in Redis
        // TODO: Broadcast to relevant hives
        return new UserPresence(
            principal.getName(),
            update.status(),
            update.activity(),
            System.currentTimeMillis()
        );
    }
    
    @MessageMapping("/hive/{hiveId}/join")
    @SendTo("/topic/hive/{hiveId}/presence")
    public PresenceNotification joinHive(@DestinationVariable String hiveId, Principal principal) {
        // TODO: Add user to hive presence list
        return new PresenceNotification(
            principal.getName(),
            "joined",
            hiveId,
            System.currentTimeMillis()
        );
    }
    
    @MessageMapping("/hive/{hiveId}/leave")
    @SendTo("/topic/hive/{hiveId}/presence")
    public PresenceNotification leaveHive(@DestinationVariable String hiveId, Principal principal) {
        // TODO: Remove user from hive presence list
        return new PresenceNotification(
            principal.getName(),
            "left",
            hiveId,
            System.currentTimeMillis()
        );
    }
    
    @MessageMapping("/hive/{hiveId}/members")
    @SendToUser("/queue/hive/members")
    public List<UserPresence> getHiveMembers(@DestinationVariable String hiveId) {
        // TODO: Get active members from Redis
        return List.of();
    }
    
    public record PresenceNotification(
        String userId,
        String action,
        String hiveId,
        long timestamp
    ) {}
}
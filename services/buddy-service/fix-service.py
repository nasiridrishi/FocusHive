#!/usr/bin/env python3
import re

def fix_service_file():
    with open('src/main/java/com/focushive/buddy/service/BuddyPartnershipService.java', 'r') as f:
        content = f.read()

    # Fix createPartnershipRequest method
    content = re.sub(
        r'UUID user1Id = UUID\.fromString\(requestDto\.getRequesterId\(\)\);\s*UUID user2Id = UUID\.fromString\(requestDto\.getRecipientId\(\)\);',
        'String user1Id = requestDto.getRequesterId();\n        String user2Id = requestDto.getRecipientId();',
        content
    )

    # Fix userExists method
    content = re.sub(
        r'private boolean userExists\(String userId\) \{[^}]+\}',
        '''private boolean userExists(String userId) {
        // Check if user exists - no UUID validation needed
        return userRepository.findById(userId).isPresent();
    }''',
        content
    )

    # Fix hasExistingPartnership method
    content = re.sub(
        r'private boolean hasExistingPartnership\(String userId1, String userId2\) \{[^}]+\}',
        '''private boolean hasExistingPartnership(String userId1, String userId2) {
        return partnershipRepository.findPartnershipBetweenUsers(userId1, userId2)
                .map(p -> p.getStatus() == PartnershipStatus.ACTIVE || p.getStatus() == PartnershipStatus.PAUSED)
                .orElse(false);
    }''',
        content
    )

    # Fix hasExistingPendingRequest method
    content = re.sub(
        r'private boolean hasExistingPendingRequest\(String userId1, String userId2\) \{[^}]+\}',
        '''private boolean hasExistingPendingRequest(String userId1, String userId2) {
        return partnershipRepository.findPartnershipBetweenUsers(userId1, userId2)
                .map(p -> p.getStatus() == PartnershipStatus.PENDING)
                .orElse(false);
    }''',
        content
    )

    # Fix all partnership.involvesUser calls with UUID conversion
    content = re.sub(
        r'UUID (\w+)Uuid = UUID\.fromString\((\w+)\);\s*if \(!partnership\.involvesUser\(\1Uuid(?:\.toString\(\))?\)\)',
        r'if (!partnership.involvesUser(\2))',
        content
    )

    # Fix all partnership.getPartnerIdFor calls with UUID conversion
    content = re.sub(
        r'String partnerId = partnership\.getPartnerIdFor\((\w+)Uuid(?:\.toString\(\))?\)(?:\.toString\(\))?;',
        r'String partnerId = partnership.getPartnerIdFor(\1);',
        content
    )

    # Fix clearUserPartnershipCache calls
    content = re.sub(
        r'clearUserPartnershipCache\(partnership\.getUser1Id\(\)\.toString\(\)\);',
        'clearUserPartnershipCache(partnership.getUser1Id());',
        content
    )
    content = re.sub(
        r'clearUserPartnershipCache\(partnership\.getUser2Id\(\)\.toString\(\)\);',
        'clearUserPartnershipCache(partnership.getUser2Id());',
        content
    )

    # Fix findPartnershipHistory and getPartnershipStatistics methods
    content = re.sub(
        r'try \{\s*UUID userUuid = UUID\.fromString\(userId\);\s*List<BuddyPartnership> partnerships = partnershipRepository\.findAllPartnershipsByUserId\(userUuid\);',
        'List<BuddyPartnership> partnerships = partnershipRepository.findAllPartnershipsByUserId(userId);',
        content
    )

    # Fix getPendingRequests method
    content = re.sub(
        r'try \{\s*UUID userUuid = UUID\.fromString\(userId\);\s*List<BuddyPartnership> pendingRequests = partnershipRepository\.findPendingPartnershipsByUserId\(userUuid\);',
        'List<BuddyPartnership> pendingRequests = partnershipRepository.findPendingPartnershipsByUserId(userId);',
        content
    )

    # Fix mapToResponseDto method
    content = re.sub(
        r'\.user1Id\(partnership\.getUser1Id\(\)\.toString\(\)\)',
        '.user1Id(partnership.getUser1Id())',
        content
    )
    content = re.sub(
        r'\.user2Id\(partnership\.getUser2Id\(\)\.toString\(\)\)',
        '.user2Id(partnership.getUser2Id())',
        content
    )

    # Fix notification service calls
    content = re.sub(
        r'notificationService\.notify\(\s*partnership\.getUser1Id\(\)\.toString\(\),',
        'notificationService.notify(\n                    partnership.getUser1Id(),',
        content
    )
    content = re.sub(
        r'notificationService\.notify\(\s*partnership\.getUser2Id\(\)\.toString\(\),',
        'notificationService.notify(\n                    partnership.getUser2Id(),',
        content
    )

    # Remove all remaining UUID.fromString() conversions for simple variables
    content = re.sub(
        r'UUID \w+Uuid = UUID\.fromString\(\w+\);?\n',
        '',
        content
    )

    # Fix any remaining references to UUID variables
    content = re.sub(
        r'(\w+)Uuid(?:\.toString\(\))?',
        r'\1',
        content,
        flags=re.IGNORECASE
    )

    with open('src/main/java/com/focushive/buddy/service/BuddyPartnershipService.java', 'w') as f:
        f.write(content)

    print("Service file fixed!")

if __name__ == '__main__':
    fix_service_file()
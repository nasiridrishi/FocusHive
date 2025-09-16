#!/bin/bash

# Fix all compilation issues in BuddyPartnershipService.java

FILE="src/main/java/com/focushive/buddy/service/BuddyPartnershipService.java"

# Fix variable name issues
sed -i '' 's/partnershipRepository\.findAllPartnershipsByUserId(user)/partnershipRepository.findAllPartnershipsByUserId(userId)/g' "$FILE"

# Fix missing try statement for line 860
sed -i '' '860s/List<BuddyPartnership> allPartnerships/            List<BuddyPartnership> allPartnerships/' "$FILE"

# Remove orphaned return statement at line 985
sed -i '' '985,987d' "$FILE"

# Remove orphaned catch blocks at lines 988-990
sed -i '' '988,990d' "$FILE"

# Remove orphaned catch blocks at lines 1046-1053
sed -i '' '1046,1053d' "$FILE"

# Fix partnership.getPartnerIdFor calls
sed -i '' 's/partnership\.getPartnerIdFor(approver)/partnership.getPartnerIdFor(approverId)/g' "$FILE"
sed -i '' 's/partnership\.getPartnerIdFor(rejector)/partnership.getPartnerIdFor(rejectorId)/g' "$FILE"
sed -i '' 's/partnership\.getPartnerIdFor(requester)/partnership.getPartnerIdFor(requesterId)/g' "$FILE"
sed -i '' 's/partnership\.getPartnerIdFor(responder)/partnership.getPartnerIdFor(responderId)/g' "$FILE"

echo "Fixed service file"
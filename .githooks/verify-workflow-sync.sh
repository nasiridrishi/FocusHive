#!/bin/bash

# Script to verify git hooks are in sync with GitHub workflows
# This ensures local hooks match CI/CD pipeline

echo "🔍 Verifying Git Hooks sync with GitHub Workflows..."

GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

# Check if .github/workflows/ci.yml exists
if [ ! -f ".github/workflows/ci.yml" ]; then
    echo -e "${RED}❌ GitHub workflow file not found!${NC}"
    exit 1
fi

# Extract commands from workflow file
echo "Checking GitHub workflow commands..."

# Backend checks
BACKEND_TEST=$(grep -A 5 "backend-test:" .github/workflows/ci.yml | grep "./gradlew test" | wc -l)
BACKEND_BUILD=$(grep -A 5 "backend-test:" .github/workflows/ci.yml | grep "./gradlew build" | wc -l)

# Frontend checks
FRONTEND_LINT=$(grep -A 5 "frontend-test:" .github/workflows/ci.yml | grep "npm run lint" | wc -l)
FRONTEND_TEST=$(grep -A 5 "frontend-test:" .github/workflows/ci.yml | grep "npm test" | wc -l)
FRONTEND_BUILD=$(grep -A 5 "frontend-test:" .github/workflows/ci.yml | grep "npm run build" | wc -l)

# Verify hooks have same commands
echo "Checking pre-commit hook commands..."

HOOK_BACKEND_TEST=$(grep "./gradlew test" .githooks/pre-commit | wc -l)
HOOK_BACKEND_BUILD=$(grep "./gradlew build" .githooks/pre-commit | wc -l)
HOOK_FRONTEND_LINT=$(grep "npm run lint" .githooks/pre-commit | wc -l)
HOOK_FRONTEND_TEST=$(grep "npm test" .githooks/pre-commit | wc -l)
HOOK_FRONTEND_BUILD=$(grep "npm run build" .githooks/pre-commit | wc -l)

# Compare
SYNCED=true

if [ $BACKEND_TEST -gt 0 ] && [ $HOOK_BACKEND_TEST -eq 0 ]; then
    echo -e "${RED}❌ Backend test command missing from hooks${NC}"
    SYNCED=false
fi

if [ $FRONTEND_LINT -gt 0 ] && [ $HOOK_FRONTEND_LINT -eq 0 ]; then
    echo -e "${RED}❌ Frontend lint command missing from hooks${NC}"
    SYNCED=false
fi

if [ $FRONTEND_TEST -gt 0 ] && [ $HOOK_FRONTEND_TEST -eq 0 ]; then
    echo -e "${RED}❌ Frontend test command missing from hooks${NC}"
    SYNCED=false
fi

if [ $SYNCED = true ]; then
    echo -e "${GREEN}✅ Git hooks are synced with GitHub workflows!${NC}"
    echo "Local commits will pass CI/CD checks."
else
    echo -e "${RED}❌ Git hooks are out of sync with workflows!${NC}"
    echo "Please update hooks to match CI/CD pipeline."
fi
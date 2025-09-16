#!/bin/bash

# Script to run GitHub Actions locally using act on remote server
# This syncs the current code to the remote server and runs act

set -e

REMOTE_HOST="nasir@192.168.1.7"
REMOTE_PROJECT_DIR="/home/nasir/uol/focushive/services/notification-service"
LOCAL_PROJECT_DIR="$(pwd)"

echo "🔄 Syncing project files to remote server..."

# Create the remote directory if it doesn't exist
ssh "$REMOTE_HOST" "mkdir -p $REMOTE_PROJECT_DIR"

# Sync the current project to remote server
rsync -avz --exclude='.git' --exclude='build/' --exclude='.gradle/' \
      "$LOCAL_PROJECT_DIR/" "$REMOTE_HOST:$REMOTE_PROJECT_DIR/"

echo "✅ Files synced successfully"

echo "🚀 Running GitHub Actions workflow with act..."

# Run act on the remote server
ssh -t "$REMOTE_HOST" "export PATH=\$PATH:\$HOME/bin && cd $REMOTE_PROJECT_DIR && act -j test-and-build --container-architecture linux/amd64 --pull=false"

echo "✅ Workflow execution completed"
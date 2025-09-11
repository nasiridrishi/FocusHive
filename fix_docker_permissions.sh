#!/bin/bash

# Docker Permissions Fix Script
# This script fixes Docker permission issues on Linux systems

set -e  # Exit on any error

echo "🐳 Docker Permissions Fix Script"
echo "================================="

# Check if running as root
if [[ $EUID -eq 0 ]]; then
   echo "❌ This script should NOT be run as root/sudo"
   echo "   Run it as your regular user account"
   exit 1
fi

# Get current username
USERNAME=$(whoami)
echo "👤 Current user: $USERNAME"

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    echo "❌ Docker is not installed or not in PATH"
    echo "   Please install Docker first"
    exit 1
fi

echo "✅ Docker is installed"

# Check if docker group exists
if ! getent group docker &> /dev/null; then
    echo "⚠️  Docker group doesn't exist. Creating it..."
    sudo groupadd docker
    echo "✅ Docker group created"
else
    echo "✅ Docker group exists"
fi

# Check if user is already in docker group
if groups $USERNAME | grep -q '\bdocker\b'; then
    echo "✅ User $USERNAME is already in docker group"
else
    echo "➕ Adding user $USERNAME to docker group..."
    sudo usermod -aG docker $USERNAME
    echo "✅ User added to docker group"
fi

# Set proper permissions on Docker socket (if needed)
if [[ -S /var/run/docker.sock ]]; then
    echo "🔧 Checking Docker socket permissions..."
    sudo chown root:docker /var/run/docker.sock
    sudo chmod 660 /var/run/docker.sock
    echo "✅ Docker socket permissions set correctly"
fi

# Test Docker access with current session
echo ""
echo "🧪 Testing Docker access..."
if sg docker -c "docker version --format '{{.Server.Version}}'" &> /dev/null; then
    echo "✅ Docker access working with docker group"
    echo ""
    echo "🎉 SUCCESS: Docker permissions are now fixed!"
    echo ""
    echo "📝 IMPORTANT: To use Docker commands in your current terminal,"
    echo "   you have two options:"
    echo ""
    echo "   Option 1 (Recommended): Restart your terminal session"
    echo "   - Close this terminal and open a new one"
    echo "   - Your docker group membership will be active"
    echo ""
    echo "   Option 2 (Immediate): Use 'sg docker -c' prefix"
    echo "   - Run: sg docker -c 'docker ps'"
    echo "   - Run: sg docker -c 'docker --version'"
    echo ""
    echo "   Option 3 (Fresh login shell): Run this command:"
    echo "   - exec su -l \$USER"
    echo ""
else
    echo "❌ Docker access test failed"
    echo "   There might be additional issues. Check:"
    echo "   1. Docker daemon is running: sudo systemctl status docker"
    echo "   2. Docker socket exists: ls -la /var/run/docker.sock"
    exit 1
fi

# Final verification
echo "🔍 Final verification:"
echo "   Current groups: $(groups)"
echo "   Docker group members: $(getent group docker)"
echo ""
echo "✨ Script completed successfully!"

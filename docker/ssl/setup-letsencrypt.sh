#!/bin/bash

# FocusHive Let's Encrypt SSL Certificate Setup Script
# This script sets up SSL certificates using Let's Encrypt

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
DOMAIN=${1:-focushive.com}
WWW_DOMAIN="www.${DOMAIN}"
EMAIL=${2:-admin@focushive.com}
STAGING=${3:-false}

# Function to print colored output
print_message() {
    echo -e "${2}${1}${NC}"
}

# Function to check prerequisites
check_prerequisites() {
    print_message "Checking prerequisites..." "$YELLOW"
    
    # Check if running as root or with sudo
    if [ "$EUID" -ne 0 ]; then 
        print_message "Please run as root or with sudo" "$RED"
        exit 1
    fi
    
    # Check if Docker is installed
    if ! command -v docker &> /dev/null; then
        print_message "Docker is not installed" "$RED"
        exit 1
    fi
    
    # Check if port 80 is available
    if lsof -Pi :80 -sTCP:LISTEN -t >/dev/null 2>&1; then
        print_message "Port 80 is already in use. Please stop any services using port 80." "$RED"
        print_message "You can check what's using port 80 with: sudo lsof -i :80" "$YELLOW"
        exit 1
    fi
    
    print_message "Prerequisites check passed!" "$GREEN"
}

# Function to validate domain
validate_domain() {
    print_message "Validating domain configuration..." "$YELLOW"
    
    # Check if domain resolves to this server
    SERVER_IP=$(curl -s https://api.ipify.org)
    DOMAIN_IP=$(dig +short $DOMAIN | tail -n1)
    
    if [ "$SERVER_IP" != "$DOMAIN_IP" ]; then
        print_message "⚠️  Warning: Domain $DOMAIN does not resolve to this server" "$YELLOW"
        print_message "Server IP: $SERVER_IP" "$YELLOW"
        print_message "Domain IP: $DOMAIN_IP" "$YELLOW"
        
        read -p "Continue anyway? (y/N): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            print_message "Setup cancelled" "$YELLOW"
            exit 1
        fi
    else
        print_message "Domain validation successful!" "$GREEN"
    fi
}

# Function to setup Let's Encrypt
setup_letsencrypt() {
    print_message "Setting up Let's Encrypt certificates..." "$YELLOW"
    
    # Create directories
    mkdir -p /etc/letsencrypt
    mkdir -p /var/lib/letsencrypt
    mkdir -p ./docker/nginx/ssl
    
    # Determine staging flag
    STAGING_FLAG=""
    if [ "$STAGING" = "true" ]; then
        STAGING_FLAG="--staging"
        print_message "Using Let's Encrypt staging environment (for testing)" "$YELLOW"
    fi
    
    # Run certbot
    print_message "Requesting certificate for $DOMAIN and $WWW_DOMAIN..." "$YELLOW"
    
    docker run -it --rm \
        -v /etc/letsencrypt:/etc/letsencrypt \
        -v /var/lib/letsencrypt:/var/lib/letsencrypt \
        -p 80:80 \
        certbot/certbot certonly \
        --standalone \
        --preferred-challenges http \
        --email $EMAIL \
        --agree-tos \
        --no-eff-email \
        --keep-until-expiring \
        --rsa-key-size 4096 \
        $STAGING_FLAG \
        -d $DOMAIN \
        -d $WWW_DOMAIN
    
    if [ $? -eq 0 ]; then
        print_message "Certificate obtained successfully!" "$GREEN"
    else
        print_message "Failed to obtain certificate" "$RED"
        exit 1
    fi
}

# Function to create symbolic links
create_symlinks() {
    print_message "Creating symbolic links..." "$YELLOW"
    
    # Create symbolic links to certificates
    ln -sf /etc/letsencrypt/live/$DOMAIN/fullchain.pem ./docker/nginx/ssl/fullchain.pem
    ln -sf /etc/letsencrypt/live/$DOMAIN/privkey.pem ./docker/nginx/ssl/privkey.pem
    ln -sf /etc/letsencrypt/live/$DOMAIN/chain.pem ./docker/nginx/ssl/chain.pem
    
    # Set proper permissions
    chmod 600 /etc/letsencrypt/live/$DOMAIN/privkey.pem
    chmod 644 /etc/letsencrypt/live/$DOMAIN/fullchain.pem
    chmod 644 /etc/letsencrypt/live/$DOMAIN/chain.pem
    
    print_message "Symbolic links created!" "$GREEN"
}

# Function to setup auto-renewal
setup_renewal() {
    print_message "Setting up auto-renewal..." "$YELLOW"
    
    # Create renewal script
    cat > /etc/letsencrypt/renewal-hooks/deploy/focushive-reload.sh << 'EOF'
#!/bin/bash
# Reload nginx after certificate renewal
docker exec focushive-nginx nginx -s reload 2>/dev/null || true
EOF
    
    chmod +x /etc/letsencrypt/renewal-hooks/deploy/focushive-reload.sh
    
    # Add cron job for renewal
    CRON_JOB="0 0,12 * * * /usr/bin/docker run --rm -v /etc/letsencrypt:/etc/letsencrypt certbot/certbot renew --quiet"
    
    # Check if cron job already exists
    if ! crontab -l 2>/dev/null | grep -q "certbot renew"; then
        (crontab -l 2>/dev/null; echo "$CRON_JOB") | crontab -
        print_message "Auto-renewal cron job added!" "$GREEN"
    else
        print_message "Auto-renewal cron job already exists" "$YELLOW"
    fi
    
    # Test renewal
    print_message "Testing renewal configuration..." "$YELLOW"
    docker run --rm \
        -v /etc/letsencrypt:/etc/letsencrypt \
        certbot/certbot renew --dry-run
    
    if [ $? -eq 0 ]; then
        print_message "Renewal test successful!" "$GREEN"
    else
        print_message "Renewal test failed - please check configuration" "$RED"
    fi
}

# Function to generate Diffie-Hellman parameters
generate_dhparam() {
    print_message "Generating Diffie-Hellman parameters (this may take a while)..." "$YELLOW"
    
    if [ ! -f ./docker/nginx/ssl/dhparam.pem ]; then
        openssl dhparam -out ./docker/nginx/ssl/dhparam.pem 2048
        print_message "DH parameters generated!" "$GREEN"
    else
        print_message "DH parameters already exist" "$YELLOW"
    fi
}

# Function to show next steps
show_next_steps() {
    print_message "\n✅ SSL Setup Complete!" "$GREEN"
    print_message "================================" "$GREEN"
    
    echo "Certificate details:"
    echo "  Domain: $DOMAIN, $WWW_DOMAIN"
    echo "  Email: $EMAIL"
    echo "  Certificate path: /etc/letsencrypt/live/$DOMAIN/"
    echo ""
    
    print_message "Next steps:" "$YELLOW"
    echo "1. Update your .env file with:"
    echo "   SSL_CERT_PATH=/etc/letsencrypt/live/$DOMAIN/fullchain.pem"
    echo "   SSL_KEY_PATH=/etc/letsencrypt/live/$DOMAIN/privkey.pem"
    echo "   SSL_CHAIN_PATH=/etc/letsencrypt/live/$DOMAIN/chain.pem"
    echo ""
    echo "2. Start FocusHive with SSL:"
    echo "   docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d"
    echo ""
    echo "3. Test your SSL configuration:"
    echo "   https://www.ssllabs.com/ssltest/analyze.html?d=$DOMAIN"
    echo ""
    
    print_message "Auto-renewal is configured and will run twice daily." "$GREEN"
}

# Main execution
main() {
    print_message "🔒 FocusHive Let's Encrypt SSL Setup" "$GREEN"
    print_message "=====================================" "$GREEN"
    
    echo "Domain: $DOMAIN"
    echo "Email: $EMAIL"
    echo "Staging: $STAGING"
    echo ""
    
    read -p "Continue with these settings? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        print_message "Setup cancelled" "$YELLOW"
        exit 0
    fi
    
    check_prerequisites
    validate_domain
    setup_letsencrypt
    create_symlinks
    generate_dhparam
    setup_renewal
    show_next_steps
}

# Show usage if --help is provided
if [ "$1" = "--help" ] || [ "$1" = "-h" ]; then
    echo "Usage: $0 [domain] [email] [staging]"
    echo ""
    echo "Arguments:"
    echo "  domain   - Your domain name (default: focushive.com)"
    echo "  email    - Email for Let's Encrypt notifications (default: admin@focushive.com)"
    echo "  staging  - Use staging environment for testing (true/false, default: false)"
    echo ""
    echo "Examples:"
    echo "  $0                                    # Use defaults"
    echo "  $0 mydomain.com admin@mydomain.com   # Custom domain and email"
    echo "  $0 test.com test@test.com true       # Use staging for testing"
    exit 0
fi

# Run main function
main
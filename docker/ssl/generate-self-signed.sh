#!/bin/bash

# FocusHive Self-Signed Certificate Generator
# For development and testing purposes only

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
DOMAIN=${1:-localhost}
DAYS=${2:-365}
SSL_DIR="./docker/nginx/ssl"

# Function to print colored output
print_message() {
    echo -e "${2}${1}${NC}"
}

# Function to generate certificate
generate_certificate() {
    print_message "Generating self-signed certificate for $DOMAIN..." "$YELLOW"
    
    # Create SSL directory if it doesn't exist
    mkdir -p $SSL_DIR
    
    # Generate private key
    print_message "Generating private key..." "$YELLOW"
    openssl genrsa -out $SSL_DIR/privkey.pem 2048
    
    # Generate certificate signing request
    print_message "Generating certificate signing request..." "$YELLOW"
    openssl req -new \
        -key $SSL_DIR/privkey.pem \
        -out $SSL_DIR/cert.csr \
        -subj "/C=US/ST=State/L=City/O=FocusHive/OU=Development/CN=$DOMAIN"
    
    # Generate self-signed certificate
    print_message "Generating self-signed certificate..." "$YELLOW"
    openssl x509 -req \
        -days $DAYS \
        -in $SSL_DIR/cert.csr \
        -signkey $SSL_DIR/privkey.pem \
        -out $SSL_DIR/fullchain.pem \
        -extensions v3_ca \
        -extfile <(cat <<EOF
[v3_ca]
subjectAltName = @alt_names
[alt_names]
DNS.1 = $DOMAIN
DNS.2 = *.$DOMAIN
DNS.3 = localhost
IP.1 = 127.0.0.1
IP.2 = ::1
EOF
    )
    
    # Copy certificate as chain (for compatibility)
    cp $SSL_DIR/fullchain.pem $SSL_DIR/chain.pem
    
    # Clean up CSR
    rm $SSL_DIR/cert.csr
    
    # Set proper permissions
    chmod 600 $SSL_DIR/privkey.pem
    chmod 644 $SSL_DIR/fullchain.pem
    chmod 644 $SSL_DIR/chain.pem
    
    print_message "Certificate generated successfully!" "$GREEN"
}

# Function to show certificate info
show_certificate_info() {
    print_message "\nCertificate Information:" "$YELLOW"
    openssl x509 -in $SSL_DIR/fullchain.pem -noout -text | grep -A 2 "Subject:\|DNS:\|Validity"
}

# Function to create trust script
create_trust_script() {
    cat > $SSL_DIR/trust-certificate.sh << 'EOF'
#!/bin/bash

# Script to trust the self-signed certificate on various systems

CERT_FILE="./fullchain.pem"

if [ ! -f "$CERT_FILE" ]; then
    echo "Certificate file not found: $CERT_FILE"
    exit 1
fi

# Detect OS
if [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS
    echo "Adding certificate to macOS keychain..."
    sudo security add-trusted-cert -d -r trustRoot -k /Library/Keychains/System.keychain "$CERT_FILE"
    echo "Certificate added to macOS keychain"
    
elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
    # Linux
    if [ -d "/usr/local/share/ca-certificates" ]; then
        # Ubuntu/Debian
        echo "Adding certificate to Ubuntu/Debian..."
        sudo cp "$CERT_FILE" /usr/local/share/ca-certificates/focushive-dev.crt
        sudo update-ca-certificates
        echo "Certificate added to system trust store"
        
    elif [ -d "/etc/pki/ca-trust/source/anchors" ]; then
        # RHEL/CentOS/Fedora
        echo "Adding certificate to RHEL/CentOS/Fedora..."
        sudo cp "$CERT_FILE" /etc/pki/ca-trust/source/anchors/focushive-dev.crt
        sudo update-ca-trust
        echo "Certificate added to system trust store"
    else
        echo "Unknown Linux distribution"
        echo "Please manually add the certificate to your system's trust store"
    fi
    
elif [[ "$OSTYPE" == "msys" ]] || [[ "$OSTYPE" == "cygwin" ]]; then
    # Windows
    echo "For Windows, import the certificate manually:"
    echo "1. Open 'certmgr.msc'"
    echo "2. Navigate to 'Trusted Root Certification Authorities'"
    echo "3. Right-click 'Certificates' and select 'All Tasks' > 'Import'"
    echo "4. Import the file: $CERT_FILE"
else
    echo "Unknown operating system: $OSTYPE"
    echo "Please manually add the certificate to your system's trust store"
fi

# Add to Chrome/Chromium (Linux)
if command -v google-chrome &> /dev/null || command -v chromium &> /dev/null; then
    echo ""
    echo "For Chrome/Chromium browsers:"
    echo "1. Navigate to chrome://settings/certificates"
    echo "2. Click 'Authorities' tab"
    echo "3. Click 'Import' and select: $CERT_FILE"
    echo "4. Check 'Trust this certificate for identifying websites'"
fi

# Add to Firefox
echo ""
echo "For Firefox:"
echo "1. Navigate to about:preferences#privacy"
echo "2. Scroll to 'Certificates' and click 'View Certificates'"
echo "3. Click 'Authorities' tab"
echo "4. Click 'Import' and select: $CERT_FILE"
echo "5. Check 'Trust this CA to identify websites'"
EOF
    
    chmod +x $SSL_DIR/trust-certificate.sh
    print_message "Trust script created: $SSL_DIR/trust-certificate.sh" "$GREEN"
}

# Function to show next steps
show_next_steps() {
    print_message "\n✅ Self-Signed Certificate Generated!" "$GREEN"
    print_message "======================================" "$GREEN"
    
    echo "Certificate files:"
    echo "  Private Key: $SSL_DIR/privkey.pem"
    echo "  Certificate: $SSL_DIR/fullchain.pem"
    echo "  Chain: $SSL_DIR/chain.pem"
    echo ""
    
    print_message "⚠️  WARNING: This is a self-signed certificate!" "$YELLOW"
    print_message "Use only for development and testing." "$YELLOW"
    echo ""
    
    print_message "To trust this certificate on your system:" "$YELLOW"
    echo "  cd $SSL_DIR && ./trust-certificate.sh"
    echo ""
    
    print_message "To use with Docker:" "$YELLOW"
    echo "  docker-compose up -d"
    echo ""
    
    print_message "Access your application at:" "$GREEN"
    echo "  https://$DOMAIN"
    echo "  https://localhost"
    echo ""
    
    print_message "Note: Browsers will show a security warning." "$YELLOW"
    print_message "You can proceed by clicking 'Advanced' > 'Proceed to $DOMAIN (unsafe)'" "$YELLOW"
}

# Main execution
main() {
    print_message "🔐 FocusHive Self-Signed Certificate Generator" "$GREEN"
    print_message "===============================================" "$GREEN"
    print_message "⚠️  FOR DEVELOPMENT USE ONLY" "$RED"
    echo ""
    
    echo "Domain: $DOMAIN"
    echo "Validity: $DAYS days"
    echo "Output: $SSL_DIR"
    echo ""
    
    read -p "Generate self-signed certificate? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        print_message "Certificate generation cancelled" "$YELLOW"
        exit 0
    fi
    
    generate_certificate
    show_certificate_info
    create_trust_script
    show_next_steps
}

# Show usage if --help is provided
if [ "$1" = "--help" ] || [ "$1" = "-h" ]; then
    echo "Usage: $0 [domain] [days]"
    echo ""
    echo "Arguments:"
    echo "  domain - Domain name for the certificate (default: localhost)"
    echo "  days   - Certificate validity in days (default: 365)"
    echo ""
    echo "Examples:"
    echo "  $0                    # Generate for localhost, valid for 365 days"
    echo "  $0 dev.focushive.com  # Generate for custom domain"
    echo "  $0 localhost 730      # Generate for localhost, valid for 2 years"
    exit 0
fi

# Run main function
main
# SSL/TLS Certificate Setup for FocusHive

This guide covers setting up SSL/TLS certificates for secure HTTPS connections in production.

## Options for SSL Certificates

### 1. Let's Encrypt (Recommended for Production)

Free, automated SSL certificates that renew automatically.

#### Setup with Certbot

```bash
# Run the setup script
./docker/ssl/setup-letsencrypt.sh

# Or manually:
docker run -it --rm \
  -v /etc/letsencrypt:/etc/letsencrypt \
  -v /var/lib/letsencrypt:/var/lib/letsencrypt \
  -p 80:80 \
  certbot/certbot certonly \
  --standalone \
  --email admin@focushive.com \
  --agree-tos \
  --no-eff-email \
  -d focushive.com \
  -d www.focushive.com
```

#### Auto-renewal

Add to crontab:
```bash
0 0 * * * /usr/bin/docker run --rm -v /etc/letsencrypt:/etc/letsencrypt certbot/certbot renew --quiet
```

### 2. Self-Signed Certificates (Development Only)

For local development and testing:

```bash
# Generate self-signed certificate
./docker/ssl/generate-self-signed.sh
```

### 3. Commercial SSL Certificate

If you have purchased an SSL certificate:

1. Place your certificate files in the appropriate directories:
   - Certificate: `docker/nginx/ssl/fullchain.pem`
   - Private Key: `docker/nginx/ssl/privkey.pem`
   - CA Chain: `docker/nginx/ssl/chain.pem`

2. Ensure proper permissions:
```bash
chmod 600 docker/nginx/ssl/privkey.pem
chmod 644 docker/nginx/ssl/fullchain.pem
chmod 644 docker/nginx/ssl/chain.pem
```

## Directory Structure

```
docker/
├── nginx/
│   └── ssl/
│       ├── fullchain.pem    # Full certificate chain
│       ├── privkey.pem       # Private key
│       └── chain.pem         # CA chain certificate
└── ssl/
    ├── README.md             # This file
    ├── setup-letsencrypt.sh  # Let's Encrypt setup script
    └── generate-self-signed.sh # Self-signed cert generator
```

## Nginx Configuration

The production Nginx configuration (`docker/nginx/nginx.prod.conf`) is already configured to:
- Redirect HTTP to HTTPS
- Use strong SSL ciphers
- Enable HSTS (HTTP Strict Transport Security)
- Enable OCSP stapling
- Support HTTP/2

## Security Best Practices

1. **Strong Ciphers**: Only TLS 1.2 and 1.3 are enabled
2. **HSTS**: Forces browsers to use HTTPS for 1 year
3. **OCSP Stapling**: Improves SSL handshake performance
4. **Certificate Pinning**: Consider implementing for mobile apps
5. **Regular Updates**: Keep certificates renewed before expiry

## Testing SSL Configuration

After setup, test your SSL configuration:

```bash
# Test with SSL Labs (production)
# Visit: https://www.ssllabs.com/ssltest/analyze.html?d=focushive.com

# Test locally with OpenSSL
openssl s_client -connect focushive.com:443 -tls1_2

# Test certificate expiry
echo | openssl s_client -connect focushive.com:443 2>/dev/null | openssl x509 -noout -dates
```

## Troubleshooting

### Certificate Not Trusted
- Ensure the full certificate chain is included
- Check certificate validity dates
- Verify domain names match

### Mixed Content Warnings
- Ensure all resources are loaded over HTTPS
- Update frontend API URLs to use HTTPS

### Certificate Renewal Failed
- Check port 80 is accessible for Let's Encrypt validation
- Ensure DNS is properly configured
- Check certbot logs: `/var/log/letsencrypt/letsencrypt.log`

## Environment Variables

Set these in your `.env` file:

```env
# SSL Configuration
SSL_CERT_PATH=/etc/letsencrypt/live/focushive.com/fullchain.pem
SSL_KEY_PATH=/etc/letsencrypt/live/focushive.com/privkey.pem
SSL_CHAIN_PATH=/etc/letsencrypt/live/focushive.com/chain.pem

# HTTPS URLs for production
PRODUCTION_API_URL=https://api.focushive.com
PRODUCTION_WS_URL=wss://api.focushive.com
```

## Docker Compose SSL Volume

For production deployment with Let's Encrypt:

```yaml
services:
  nginx:
    volumes:
      - /etc/letsencrypt:/etc/letsencrypt:ro
      - /var/lib/letsencrypt:/var/lib/letsencrypt:ro
```
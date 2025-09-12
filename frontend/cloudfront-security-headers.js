// CloudFront Function for Security Headers
// This function runs on CloudFront Edge locations to add security headers

function handler(event) {
    var response = event.response;
    var headers = response.headers;
    
    // Strict Transport Security (HSTS)
    headers['strict-transport-security'] = {
        value: 'max-age=31536000; includeSubDomains; preload'
    };
    
    // Prevent clickjacking
    headers['x-frame-options'] = {
        value: 'DENY'
    };
    
    // Prevent MIME type sniffing
    headers['x-content-type-options'] = {
        value: 'nosniff'
    };
    
    // XSS Protection
    headers['x-xss-protection'] = {
        value: '1; mode=block'
    };
    
    // Referrer Policy
    headers['referrer-policy'] = {
        value: 'strict-origin-when-cross-origin'
    };
    
    // Cross-Origin Policies
    headers['cross-origin-opener-policy'] = {
        value: 'same-origin'
    };
    
    headers['cross-origin-embedder-policy'] = {
        value: 'credentialless'
    };
    
    headers['cross-origin-resource-policy'] = {
        value: 'same-origin'
    };
    
    // Permissions Policy
    headers['permissions-policy'] = {
        value: 'geolocation=(), microphone=(self), camera=(self), payment=(), usb=(), magnetometer=(), gyroscope=(), accelerometer=(), ambient-light-sensor=(), autoplay=(self), encrypted-media=(self), fullscreen=(self), picture-in-picture=(self), screen-wake-lock=(self), web-share=(self)'
    };
    
    // Content Security Policy
    headers['content-security-policy'] = {
        value: "default-src 'self'; script-src 'self' 'unsafe-inline' https://www.spotify.com; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; font-src 'self' https://fonts.gstatic.com data:; img-src 'self' data: https: blob:; media-src 'self' data: https: blob:; connect-src 'self' wss: ws: https://api.spotify.com; worker-src 'self' blob:; child-src 'self'; frame-src 'none'; object-src 'none'; base-uri 'self'; form-action 'self'; frame-ancestors 'none'"
    };
    
    // Cache control based on content type
    var request = event.request;
    var uri = request.uri;
    
    // Static assets - allow long-term caching
    if (uri.match(/\.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$/)) {
        headers['cache-control'] = {
            value: 'public, max-age=31536000, immutable'
        };
    }
    // Service worker - no caching
    else if (uri === '/sw.js') {
        headers['cache-control'] = {
            value: 'no-cache, no-store, must-revalidate'
        };
        headers['pragma'] = {
            value: 'no-cache'
        };
        headers['expires'] = {
            value: '0'
        };
    }
    // Manifest file
    else if (uri === '/manifest.webmanifest') {
        headers['content-type'] = {
            value: 'application/manifest+json'
        };
        headers['cache-control'] = {
            value: 'public, max-age=86400'
        };
    }
    // HTML files and default
    else {
        headers['cache-control'] = {
            value: 'no-cache, no-store, must-revalidate'
        };
        headers['pragma'] = {
            value: 'no-cache'
        };
        headers['expires'] = {
            value: '0'
        };
    }
    
    return response;
}
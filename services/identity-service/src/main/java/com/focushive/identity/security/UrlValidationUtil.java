package com.focushive.identity.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * URL Validation Utility for OWASP A10: Server-Side Request Forgery (SSRF) Prevention.
 * Validates URLs to prevent SSRF attacks and open redirects.
 */
@Component
@Slf4j
public class UrlValidationUtil {

    // Allowed schemes for URLs
    private static final List<String> ALLOWED_SCHEMES = Arrays.asList("http", "https");

    // Blocked internal/private IP ranges (CIDR notation)
    private static final List<String> BLOCKED_IP_RANGES = Arrays.asList(
        "127.0.0.0/8",      // Loopback
        "10.0.0.0/8",       // Private Class A
        "172.16.0.0/12",    // Private Class B
        "192.168.0.0/16",   // Private Class C
        "169.254.0.0/16",   // Link-local
        "0.0.0.0/8",        // Invalid/reserved
        "224.0.0.0/4",      // Multicast
        "240.0.0.0/4"       // Reserved
    );

    // Common cloud metadata service endpoints
    private static final List<String> BLOCKED_HOSTNAMES = Arrays.asList(
        "metadata.google.internal",        // Google Cloud
        "169.254.169.254",                 // AWS, Azure, Google metadata
        "metadata",                        // Generic metadata
        "instance-data",                   // AWS instance data
        "compute.internal",                // Google internal
        "consul",                          // Consul service discovery
        "vault",                           // HashiCorp Vault
        "etcd"                            // etcd key-value store
    );

    // Pattern for detecting protocol-relative URLs
    private static final Pattern PROTOCOL_RELATIVE_PATTERN = Pattern.compile("^//.*");

    // Pattern for detecting data URLs
    private static final Pattern DATA_URL_PATTERN = Pattern.compile("^data:.*", Pattern.CASE_INSENSITIVE);

    // Pattern for detecting file URLs
    private static final Pattern FILE_URL_PATTERN = Pattern.compile("^file:.*", Pattern.CASE_INSENSITIVE);

    // Allowed redirect domains (should be configurable in production)
    private static final List<String> ALLOWED_REDIRECT_DOMAINS = Arrays.asList(
        "localhost",
        "focushive.com",
        "127.0.0.1"
    );

    /**
     * A10: Validate URL to prevent SSRF attacks.
     *
     * @param urlString The URL to validate
     * @return true if URL is safe, false otherwise
     */
    public boolean isUrlSafe(String urlString) {
        if (urlString == null || urlString.trim().isEmpty()) {
            log.warn("SSRF Prevention: Empty or null URL provided");
            return false;
        }

        urlString = urlString.trim();

        // Reject protocol-relative URLs
        if (PROTOCOL_RELATIVE_PATTERN.matcher(urlString).matches()) {
            log.warn("SSRF Prevention: Protocol-relative URL rejected: {}", sanitizeUrl(urlString));
            return false;
        }

        // Reject data URLs
        if (DATA_URL_PATTERN.matcher(urlString).matches()) {
            log.warn("SSRF Prevention: Data URL rejected: {}", sanitizeUrl(urlString));
            return false;
        }

        // Reject file URLs
        if (FILE_URL_PATTERN.matcher(urlString).matches()) {
            log.warn("SSRF Prevention: File URL rejected: {}", sanitizeUrl(urlString));
            return false;
        }

        try {
            URL url = new URL(urlString);

            // Check scheme
            if (!ALLOWED_SCHEMES.contains(url.getProtocol().toLowerCase())) {
                log.warn("SSRF Prevention: Disallowed scheme '{}' in URL: {}",
                    url.getProtocol(), sanitizeUrl(urlString));
                return false;
            }

            // Check hostname
            String hostname = url.getHost();
            if (hostname == null || hostname.trim().isEmpty()) {
                log.warn("SSRF Prevention: Empty hostname in URL: {}", sanitizeUrl(urlString));
                return false;
            }

            // Check for blocked hostnames
            if (isHostnameBlocked(hostname)) {
                log.warn("SSRF Prevention: Blocked hostname '{}' in URL: {}",
                    hostname, sanitizeUrl(urlString));
                return false;
            }

            // Resolve IP address and check if it's in blocked ranges
            if (isIpAddressBlocked(hostname)) {
                log.warn("SSRF Prevention: Blocked IP address '{}' in URL: {}",
                    hostname, sanitizeUrl(urlString));
                return false;
            }

            // Check port (reject common internal service ports)
            int port = url.getPort();
            if (port != -1 && isPortBlocked(port)) {
                log.warn("SSRF Prevention: Blocked port '{}' in URL: {}",
                    port, sanitizeUrl(urlString));
                return false;
            }

            log.debug("SSRF Prevention: URL validation passed for: {}", sanitizeUrl(urlString));
            return true;

        } catch (MalformedURLException e) {
            log.warn("SSRF Prevention: Malformed URL: {}, error: {}",
                sanitizeUrl(urlString), e.getMessage());
            return false;
        }
    }

    /**
     * A10: Validate redirect URL to prevent open redirect attacks.
     *
     * @param redirectUrl The redirect URL to validate
     * @return true if redirect is safe, false otherwise
     */
    public boolean isRedirectSafe(String redirectUrl) {
        if (!isUrlSafe(redirectUrl)) {
            return false;
        }

        try {
            URL url = new URL(redirectUrl);
            String hostname = url.getHost();

            // Check if redirect domain is in allowed list
            boolean isAllowed = ALLOWED_REDIRECT_DOMAINS.stream()
                .anyMatch(domain -> hostname.equals(domain) || hostname.endsWith("." + domain));

            if (!isAllowed) {
                log.warn("Open Redirect Prevention: Disallowed redirect domain '{}' in URL: {}",
                    hostname, sanitizeUrl(redirectUrl));
                return false;
            }

            log.debug("Open Redirect Prevention: Redirect validation passed for: {}",
                sanitizeUrl(redirectUrl));
            return true;

        } catch (MalformedURLException e) {
            log.warn("Open Redirect Prevention: Malformed redirect URL: {}, error: {}",
                sanitizeUrl(redirectUrl), e.getMessage());
            return false;
        }
    }

    /**
     * Check if hostname is in the blocked list.
     */
    private boolean isHostnameBlocked(String hostname) {
        return BLOCKED_HOSTNAMES.stream()
            .anyMatch(blocked -> hostname.equalsIgnoreCase(blocked));
    }

    /**
     * Check if IP address (resolved from hostname) is in blocked ranges.
     */
    private boolean isIpAddressBlocked(String hostname) {
        try {
            InetAddress address = InetAddress.getByName(hostname);
            String ip = address.getHostAddress();

            // Check against blocked IP ranges
            for (String range : BLOCKED_IP_RANGES) {
                if (isIpInRange(ip, range)) {
                    return true;
                }
            }

            return false;

        } catch (UnknownHostException e) {
            log.warn("SSRF Prevention: Could not resolve hostname '{}': {}", hostname, e.getMessage());
            // If we can't resolve it, consider it blocked for safety
            return true;
        }
    }

    /**
     * Check if an IP address is within a CIDR range.
     */
    private boolean isIpInRange(String ip, String cidr) {
        try {
            String[] parts = cidr.split("/");
            InetAddress targetAddr = InetAddress.getByName(ip);
            InetAddress rangeAddr = InetAddress.getByName(parts[0]);
            int prefixLength = Integer.parseInt(parts[1]);

            byte[] targetBytes = targetAddr.getAddress();
            byte[] rangeBytes = rangeAddr.getAddress();

            if (targetBytes.length != rangeBytes.length) {
                return false;
            }

            int bytesToCheck = prefixLength / 8;
            int bitsToCheck = prefixLength % 8;

            // Check full bytes
            for (int i = 0; i < bytesToCheck; i++) {
                if (targetBytes[i] != rangeBytes[i]) {
                    return false;
                }
            }

            // Check remaining bits
            if (bitsToCheck > 0 && bytesToCheck < targetBytes.length) {
                int mask = 0xFF << (8 - bitsToCheck);
                return (targetBytes[bytesToCheck] & mask) == (rangeBytes[bytesToCheck] & mask);
            }

            return true;

        } catch (Exception e) {
            log.warn("SSRF Prevention: Error checking IP range for '{}' in '{}': {}",
                ip, cidr, e.getMessage());
            return false;
        }
    }

    /**
     * Check if port is blocked (common internal service ports).
     */
    private boolean isPortBlocked(int port) {
        // Block common internal service ports
        int[] blockedPorts = {
            22,     // SSH
            23,     // Telnet
            25,     // SMTP
            53,     // DNS
            110,    // POP3
            143,    // IMAP
            993,    // IMAPS
            995,    // POP3S
            1433,   // SQL Server
            1521,   // Oracle DB
            3306,   // MySQL
            3389,   // RDP
            5432,   // PostgreSQL
            5984,   // CouchDB
            6379,   // Redis
            8086,   // InfluxDB
            9042,   // Cassandra
            9200,   // Elasticsearch
            11211,  // Memcached
            27017,  // MongoDB
            50070   // Hadoop
        };

        return Arrays.stream(blockedPorts).anyMatch(blocked -> blocked == port);
    }

    /**
     * Sanitize URL for logging to prevent log injection.
     */
    private String sanitizeUrl(String url) {
        if (url == null) {
            return "null";
        }
        // Remove newlines and limit length
        String sanitized = url.replaceAll("[\r\n\t]", " ");
        if (sanitized.length() > 200) {
            sanitized = sanitized.substring(0, 200) + "...";
        }
        return sanitized;
    }
}
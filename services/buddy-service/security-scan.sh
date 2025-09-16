#!/bin/bash

# =============================================================================
# Security Scanning Script for Buddy Service Docker Image
# Performs comprehensive security analysis using multiple tools
# =============================================================================

set -euo pipefail

# Configuration
IMAGE_NAME="buddy-service"
IMAGE_TAG="latest"
FULL_IMAGE_NAME="${IMAGE_NAME}:${IMAGE_TAG}"
SCAN_RESULTS_DIR="./security-scan-results"
DATE_STAMP=$(date +"%Y%m%d_%H%M%S")

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Create results directory
mkdir -p "${SCAN_RESULTS_DIR}"

echo -e "${BLUE}==============================================================================${NC}"
echo -e "${BLUE}Security Scanning Suite for Buddy Service${NC}"
echo -e "${BLUE}Image: ${FULL_IMAGE_NAME}${NC}"
echo -e "${BLUE}Timestamp: ${DATE_STAMP}${NC}"
echo -e "${BLUE}==============================================================================${NC}"

# Function to check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Function to install trivy if not present
install_trivy() {
    if ! command_exists trivy; then
        echo -e "${YELLOW}Installing Trivy security scanner...${NC}"
        if [[ "$OSTYPE" == "darwin"* ]]; then
            brew install trivy
        elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
            curl -sfL https://raw.githubusercontent.com/aquasecurity/trivy/main/contrib/install.sh | sh -s -- -b /usr/local/bin
        else
            echo -e "${RED}Please install Trivy manually: https://aquasecurity.github.io/trivy/latest/getting-started/installation/${NC}"
            exit 1
        fi
    fi
}

# Function to install hadolint if not present
install_hadolint() {
    if ! command_exists hadolint; then
        echo -e "${YELLOW}Installing Hadolint Dockerfile linter...${NC}"
        if [[ "$OSTYPE" == "darwin"* ]]; then
            brew install hadolint
        elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
            wget -O /usr/local/bin/hadolint https://github.com/hadolint/hadolint/releases/latest/download/hadolint-Linux-x86_64
            chmod +x /usr/local/bin/hadolint
        else
            echo -e "${RED}Please install Hadolint manually: https://github.com/hadolint/hadolint${NC}"
            exit 1
        fi
    fi
}

# Function to scan Dockerfile with Hadolint
scan_dockerfile() {
    echo -e "${BLUE}Scanning Dockerfile with Hadolint...${NC}"

    if command_exists hadolint; then
        hadolint Dockerfile --config .hadolint.yaml --format json > "${SCAN_RESULTS_DIR}/hadolint_${DATE_STAMP}.json" || true
        hadolint Dockerfile --config .hadolint.yaml > "${SCAN_RESULTS_DIR}/hadolint_${DATE_STAMP}.txt" || true

        # Check exit code for critical issues
        if hadolint Dockerfile --config .hadolint.yaml --failure-threshold error >/dev/null 2>&1; then
            echo -e "${GREEN}✅ Dockerfile security scan passed${NC}"
        else
            echo -e "${YELLOW}⚠️  Dockerfile has security warnings - check results${NC}"
        fi
    else
        echo -e "${YELLOW}Hadolint not available, skipping Dockerfile scan${NC}"
    fi
}

# Function to scan image with Trivy
scan_image_vulnerabilities() {
    echo -e "${BLUE}Scanning Docker image for vulnerabilities...${NC}"

    if command_exists trivy; then
        # Comprehensive vulnerability scan
        trivy image --format json --output "${SCAN_RESULTS_DIR}/trivy_vulnerabilities_${DATE_STAMP}.json" "${FULL_IMAGE_NAME}" || true
        trivy image --format table --output "${SCAN_RESULTS_DIR}/trivy_vulnerabilities_${DATE_STAMP}.txt" "${FULL_IMAGE_NAME}" || true

        # Security scan focusing on HIGH and CRITICAL
        trivy image --severity HIGH,CRITICAL --format table "${FULL_IMAGE_NAME}" > "${SCAN_RESULTS_DIR}/trivy_critical_${DATE_STAMP}.txt" || true

        # Configuration scan
        trivy config . --format json --output "${SCAN_RESULTS_DIR}/trivy_config_${DATE_STAMP}.json" || true
        trivy config . --format table --output "${SCAN_RESULTS_DIR}/trivy_config_${DATE_STAMP}.txt" || true

        # Count critical vulnerabilities
        CRITICAL_COUNT=$(trivy image --severity CRITICAL --format json "${FULL_IMAGE_NAME}" 2>/dev/null | jq '[.Results[]?.Vulnerabilities[]? | select(.Severity == "CRITICAL")] | length' || echo "0")
        HIGH_COUNT=$(trivy image --severity HIGH --format json "${FULL_IMAGE_NAME}" 2>/dev/null | jq '[.Results[]?.Vulnerabilities[]? | select(.Severity == "HIGH")] | length' || echo "0")

        echo -e "${BLUE}Vulnerability Summary:${NC}"
        echo -e "Critical: ${CRITICAL_COUNT}"
        echo -e "High: ${HIGH_COUNT}"

        if [ "${CRITICAL_COUNT}" -eq 0 ] && [ "${HIGH_COUNT}" -eq 0 ]; then
            echo -e "${GREEN}✅ No HIGH or CRITICAL vulnerabilities found${NC}"
            return 0
        else
            echo -e "${RED}❌ Found HIGH or CRITICAL vulnerabilities${NC}"
            return 1
        fi
    else
        echo -e "${YELLOW}Trivy not available, skipping vulnerability scan${NC}"
        return 1
    fi
}

# Function to check image size
check_image_size() {
    echo -e "${BLUE}Checking image size...${NC}"

    if docker image inspect "${FULL_IMAGE_NAME}" >/dev/null 2>&1; then
        IMAGE_SIZE_BYTES=$(docker image inspect "${FULL_IMAGE_NAME}" --format='{{.Size}}')
        IMAGE_SIZE_MB=$((IMAGE_SIZE_BYTES / 1024 / 1024))

        echo "Image size: ${IMAGE_SIZE_MB} MB" > "${SCAN_RESULTS_DIR}/image_size_${DATE_STAMP}.txt"
        echo -e "Image size: ${IMAGE_SIZE_MB} MB"

        if [ "${IMAGE_SIZE_MB}" -lt 200 ]; then
            echo -e "${GREEN}✅ Image size requirement met (<200MB)${NC}"
            return 0
        else
            echo -e "${RED}❌ Image size exceeds 200MB requirement${NC}"
            return 1
        fi
    else
        echo -e "${RED}❌ Image not found: ${FULL_IMAGE_NAME}${NC}"
        return 1
    fi
}

# Function to test container startup time
test_startup_time() {
    echo -e "${BLUE}Testing container startup time...${NC}"

    # Remove any existing test container
    docker rm -f buddy-service-test >/dev/null 2>&1 || true

    # Start timing
    START_TIME=$(date +%s)

    # Start container and wait for health check
    docker run -d --name buddy-service-test \
        -e SPRING_PROFILES_ACTIVE=docker \
        -e SPRING_DATASOURCE_URL=jdbc:h2:mem:testdb \
        -e SPRING_DATA_REDIS_HOST=localhost \
        -e SPRING_JPA_HIBERNATE_DDL_AUTO=create-drop \
        -e MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health \
        "${FULL_IMAGE_NAME}" >/dev/null

    # Wait for health check to pass (max 60 seconds)
    TIMEOUT=60
    ELAPSED=0
    while [ $ELAPSED -lt $TIMEOUT ]; do
        if docker exec buddy-service-test curl -f http://localhost:8087/actuator/health >/dev/null 2>&1; then
            END_TIME=$(date +%s)
            STARTUP_TIME=$((END_TIME - START_TIME))

            echo "Startup time: ${STARTUP_TIME} seconds" > "${SCAN_RESULTS_DIR}/startup_time_${DATE_STAMP}.txt"
            echo -e "Startup time: ${STARTUP_TIME} seconds"

            # Cleanup
            docker rm -f buddy-service-test >/dev/null 2>&1 || true

            if [ "${STARTUP_TIME}" -lt 30 ]; then
                echo -e "${GREEN}✅ Startup time requirement met (<30s)${NC}"
                return 0
            else
                echo -e "${RED}❌ Startup time exceeds 30s requirement${NC}"
                return 1
            fi
        fi

        sleep 2
        ELAPSED=$((ELAPSED + 2))
    done

    # Cleanup on timeout
    docker rm -f buddy-service-test >/dev/null 2>&1 || true
    echo -e "${RED}❌ Container failed to start within timeout${NC}"
    return 1
}

# Function to generate summary report
generate_summary_report() {
    echo -e "${BLUE}Generating summary report...${NC}"

    SUMMARY_FILE="${SCAN_RESULTS_DIR}/security_summary_${DATE_STAMP}.md"

    cat > "${SUMMARY_FILE}" << EOF
# Security Scan Summary Report

**Date:** $(date)
**Image:** ${FULL_IMAGE_NAME}

## Results Overview

### Dockerfile Security (Hadolint)
$(if [ -f "${SCAN_RESULTS_DIR}/hadolint_${DATE_STAMP}.txt" ]; then
    if grep -q "No issues found" "${SCAN_RESULTS_DIR}/hadolint_${DATE_STAMP}.txt" 2>/dev/null; then
        echo "✅ PASSED - No security issues found"
    else
        echo "⚠️ WARNINGS - See detailed report"
    fi
else
    echo "❓ NOT SCANNED"
fi)

### Vulnerability Assessment (Trivy)
$(if [ -f "${SCAN_RESULTS_DIR}/trivy_critical_${DATE_STAMP}.txt" ]; then
    CRITICAL_ISSUES=$(grep -c "CRITICAL" "${SCAN_RESULTS_DIR}/trivy_critical_${DATE_STAMP}.txt" 2>/dev/null || echo "0")
    HIGH_ISSUES=$(grep -c "HIGH" "${SCAN_RESULTS_DIR}/trivy_critical_${DATE_STAMP}.txt" 2>/dev/null || echo "0")
    if [ "$CRITICAL_ISSUES" -eq 0 ] && [ "$HIGH_ISSUES" -eq 0 ]; then
        echo "✅ PASSED - No HIGH/CRITICAL vulnerabilities"
    else
        echo "❌ FAILED - Found $CRITICAL_ISSUES CRITICAL and $HIGH_ISSUES HIGH vulnerabilities"
    fi
else
    echo "❓ NOT SCANNED"
fi)

### Image Size
$(if [ -f "${SCAN_RESULTS_DIR}/image_size_${DATE_STAMP}.txt" ]; then
    SIZE_MB=$(cat "${SCAN_RESULTS_DIR}/image_size_${DATE_STAMP}.txt" | grep -o '[0-9]\+ MB' | grep -o '[0-9]\+')
    if [ "$SIZE_MB" -lt 200 ]; then
        echo "✅ PASSED - ${SIZE_MB} MB (<200MB requirement)"
    else
        echo "❌ FAILED - ${SIZE_MB} MB (exceeds 200MB requirement)"
    fi
else
    echo "❓ NOT TESTED"
fi)

### Startup Time
$(if [ -f "${SCAN_RESULTS_DIR}/startup_time_${DATE_STAMP}.txt" ]; then
    STARTUP_SEC=$(cat "${SCAN_RESULTS_DIR}/startup_time_${DATE_STAMP}.txt" | grep -o '[0-9]\+ seconds' | grep -o '[0-9]\+')
    if [ "$STARTUP_SEC" -lt 30 ]; then
        echo "✅ PASSED - ${STARTUP_SEC} seconds (<30s requirement)"
    else
        echo "❌ FAILED - ${STARTUP_SEC} seconds (exceeds 30s requirement)"
    fi
else
    echo "❓ NOT TESTED"
fi)

## Detailed Reports

- Dockerfile scan: \`hadolint_${DATE_STAMP}.txt\`
- Vulnerability scan: \`trivy_vulnerabilities_${DATE_STAMP}.txt\`
- Critical vulnerabilities: \`trivy_critical_${DATE_STAMP}.txt\`
- Configuration scan: \`trivy_config_${DATE_STAMP}.txt\`

## Recommendations

1. Review all HIGH and CRITICAL vulnerabilities
2. Update base images to latest patched versions
3. Minimize image layers and installed packages
4. Implement regular security scanning in CI/CD pipeline
5. Monitor for new vulnerabilities continuously

EOF

    echo -e "${GREEN}Summary report generated: ${SUMMARY_FILE}${NC}"
}

# Main execution
main() {
    local exit_code=0

    # Install required tools
    install_trivy
    install_hadolint

    # Run security scans
    echo -e "${BLUE}Starting security scan suite...${NC}"

    scan_dockerfile || exit_code=1
    scan_image_vulnerabilities || exit_code=1
    check_image_size || exit_code=1
    test_startup_time || exit_code=1

    # Generate summary
    generate_summary_report

    echo -e "${BLUE}==============================================================================${NC}"
    if [ $exit_code -eq 0 ]; then
        echo -e "${GREEN}🎉 All security checks passed!${NC}"
    else
        echo -e "${RED}❌ Some security checks failed. Review the results in ${SCAN_RESULTS_DIR}${NC}"
    fi
    echo -e "${BLUE}==============================================================================${NC}"

    return $exit_code
}

# Check if running as script or sourced
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi
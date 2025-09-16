#!/bin/bash

# Optimized startup script for Notification Service with JVM tuning
# This script applies performance optimizations for production deployment

set -e

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
APP_NAME="notification-service"
JAR_FILE="build/libs/${APP_NAME}.jar"
LOG_DIR="logs"
PID_FILE="${LOG_DIR}/${APP_NAME}.pid"

# Environment detection
detect_environment() {
    # Detect available memory
    if [[ "$OSTYPE" == "linux-gnu"* ]]; then
        TOTAL_MEM_KB=$(grep MemTotal /proc/meminfo | awk '{print $2}')
        TOTAL_MEM_GB=$((TOTAL_MEM_KB / 1024 / 1024))
    elif [[ "$OSTYPE" == "darwin"* ]]; then
        TOTAL_MEM_BYTES=$(sysctl -n hw.memsize)
        TOTAL_MEM_GB=$((TOTAL_MEM_BYTES / 1024 / 1024 / 1024))
    else
        TOTAL_MEM_GB=8  # Default assumption
    fi

    # Detect CPU cores
    if [[ "$OSTYPE" == "linux-gnu"* ]]; then
        CPU_CORES=$(nproc)
    elif [[ "$OSTYPE" == "darwin"* ]]; then
        CPU_CORES=$(sysctl -n hw.ncpu)
    else
        CPU_CORES=4  # Default assumption
    fi

    echo -e "${GREEN}System Detection:${NC}"
    echo "  Memory: ${TOTAL_MEM_GB}GB"
    echo "  CPU Cores: ${CPU_CORES}"
}

# Calculate JVM memory settings based on system resources
calculate_jvm_memory() {
    # Use 50% of system memory for heap (max)
    MAX_HEAP_GB=$((TOTAL_MEM_GB / 2))
    if [ $MAX_HEAP_GB -lt 1 ]; then
        MAX_HEAP_GB=1
    elif [ $MAX_HEAP_GB -gt 8 ]; then
        MAX_HEAP_GB=8  # Cap at 8GB for this service
    fi

    # Initial heap is 50% of max heap
    INIT_HEAP_GB=$((MAX_HEAP_GB / 2))

    # Direct memory is 1/8 of max heap
    DIRECT_MEM_MB=$((MAX_HEAP_GB * 128))

    echo -e "${GREEN}JVM Memory Settings:${NC}"
    echo "  Initial Heap: ${INIT_HEAP_GB}g"
    echo "  Maximum Heap: ${MAX_HEAP_GB}g"
    echo "  Direct Memory: ${DIRECT_MEM_MB}m"
}

# Build JVM options dynamically
build_jvm_options() {
    JVM_OPTS=""

    # Memory settings
    JVM_OPTS="$JVM_OPTS -Xms${INIT_HEAP_GB}g"
    JVM_OPTS="$JVM_OPTS -Xmx${MAX_HEAP_GB}g"
    JVM_OPTS="$JVM_OPTS -XX:MaxDirectMemorySize=${DIRECT_MEM_MB}m"
    JVM_OPTS="$JVM_OPTS -XX:MetaspaceSize=256m"
    JVM_OPTS="$JVM_OPTS -XX:MaxMetaspaceSize=512m"

    # Garbage Collection
    JVM_OPTS="$JVM_OPTS -XX:+UseG1GC"
    JVM_OPTS="$JVM_OPTS -XX:MaxGCPauseMillis=200"
    JVM_OPTS="$JVM_OPTS -XX:G1HeapRegionSize=16m"
    JVM_OPTS="$JVM_OPTS -XX:InitiatingHeapOccupancyPercent=45"
    JVM_OPTS="$JVM_OPTS -XX:+UseStringDeduplication"

    # Parallel/Concurrent GC threads based on CPU cores
    GC_THREADS=$((CPU_CORES / 2))
    if [ $GC_THREADS -lt 2 ]; then
        GC_THREADS=2
    fi
    JVM_OPTS="$JVM_OPTS -XX:ParallelGCThreads=${GC_THREADS}"
    JVM_OPTS="$JVM_OPTS -XX:ConcGCThreads=$((GC_THREADS / 2))"

    # Performance optimizations
    JVM_OPTS="$JVM_OPTS -XX:+TieredCompilation"
    JVM_OPTS="$JVM_OPTS -XX:+UseCompressedOops"
    JVM_OPTS="$JVM_OPTS -XX:+UseCompressedClassPointers"

    # Container support (if running in container)
    if [ -f /.dockerenv ]; then
        JVM_OPTS="$JVM_OPTS -XX:+UseContainerSupport"
        JVM_OPTS="$JVM_OPTS -XX:MaxRAMPercentage=75.0"
        echo -e "${YELLOW}Container environment detected${NC}"
    fi

    # Monitoring
    JVM_OPTS="$JVM_OPTS -Xlog:gc*:file=${LOG_DIR}/gc.log:time,uptime,level,tags:filecount=10,filesize=10M"
    JVM_OPTS="$JVM_OPTS -XX:+HeapDumpOnOutOfMemoryError"
    JVM_OPTS="$JVM_OPTS -XX:HeapDumpPath=${LOG_DIR}/heap-dump.hprof"
    JVM_OPTS="$JVM_OPTS -XX:ErrorFile=${LOG_DIR}/hs_err_pid%p.log"

    # JMX for monitoring
    JVM_OPTS="$JVM_OPTS -Dcom.sun.management.jmxremote=true"
    JVM_OPTS="$JVM_OPTS -Dcom.sun.management.jmxremote.port=9090"
    JVM_OPTS="$JVM_OPTS -Dcom.sun.management.jmxremote.authenticate=false"
    JVM_OPTS="$JVM_OPTS -Dcom.sun.management.jmxremote.ssl=false"

    # Application settings
    JVM_OPTS="$JVM_OPTS -Djava.awt.headless=true"
    JVM_OPTS="$JVM_OPTS -Dfile.encoding=UTF-8"
    JVM_OPTS="$JVM_OPTS -Duser.timezone=UTC"
    JVM_OPTS="$JVM_OPTS -Djava.security.egd=file:/dev/./urandom"

    # Spring profile
    JVM_OPTS="$JVM_OPTS -Dspring.profiles.active=${SPRING_PROFILE:-prod}"
}

# Check prerequisites
check_prerequisites() {
    echo -e "${GREEN}Checking prerequisites...${NC}"

    # Check Java version
    if ! command -v java &> /dev/null; then
        echo -e "${RED}Java is not installed${NC}"
        exit 1
    fi

    JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
    if [[ "$JAVA_VERSION" -lt 21 ]]; then
        echo -e "${RED}Java 21 or higher is required (found: $JAVA_VERSION)${NC}"
        exit 1
    fi
    echo "  ✓ Java $JAVA_VERSION"

    # Check JAR file
    if [ ! -f "$JAR_FILE" ]; then
        echo -e "${YELLOW}JAR file not found, building...${NC}"
        ./gradlew build -x test
    fi
    echo "  ✓ JAR file exists"

    # Create log directory
    mkdir -p "$LOG_DIR"
    echo "  ✓ Log directory ready"

    # Check if service is already running
    if [ -f "$PID_FILE" ]; then
        PID=$(cat "$PID_FILE")
        if ps -p "$PID" > /dev/null 2>&1; then
            echo -e "${YELLOW}Service is already running (PID: $PID)${NC}"
            exit 1
        else
            rm "$PID_FILE"
        fi
    fi
}

# Start the service
start_service() {
    echo -e "${GREEN}Starting $APP_NAME...${NC}"

    # Export JVM options
    export JAVA_OPTS="$JVM_OPTS"

    # Start application
    nohup java $JVM_OPTS -jar "$JAR_FILE" \
        >> "${LOG_DIR}/application.log" 2>&1 &

    PID=$!
    echo $PID > "$PID_FILE"

    # Wait for service to start
    echo -n "Waiting for service to start"
    for i in {1..30}; do
        if curl -s http://localhost:8083/health > /dev/null 2>&1; then
            echo -e "\n${GREEN}Service started successfully (PID: $PID)${NC}"
            echo "Logs: tail -f ${LOG_DIR}/application.log"
            echo "Health: http://localhost:8083/health"
            echo "Metrics: http://localhost:8083/actuator/prometheus"
            return 0
        fi
        echo -n "."
        sleep 1
    done

    echo -e "\n${RED}Service failed to start. Check logs: ${LOG_DIR}/application.log${NC}"
    return 1
}

# Stop the service
stop_service() {
    if [ ! -f "$PID_FILE" ]; then
        echo -e "${YELLOW}Service is not running${NC}"
        return 0
    fi

    PID=$(cat "$PID_FILE")
    echo -e "${GREEN}Stopping $APP_NAME (PID: $PID)...${NC}"

    # Graceful shutdown
    kill -TERM "$PID" 2>/dev/null

    # Wait for shutdown
    for i in {1..30}; do
        if ! ps -p "$PID" > /dev/null 2>&1; then
            rm "$PID_FILE"
            echo -e "${GREEN}Service stopped successfully${NC}"
            return 0
        fi
        sleep 1
    done

    # Force kill if necessary
    echo -e "${YELLOW}Force killing service...${NC}"
    kill -KILL "$PID" 2>/dev/null
    rm "$PID_FILE"
    echo -e "${GREEN}Service stopped${NC}"
}

# Display JVM info
show_jvm_info() {
    echo -e "${GREEN}JVM Options:${NC}"
    echo "$JVM_OPTS" | tr ' ' '\n' | grep -v '^$'
}

# Main execution
main() {
    case "${1:-}" in
        start)
            detect_environment
            calculate_jvm_memory
            build_jvm_options
            check_prerequisites
            start_service
            ;;
        stop)
            stop_service
            ;;
        restart)
            stop_service
            sleep 2
            detect_environment
            calculate_jvm_memory
            build_jvm_options
            check_prerequisites
            start_service
            ;;
        info)
            detect_environment
            calculate_jvm_memory
            build_jvm_options
            show_jvm_info
            ;;
        *)
            echo "Usage: $0 {start|stop|restart|info}"
            echo ""
            echo "Commands:"
            echo "  start   - Start the service with optimized JVM settings"
            echo "  stop    - Stop the running service"
            echo "  restart - Restart the service"
            echo "  info    - Display JVM configuration"
            exit 1
            ;;
    esac
}

# Run main function
main "$@"
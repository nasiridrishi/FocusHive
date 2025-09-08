# FocusHive Performance Monitoring Setup

This guide covers setting up comprehensive performance monitoring for the FocusHive application.

## Monitoring Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Application   │    │   Prometheus    │    │     Grafana     │
│   (Micrometer)  ├────▶   (Metrics)     ├────▶   (Dashboard)   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Application   │    │     Redis       │    │   AlertManager  │
│      Logs       │    │   (Monitoring)  │    │   (Alerting)    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## Prometheus Configuration

### 1. Prometheus Configuration File

Create `/opt/prometheus/prometheus.yml`:

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

rule_files:
  - "focushive_rules.yml"

alerting:
  alertmanagers:
    - static_configs:
        - targets:
          - alertmanager:9093

scrape_configs:
  # FocusHive Backend
  - job_name: 'focushive-backend'
    static_configs:
      - targets: ['localhost:8080']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 10s
    scrape_timeout: 10s

  # Identity Service
  - job_name: 'identity-service'
    static_configs:
      - targets: ['localhost:8081']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 10s
    scrape_timeout: 10s

  # Redis Monitoring
  - job_name: 'redis'
    static_configs:
      - targets: ['localhost:9121']
    scrape_interval: 10s

  # PostgreSQL Monitoring
  - job_name: 'postgres'
    static_configs:
      - targets: ['localhost:9187']
    scrape_interval: 10s

  # Node Exporter (System Metrics)
  - job_name: 'node'
    static_configs:
      - targets: ['localhost:9100']
    scrape_interval: 10s

  # Frontend Performance (via custom exporter)
  - job_name: 'frontend-performance'
    static_configs:
      - targets: ['localhost:3001']
    metrics_path: '/metrics'
    scrape_interval: 30s
```

### 2. FocusHive Alert Rules

Create `/opt/prometheus/focushive_rules.yml`:

```yaml
groups:
  - name: focushive.rules
    rules:
      # High Response Time
      - alert: HighResponseTime
        expr: histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m])) > 0.5
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "High response time detected"
          description: "95th percentile response time is {{ $value }}s"

      # High Error Rate
      - alert: HighErrorRate
        expr: rate(http_server_requests_total{status=~"5.."}[5m]) / rate(http_server_requests_total[5m]) > 0.01
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "High error rate detected"
          description: "Error rate is {{ $value | humanizePercentage }}"

      # Database Connection Pool
      - alert: DatabaseConnectionPoolHigh
        expr: hikari_connections_active / hikari_connections_max > 0.8
        for: 3m
        labels:
          severity: warning
        annotations:
          summary: "Database connection pool usage high"
          description: "Connection pool usage is {{ $value | humanizePercentage }}"

      # Memory Usage
      - alert: HighMemoryUsage
        expr: jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} > 0.85
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High JVM memory usage"
          description: "JVM heap memory usage is {{ $value | humanizePercentage }}"

      # Cache Hit Rate
      - alert: LowCacheHitRate
        expr: rate(cache_gets_total{result="hit"}[10m]) / rate(cache_gets_total[10m]) < 0.7
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Low cache hit rate"
          description: "Cache hit rate is {{ $value | humanizePercentage }}"

      # WebSocket Connections
      - alert: HighWebSocketConnections
        expr: focushive_websocket_connections_active > 1000
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "High number of WebSocket connections"
          description: "Active WebSocket connections: {{ $value }}"

      # Presence System Load
      - alert: HighPresenceSystemLoad
        expr: rate(focushive_presence_updates_total[5m]) > 100
        for: 3m
        labels:
          severity: warning
        annotations:
          summary: "High presence system load"
          description: "Presence updates per second: {{ $value }}"
```

## Grafana Dashboard Configuration

### 1. FocusHive Overview Dashboard

JSON configuration for main dashboard:

```json
{
  "dashboard": {
    "id": null,
    "title": "FocusHive Performance Overview",
    "tags": ["focushive", "performance"],
    "timezone": "browser",
    "panels": [
      {
        "id": 1,
        "title": "Response Time (95th Percentile)",
        "type": "stat",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))",
            "legendFormat": "95th Percentile"
          }
        ],
        "fieldConfig": {
          "defaults": {
            "unit": "s",
            "thresholds": {
              "steps": [
                {"color": "green", "value": null},
                {"color": "yellow", "value": 0.2},
                {"color": "red", "value": 0.5}
              ]
            }
          }
        }
      },
      {
        "id": 2,
        "title": "Request Rate",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(http_server_requests_total[5m])",
            "legendFormat": "{{method}} {{uri}}"
          }
        ]
      },
      {
        "id": 3,
        "title": "Error Rate",
        "type": "stat",
        "targets": [
          {
            "expr": "rate(http_server_requests_total{status=~\"5..\"}[5m]) / rate(http_server_requests_total[5m])",
            "legendFormat": "Error Rate"
          }
        ],
        "fieldConfig": {
          "defaults": {
            "unit": "percentunit",
            "thresholds": {
              "steps": [
                {"color": "green", "value": null},
                {"color": "yellow", "value": 0.01},
                {"color": "red", "value": 0.05}
              ]
            }
          }
        }
      },
      {
        "id": 4,
        "title": "Database Connection Pool",
        "type": "graph",
        "targets": [
          {
            "expr": "hikari_connections_active",
            "legendFormat": "Active"
          },
          {
            "expr": "hikari_connections_max",
            "legendFormat": "Max"
          }
        ]
      },
      {
        "id": 5,
        "title": "JVM Memory Usage",
        "type": "graph",
        "targets": [
          {
            "expr": "jvm_memory_used_bytes{area=\"heap\"}",
            "legendFormat": "Heap Used"
          },
          {
            "expr": "jvm_memory_max_bytes{area=\"heap\"}",
            "legendFormat": "Heap Max"
          }
        ],
        "fieldConfig": {
          "defaults": {
            "unit": "bytes"
          }
        }
      },
      {
        "id": 6,
        "title": "Cache Performance",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(cache_gets_total{result=\"hit\"}[5m])",
            "legendFormat": "Cache Hits"
          },
          {
            "expr": "rate(cache_gets_total{result=\"miss\"}[5m])",
            "legendFormat": "Cache Misses"
          }
        ]
      },
      {
        "id": 7,
        "title": "WebSocket Connections",
        "type": "stat",
        "targets": [
          {
            "expr": "focushive_websocket_connections_active",
            "legendFormat": "Active Connections"
          }
        ]
      },
      {
        "id": 8,
        "title": "Presence Updates",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(focushive_presence_updates_total[5m])",
            "legendFormat": "Updates/sec"
          }
        ]
      }
    ],
    "time": {
      "from": "now-1h",
      "to": "now"
    },
    "refresh": "30s"
  }
}
```

### 2. Database Performance Dashboard

```json
{
  "dashboard": {
    "title": "FocusHive Database Performance",
    "panels": [
      {
        "title": "Query Response Time",
        "type": "graph",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, rate(hibernate_query_execution_seconds_bucket[5m]))",
            "legendFormat": "95th Percentile"
          },
          {
            "expr": "histogram_quantile(0.50, rate(hibernate_query_execution_seconds_bucket[5m]))",
            "legendFormat": "50th Percentile"
          }
        ]
      },
      {
        "title": "Connection Pool Metrics",
        "type": "graph",
        "targets": [
          {
            "expr": "hikari_connections_active",
            "legendFormat": "Active"
          },
          {
            "expr": "hikari_connections_idle",
            "legendFormat": "Idle"
          },
          {
            "expr": "hikari_connections_pending",
            "legendFormat": "Pending"
          }
        ]
      },
      {
        "title": "Query Count by Type",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(hibernate_query_executions_total[5m])",
            "legendFormat": "{{query_type}}"
          }
        ]
      }
    ]
  }
}
```

## Custom Metrics Implementation

### Backend Custom Metrics

Add to Spring Boot application:

```java
@Component
public class FocusHiveMetrics {
    
    private final Counter presenceUpdates;
    private final Gauge activeWebSocketConnections;
    private final Timer sessionDuration;
    private final Counter cacheOperations;
    
    public FocusHiveMetrics(MeterRegistry meterRegistry) {
        this.presenceUpdates = Counter.builder("focushive.presence.updates")
            .description("Number of presence updates")
            .register(meterRegistry);
            
        this.activeWebSocketConnections = Gauge.builder("focushive.websocket.connections.active")
            .description("Active WebSocket connections")
            .register(meterRegistry, this, FocusHiveMetrics::getActiveConnections);
            
        this.sessionDuration = Timer.builder("focushive.session.duration")
            .description("Focus session duration")
            .register(meterRegistry);
            
        this.cacheOperations = Counter.builder("focushive.cache.operations")
            .tag("operation", "get")
            .description("Cache operations")
            .register(meterRegistry);
    }
    
    public void recordPresenceUpdate() {
        presenceUpdates.increment();
    }
    
    public void recordSessionDuration(Duration duration) {
        sessionDuration.record(duration);
    }
    
    public void recordCacheOperation(String operation, String result) {
        Counter.builder("focushive.cache.operations")
            .tag("operation", operation)
            .tag("result", result)
            .register(meterRegistry)
            .increment();
    }
    
    private double getActiveConnections() {
        // Return actual connection count
        return WebSocketConnectionManager.getActiveConnectionCount();
    }
}
```

### Frontend Performance Monitoring

JavaScript performance monitoring:

```typescript
// Frontend metrics exporter
class PerformanceMetrics {
    private metrics: Map<string, number> = new Map();
    
    recordPageLoad(duration: number) {
        this.metrics.set('page_load_duration', duration);
    }
    
    recordApiCall(endpoint: string, duration: number, status: number) {
        this.metrics.set(`api_${endpoint}_duration`, duration);
        this.metrics.set(`api_${endpoint}_status`, status);
    }
    
    recordWebSocketLatency(latency: number) {
        this.metrics.set('websocket_latency', latency);
    }
    
    recordBundleSize(size: number) {
        this.metrics.set('bundle_size', size);
    }
    
    exportMetrics(): string {
        let output = '';
        this.metrics.forEach((value, key) => {
            output += `focushive_frontend_${key} ${value}\n`;
        });
        return output;
    }
}

// Export endpoint
app.get('/metrics', (req, res) => {
    res.set('Content-Type', 'text/plain');
    res.send(performanceMetrics.exportMetrics());
});
```

## AlertManager Configuration

### AlertManager Config

Create `/opt/alertmanager/alertmanager.yml`:

```yaml
global:
  smtp_smarthost: 'localhost:587'
  smtp_from: 'alerts@focushive.com'

route:
  group_by: ['alertname']
  group_wait: 10s
  group_interval: 10s
  repeat_interval: 1h
  receiver: 'web.hook'
  routes:
    - match:
        severity: critical
      receiver: 'critical-alerts'
    - match:
        severity: warning
      receiver: 'warning-alerts'

receivers:
  - name: 'web.hook'
    webhook_configs:
      - url: 'http://localhost:5001/webhook'

  - name: 'critical-alerts'
    email_configs:
      - to: 'dev-team@focushive.com'
        subject: 'CRITICAL: FocusHive Alert'
        body: |
          {{ range .Alerts }}
          Alert: {{ .Annotations.summary }}
          Description: {{ .Annotations.description }}
          {{ end }}
    slack_configs:
      - api_url: 'YOUR_SLACK_WEBHOOK_URL'
        channel: '#alerts'
        title: 'CRITICAL FocusHive Alert'

  - name: 'warning-alerts'
    email_configs:
      - to: 'dev-team@focushive.com'
        subject: 'WARNING: FocusHive Alert'
        body: |
          {{ range .Alerts }}
          Alert: {{ .Annotations.summary }}
          Description: {{ .Annotations.description }}
          {{ end }}
```

## Docker Compose for Monitoring Stack

Create `docker-compose.monitoring.yml`:

```yaml
version: '3.8'
services:
  prometheus:
    image: prom/prometheus:latest
    container_name: prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./monitoring/prometheus:/etc/prometheus
      - prometheus_data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--web.console.libraries=/etc/prometheus/console_libraries'
      - '--web.console.templates=/etc/prometheus/consoles'

  grafana:
    image: grafana/grafana:latest
    container_name: grafana
    ports:
      - "3000:3000"
    volumes:
      - grafana_data:/var/lib/grafana
      - ./monitoring/grafana/provisioning:/etc/grafana/provisioning
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin

  alertmanager:
    image: prom/alertmanager:latest
    container_name: alertmanager
    ports:
      - "9093:9093"
    volumes:
      - ./monitoring/alertmanager:/etc/alertmanager

  redis-exporter:
    image: oliver006/redis_exporter
    container_name: redis-exporter
    ports:
      - "9121:9121"
    environment:
      - REDIS_ADDR=redis://redis:6379

  postgres-exporter:
    image: prometheuscommunity/postgres-exporter
    container_name: postgres-exporter
    ports:
      - "9187:9187"
    environment:
      - DATA_SOURCE_NAME=postgresql://focushive_user:focushive_pass@postgres:5432/focushive

  node-exporter:
    image: prom/node-exporter:latest
    container_name: node-exporter
    ports:
      - "9100:9100"
    volumes:
      - /proc:/host/proc:ro
      - /sys:/host/sys:ro
      - /:/rootfs:ro
    command:
      - '--path.procfs=/host/proc'
      - '--path.rootfs=/rootfs'
      - '--path.sysfs=/host/sys'

volumes:
  prometheus_data:
  grafana_data:
```

## Monitoring Setup Scripts

### Setup Script

Create `setup-monitoring.sh`:

```bash
#!/bin/bash

# FocusHive Monitoring Setup Script

set -e

MONITORING_DIR="./monitoring"
PROMETHEUS_DIR="$MONITORING_DIR/prometheus"
GRAFANA_DIR="$MONITORING_DIR/grafana"
ALERTMANAGER_DIR="$MONITORING_DIR/alertmanager"

echo "Setting up FocusHive monitoring..."

# Create directories
mkdir -p "$PROMETHEUS_DIR"
mkdir -p "$GRAFANA_DIR/provisioning/dashboards"
mkdir -p "$GRAFANA_DIR/provisioning/datasources"
mkdir -p "$ALERTMANAGER_DIR"

# Copy configuration files
cp docs/monitoring/prometheus.yml "$PROMETHEUS_DIR/"
cp docs/monitoring/focushive_rules.yml "$PROMETHEUS_DIR/"
cp docs/monitoring/alertmanager.yml "$ALERTMANAGER_DIR/"

# Setup Grafana datasource
cat > "$GRAFANA_DIR/provisioning/datasources/prometheus.yml" << EOF
apiVersion: 1
datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
EOF

# Setup Grafana dashboards
cat > "$GRAFANA_DIR/provisioning/dashboards/dashboard.yml" << EOF
apiVersion: 1
providers:
  - name: 'focushive'
    orgId: 1
    folder: ''
    type: file
    disableDeletion: false
    updateIntervalSeconds: 10
    options:
      path: /etc/grafana/provisioning/dashboards
EOF

echo "Starting monitoring stack..."
docker-compose -f docker-compose.monitoring.yml up -d

echo "Waiting for services to start..."
sleep 30

echo "Monitoring setup complete!"
echo "Prometheus: http://localhost:9090"
echo "Grafana: http://localhost:3000 (admin/admin)"
echo "AlertManager: http://localhost:9093"
```

### Health Check Script

Create `check-monitoring.sh`:

```bash
#!/bin/bash

# Health check for monitoring services

echo "Checking monitoring services..."

services=(
    "Prometheus:http://localhost:9090/-/healthy"
    "Grafana:http://localhost:3000/api/health"
    "AlertManager:http://localhost:9093/-/healthy"
)

for service in "${services[@]}"; do
    name=$(echo $service | cut -d: -f1)
    url=$(echo $service | cut -d: -f2-3)
    
    if curl -f -s "$url" > /dev/null; then
        echo "✓ $name is healthy"
    else
        echo "✗ $name is not responding"
    fi
done

echo "Checking application metrics..."
if curl -f -s "http://localhost:8080/actuator/prometheus" | head -10; then
    echo "✓ Backend metrics available"
else
    echo "✗ Backend metrics not available"
fi
```

## Usage Guidelines

### 1. Initial Setup

```bash
# Setup monitoring infrastructure
chmod +x setup-monitoring.sh
./setup-monitoring.sh

# Verify setup
chmod +x check-monitoring.sh
./check-monitoring.sh
```

### 2. Dashboard Access

- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000 (admin/admin)
- **AlertManager**: http://localhost:9093

### 3. Custom Metrics

Add custom metrics to your application code and they will automatically be scraped by Prometheus and displayed in Grafana.

### 4. Alert Configuration

Modify alert rules in `focushive_rules.yml` and restart Prometheus to apply changes.

### 5. Dashboard Customization

Import additional dashboards or create custom ones through the Grafana interface.

This monitoring setup provides comprehensive observability into FocusHive performance and enables proactive issue detection and resolution.
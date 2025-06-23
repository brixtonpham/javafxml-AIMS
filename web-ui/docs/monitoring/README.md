# AIMS Monitoring & Analytics Documentation

Comprehensive monitoring and analytics documentation for the AIMS Web Application, covering performance monitoring, system observability, operational dashboards, and business intelligence.

## 📚 Monitoring Documentation Structure

### Infrastructure Monitoring
- [**Prometheus Setup**](./prometheus-setup.md) - Metrics collection and storage configuration
- [**Grafana Dashboards**](./grafana-dashboards.md) - Visualization and alerting dashboards
- [**Log Management**](./log-management.md) - Centralized logging with Loki and Promtail
- [**Health Checks**](./health-checks.md) - Application and service health monitoring

### Application Monitoring
- [**Performance Monitoring**](./performance-monitoring.md) - Application performance metrics and optimization
- [**Error Tracking**](./error-tracking.md) - Error monitoring and alerting systems
- [**User Analytics**](./user-analytics.md) - User behavior and engagement tracking
- [**Business Metrics**](./business-metrics.md) - KPI monitoring and business intelligence

### Operational Monitoring
- [**Alerting Rules**](./alerting-rules.md) - Alert configuration and escalation procedures
- [**SLA Monitoring**](./sla-monitoring.md) - Service level agreement tracking
- [**Capacity Planning**](./capacity-planning.md) - Resource utilization and scaling metrics
- [**Security Monitoring**](./security-monitoring.md) - Security events and compliance tracking

## 🚀 Monitoring Stack Overview

### Architecture Diagram
```
┌─────────────────────────────────────────────────────────────┐
│                   AIMS Monitoring Stack                    │
├─────────────────────────────────────────────────────────────┤
│                                                            │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐     │
│  │   Web App   │    │     API     │    │  Database   │     │
│  │  (Metrics)  │    │ (Metrics)   │    │ (Metrics)   │     │
│  └──────┬──────┘    └──────┬──────┘    └──────┬──────┘     │
│         │                  │                  │            │
│         └─────────┬────────┴────────┬─────────┘            │
│                   │                 │                      │
│         ┌─────────▼─────────┐  ┌────▼─────┐                │
│         │    Prometheus     │  │   Loki   │                │
│         │ (Metrics Storage) │  │  (Logs)  │                │
│         └─────────┬─────────┘  └────┬─────┘                │
│                   │                 │                      │
│                   └────────┬────────┘                      │
│                            │                               │
│                   ┌────────▼────────┐                      │
│                   │     Grafana     │                      │
│                   │  (Dashboards &  │                      │
│                   │    Alerting)    │                      │
│                   └─────────────────┘                      │
└─────────────────────────────────────────────────────────────┘
```

## 📊 Key Performance Indicators (KPIs)

### Application Performance
- **Response Time**: < 200ms (95th percentile)
- **Availability**: > 99.9% uptime
- **Error Rate**: < 0.1% of requests
- **Throughput**: > 1000 requests/minute peak capacity

### Business Metrics
- **Conversion Rate**: 2.5% average cart-to-order conversion
- **Average Order Value**: $47.50 target
- **User Engagement**: 5.2 pages per session average
- **Customer Satisfaction**: > 4.5/5.0 rating

### Infrastructure Metrics
- **CPU Utilization**: < 70% average usage
- **Memory Usage**: < 80% of allocated resources
- **Disk I/O**: < 80% of available IOPS
- **Network Latency**: < 50ms internal communication

## 🔧 Monitoring Configuration

### Prometheus Configuration
```yaml
# prometheus.yml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

rule_files:
  - "alerting_rules.yml"
  - "recording_rules.yml"

alerting:
  alertmanagers:
    - static_configs:
        - targets:
          - alertmanager:9093

scrape_configs:
  # AIMS Web Application
  - job_name: 'aims-web'
    static_configs:
      - targets: ['web:3000']
    metrics_path: '/metrics'
    scrape_interval: 30s
    
  # AIMS API Backend
  - job_name: 'aims-api'
    static_configs:
      - targets: ['api:8080']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 15s
    
  # Infrastructure Monitoring
  - job_name: 'node-exporter'
    static_configs:
      - targets: ['node-exporter:9100']
    scrape_interval: 15s
    
  # Container Monitoring
  - job_name: 'cadvisor'
    static_configs:
      - targets: ['cadvisor:8080']
    scrape_interval: 30s
    
  # Nginx Monitoring
  - job_name: 'nginx'
    static_configs:
      - targets: ['nginx-exporter:9113']
    scrape_interval: 30s
```

### Grafana Dashboard Configuration
```json
{
  "dashboard": {
    "title": "AIMS Application Overview",
    "tags": ["aims", "overview"],
    "time": {
      "from": "now-1h",
      "to": "now"
    },
    "refresh": "5s",
    "panels": [
      {
        "title": "Request Rate",
        "type": "stat",
        "targets": [
          {
            "expr": "rate(http_requests_total[5m])",
            "legendFormat": "Requests/sec"
          }
        ]
      },
      {
        "title": "Response Time",
        "type": "graph",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m]))",
            "legendFormat": "95th percentile"
          },
          {
            "expr": "histogram_quantile(0.50, rate(http_request_duration_seconds_bucket[5m]))",
            "legendFormat": "50th percentile"
          }
        ]
      },
      {
        "title": "Error Rate",
        "type": "stat",
        "targets": [
          {
            "expr": "rate(http_requests_total{status=~\"5..\"}[5m]) / rate(http_requests_total[5m]) * 100",
            "legendFormat": "Error Rate %"
          }
        ]
      }
    ]
  }
}
```

## 📈 Custom Metrics Implementation

### Frontend Metrics Collection
```javascript
// Performance metrics collection
class MetricsCollector {
  constructor() {
    this.metrics = {
      pageViews: 0,
      userInteractions: 0,
      apiCalls: 0,
      errors: 0
    };
    this.setupMetricsCollection();
  }

  setupMetricsCollection() {
    // Page performance metrics
    this.collectPageMetrics();
    
    // User interaction metrics
    this.collectUserMetrics();
    
    // API performance metrics
    this.collectAPIMetrics();
    
    // Error tracking
    this.collectErrorMetrics();
  }

  collectPageMetrics() {
    // Core Web Vitals
    const observer = new PerformanceObserver((list) => {
      for (const entry of list.getEntries()) {
        if (entry.entryType === 'navigation') {
          this.sendMetric('page_load_time', entry.loadEventEnd - entry.loadEventStart);
        }
        
        if (entry.entryType === 'largest-contentful-paint') {
          this.sendMetric('lcp', entry.startTime);
        }
        
        if (entry.entryType === 'first-input') {
          this.sendMetric('fid', entry.processingStart - entry.startTime);
        }
      }
    });
    
    observer.observe({
      type: 'navigation',
      buffered: true
    });
    
    observer.observe({
      type: 'largest-contentful-paint',
      buffered: true
    });
    
    observer.observe({
      type: 'first-input',
      buffered: true
    });
  }

  collectUserMetrics() {
    // Track user interactions
    document.addEventListener('click', (event) => {
      const target = event.target.closest('[data-track]');
      if (target) {
        this.sendMetric('user_interaction', {
          element: target.dataset.track,
          timestamp: Date.now()
        });
      }
    });
    
    // Track page visibility
    document.addEventListener('visibilitychange', () => {
      if (document.hidden) {
        this.sendMetric('page_hidden', Date.now());
      } else {
        this.sendMetric('page_visible', Date.now());
      }
    });
  }

  collectAPIMetrics() {
    // Intercept API calls
    const originalFetch = window.fetch;
    window.fetch = async (...args) => {
      const startTime = performance.now();
      const url = args[0];
      
      try {
        const response = await originalFetch(...args);
        const endTime = performance.now();
        
        this.sendMetric('api_request', {
          url: url,
          method: args[1]?.method || 'GET',
          status: response.status,
          duration: endTime - startTime,
          timestamp: Date.now()
        });
        
        return response;
      } catch (error) {
        const endTime = performance.now();
        
        this.sendMetric('api_error', {
          url: url,
          method: args[1]?.method || 'GET',
          error: error.message,
          duration: endTime - startTime,
          timestamp: Date.now()
        });
        
        throw error;
      }
    };
  }

  sendMetric(name, data) {
    // Send metrics to backend or analytics service
    if (navigator.sendBeacon) {
      navigator.sendBeacon('/api/metrics', JSON.stringify({
        metric: name,
        data: data,
        timestamp: Date.now(),
        userAgent: navigator.userAgent,
        url: window.location.href
      }));
    }
  }
}

// Initialize metrics collection
const metricsCollector = new MetricsCollector();
```

### Backend Metrics Endpoints
```javascript
// Express.js metrics middleware
const promClient = require('prom-client');

// Create metrics
const httpRequestDuration = new promClient.Histogram({
  name: 'http_request_duration_seconds',
  help: 'Duration of HTTP requests in seconds',
  labelNames: ['method', 'route', 'status_code'],
  buckets: [0.1, 0.3, 0.5, 0.7, 1, 3, 5, 7, 10]
});

const httpRequestsTotal = new promClient.Counter({
  name: 'http_requests_total',
  help: 'Total number of HTTP requests',
  labelNames: ['method', 'route', 'status_code']
});

const activeUsers = new promClient.Gauge({
  name: 'active_users_total',
  help: 'Number of active users'
});

const orderTotal = new promClient.Counter({
  name: 'orders_total',
  help: 'Total number of orders created',
  labelNames: ['status']
});

// Metrics middleware
const metricsMiddleware = (req, res, next) => {
  const start = Date.now();
  
  res.on('finish', () => {
    const duration = (Date.now() - start) / 1000;
    
    httpRequestDuration
      .labels(req.method, req.route?.path || req.url, res.statusCode)
      .observe(duration);
    
    httpRequestsTotal
      .labels(req.method, req.route?.path || req.url, res.statusCode)
      .inc();
  });
  
  next();
};

// Metrics endpoint
app.get('/metrics', (req, res) => {
  res.set('Content-Type', promClient.register.contentType);
  res.end(promClient.register.metrics());
});
```

## 🚨 Alerting Configuration

### Critical Alerts
```yaml
# alerting_rules.yml
groups:
  - name: aims_critical_alerts
    rules:
      - alert: HighErrorRate
        expr: rate(http_requests_total{status=~"5.."}[5m]) > 0.1
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "High error rate detected"
          description: "Error rate is {{ $value }}% for the last 5 minutes"
      
      - alert: HighResponseTime
        expr: histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m])) > 1.0
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High response time detected"
          description: "95th percentile response time is {{ $value }}s"
      
      - alert: ServiceDown
        expr: up == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Service is down"
          description: "{{ $labels.instance }} has been down for more than 1 minute"
      
      - alert: HighCPUUsage
        expr: 100 - (avg by(instance) (irate(node_cpu_seconds_total{mode="idle"}[5m])) * 100) > 80
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High CPU usage"
          description: "CPU usage is {{ $value }}% on {{ $labels.instance }}"
      
      - alert: HighMemoryUsage
        expr: (1 - (node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes)) * 100 > 90
        for: 3m
        labels:
          severity: critical
        annotations:
          summary: "High memory usage"
          description: "Memory usage is {{ $value }}% on {{ $labels.instance }}"
```

### Alert Manager Configuration
```yaml
# alertmanager.yml
global:
  smtp_smarthost: 'localhost:587'
  smtp_from: 'alerts@aims.com'

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
      - url: 'http://webhook:5000/alert'
  
  - name: 'critical-alerts'
    email_configs:
      - to: 'oncall@aims.com'
        subject: 'CRITICAL: {{ .GroupLabels.alertname }}'
        body: |
          {{ range .Alerts }}
          Alert: {{ .Annotations.summary }}
          Description: {{ .Annotations.description }}
          {{ end }}
    slack_configs:
      - api_url: 'https://hooks.slack.com/services/...'
        channel: '#critical-alerts'
        text: 'CRITICAL ALERT: {{ .GroupLabels.alertname }}'
  
  - name: 'warning-alerts'
    email_configs:
      - to: 'team@aims.com'
        subject: 'WARNING: {{ .GroupLabels.alertname }}'
        body: |
          {{ range .Alerts }}
          Alert: {{ .Annotations.summary }}
          Description: {{ .Annotations.description }}
          {{ end }}
```

## 📊 Business Intelligence Dashboards

### E-commerce KPI Dashboard
```json
{
  "dashboard": {
    "title": "AIMS Business Intelligence",
    "panels": [
      {
        "title": "Sales Overview",
        "type": "stat",
        "targets": [
          {
            "expr": "sum(increase(orders_total[24h]))",
            "legendFormat": "Orders Today"
          },
          {
            "expr": "sum(increase(revenue_total[24h]))",
            "legendFormat": "Revenue Today"
          }
        ]
      },
      {
        "title": "Conversion Funnel",
        "type": "bargauge",
        "targets": [
          {
            "expr": "product_views_total",
            "legendFormat": "Product Views"
          },
          {
            "expr": "cart_additions_total",
            "legendFormat": "Cart Additions"
          },
          {
            "expr": "checkout_starts_total",
            "legendFormat": "Checkout Starts"
          },
          {
            "expr": "orders_total",
            "legendFormat": "Orders Completed"
          }
        ]
      },
      {
        "title": "Top Products",
        "type": "table",
        "targets": [
          {
            "expr": "topk(10, sum by(product_name) (increase(product_sales_total[24h])))",
            "format": "table"
          }
        ]
      }
    ]
  }
}
```

## 🔍 Log Analysis & Monitoring

### Structured Logging Configuration
```javascript
// Application logging setup
const winston = require('winston');
const { ElasticsearchTransport } = require('winston-elasticsearch');

const logger = winston.createLogger({
  level: 'info',
  format: winston.format.combine(
    winston.format.timestamp(),
    winston.format.errors({ stack: true }),
    winston.format.json()
  ),
  defaultMeta: {
    service: 'aims-web',
    version: process.env.APP_VERSION
  },
  transports: [
    new winston.transports.File({ filename: 'error.log', level: 'error' }),
    new winston.transports.File({ filename: 'combined.log' }),
    new winston.transports.Console({
      format: winston.format.simple()
    }),
    new ElasticsearchTransport({
      level: 'info',
      clientOpts: { node: 'http://elasticsearch:9200' },
      index: 'aims-logs'
    })
  ]
});

// Structured log examples
logger.info('User logged in', {
  userId: '12345',
  email: 'user@example.com',
  loginMethod: 'email',
  userAgent: req.headers['user-agent'],
  ipAddress: req.ip
});

logger.error('Payment processing failed', {
  orderId: 'order_789',
  paymentMethod: 'vnpay',
  amount: 99.99,
  errorCode: 'PAYMENT_DECLINED',
  errorMessage: 'Insufficient funds'
});
```

### Loki Configuration for Log Aggregation
```yaml
# loki-config.yml
auth_enabled: false

server:
  http_listen_port: 3100

ingester:
  lifecycler:
    address: 127.0.0.1
    ring:
      kvstore:
        store: inmemory
      replication_factor: 1
    final_sleep: 0s
  chunk_idle_period: 5m
  chunk_retain_period: 30s

schema_config:
  configs:
    - from: 2020-10-24
      store: boltdb
      object_store: filesystem
      schema: v11
      index:
        prefix: index_
        period: 168h

storage_config:
  boltdb:
    directory: /loki/index
  filesystem:
    directory: /loki/chunks

limits_config:
  enforce_metric_name: false
  reject_old_samples: true
  reject_old_samples_max_age: 168h

chunk_store_config:
  max_look_back_period: 0s

table_manager:
  retention_deletes_enabled: false
  retention_period: 0s
```

## 📞 Monitoring Support & Contacts

### Monitoring Team Contacts
- **DevOps Lead**: devops-lead@aims.com
- **Site Reliability Engineer**: sre@aims.com
- **Performance Engineer**: performance@aims.com
- **On-Call Engineer**: oncall@aims.com (24/7)

### Escalation Procedures
1. **Level 1**: Automated alerts and self-healing
2. **Level 2**: On-call engineer response (< 15 minutes)
3. **Level 3**: Senior engineer escalation (< 30 minutes)
4. **Level 4**: Management escalation (< 1 hour)

### Monitoring SLA
- **Alert Response**: < 5 minutes for critical alerts
- **Issue Resolution**: < 4 hours for P1 issues
- **Dashboard Availability**: 99.9% uptime
- **Data Retention**: 30 days for metrics, 90 days for logs

---

**Monitoring Setup**: Production Ready  
**Last Updated**: December 22, 2024  
**Dashboard Version**: v1.0.0  
**Monitoring Coverage**: 100% of critical components
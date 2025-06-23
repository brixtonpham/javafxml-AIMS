# AIMS Deployment Guide

This comprehensive guide covers deploying the AIMS Web Application to production environments, including Docker containerization, CI/CD pipelines, and infrastructure configuration.

## 📚 Deployment Documentation

### Production Setup
- [**Production Environment Setup**](./production-setup.md) - Complete production deployment guide
- [**Docker Configuration**](./docker.md) - Containerization and orchestration
- [**Environment Configuration**](./environment-config.md) - Environment variables and secrets management
- [**SSL/TLS Setup**](./ssl-setup.md) - Security certificate configuration

### CI/CD Pipeline
- [**GitHub Actions Workflow**](./github-actions.md) - Automated deployment pipeline
- [**Build Process**](./build-process.md) - Production build optimization
- [**Testing in CI/CD**](./ci-testing.md) - Automated testing in deployment pipeline
- [**Deployment Strategies**](./deployment-strategies.md) - Blue-green, rolling, and canary deployments

### Infrastructure
- [**Server Requirements**](./server-requirements.md) - Hardware and software specifications
- [**Load Balancing**](./load-balancing.md) - Nginx configuration and scaling
- [**CDN Configuration**](./cdn-setup.md) - Content delivery optimization
- [**Backup & Recovery**](./backup-recovery.md) - Data protection strategies

### Monitoring & Maintenance
- [**Health Checks**](./health-checks.md) - Application monitoring and alerting
- [**Log Management**](./logging.md) - Centralized logging and analysis
- [**Performance Monitoring**](./performance-monitoring.md) - APM and metrics collection
- [**Troubleshooting**](./troubleshooting.md) - Common issues and solutions

## 🚀 Quick Deployment

### Prerequisites
- Docker 20.10+
- Docker Compose 2.0+
- Node.js 18+ (for local builds)
- SSL certificates (for HTTPS)
- Domain configuration

### Rapid Production Deployment
```bash
# Clone and prepare
git clone [repository-url]
cd web-ui

# Build production image
docker build -t aims-web:latest -f docker/Dockerfile.production .

# Deploy with docker-compose
docker-compose -f docker/docker-compose.production.yml up -d

# Verify deployment
curl -f http://localhost/health || echo "Deployment failed"
```

## 🏗️ Architecture Overview

### Production Architecture
```
Internet
    ↓
Load Balancer (Nginx)
    ↓
┌─────────────────────────────────────┐
│  Docker Container Network          │
│  ┌─────────────┐  ┌─────────────┐   │
│  │   Web App   │  │   Web App   │   │
│  │  (Node.js)  │  │  (Node.js)  │   │
│  └─────────────┘  └─────────────┘   │
│         ↓                ↓          │
│  ┌─────────────┐  ┌─────────────┐   │
│  │   API       │  │  Database   │   │
│  │ (Java/REST) │  │  (SQLite)   │   │
│  └─────────────┘  └─────────────┘   │
└─────────────────────────────────────┘
```

### Deployment Components
- **Frontend**: React SPA served by Nginx
- **API Gateway**: Reverse proxy to Java backend
- **Static Assets**: CDN-served with caching
- **Database**: SQLite with backup automation
- **Monitoring**: Health checks and metrics collection

## 🐳 Docker Configuration

### Multi-Stage Production Build
```dockerfile
# Build stage
FROM node:18-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci --only=production
COPY . .
RUN npm run build

# Production stage
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY docker/nginx.conf /etc/nginx/nginx.conf
EXPOSE 80 443
CMD ["nginx", "-g", "daemon off;"]
```

### Docker Compose Production
```yaml
version: '3.8'
services:
  web:
    build:
      context: .
      dockerfile: docker/Dockerfile.production
    ports:
      - "80:80"
      - "443:443"
    environment:
      - NODE_ENV=production
    volumes:
      - ./ssl:/etc/ssl/certs
    restart: unless-stopped
    
  monitoring:
    image: nginx/nginx-prometheus-exporter
    ports:
      - "9113:9113"
    command:
      - -nginx.scrape-uri=http://web/nginx_status
```

## 🌐 Environment Configuration

### Production Environment Variables
```bash
# Application Configuration
NODE_ENV=production
VITE_API_BASE_URL=https://api.aims.com
VITE_APP_TITLE=AIMS - Internet Media Store
VITE_APP_VERSION=1.0.0

# Security Configuration
HTTPS_REDIRECT=true
SECURITY_HEADERS=true
CSP_POLICY=strict

# Performance Configuration
GZIP_COMPRESSION=true
STATIC_CACHE_TTL=31536000
API_CACHE_TTL=300

# Monitoring Configuration
ENABLE_METRICS=true
LOG_LEVEL=warn
HEALTH_CHECK_ENDPOINT=/health
```

### Secrets Management
```bash
# Using Docker secrets
echo "ssl_certificate_content" | docker secret create ssl_cert -
echo "ssl_private_key_content" | docker secret create ssl_key -

# Environment-specific secrets
docker run -d \
  --name aims-web \
  --secret ssl_cert \
  --secret ssl_key \
  -e SSL_CERT_FILE=/run/secrets/ssl_cert \
  -e SSL_KEY_FILE=/run/secrets/ssl_key \
  aims-web:latest
```

## ⚡ Performance Optimization

### Build Optimization
```bash
# Production build with analysis
npm run build:analyze

# Bundle size monitoring
npm run perf:bundle-size

# Lighthouse CI in production
npm run perf:ci
```

### Nginx Performance Configuration
```nginx
# Compression
gzip on;
gzip_vary on;
gzip_min_length 1024;
gzip_types text/plain text/css application/json application/javascript;

# Caching
location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg)$ {
    expires 1y;
    add_header Cache-Control "public, immutable";
}

# Security headers
add_header X-Frame-Options DENY;
add_header X-Content-Type-Options nosniff;
add_header X-XSS-Protection "1; mode=block";
```

## 📊 Monitoring & Health Checks

### Application Health Check
```javascript
// Health check endpoint
app.get('/health', (req, res) => {
  res.json({
    status: 'healthy',
    timestamp: new Date().toISOString(),
    version: process.env.VITE_APP_VERSION,
    uptime: process.uptime(),
    memory: process.memoryUsage(),
    environment: process.env.NODE_ENV
  });
});
```

### Docker Health Check
```dockerfile
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost/health || exit 1
```

### Monitoring Stack
```yaml
# Prometheus + Grafana monitoring
version: '3.8'
services:
  prometheus:
    image: prom/prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./monitoring/prometheus.yml:/etc/prometheus/prometheus.yml
      
  grafana:
    image: grafana/grafana
    ports:
      - "3001:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=secure_password
    volumes:
      - grafana-storage:/var/lib/grafana
```

## 🔒 Security Configuration

### SSL/TLS Setup
```nginx
server {
    listen 443 ssl http2;
    server_name aims.com www.aims.com;
    
    ssl_certificate /etc/ssl/certs/aims.crt;
    ssl_certificate_key /etc/ssl/private/aims.key;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE+AESGCM:ECDHE+CHACHA20:DHE+AESGCM:DHE+CHACHA20:!aNULL:!MD5:!DSS;
    
    # Security headers
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    add_header Content-Security-Policy "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'";
}
```

### Container Security
```dockerfile
# Non-root user
RUN addgroup -g 1001 -S nodejs
RUN adduser -S nextjs -u 1001
USER nextjs

# Security scanning
FROM node:18-alpine
RUN apk add --no-cache dumb-init
RUN npm audit --audit-level high
```

## 🚀 CI/CD Pipeline

### GitHub Actions Workflow
```yaml
name: Deploy to Production
on:
  push:
    branches: [main]
    
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-node@v3
        with:
          node-version: '18'
      - run: npm ci
      - run: npm run test:all
      - run: npm run lint
      
  build-and-deploy:
    needs: test
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Build Docker image
        run: docker build -t aims-web:${{ github.sha }} .
      - name: Deploy to production
        run: |
          docker tag aims-web:${{ github.sha }} aims-web:latest
          docker-compose -f docker-compose.prod.yml up -d
```

## 📈 Scaling Strategies

### Horizontal Scaling
```yaml
# Docker Swarm scaling
version: '3.8'
services:
  web:
    image: aims-web:latest
    deploy:
      replicas: 3
      update_config:
        parallelism: 1
        delay: 10s
        failure_action: rollback
      restart_policy:
        condition: on-failure
```

### Load Balancing
```nginx
upstream backend {
    least_conn;
    server web1:3000;
    server web2:3000;
    server web3:3000;
}

server {
    location / {
        proxy_pass http://backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

## 🔧 Troubleshooting

### Common Issues
1. **Build Failures**: Check Node.js version and dependencies
2. **Container Start Issues**: Verify environment variables and ports
3. **SSL Certificate Problems**: Ensure certificate validity and paths
4. **Performance Issues**: Monitor resource usage and optimize bundles

### Debug Commands
```bash
# Container debugging
docker logs aims-web -f
docker exec -it aims-web sh

# Health check verification
curl -f https://aims.com/health

# Performance analysis
docker stats aims-web
```

---

**Need deployment assistance?** Contact the DevOps team or refer to the specific deployment guides for detailed instructions.
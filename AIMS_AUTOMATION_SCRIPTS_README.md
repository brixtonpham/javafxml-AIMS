# AIMS Project Automation Scripts

## 🚀 Overview

Complete automation solution for dual-side startup of the AIMS e-commerce platform, featuring Spring Boot backend (Java 21, Maven, port 8080) and React frontend (Node.js, Vite, port 3000) with comprehensive health monitoring and graceful shutdown capabilities.

## 📁 File Structure

```
AIMS_PROJECT/
├── start-aims.sh              # Master orchestrator script
├── stop-aims.sh               # Graceful shutdown script
├── health-check.sh            # Service monitoring script
├── scripts/
│   ├── start-backend.sh       # Spring Boot startup
│   └── start-frontend.sh      # React/Vite startup
└── logs/                      # Centralized logging directory
    ├── startup.log
    ├── backend.log
    ├── frontend.log
    ├── shutdown.log
    └── health-check.log
```

## 🛠️ Scripts Overview

### 1. [`start-aims.sh`](start-aims.sh) - Master Orchestrator
**Purpose**: One-command startup for the entire AIMS platform

**Features**:
- ✅ Pre-flight system checks (Java 21, Maven, Node.js, npm)
- ✅ Port availability verification (8080, 3000)
- ✅ Automatic terminal management for macOS
- ✅ Service dependency management
- ✅ Health check integration
- ✅ Comprehensive error handling
- ✅ Beautiful CLI interface with colored output

**Usage**:
```bash
./start-aims.sh
```

### 2. [`scripts/start-backend.sh`](scripts/start-backend.sh) - Backend Startup
**Purpose**: Spring Boot API server initialization

**Features**:
- ✅ Environment variable configuration
- ✅ Database connectivity checks
- ✅ Maven compilation and testing
- ✅ Detailed logging
- ✅ API endpoint documentation

**Manual Usage**:
```bash
./scripts/start-backend.sh
```

### 3. [`scripts/start-frontend.sh`](scripts/start-frontend.sh) - Frontend Startup
**Purpose**: React + Vite development server initialization

**Features**:
- ✅ Dependency management (npm install)
- ✅ Package.json change detection
- ✅ Environment configuration
- ✅ Hot module replacement setup
- ✅ Development server optimization

**Manual Usage**:
```bash
./scripts/start-frontend.sh
```

### 4. [`stop-aims.sh`](stop-aims.sh) - Graceful Shutdown
**Purpose**: Safe termination of all AIMS services

**Features**:
- ✅ Graceful process termination (SIGTERM → SIGKILL)
- ✅ Port-based process detection
- ✅ Process name pattern matching
- ✅ Alternative port cleanup
- ✅ Final verification
- ✅ Comprehensive cleanup reporting

**Usage**:
```bash
./stop-aims.sh
```

### 5. [`health-check.sh`](health-check.sh) - Service Monitoring
**Purpose**: Comprehensive system health verification

**Features**:
- ✅ Service availability checks (ports 8080, 3000)
- ✅ HTTP health endpoint testing
- ✅ API endpoint validation
- ✅ CORS configuration verification
- ✅ Frontend-backend integration testing
- ✅ System resource monitoring
- ✅ Log file error analysis
- ✅ Detailed health reporting

**Usage**:
```bash
./health-check.sh
```

## 🖥️ Platform Support

### macOS (Primary Target)
- ✅ Automatic Terminal.app integration
- ✅ Native osascript terminal management
- ✅ Memory pressure monitoring
- ✅ System resource optimization

### Linux (Fallback Support)
- ✅ GNOME Terminal integration
- ✅ xterm fallback
- ✅ Background process management
- ✅ Standard Unix tools

## 🔧 System Requirements

### Prerequisites
- **Java 21+** (OpenJDK or Oracle JDK)
- **Apache Maven 3.6+**
- **Node.js 18+**
- **npm 8+**

### Optional Tools (Enhanced Functionality)
- `curl` - HTTP health checks
- `lsof` - Port monitoring
- `nc` (netcat) - Connection testing
- `bc` - Mathematical calculations

### Installation Verification
```bash
# Check all prerequisites
java -version    # Should show Java 21+
mvn -version     # Should show Maven 3.6+
node -v          # Should show Node 18+
npm -v           # Should show npm 8+
```

## 🚀 Quick Start Guide

### 1. Initial Setup (One-time)
```bash
# Clone/navigate to AIMS project
cd /path/to/aims-project

# Verify scripts are executable
ls -la *.sh scripts/*.sh

# If not executable, run:
chmod +x start-aims.sh stop-aims.sh health-check.sh scripts/*.sh
```

### 2. Start Development Environment
```bash
# Start everything with one command
./start-aims.sh

# Wait for services to initialize (30-60 seconds)
# Frontend: http://localhost:3000
# Backend: http://localhost:8080
# API Docs: http://localhost:8080/swagger-ui/index.html
```

### 3. Monitor System Health
```bash
# Check service status anytime
./health-check.sh

# View live logs
tail -f logs/backend.log
tail -f logs/frontend.log
```

### 4. Shutdown Services
```bash
# Graceful shutdown of all services
./stop-aims.sh
```

## 📊 Service URLs

| Service | URL | Description |
|---------|-----|-------------|
| **Frontend** | http://localhost:3000 | React application |
| **Backend API** | http://localhost:8080 | Spring Boot REST API |
| **API Documentation** | http://localhost:8080/swagger-ui/index.html | Interactive API docs |
| **API Schema** | http://localhost:8080/v3/api-docs | OpenAPI specification |
| **Health Check** | http://localhost:8080/actuator/health | Backend health endpoint |

## 📋 Development Workflow

### Daily Development Cycle
```bash
# Morning startup
./start-aims.sh

# Verify everything is working
./health-check.sh

# Development work...
# Frontend: http://localhost:3000
# Backend: http://localhost:8080

# Evening shutdown
./stop-aims.sh
```

### Troubleshooting
```bash
# Check what's running on ports
lsof -i :8080,:3000

# View detailed logs
tail -50 logs/startup.log
tail -50 logs/backend.log
tail -50 logs/frontend.log

# Force cleanup if needed
./stop-aims.sh
pkill -f "spring-boot\|vite\|npm.*dev"
```

## 🔍 Logging System

### Log Files Location: [`logs/`](logs/)

| Log File | Purpose |
|----------|---------|
| `startup.log` | Master script execution logs |
| `backend.log` | Spring Boot application logs |
| `frontend.log` | React/Vite development server logs |
| `shutdown.log` | Service termination logs |
| `health-check.log` | System monitoring logs |

### Log Rotation
- Manual cleanup: `rm logs/*.log`
- Logs are appended (not overwritten)
- Consider implementing logrotate for production

## ⚡ Performance Features

### Backend Optimizations
- Maven dependency caching
- Parallel test execution
- JVM memory optimization
- Database connection pooling

### Frontend Optimizations
- Vite hot module replacement
- Dependency pre-bundling
- Code splitting
- Asset optimization

### System Resource Monitoring
- Memory usage tracking
- Disk space monitoring
- CPU load analysis
- Process health verification

## 🛡️ Error Handling

### Automatic Recovery
- Port conflict resolution
- Dependency installation
- Service restart capabilities
- Graceful degradation

### Manual Intervention Points
- Java version compatibility
- Maven repository issues
- Node.js dependency conflicts
- Database connectivity problems

## 🔧 Customization Options

### Environment Variables
```bash
# Backend configuration
export SPRING_PROFILES_ACTIVE=dev
export SERVER_PORT=8080
export SPRING_DATASOURCE_URL=jdbc:sqlite:aims_database.db

# Frontend configuration
export NODE_ENV=development
export VITE_API_BASE_URL=http://localhost:8080
export VITE_APP_TITLE="AIMS E-commerce Platform"
```

### Port Configuration
Edit the scripts to change default ports:
- `BACKEND_PORT=8080` → Custom backend port
- `FRONTEND_PORT=3000` → Custom frontend port

## 📈 Success Metrics

### Startup Success Indicators
- ✅ All pre-flight checks pass
- ✅ Both terminals open automatically
- ✅ Services respond to health checks
- ✅ Integration tests pass
- ✅ No errors in logs

### Health Check Indicators
- ✅ HTTP 200 responses from both services
- ✅ CORS headers configured properly
- ✅ API endpoints accessible
- ✅ System resources within limits
- ✅ No recent errors in logs

## 🚨 Troubleshooting Guide

### Common Issues

1. **Port Already in Use**
   ```bash
   # Solution: Scripts automatically handle this
   ./stop-aims.sh  # Force cleanup
   ./start-aims.sh # Restart
   ```

2. **Java Version Issues**
   ```bash
   # Check version
   java -version
   # Update JAVA_HOME if needed
   export JAVA_HOME=/path/to/java21
   ```

3. **Node Dependencies Issues**
   ```bash
   cd web-ui
   rm -rf node_modules package-lock.json
   npm install
   cd ..
   ./start-aims.sh
   ```

4. **Database Issues**
   ```bash
   # Check database file
   ls -la aims_database.db
   # Reset if needed (WARNING: Data loss)
   rm aims_database.db
   ./start-aims.sh
   ```

## 🎯 Next Steps

### Production Deployment
- Docker containerization
- Environment-specific configurations
- Load balancing setup
- SSL/TLS certificate configuration
- Database migration scripts

### CI/CD Integration
- GitHub Actions workflow
- Automated testing pipeline
- Quality gate checks
- Deployment automation

## 📞 Support

### Documentation
- [AIMS Project README](README.md)
- [Deployment Guide](AIMS_PROJECT_DEPLOYMENT_GUIDE.md)
- [Web Application Plan](AIMS_WEB_APPLICATION_COMPLETE_IMPLEMENTATION_PLAN.md)

### Getting Help
1. Check logs: `tail -f logs/*.log`
2. Run health check: `./health-check.sh`
3. Review this documentation
4. Contact development team

---

## 🎉 Success! 

You now have a complete automation solution for the AIMS project with:
- **One-command startup**: `./start-aims.sh`
- **Health monitoring**: `./health-check.sh`
- **Graceful shutdown**: `./stop-aims.sh`
- **Comprehensive logging**: `logs/`
- **Cross-platform support**: macOS + Linux

**Happy coding! 🚀**
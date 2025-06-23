# AIMS Developer Guide

Welcome to the AIMS Web Application developer documentation. This guide provides comprehensive information for developers working on the AIMS platform.

## 📚 Developer Documentation

### Getting Started
- [**Development Setup**](./setup.md) - Environment setup and installation guide
- [**Architecture Overview**](./architecture.md) - System design and technical architecture
- [**Development Workflow**](./workflow.md) - Development processes and best practices
- [**Contributing Guidelines**](./contributing.md) - How to contribute to the project

### Technical Documentation
- [**API Reference**](../api/) - Complete API documentation and integration guides
- [**Component Library**](./components.md) - Reusable UI component documentation
- [**State Management**](./state-management.md) - Application state architecture
- [**Testing Strategy**](./testing.md) - Testing approaches and guidelines
- [**Performance Optimization**](./performance.md) - Performance best practices

### Code Quality
- [**Code Standards**](./code-standards.md) - Coding conventions and style guide
- [**Security Guidelines**](./security.md) - Security best practices for developers
- [**Accessibility Development**](./accessibility.md) - Building accessible features
- [**Error Handling**](./error-handling.md) - Error management strategies

### Deployment & Operations
- [**Build Process**](./build-process.md) - Build configuration and optimization
- [**Environment Configuration**](./environment-config.md) - Environment setup and variables
- [**Monitoring & Debugging**](./monitoring.md) - Development tools and debugging
- [**Dependencies Management**](./dependencies.md) - Managing project dependencies

## 🏗️ Technical Stack

### Frontend Technologies
```
React 19.1.0          - UI library with latest features
TypeScript 5.8.3      - Type safety and enhanced DX
Vite 6.3.5            - Build tool and development server
Tailwind CSS 4.1.10   - Utility-first CSS framework
```

### State Management
```
Zustand 5.0.5         - Lightweight state management
React Query 5.81.2    - Server state management
React Hook Form 7.58.1 - Form state management
```

### Development Tools
```
ESLint 9.25.0         - Code linting and quality
Prettier 3.5.3        - Code formatting
Vitest 2.1.6          - Unit testing framework
Playwright 1.48.0     - E2E testing framework
```

### Performance & Monitoring
```
Lighthouse CI         - Performance monitoring
Bundle Analyzer        - Bundle size analysis
Coverage Reports       - Code coverage tracking
Rollup Visualizer     - Bundle composition analysis
```

## 🚀 Quick Start

### Prerequisites
- Node.js 18+ (recommended: 20+)
- npm 9+ or yarn 3+
- Git 2.30+
- VS Code (recommended)

### Initial Setup
```bash
# Clone the repository
git clone [repository-url]
cd web-ui

# Install dependencies
npm install

# Set up environment
cp .env.development .env.local

# Start development server
npm run dev
```

### Development Commands
```bash
# Development
npm run dev              # Start development server
npm run build            # Build for production
npm run preview          # Preview production build

# Testing
npm run test             # Run all tests
npm run test:unit        # Run unit tests
npm run test:e2e         # Run E2E tests
npm run test:coverage    # Generate coverage report

# Code Quality
npm run lint             # Run ESLint
npm run lint:fix         # Fix ESLint issues
npm run format           # Format code with Prettier

# Performance
npm run build:analyze    # Analyze bundle size
npm run perf:lighthouse  # Run Lighthouse audit
```

## 📁 Project Structure

```
web-ui/
├── public/                 # Static assets
├── src/
│   ├── components/         # Reusable UI components
│   │   ├── ui/            # Base UI components
│   │   ├── cart/          # Cart-specific components
│   │   ├── products/      # Product-related components
│   │   ├── orders/        # Order management components
│   │   ├── admin/         # Admin dashboard components
│   │   ├── auth/          # Authentication components
│   │   ├── layout/        # Layout components
│   │   └── forms/         # Form components
│   ├── pages/             # Page components and routing
│   ├── hooks/             # Custom React hooks
│   ├── contexts/          # React context providers
│   ├── services/          # API services and external integrations
│   ├── utils/             # Utility functions
│   ├── types/             # TypeScript type definitions
│   ├── styles/            # Global styles and CSS modules
│   └── tests/             # Test files and utilities
├── docs/                  # Documentation
├── reports/               # Performance and test reports
└── coverage/              # Code coverage reports
```

## 🔧 Development Environment

### Required Extensions (VS Code)
```json
{
  "recommendations": [
    "esbenp.prettier-vscode",
    "bradlc.vscode-tailwindcss",
    "ms-vscode.vscode-typescript-next",
    "ms-playwright.playwright",
    "vitest.explorer"
  ]
}
```

### Environment Variables
```bash
# Development (.env.development)
VITE_API_BASE_URL=http://localhost:8080/api
VITE_APP_TITLE=AIMS - Internet Media Store
VITE_ENABLE_DEV_TOOLS=true

# Production (.env.production)
VITE_API_BASE_URL=https://api.aims.com
VITE_APP_TITLE=AIMS - Internet Media Store
VITE_ENABLE_DEV_TOOLS=false
```

## 🧪 Testing Strategy

### Test Types
- **Unit Tests**: Component logic and utility functions
- **Integration Tests**: Component interactions and data flow
- **E2E Tests**: Complete user workflows and scenarios
- **Performance Tests**: Load times and responsiveness
- **Accessibility Tests**: Screen reader and keyboard navigation

### Test Coverage Goals
- **Overall Coverage**: >80%
- **Component Coverage**: >85%
- **Service Coverage**: >90%
- **Critical Path Coverage**: 100%

## 🎨 Design System

### Component Hierarchy
```
Base Components (ui/)
├── Button, Input, Card
├── Modal, Toast, Loader
└── Layout, Grid, Container

Feature Components
├── ProductCard, CartItem
├── OrderTimeline, PaymentForm
└── AdminDashboard, UserMenu

Page Components
├── ProductListPage, CartPage
├── CheckoutPage, OrdersPage
└── AdminDashboard, LoginPage
```

### Styling Approach
- **Tailwind CSS**: Utility-first styling
- **Component Variants**: Consistent design patterns
- **Responsive Design**: Mobile-first approach
- **Dark Mode Support**: System preference detection
- **High Contrast Mode**: Accessibility compliance

## 🔐 Security Considerations

### Frontend Security
- **Input Validation**: Client-side validation with server verification
- **XSS Prevention**: Sanitized data rendering and CSP headers
- **CSRF Protection**: Token-based request validation
- **Secure Storage**: Encrypted local storage for sensitive data

### Authentication & Authorization
- **JWT Tokens**: Secure token-based authentication
- **Role-Based Access**: Component-level permission checks
- **Session Management**: Automatic token refresh and logout
- **Route Protection**: Authentication-required route guards

## 📊 Performance Optimization

### Build Optimization
- **Code Splitting**: Route-based and component-based chunking
- **Tree Shaking**: Unused code elimination
- **Bundle Analysis**: Regular bundle size monitoring
- **Asset Optimization**: Image compression and lazy loading

### Runtime Performance
- **React Optimization**: Memoization and virtualization
- **Network Optimization**: Request caching and batching
- **Loading States**: Progressive loading and skeleton screens
- **Error Boundaries**: Graceful error handling and recovery

## 🤝 Contributing

### Development Process
1. **Fork & Clone**: Create your development branch
2. **Feature Development**: Implement features with tests
3. **Code Review**: Submit PR with comprehensive description
4. **Testing**: Ensure all tests pass and coverage meets requirements
5. **Documentation**: Update relevant documentation
6. **Deployment**: Deploy to staging for final verification

### Pull Request Guidelines
- Clear, descriptive titles and descriptions
- Link to related issues or feature requests
- Include screenshots for UI changes
- Ensure tests pass and coverage is maintained
- Follow established code style and conventions

## 📞 Developer Support

### Internal Resources
- **Team Chat**: Development team communication channel
- **Code Reviews**: Peer review process and guidelines
- **Technical Meetings**: Weekly architecture and planning discussions
- **Knowledge Base**: Internal documentation and best practices

### External Resources
- **React Documentation**: Official React guides and API reference
- **TypeScript Handbook**: TypeScript language features and patterns
- **Tailwind CSS Docs**: Utility classes and customization options
- **Testing Library**: Testing best practices and utilities

---

**Ready to contribute?** Start with the [Development Setup](./setup.md) guide and join our development community!
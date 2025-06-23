# AIMS API Documentation

Complete API documentation for the AIMS Web Application service layer. This documentation provides comprehensive integration guides, examples, and technical specifications for all available APIs.

## 📚 API Documentation Structure

### Core APIs
- [**Authentication API**](./authentication.md) - User authentication and session management
- [**Product API**](./products.md) - Product catalog and inventory management
- [**Cart API**](./cart.md) - Shopping cart operations and management
- [**Order API**](./orders.md) - Order processing and management
- [**Payment API**](./payment.md) - Payment processing and VNPay integration

### Advanced APIs
- [**Admin API**](./admin.md) - Administrative operations and management
- [**User Management API**](./users.md) - User account and profile management
- [**Delivery API**](./delivery.md) - Delivery calculations and management
- [**Refund API**](./refunds.md) - Refund processing and management

### Integration Resources
- [**OpenAPI Specification**](./openapi.yaml) - Complete OpenAPI 3.0 specification
- [**API Examples**](./examples/) - Request/response examples and use cases
- [**SDK Documentation**](./sdk/) - Client SDK and integration libraries
- [**Testing Guide**](./testing.md) - API testing strategies and tools

## 🚀 Quick Start

### Base Configuration
```javascript
const API_BASE_URL = 'https://api.aims.com'
// Development: http://localhost:8080/api

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'application/json'
  }
});
```

### Authentication Setup
```javascript
// Add authentication token to requests
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('auth_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});
```

### Basic API Usage
```javascript
// Get products
const products = await apiClient.get('/products');

// Add to cart
const cartResponse = await apiClient.post('/cart/items', {
  productId: 'prod_123',
  quantity: 2
});

// Create order
const order = await apiClient.post('/orders', {
  cartSessionId: 'session_xyz'
});
```

## 📋 API Standards

### Response Format
All API responses follow a consistent structure:

```json
{
  "success": true,
  "message": "Operation completed successfully",
  "data": {},
  "pagination": {
    "page": 1,
    "pageSize": 20,
    "total": 100,
    "totalPages": 5
  },
  "errors": []
}
```

### Error Handling
```json
{
  "success": false,
  "message": "Validation failed",
  "errors": [
    {
      "field": "email",
      "code": "INVALID_EMAIL",
      "message": "Please provide a valid email address"
    }
  ]
}
```

### HTTP Status Codes
- `200` - Success
- `201` - Created
- `400` - Bad Request
- `401` - Unauthorized
- `403` - Forbidden
- `404` - Not Found
- `422` - Validation Error
- `500` - Internal Server Error

## 🔐 Authentication

### JWT Token Authentication
```bash
# Login request
POST /auth/login
{
  "email": "user@example.com",
  "password": "secure_password"
}

# Response
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "user": {
      "id": "user_123",
      "email": "user@example.com",
      "role": "customer"
    }
  }
}
```

### Using Authentication Token
```bash
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

## 📊 Rate Limiting

### Rate Limit Headers
```bash
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 999
X-RateLimit-Reset: 1609459200
```

### Rate Limits by Endpoint
- **General API**: 1000 requests/hour
- **Authentication**: 10 requests/minute
- **File Upload**: 100 requests/hour
- **Admin Operations**: 500 requests/hour

## 🛠️ Development Tools

### API Testing with cURL
```bash
# Test product endpoint
curl -X GET "https://api.aims.com/products" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json"
```

### Postman Collection
Import our [Postman collection](./postman/AIMS-API.postman_collection.json) for interactive API testing.

### Swagger UI
Access interactive API documentation at: https://api.aims.com/docs

## 📈 Performance & Monitoring

### Response Time Targets
- **Product Catalog**: < 200ms
- **Cart Operations**: < 150ms
- **Order Processing**: < 500ms
- **Payment Operations**: < 1000ms

### Monitoring Endpoints
```bash
# Health check
GET /health

# API metrics
GET /metrics

# API status
GET /status
```

## 🔒 Security

### Security Headers
```bash
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 1; mode=block
Strict-Transport-Security: max-age=31536000
```

### Input Validation
- All inputs are validated and sanitized
- SQL injection protection
- XSS prevention
- CSRF protection with tokens

## 📞 Support

### Developer Support
- **API Issues**: Contact development team
- **Integration Help**: See [integration guides](./integration/)
- **Error Reporting**: Submit issues with full request/response details

### SLA & Availability
- **Uptime**: 99.9% availability
- **Support Hours**: 24/7 for critical issues
- **Response Time**: < 4 hours for API issues

---

**Version**: 1.0.0  
**Last Updated**: December 2024  
**Compatible API Version**: v1
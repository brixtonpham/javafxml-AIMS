# API Error Analysis & Fix Plan
## CORS and 403 Forbidden Errors Resolution

### Executive Summary

Multiple failed requests to `localhost:8080` with CORS policy violations and 403 Forbidden responses affecting:
- `GET /cart` → 403 Forbidden + CORS blocked
- `GET /orders/user?page=1&pageSize=10` → 403 Forbidden + CORS blocked

**Client Origin:** `http://localhost:3000`
**Server Origin:** `http://localhost:8080`

---

## Root Cause Analysis

### 1. Frontend-Backend URL Mismatch
- **Frontend calls:** `/cart` and `/orders/user`
- **Backend expects:** `/api/cart/{sessionId}` and `/api/orders/user/{userId}`
- **Impact:** Endpoints not found, falling back to authentication requirement

### 2. Security Configuration Issues
- [`SecurityConfig.java:47`](src/main/java/com/aims/core/config/SecurityConfig.java) permits `/api/**` but individual endpoints still require authentication
- Missing explicit `permitAll()` for public cart and order endpoints
- Default authentication requirement blocking public access

### 3. CORS Configuration Conflicts
- **Duplicate CORS setup:** Both [`CorsConfig.java`](src/main/java/com/aims/core/config/CorsConfig.java) and [`WebConfig.java`](src/main/java/com/aims/core/config/WebConfig.java) define CORS
- **Configuration override issues:** Multiple CORS sources causing conflicts
- **Missing preflight handling:** OPTIONS requests not properly configured

### 4. Frontend Service Layer Issues
- [`cartService.ts:12`](web-ui/src/services/cartService.ts) - Incorrect URL pattern
- [`orderService.ts:27`](web-ui/src/services/orderService.ts) - Missing user ID parameter
- Session management not aligned with backend expectations

---

## Implementation Plan

### Phase 1: Backend Security Configuration Fixes (HIGH PRIORITY)

#### Target Files:
- [`SecurityConfig.java`](src/main/java/com/aims/core/config/SecurityConfig.java)
- [`CorsConfig.java`](src/main/java/com/aims/core/config/CorsConfig.java)
- [`WebConfig.java`](src/main/java/com/aims/core/config/WebConfig.java)

#### Changes Required:

**1. Update SecurityConfig.java**
```java
.authorizeHttpRequests(authz -> authz
    // Allow all cart and order endpoints for public access
    .requestMatchers("/api/cart/**").permitAll()
    .requestMatchers("/api/orders/**").permitAll()
    .requestMatchers("/api/products/**").permitAll()
    .requestMatchers("/api/**").permitAll()
    
    // Allow health check and actuator endpoints
    .requestMatchers("/actuator/**").permitAll()
    
    // Allow Swagger/OpenAPI documentation
    .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
    
    // Allow static resources
    .requestMatchers("/static/**", "/css/**", "/js/**", "/images/**").permitAll()
    
    // Require authentication for any other requests
    .anyRequest().authenticated()
)
```

**2. Consolidate CORS Configuration**
- Remove CORS configuration from [`WebConfig.java`](src/main/java/com/aims/core/config/WebConfig.java)
- Keep only [`CorsConfig.java`](src/main/java/com/aims/core/config/CorsConfig.java) as single source of truth
- Ensure proper preflight OPTIONS handling

### Phase 2: Frontend API Service Layer Fixes (HIGH PRIORITY)

#### Target Files:
- [`cartService.ts`](web-ui/src/services/cartService.ts)
- [`orderService.ts`](web-ui/src/services/orderService.ts)

#### URL Pattern Alignment:

**Current Issues:**
```typescript
// Frontend calls (INCORRECT)
GET /cart → Should be: GET /api/cart/{sessionId}
GET /orders/user → Should be: GET /api/orders/user/{userId}
```

**Required Fixes:**

**1. Fix cartService.ts**
```typescript
// Update getCart method to include sessionId
async getCart(sessionId?: string): Promise<Cart> {
  if (!sessionId) {
    sessionId = await this.ensureCartSession();
  }
  const response = await api.get<Cart>(`/cart/${sessionId}`);
  return response.data;
}
```

**2. Fix orderService.ts**
```typescript
// Update getUserOrders method to include userId
async getUserOrders(userId?: string, page = 1, pageSize = 20): Promise<PaginatedResponse<Order>> {
  if (!userId) {
    userId = this.getCurrentUserId();
  }
  return paginatedRequest<Order>(`/orders/user/${userId}`, {
    page,
    pageSize
  });
}

// Add helper method
private getCurrentUserId(): string {
  const userData = localStorage.getItem('user_data');
  return userData ? JSON.parse(userData).id : 'guest';
}
```

### Phase 3: CORS Configuration Cleanup (MEDIUM PRIORITY)

#### Changes Required:

**1. Remove Duplicate CORS from WebConfig.java**
```java
// DELETE this entire method from WebConfig.java
@Override
public void addCorsMappings(CorsRegistry registry) {
    // REMOVE THIS ENTIRE METHOD
}
```

**2. Enhance CorsConfig.java**
```java
// Ensure comprehensive CORS configuration
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    
    // Allow frontend origins
    configuration.setAllowedOriginPatterns(Arrays.asList(
        "http://localhost:3000",
        "http://127.0.0.1:3000",
        "http://localhost:3001",
        "http://127.0.0.1:3001"
    ));
    
    // Allow HTTP methods including OPTIONS for preflight
    configuration.setAllowedMethods(Arrays.asList(
        "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
    ));
    
    // Allow all headers
    configuration.setAllowedHeaders(Arrays.asList("*"));
    configuration.setAllowCredentials(true);
    configuration.setMaxAge(3600L);
    
    // Apply to all API endpoints
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", configuration);
    
    return source;
}
```

---

## Expected Fixes

### Before Implementation:
- ❌ `GET /cart` → 403 Forbidden + CORS blocked
- ❌ `GET /orders/user` → 403 Forbidden + CORS blocked
- ❌ Frontend-backend communication failing
- ❌ Multiple CORS configuration conflicts

### After Implementation:
- ✅ `GET /api/cart/{sessionId}` → 200 OK with cart data
- ✅ `GET /api/orders/user/{userId}` → 200 OK with user orders
- ✅ Proper CORS headers in all responses
- ✅ Frontend-backend communication working
- ✅ Single, consistent CORS configuration

---

## Testing Strategy

### Backend Testing:
```bash
# Test cart endpoint
curl -X GET "http://localhost:8080/api/cart/test-session" \
  -H "Origin: http://localhost:3000"

# Test orders endpoint  
curl -X GET "http://localhost:8080/api/orders/user/test-user" \
  -H "Origin: http://localhost:3000"

# Test CORS preflight
curl -X OPTIONS "http://localhost:8080/api/cart/test-session" \
  -H "Origin: http://localhost:3000" \
  -H "Access-Control-Request-Method: GET"
```

### Frontend Testing:
1. Open browser dev tools
2. Navigate to cart page
3. Check network tab for successful API calls
4. Verify no CORS errors in console

---

## Risk Assessment

### Low Risk:
- CORS configuration updates
- Frontend URL pattern fixes

### Medium Risk:
- Security configuration changes
- Backend endpoint path requirements

### Mitigation Strategy:
- Test each change incrementally
- Keep backup configurations
- Validate with curl/Postman before frontend testing
- Monitor application logs during testing

---

## Implementation Checklist

### Backend Changes:
- [ ] Update [`SecurityConfig.java`](src/main/java/com/aims/core/config/SecurityConfig.java) - Add explicit permitAll() for public endpoints
- [ ] Remove CORS from [`WebConfig.java`](src/main/java/com/aims/core/config/WebConfig.java) - Eliminate duplicate configuration
- [ ] Verify [`CorsConfig.java`](src/main/java/com/aims/core/config/CorsConfig.java) - Ensure comprehensive CORS setup
- [ ] Test backend endpoints with curl/Postman

### Frontend Changes:
- [ ] Fix [`cartService.ts`](web-ui/src/services/cartService.ts) - Update URL patterns and session handling
- [ ] Fix [`orderService.ts`](web-ui/src/services/orderService.ts) - Add user ID parameters and URL corrections
- [ ] Test frontend API calls in browser dev tools

### Validation:
- [ ] No CORS errors in browser console
- [ ] Successful API responses (200/201 status codes)
- [ ] Proper cart and order functionality
- [ ] End-to-end user workflow testing

---

## Next Steps

1. **Immediate**: Implement backend security configuration fixes
2. **Short-term**: Update frontend service layer URLs
3. **Medium-term**: Comprehensive testing and validation
4. **Long-term**: Consider implementing proper authentication when needed

---

*Generated on: 2025-06-23 10:56 AM*
*Project: AIMS Web API - CORS and 403 Error Resolution*
# AIMS Runtime Issues Remediation Plan
## Critical Bug Fixes & Functionality Verification

## 🚨 **Critical Issues Identified**

### **Issue 1: Foreign Key Constraint Failure (CRITICAL)**
```
Error Code: 19
Message: [SQLITE_CONSTRAINT_FOREIGNKEY] A foreign key constraint failed
Context: Adding product "Abbey Road" to cart
```

### **Issue 2: Deprecated API Usage (WARNING)**
```
VNPayAdapterImpl.java uses or overrides a deprecated API
```

### **Issue 3: Missing CSS Resources (MINOR)**
```
Warning: Global CSS not found at /styles/global.css
Warning: Theme CSS not found at /styles/theme.css
```

---

## 🔧 **Remediation Strategy**

### **Phase 1: Critical Database Fix (URGENT)**
**Priority**: CRITICAL - Blocks core functionality

**Root Cause Analysis:**
- Cart operations failing due to FK constraint violations
- Likely issues:
  1. CART table not properly initialized
  2. Missing cartSessionID in CART table
  3. Product ID doesn't exist in PRODUCT table
  4. User ID reference issues

**Tasks:**
1. **Database State Investigation**
   - Check if CART table has required records
   - Verify PRODUCT table has "Abbey Road" record
   - Validate FK relationships in schema

2. **Cart Service Fix**
   - Fix cart session creation logic
   - Ensure proper FK relationship handling
   - Add proper error handling and logging

3. **Data Initialization**
   - Ensure database is properly seeded
   - Verify all required test data exists
   - Fix any missing references

### **Phase 2: Service Integration Validation (HIGH)**
**Priority**: HIGH - Ensure all services work end-to-end

**Tasks:**
1. **Cart Service Complete Testing**
   - Add to cart functionality
   - Remove from cart
   - Update quantities
   - Clear cart operations

2. **Product Service Validation**
   - Search functionality
   - Category filtering
   - Price sorting
   - Pagination

3. **Order Service Testing**
   - Order creation from cart
   - Delivery info handling
   - Payment processing integration

### **Phase 3: UI Functionality Verification (MEDIUM)**
**Priority**: MEDIUM - Ensure UI components work correctly

**Tasks:**
1. **Home Screen Complete Testing**
   - Product display
   - Search and filtering
   - Add to cart from product cards
   - Navigation

2. **Product Detail Screen Testing**
   - Product information display
   - Add to cart with quantities
   - Stock validation

3. **Cart Screen Testing**
   - Cart display
   - Item management
   - Checkout process

### **Phase 4: System Integration Testing (MEDIUM)**
**Priority**: MEDIUM - End-to-end flow validation

**Tasks:**
1. **Complete Customer Journey**
   - Browse products → View details → Add to cart → Checkout
   - Delivery info → Order summary → Payment

2. **Edge Case Handling**
   - Out of stock products
   - Invalid quantities
   - Network/service failures

### **Phase 5: Minor Issues Resolution (LOW)**
**Priority**: LOW - Polish and cleanup

**Tasks:**
1. **Deprecated API Fixes**
   - Update VNPayAdapterImpl to use current APIs
   - Remove deprecation warnings

2. **CSS Resources**
   - Add missing CSS files
   - Fix styling issues

---

## 📋 **Detailed Investigation Plan**

### **Step 1: Database Investigation**
```sql
-- Check CART table structure and data
SELECT * FROM CART LIMIT 5;

-- Check PRODUCT table for Abbey Road
SELECT * FROM PRODUCT WHERE title LIKE '%Abbey Road%';

-- Check CART_ITEM table structure
SELECT * FROM CART_ITEM LIMIT 5;

-- Verify FK constraints
PRAGMA foreign_key_list(CART_ITEM);
```

### **Step 2: Cart Service Code Review**
**Files to examine:**
- `src/main/java/com/aims/core/application/impl/CartServiceImpl.java`
- `src/main/java/com/aims/core/infrastructure/database/dao/CartDAOImpl.java`
- `src/main/java/com/aims/core/infrastructure/database/dao/CartItemDAOImpl.java`

**Key methods to verify:**
- `addItemToCart(String cartSessionId, String productId, int quantity)`
- `createCartSession(String userId)` 
- `validateCartReferences()`

### **Step 3: Product Card Controller Review**
**Files to examine:**
- `src/main/java/com/aims/core/presentation/controllers/ProductCardController.java`
- Add to cart button event handler
- Cart session ID generation/retrieval

### **Step 4: Service Factory Validation**
**Files to examine:**
- Service injection and initialization
- Database connection management
- Transaction handling

---

## 🎯 **Success Criteria**

### **Phase 1 Success:**
- [ ] Cart operations work without FK errors
- [ ] Products can be added to cart successfully
- [ ] Database integrity maintained

### **Phase 2 Success:**
- [ ] All service methods work correctly
- [ ] Proper error handling implemented
- [ ] Service integration validated

### **Phase 3 Success:**
- [ ] All UI screens function properly
- [ ] User interactions work as expected
- [ ] Navigation flows correctly

### **Phase 4 Success:**
- [ ] Complete customer journey works end-to-end
- [ ] Edge cases handled gracefully
- [ ] System stability verified

### **Phase 5 Success:**
- [ ] No deprecation warnings
- [ ] Clean UI with proper styling
- [ ] Production-ready code quality

---

## 📊 **Implementation Priority Matrix**

| Issue | Priority | Impact | Effort | Order |
|-------|----------|---------|---------|--------|
| FK Constraint Fix | CRITICAL | HIGH | MEDIUM | 1 |
| Cart Service Debug | CRITICAL | HIGH | LOW | 2 |
| Database Data Fix | CRITICAL | HIGH | LOW | 3 |
| Service Integration | HIGH | MEDIUM | MEDIUM | 4 |
| UI Testing | MEDIUM | MEDIUM | LOW | 5 |
| Deprecated API | LOW | LOW | LOW | 6 |

---

## 🚀 **Execution Plan**

### **Immediate Actions (Next 1-2 hours):**
1. **Database debugging** - Find FK constraint issue
2. **Cart service fix** - Implement proper error handling
3. **Basic functionality test** - Verify add to cart works

### **Short-term (Next day):**
4. **Complete service testing** - All CRUD operations
5. **UI flow testing** - End-to-end customer journey
6. **Integration validation** - Service-UI integration

### **Medium-term (Next 2-3 days):**
7. **Edge case handling** - Error scenarios
8. **System stability** - Load and stress testing
9. **Code cleanup** - Deprecation warnings, styling

This plan addresses the immediate critical issue blocking functionality while providing a comprehensive approach to ensure all features work correctly when you run the project.
# 🏗️ Web UI Customer Flow Implementation Plan

## 📋 Executive Summary

This document provides a comprehensive implementation plan to align the AIMS web UI customer flow with the documented customer journey requirements. The plan addresses critical gaps in API connectivity, business rules implementation, customer journey flow completion, and state synchronization between the React frontend and JavaFX backend.

## 🎯 Current State Analysis

### ✅ **Strengths of Current Implementation**
- **Comprehensive UI Components**: Complete checkout wizard, VNPay integration, cart management
- **Modern Tech Stack**: React + TypeScript with proper state management (React Query, Context API)
- **API Infrastructure**: Well-structured service layer with error handling and retry mechanisms
- **Backend Integration**: Full REST API endpoints available in JavaFX backend
- **Payment System**: VNPay integration components already implemented
- **Mobile Responsive**: Proper responsive design with Tailwind CSS

### ⚠️ **Identified Critical Gaps**

1. **API Connectivity Issues**
   - Web UI not properly connected to JavaFX backend REST endpoints
   - CORS configuration misalignment between frontend/backend
   - Session management inconsistencies for guest cart functionality

2. **Business Rules Misalignment**
   - Missing AIMS-specific business logic validation (VAT, shipping thresholds)
   - Rush delivery logic not implemented per documented requirements
   - Stock management validation gaps across user journey

3. **Customer Journey Flow Discrepancies**
   - Missing rush delivery eligibility checking for Hanoi inner districts
   - Incomplete implementation of documented stock validation checkpoints
   - Payment flow error handling not aligned with VNPay integration requirements

4. **State Synchronization Issues**
   - Frontend/backend data consistency problems
   - Cart session persistence issues across page refreshes
   - Product inventory updates not propagating to UI properly

## 🚀 Implementation Strategy

### **Phase 1: API Integration & Backend Connectivity** 
**Priority: Critical | Duration: 2-3 days**

**Key Implementation Tasks:**

#### 1.1 API Configuration Updates
**File:** [`web-ui/src/services/api.ts`](web-ui/src/services/api.ts)
```typescript
// Update API base URL to connect directly to JavaFX backend
export const API_BASE_URL = import.meta.env.DEV 
  ? 'http://localhost:8080/api'  // Direct connection to JavaFX backend
  : (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api');

// Enhanced CORS configuration
const apiClient: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  timeout: API_TIMEOUT,
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
  },
  withCredentials: true, // Critical for session management
});
```

#### 1.2 Service Layer Integration
**Files to Modify:**
- [`web-ui/src/services/cartService.ts`](web-ui/src/services/cartService.ts)
- [`web-ui/src/services/orderService.ts`](web-ui/src/services/orderService.ts)
- [`web-ui/src/services/paymentService.ts`](web-ui/src/services/paymentService.ts)
- [`web-ui/src/services/productService.ts`](web-ui/src/services/productService.ts)

#### 1.3 Development Configuration
**File:** [`web-ui/vite.config.ts`](web-ui/vite.config.ts)
```typescript
export default defineConfig({
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      }
    }
  }
});
```

### **Phase 2: Customer Journey Flow Alignment**
**Priority: High | Duration: 3-4 days**

#### 2.1 Rush Delivery Implementation
**Component:** [`web-ui/src/components/DeliveryOptionsSelector.tsx`](web-ui/src/components/DeliveryOptionsSelector.tsx)

**New Features to Implement:**
```typescript
// Rush delivery eligibility checking
const checkRushDeliveryEligibility = async (deliveryInfo: DeliveryInfo) => {
  // Validate Hanoi inner district eligibility
  const isHanoiInnerDistrict = HANOI_INNER_DISTRICTS.includes(deliveryInfo.district);
  
  // Check product eligibility for rush delivery
  const eligibleItems = cart.items.filter(item => 
    item.product.isEligibleForRushDelivery
  );
  
  return {
    addressEligible: isHanoiInnerDistrict,
    itemsEligible: eligibleItems.length > 0,
    eligibleItems
  };
};
```

#### 2.2 Business Rules Implementation
**AIMS Business Rules:**
```typescript
// VAT Calculation (10%)
const calculateVAT = (amount: number): number => amount * 0.1;

// Free shipping threshold (100,000 VND)
const FREE_SHIPPING_THRESHOLD = 100000;

// Rush delivery fee (10,000 VND per eligible item)
const RUSH_DELIVERY_FEE_PER_ITEM = 10000;

// Weight-based shipping calculation
const calculateShippingFee = (items: CartItem[], deliveryInfo: DeliveryInfo): number => {
  const heaviestItem = Math.max(...items.map(item => item.product.weight));
  const baseShippingFee = getShippingFeeByWeight(heaviestItem, deliveryInfo.province);
  
  const subtotal = items.reduce((sum, item) => sum + (item.price * item.quantity), 0);
  
  return subtotal >= FREE_SHIPPING_THRESHOLD ? 0 : baseShippingFee;
};
```

### **Phase 3: State Management & Data Synchronization**
**Priority: Medium | Duration: 2-3 days**

#### 3.1 Enhanced Cart Context
**File:** [`web-ui/src/contexts/CartContext.tsx`](web-ui/src/contexts/CartContext.tsx)

```typescript
const CartProvider = ({ children }) => {
  // Session persistence
  const [sessionId, setSessionId] = useState(() => 
    localStorage.getItem('cart_session_id') || generateSessionId()
  );
  
  // Real-time synchronization
  const { data: cart, mutate } = useQuery({
    queryKey: ['cart', sessionId],
    queryFn: () => cartService.getCart(sessionId),
    refetchInterval: 30000, // Refresh every 30 seconds
    staleTime: 10000, // Consider data stale after 10 seconds
  });
};
```

## 🔧 Detailed Implementation Tasks

### **Task 1: Backend API Integration** (2-3 days)

#### **Subtask 1.1: API Configuration**
**Files to Modify:**
- [`web-ui/src/services/api.ts`](web-ui/src/services/api.ts)
- [`web-ui/vite.config.ts`](web-ui/vite.config.ts)
- [`web-ui/.env.development`](web-ui/.env.development)

**Implementation Steps:**
1. Update API base URL configuration
2. Configure development proxy settings
3. Implement proper CORS handling
4. Add request/response interceptors for session management

#### **Subtask 1.2: Service Integration**
**Files to Modify:**
- [`web-ui/src/services/cartService.ts`](web-ui/src/services/cartService.ts)
- [`web-ui/src/services/orderService.ts`](web-ui/src/services/orderService.ts)
- [`web-ui/src/services/paymentService.ts`](web-ui/src/services/paymentService.ts)
- [`web-ui/src/services/productService.ts`](web-ui/src/services/productService.ts)

**Implementation Steps:**
1. Map frontend service methods to backend REST endpoints
2. Implement proper error handling and retry logic
3. Add request/response validation
4. Handle session management for guest carts

### **Task 2: Business Rules Implementation** (3-4 days)

#### **Subtask 2.1: Rush Delivery Logic**
**Components to Create/Modify:**
- [`web-ui/src/components/RushDeliveryDialog.tsx`](web-ui/src/components/RushDeliveryDialog.tsx) (NEW)
- [`web-ui/src/components/DeliveryOptionsSelector.tsx`](web-ui/src/components/DeliveryOptionsSelector.tsx)
- [`web-ui/src/services/deliveryService.ts`](web-ui/src/services/deliveryService.ts)

**Implementation Steps:**
1. Implement Hanoi inner district validation
2. Add product eligibility checking for rush delivery
3. Calculate rush delivery fees (10,000 VND per item)
4. Integrate with delivery time slot selection

#### **Subtask 2.2: Pricing and VAT Implementation**
**Components to Modify:**
- [`web-ui/src/components/OrderSummary.tsx`](web-ui/src/components/OrderSummary.tsx)
- [`web-ui/src/components/cart/CartSummary.tsx`](web-ui/src/components/cart/CartSummary.tsx)
- [`web-ui/src/utils/pricing.ts`](web-ui/src/utils/pricing.ts) (NEW)

### **Task 3: Customer Journey Flow Completion** (2-3 days)

#### **Subtask 3.1: Navigation Flow Enhancement**
**Components to Modify:**
- [`web-ui/src/components/CheckoutWizard.tsx`](web-ui/src/components/CheckoutWizard.tsx)
- [`web-ui/src/pages/CheckoutPage.tsx`](web-ui/src/pages/CheckoutPage.tsx)

#### **Subtask 3.2: Payment Flow Enhancement**
**Components to Modify:**
- [`web-ui/src/components/VNPayProcessor.tsx`](web-ui/src/components/VNPayProcessor.tsx)
- [`web-ui/src/pages/PaymentProcessing.tsx`](web-ui/src/pages/PaymentProcessing.tsx)
- [`web-ui/src/pages/PaymentResult.tsx`](web-ui/src/pages/PaymentResult.tsx)

## 📊 Success Criteria

### **Functional Requirements**
- ✅ **Guest Shopping**: Complete guest checkout without registration
- ✅ **20 Products/Page**: Proper pagination implementation
- ✅ **Real-time Stock**: Inventory validation at cart and checkout
- ✅ **Rush Delivery**: Hanoi district eligibility + 10,000 VND fee
- ✅ **VNPay Integration**: Complete payment processing cycle
- ✅ **Business Rules**: VAT, shipping, and pricing compliance

### **Technical Requirements**  
- ✅ **API Connectivity**: All services connected to JavaFX backend
- ✅ **State Management**: Consistent frontend/backend synchronization
- ✅ **Error Handling**: Comprehensive error recovery mechanisms
- ✅ **Performance**: Sub-2s page loads, optimistic updates

### **User Experience Requirements**
- ✅ **Flow Completion**: 100% customer journey coverage
- ✅ **Mobile Responsive**: Full mobile compatibility
- ✅ **Accessibility**: WCAG 2.1 AA compliance
- ✅ **Internationalization**: Vietnamese localization support

## 🕒 Timeline Estimate

| Phase | Duration | Dependencies |
|-------|----------|--------------|
| **Phase 1**: API Integration | 2-3 days | Backend running, CORS configured |
| **Phase 2**: Flow Alignment | 3-4 days | Phase 1 complete |
| **Phase 3**: State Management | 2-3 days | Phases 1-2 complete |
| **Testing & Validation** | 1-2 days | All phases complete |
| **Total Estimated Time** | **8-12 days** | - |

## 🎛️ Implementation Approach

### **Risk Mitigation**
- **Incremental Implementation**: Phase-by-phase delivery with validation
- **Backward Compatibility**: Maintain existing functionality during updates
- **Comprehensive Testing**: Unit, integration, and E2E test coverage
- **Documentation Updates**: Keep technical docs synchronized

### **Quality Assurance**
- **Code Reviews**: Peer review for all major changes
- **Testing Strategy**: Automated testing for critical user journeys  
- **Performance Monitoring**: Real-time metrics and alerting
- **User Acceptance**: Stakeholder validation at each phase

## 📝 Key Deliverables

1. **Updated API Integration**: Fully connected web UI to JavaFX backend
2. **Enhanced Business Logic**: Complete AIMS business rules implementation
3. **Improved Customer Journey**: All documented flow steps implemented
4. **Robust State Management**: Consistent data synchronization
5. **Comprehensive Testing**: Full test coverage for critical paths
6. **Updated Documentation**: Technical decomposition updates

---

**This plan provides a roadmap to achieve complete alignment between the web UI and the documented customer journey flow, ensuring a seamless and compliant shopping experience for AIMS customers.**
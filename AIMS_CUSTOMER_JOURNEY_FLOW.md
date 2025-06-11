# AIMS: Customer Journey Flow - Guest Shopping Experience

## Overview

This document focuses specifically on the customer (guest) shopping journey within the AIMS application, detailing the complete flow from product discovery through payment and order management.

## Customer Journey Architecture

- **No Authentication Required**: Customers can shop as guests without creating accounts
- **Session-based Cart**: Cart persists throughout the session until payment completion
- **Real-time Validation**: Stock and pricing validation at multiple checkpoints
- **VNPay Integration**: Secure payment processing through VNPay gateway

## Customer Shopping Journey Flow

```mermaid
flowchart TD
    %% Application Entry
    START([Customer Opens AIMS]) --> HOME[Home Screen<br/>Guest Mode<br/>20 Random Products]
    
    %% Product Discovery
    HOME --> |Browse Products| PRODUCT_GRID[Product Grid<br/>Paginated Display<br/>20 Products/Page]
    HOME --> |Search Products| SEARCH{Search<br/>Functionality}
    HOME --> |View Cart| CART_ENTRY{Cart Status<br/>Check}
    
    SEARCH --> |Enter Search Terms| SEARCH_RESULTS[Search Results Screen<br/>Filtered Products<br/>Sort by Price]
    SEARCH --> |Category Filter| CATEGORY_FILTER[Category Filtered Results<br/>Product Type Selection]
    
    PRODUCT_GRID --> |Click Product| PRODUCT_DETAIL[Product Detail Screen<br/>Full Product Information]
    SEARCH_RESULTS --> |Click Product| PRODUCT_DETAIL
    CATEGORY_FILTER --> |Click Product| PRODUCT_DETAIL
    
    %% Product Detail Interactions
    PRODUCT_DETAIL --> |Add to Cart| STOCK_CHECK{Stock<br/>Available?}
    PRODUCT_DETAIL --> |Back to Browse| HOME
    PRODUCT_DETAIL --> |Back to Search| SEARCH_RESULTS
    
    STOCK_CHECK --> |Stock Available| ADD_SUCCESS[Item Added to Cart<br/>Stock Decremented<br/>Success Message]
    STOCK_CHECK --> |Insufficient Stock| STOCK_DIALOG[Stock Insufficient Dialog<br/>Available Quantity Display]
    
    ADD_SUCCESS --> |Continue Shopping| HOME
    ADD_SUCCESS --> |View Cart| CART_SCREEN
    STOCK_DIALOG --> |Adjust Quantity| STOCK_CHECK
    STOCK_DIALOG --> |Cancel| PRODUCT_DETAIL
    
    %% Cart Management
    CART_ENTRY --> |Empty Cart| EMPTY_CART[Empty Cart Message<br/>Continue Shopping Prompt]
    CART_ENTRY --> |Has Items| CART_SCREEN[Shopping Cart Screen<br/>Item List & Totals]
    
    EMPTY_CART --> HOME
    
    CART_SCREEN --> |Update Quantity| QUANTITY_UPDATE[Update Item Quantity<br/>Recalculate Totals]
    CART_SCREEN --> |Remove Item| REMOVE_ITEM[Remove Item Confirmation<br/>Update Cart Display]
    CART_SCREEN --> |Clear All Items| CLEAR_CONFIRM[Clear Cart Confirmation<br/>Confirm Complete Clear]
    CART_SCREEN --> |Continue Shopping| HOME
    CART_SCREEN --> |Proceed to Checkout| FINAL_STOCK_CHECK{Final Stock<br/>Validation}
    
    QUANTITY_UPDATE --> |Valid Quantity| CART_SCREEN
    QUANTITY_UPDATE --> |Invalid/Insufficient| QUANTITY_ERROR[Quantity Error<br/>Stock Limit Message]
    QUANTITY_ERROR --> CART_SCREEN
    
    REMOVE_ITEM --> CART_SCREEN
    CLEAR_CONFIRM --> |Confirm| EMPTY_CART
    CLEAR_CONFIRM --> |Cancel| CART_SCREEN
    
    %% Checkout Process
    FINAL_STOCK_CHECK --> |All Items Available| DELIVERY_INFO[Delivery Information Screen<br/>Address & Contact Details]
    FINAL_STOCK_CHECK --> |Stock Issues| STOCK_INSUFFICIENT[Stock Insufficient Dialog<br/>Update Cart Required]
    STOCK_INSUFFICIENT --> CART_SCREEN
    
    %% Delivery Information Entry
    DELIVERY_INFO --> |Enter Address| ADDRESS_VALIDATE{Address<br/>Validation}
    DELIVERY_INFO --> |Rush Order Option| RUSH_ELIGIBILITY{Rush Order<br/>Eligibility Check}
    DELIVERY_INFO --> |Back to Cart| CART_SCREEN
    
    ADDRESS_VALIDATE --> |Valid Address| SHIPPING_CALC[Calculate Shipping Fee<br/>Weight & Distance Based]
    ADDRESS_VALIDATE --> |Invalid Address| ADDRESS_ERROR[Address Validation Error<br/>Required Fields Missing]
    ADDRESS_ERROR --> DELIVERY_INFO
    
    RUSH_ELIGIBILITY --> |Eligible Address & Products| RUSH_OPTIONS[Rush Order Options Dialog<br/>Delivery Time Selection<br/>Additional 10,000 VND/item]
    RUSH_ELIGIBILITY --> |Not Eligible| RUSH_NOT_AVAILABLE[Rush Order Not Available<br/>Location or Product Restriction]
    
    RUSH_OPTIONS --> |Accept Rush Delivery| RUSH_SHIPPING_CALC[Calculate Rush Shipping<br/>Separate Regular & Rush Fees]
    RUSH_OPTIONS --> |Decline Rush| SHIPPING_CALC
    RUSH_NOT_AVAILABLE --> DELIVERY_INFO
    
    SHIPPING_CALC --> |Fee Calculated| SHIPPING_DISPLAY[Display Shipping Fee<br/>Free Shipping if >100,000 VND<br/>Total Amount Update]
    RUSH_SHIPPING_CALC --> |Rush Fee Calculated| RUSH_SHIPPING_DISPLAY[Display Separate Fees<br/>Regular + Rush Delivery<br/>Total Amount Update]
    
    SHIPPING_DISPLAY --> |Proceed to Review| ORDER_SUMMARY[Order Summary Screen<br/>Invoice Preview<br/>All Costs Breakdown]
    RUSH_SHIPPING_DISPLAY --> |Proceed to Review| ORDER_SUMMARY
    
    %% Order Review
    ORDER_SUMMARY --> |Confirm Order| PAYMENT_METHOD[Payment Method Selection<br/>VNPay Credit Card Option]
    ORDER_SUMMARY --> |Back to Delivery| DELIVERY_INFO
    ORDER_SUMMARY --> |Modify Cart| CART_SCREEN
    
    %% Payment Processing
    PAYMENT_METHOD --> |Select Credit Card| PAYMENT_INITIATE[Initiate Payment<br/>Generate VNPay Request]
    PAYMENT_METHOD --> |Back to Summary| ORDER_SUMMARY
    
    PAYMENT_INITIATE --> |VNPay Redirect| PAYMENT_GATEWAY[VNPay Payment Gateway<br/>External Payment Form<br/>Card Details Entry]
    
    PAYMENT_GATEWAY --> |Payment Success| PAYMENT_SUCCESS[Payment Success Screen<br/>Order Confirmation<br/>Transaction Details]
    PAYMENT_GATEWAY --> |Payment Failed| PAYMENT_FAILED[Payment Failed Screen<br/>Error Details<br/>Retry Options]
    PAYMENT_GATEWAY --> |User Cancelled| PAYMENT_CANCELLED[Payment Cancelled<br/>Return to Payment Method]
    
    %% Payment Results
    PAYMENT_SUCCESS --> |View Order Details| ORDER_DETAILS[Customer Order Detail Screen<br/>Complete Order Information<br/>Invoice & Transaction Data]
    PAYMENT_SUCCESS --> |Continue Shopping| HOME_SUCCESS[Return to Home<br/>Cart Cleared<br/>New Session Started]
    
    PAYMENT_FAILED --> |Retry Payment| PAYMENT_METHOD
    PAYMENT_FAILED --> |Return to Cart| CART_SCREEN
    PAYMENT_CANCELLED --> PAYMENT_METHOD
    
    HOME_SUCCESS --> HOME
    
    %% Order Management
    ORDER_DETAILS --> |Cancel Order| CANCEL_CHECK{Order Status<br/>Cancellable?}
    ORDER_DETAILS --> |Print Invoice| PRINT_INVOICE[Print/Save Invoice<br/>PDF Generation]
    ORDER_DETAILS --> |Continue Shopping| HOME
    
    CANCEL_CHECK --> |Pending Status| CANCEL_CONFIRM[Order Cancellation Confirmation<br/>Refund Information]
    CANCEL_CHECK --> |Cannot Cancel| CANCEL_NOT_ALLOWED[Cancellation Not Allowed<br/>Order Status Information]
    
    CANCEL_CONFIRM --> |Confirm Cancellation| REFUND_PROCESS[Process Refund<br/>VNPay Refund Request<br/>Full Amount Return]
    CANCEL_CONFIRM --> |Keep Order| ORDER_DETAILS
    CANCEL_NOT_ALLOWED --> ORDER_DETAILS
    
    REFUND_PROCESS --> |Refund Success| REFUND_SUCCESS[Refund Successful<br/>Confirmation Message<br/>Timeline Information]
    REFUND_PROCESS --> |Refund Failed| REFUND_FAILED[Refund Processing Failed<br/>Contact Support Message]
    
    REFUND_SUCCESS --> HOME
    REFUND_FAILED --> ORDER_DETAILS
    
    PRINT_INVOICE --> ORDER_DETAILS
    
    %% Error Handling (Global)
    HOME -.-> SYSTEM_ERROR[System Error Dialog<br/>Connection Issues<br/>Service Unavailable]
    CART_SCREEN -.-> SYSTEM_ERROR
    DELIVERY_INFO -.-> SYSTEM_ERROR
    PAYMENT_GATEWAY -.-> SYSTEM_ERROR
    SYSTEM_ERROR -.-> |Retry| HOME
    
    %% Success Notifications (Global)
    ADD_SUCCESS -.-> SUCCESS_TOAST[Success Notification<br/>Item Added Message<br/>Auto-dismiss]
    PAYMENT_SUCCESS -.-> SUCCESS_TOAST
    REFUND_SUCCESS -.-> SUCCESS_TOAST
    
    %% Styling
    classDef primaryFlow fill:#e3f2fd,stroke:#1976d2,stroke-width:3px
    classDef decisionPoint fill:#fff3e0,stroke:#f57c00,stroke-width:2px
    classDef errorState fill:#ffebee,stroke:#d32f2f,stroke-width:2px
    classDef successState fill:#e8f5e8,stroke:#388e3c,stroke-width:2px
    classDef paymentFlow fill:#f3e5f5,stroke:#7b1fa2,stroke-width:2px
    classDef dialogFlow fill:#fafafa,stroke:#616161,stroke-width:1px,stroke-dasharray: 5 5
    
    %% Apply Styles
    class HOME,PRODUCT_GRID,PRODUCT_DETAIL,CART_SCREEN,DELIVERY_INFO,ORDER_SUMMARY,ORDER_DETAILS primaryFlow
    class STOCK_CHECK,CART_ENTRY,FINAL_STOCK_CHECK,ADDRESS_VALIDATE,RUSH_ELIGIBILITY,CANCEL_CHECK decisionPoint
    class STOCK_DIALOG,ADDRESS_ERROR,PAYMENT_FAILED,SYSTEM_ERROR,REFUND_FAILED errorState
    class ADD_SUCCESS,PAYMENT_SUCCESS,REFUND_SUCCESS,SUCCESS_TOAST successState
    class PAYMENT_METHOD,PAYMENT_INITIATE,PAYMENT_GATEWAY paymentFlow
    class STOCK_INSUFFICIENT,RUSH_OPTIONS,CANCEL_CONFIRM,CLEAR_CONFIRM dialogFlow
```

## Key Customer Interaction Points

### **Product Discovery & Browsing**
1. **Home Screen Entry**: 20 random products displayed per page
2. **Search Functionality**: Product attribute-based search with results pagination
3. **Product Detail View**: Complete product information with add-to-cart functionality
4. **Category Filtering**: Filter by product type (Books, CDs, DVDs, LP Records)

### **Shopping Cart Operations**
1. **Add to Cart**: Real-time stock validation with quantity constraints
2. **Cart Modification**: Update quantities, remove items, clear entire cart
3. **Cart Persistence**: Session-based cart storage until payment completion
4. **Stock Validation**: Multiple validation points throughout checkout process

### **Checkout Process**
1. **Delivery Information**: Required fields validation with address verification
2. **Rush Order Options**: Eligibility check for Hanoi inner districts only
3. **Shipping Calculation**: Weight-based fees with free shipping threshold (100,000 VND)
4. **Order Summary**: Complete cost breakdown including VAT and shipping

### **Payment Integration**
1. **VNPay Gateway**: Secure credit card processing through VNPay Sandbox
2. **Payment States**: Success, failure, and cancellation handling
3. **Transaction Tracking**: Complete transaction history and reference numbers
4. **Error Recovery**: Retry mechanisms for failed payments

### **Order Management**
1. **Order Viewing**: Complete order details with invoice information
2. **Cancellation Rules**: Status-based cancellation eligibility
3. **Refund Processing**: Automated refund through VNPay for cancelled orders
4. **Order Tracking**: Status updates throughout order lifecycle

## Business Rules for Customers

### **Shopping Constraints**
- **Product Display**: 20 products per page with pagination navigation
- **Search Results**: 20 related products per search page with sorting options
- **Cart Limitations**: One cart per software session
- **Guest Checkout**: No account creation required for purchases

### **Pricing and Fees**
- **VAT Application**: 10% VAT added to all product prices
- **Shipping Fees**: Based on heaviest item weight and delivery location
- **Free Shipping**: Orders over 100,000 VND (excluding rush delivery items)
- **Rush Delivery**: Additional 10,000 VND per eligible item

### **Delivery Options**
- **Standard Delivery**: Available nationwide with location-based pricing
- **Rush Delivery**: 2-hour delivery for Hanoi inner districts only
- **Eligibility**: Product-specific rush delivery availability
- **Address Validation**: Real-time address verification with fee calculation

### **Payment and Refunds**
- **Payment Methods**: Credit cards only through VNPay integration
- **Refund Policy**: Full refund for cancelled orders in eligible status
- **Transaction Security**: Secure payment processing with transaction tracking
- **Cancellation Window**: Orders can be cancelled before approval by product manager

## Error Handling and Validation

### **Stock Management**
- **Real-time Validation**: Stock checks at add-to-cart and checkout
- **Insufficient Stock**: Clear messaging with available quantities
- **Concurrent Access**: Handle multiple customers accessing same products
- **Stock Updates**: Immediate inventory updates after successful payment

### **Form Validation**
- **Required Fields**: All delivery information fields must be completed
- **Address Format**: Proper address format validation with province/city selection
- **Contact Information**: Email and phone number format validation
- **Data Persistence**: Form data preserved during validation errors

### **Payment Error Recovery**
- **Network Issues**: Timeout handling with retry options
- **Gateway Errors**: Clear error messaging with alternative actions
- **Transaction Failures**: Graceful degradation with cart preservation
- **Security Validation**: Secure token handling and validation

---

*This customer journey documentation reflects the complete guest shopping experience in the AIMS application, from initial product discovery through successful order completion and management.*
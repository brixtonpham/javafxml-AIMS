# VNPay Payment Flow Manual Testing Procedures

## Overview
This document provides comprehensive manual testing procedures for the VNPay payment integration using sandbox environment credentials. These tests should be performed to verify the complete payment flow works correctly with real user interactions.

## Test Environment Setup

### VNPay Sandbox Credentials ✅
- **Terminal ID (TMN Code)**: YFW5M6GN
- **Hash Secret**: 3RCPI4281FRSY2W6P3E9QD3JZJICJB5M
- **Payment URL**: https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
- **API URL**: https://sandbox.vnpayment.vn/merchant_webapi/api/transaction
- **Return URL**: http://localhost:8080/aims/payment/vnpay/return

### Test Card Information
- **Bank**: NCB (National Citizen Bank)
- **Card Number**: 9704198526191432198
- **Cardholder Name**: NGUYEN VAN A
- **Issue Date**: 07/15
- **OTP for completion**: 123456

### Additional Test Resources
- **VNPay Demo Site**: https://sandbox.vnpayment.vn/apis/vnpay-demo/
- **Merchant Admin Portal**: https://sandbox.vnpayment.vn/merchantv2/
- **Testing Portal**: https://sandbox.vnpayment.vn/vnpaygw-sit-testing/user/login

## Manual Test Cases

---

## Test Case 1: Successful Payment Flow with Domestic Debit Card

### **Objective**: Verify complete successful payment using NCB test card

### **Preconditions**:
- AIMS application is running
- Database is accessible
- VNPay sandbox environment is available
- Test card details available

### **Test Steps**:

1. **Navigate to Product Catalog**
   - [ ] Launch AIMS application
   - [ ] Browse product catalog
   - [ ] Select a product (e.g., book priced at 50,000 VND)
   - [ ] Add product to cart

2. **Proceed to Checkout**
   - [ ] Click "Proceed to Checkout" or similar
   - [ ] Review cart contents
   - [ ] Enter delivery information
   - [ ] Proceed to payment method selection

3. **Select VNPay Payment Method**
   - [ ] Select "VNPay Credit/Debit Card" payment option
   - [ ] Choose "Domestic Debit Card" if options are available
   - [ ] Click "Proceed to Payment"

4. **Verify Payment URL Generation**
   - [ ] **Expected**: Browser should open VNPay payment page
   - [ ] **Verify URL contains**: `sandbox.vnpayment.vn`
   - [ ] **Verify amount**: Should show correct amount (50,000 VND)
   - [ ] **Verify order info**: Should contain order reference

5. **Complete Payment at VNPay Gateway**
   - [ ] Select "NCB" bank from the list
   - [ ] Enter card number: `9704198526191432198`
   - [ ] Enter cardholder name: `NGUYEN VAN A`
   - [ ] Enter issue date: `07/15`
   - [ ] Click "Continue" or "Thanh toán"

6. **Enter OTP**
   - [ ] **Expected**: OTP input screen appears
   - [ ] Enter OTP: `123456`
   - [ ] Click "Confirm" or "Xác nhận"

7. **Verify Payment Success**
   - [ ] **Expected**: VNPay shows successful payment message
   - [ ] **Expected**: Automatic redirect back to AIMS application
   - [ ] **Verify**: Return URL should be localhost:8080/aims/payment/vnpay/return

8. **Verify Application State**
   - [ ] **Expected**: AIMS shows payment success screen
   - [ ] **Expected**: Order status updated to "APPROVED" or "PAID"
   - [ ] **Expected**: Transaction record created in database
   - [ ] **Expected**: Email confirmation sent (if implemented)

### **Expected Results**:
- ✅ Payment completed successfully
- ✅ User redirected back to AIMS with success message
- ✅ Order status updated to paid/approved
- ✅ Transaction recorded in database with status "SUCCESS"

### **Test Data to Record**:
- Order ID: ________________
- Transaction ID: ________________
- VNPay Transaction No: ________________
- Payment Amount: ________________
- Completion Time: ________________

---

## Test Case 2: Payment Cancellation Flow

### **Objective**: Verify user can cancel payment and return to application

### **Test Steps**:

1. **Follow steps 1-4 from Test Case 1**

2. **Cancel at VNPay Gateway**
   - [ ] At VNPay payment page, click "Cancel" or "Hủy bỏ"
   - [ ] **Expected**: Cancellation confirmation dialog appears
   - [ ] Confirm cancellation

3. **Verify Cancellation Handling**
   - [ ] **Expected**: Redirect back to AIMS application
   - [ ] **Expected**: AIMS shows payment cancelled message
   - [ ] **Expected**: Order remains in "PENDING_PAYMENT" status
   - [ ] **Expected**: Transaction status set to "CANCELLED"

4. **Verify Recovery Options**
   - [ ] **Expected**: User can retry payment with same or different method
   - [ ] **Expected**: User can modify order before retry

### **Expected Results**:
- ✅ Cancellation handled gracefully
- ✅ User can retry payment
- ✅ No duplicate orders created

---

## Test Case 3: Payment Timeout Scenario

### **Objective**: Verify handling when user abandons payment

### **Test Steps**:

1. **Follow steps 1-4 from Test Case 1**

2. **Abandon Payment**
   - [ ] At VNPay payment page, wait without taking action
   - [ ] **Note**: VNPay session typically expires after 15 minutes
   - [ ] Or close browser tab/window

3. **Return to Application**
   - [ ] Navigate back to AIMS application
   - [ ] Check order status

4. **Verify Timeout Handling**
   - [ ] **Expected**: Order remains in "PENDING_PAYMENT" status
   - [ ] **Expected**: Transaction shows "TIMEOUT" or "PENDING" status
   - [ ] **Expected**: User can restart payment process

### **Expected Results**:
- ✅ Timeout handled without errors
- ✅ User can retry payment
- ✅ No stuck transactions

---

## Test Case 4: Invalid Card Information

### **Objective**: Verify handling of invalid payment details

### **Test Steps**:

1. **Follow steps 1-4 from Test Case 1**

2. **Enter Invalid Card Details**
   - [ ] Enter incorrect card number: `1234567890123456`
   - [ ] Or enter correct card with wrong name/date
   - [ ] Attempt to proceed

3. **Verify Error Handling**
   - [ ] **Expected**: VNPay shows appropriate error message
   - [ ] **Expected**: User can correct information and retry
   - [ ] **Expected**: No transaction created in AIMS for failed attempt

4. **Test Correct Information After Error**
   - [ ] Enter correct card details: `9704198526191432198`
   - [ ] Complete payment successfully
   - [ ] **Expected**: Payment processes normally

### **Expected Results**:
- ✅ Invalid card information rejected by VNPay
- ✅ User can correct and retry
- ✅ Successful retry after correction

---

## Test Case 5: Network Interruption During Payment

### **Objective**: Verify handling of network issues during payment

### **Test Steps**:

1. **Follow steps 1-5 from Test Case 1**

2. **Simulate Network Interruption**
   - [ ] After entering card details but before OTP
   - [ ] Disconnect network/WiFi temporarily
   - [ ] Or close browser and reconnect after 2-3 minutes

3. **Restore Connection**
   - [ ] Reconnect network
   - [ ] Navigate back to AIMS application

4. **Check Payment Status**
   - [ ] Use "Check Payment Status" feature if available
   - [ ] Or restart payment process

5. **Verify Recovery**
   - [ ] **Expected**: System can determine payment status
   - [ ] **Expected**: User can restart payment if needed
   - [ ] **Expected**: No duplicate charges

### **Expected Results**:
- ✅ Network interruption handled gracefully
- ✅ Payment status can be verified
- ✅ No duplicate transactions

---

## Test Case 6: Multiple Payment Methods

### **Objective**: Test different payment method types

### **Test Steps**:

#### **6A: Credit Card Payment**
1. **Follow steps 1-3 from Test Case 1**
2. **Select Credit Card option** (if available)
3. **Complete payment with test card**
4. **Verify**: URL contains `vnp_BankCode=INTCARD` or no bank code

#### **6B: Different Bank Selection**
1. **Follow steps 1-4 from Test Case 1**
2. **Select different bank** (e.g., VCB, TCB if available)
3. **Use same test card** (should work across banks in sandbox)
4. **Complete payment**

### **Expected Results**:
- ✅ Both credit and debit card options work
- ✅ Different bank selections function correctly

---

## Test Case 7: Large Amount Payment

### **Objective**: Test payment with large amount (verify no amount limits)

### **Test Steps**:

1. **Create Large Order**
   - [ ] Add multiple high-value products to cart
   - [ ] Total should exceed 1,000,000 VND (1 million VND)

2. **Complete Payment Process**
   - [ ] Follow standard payment flow
   - [ ] **Verify**: Amount displays correctly in VNPay (10,000,000+ cents)
   - [ ] Complete with test card

3. **Verify Large Amount Handling**
   - [ ] **Expected**: Payment processes without amount-related errors
   - [ ] **Expected**: All systems handle large numbers correctly

### **Expected Results**:
- ✅ Large amounts processed correctly
- ✅ No overflow or display issues

---

## Test Case 8: Concurrent Payment Attempts

### **Objective**: Test multiple users paying simultaneously

### **Test Steps**:

1. **Setup Multiple Browser Sessions**
   - [ ] Open 2-3 different browser windows/incognito sessions
   - [ ] Create different orders in each session

2. **Initiate Concurrent Payments**
   - [ ] Start payment process in all sessions simultaneously
   - [ ] Use same test card for all payments

3. **Complete Payments**
   - [ ] Complete payments in sequence
   - [ ] **Verify**: Each payment processes independently
   - [ ] **Verify**: No interference between sessions

### **Expected Results**:
- ✅ Concurrent payments handled correctly
- ✅ No session conflicts
- ✅ All transactions recorded properly

---

## Test Case 9: Browser Compatibility

### **Objective**: Verify payment works across different browsers

### **Test Steps**:

1. **Test in Chrome**
   - [ ] Complete full payment flow
   - [ ] Note any issues

2. **Test in Firefox**
   - [ ] Complete full payment flow
   - [ ] Note any issues

3. **Test in Safari** (if available)
   - [ ] Complete full payment flow
   - [ ] Note any issues

4. **Test in Edge**
   - [ ] Complete full payment flow
   - [ ] Note any issues

### **Expected Results**:
- ✅ Payment works in all major browsers
- ✅ Consistent user experience across browsers

---

## Test Case 10: Mobile Browser Testing

### **Objective**: Verify payment works on mobile devices

### **Test Steps**:

1. **Test on Mobile Chrome**
   - [ ] Access AIMS application on mobile
   - [ ] Complete payment flow
   - [ ] **Verify**: VNPay pages are mobile-responsive

2. **Test on Mobile Safari** (if available)
   - [ ] Complete payment flow on iOS device
   - [ ] **Verify**: No mobile-specific issues

### **Expected Results**:
- ✅ Mobile payment experience is smooth
- ✅ VNPay pages display correctly on mobile

---

## Performance Testing

### **Test Case 11: Payment Processing Performance**

### **Objective**: Verify payment processing meets performance requirements

### **Test Steps**:

1. **Measure Payment URL Generation**
   - [ ] Record time from "Pay Now" click to VNPay redirect
   - [ ] **Target**: < 3 seconds
   - [ ] Actual time: ________________

2. **Measure VNPay Response Time**
   - [ ] Record time from payment completion to AIMS callback
   - [ ] **Target**: < 10 seconds
   - [ ] Actual time: ________________

3. **Measure Database Updates**
   - [ ] Check time for transaction status updates
   - [ ] **Target**: < 2 seconds
   - [ ] Actual time: ________________

### **Expected Results**:
- ✅ All operations complete within target times
- ✅ No performance degradation under normal load

---

## Error Scenario Testing

### **Test Case 12: VNPay Service Unavailable**

### **Test Steps**:

1. **Simulate VNPay Downtime**
   - [ ] Block access to vnpayment.vn domain (hosts file)
   - [ ] Or test when VNPay maintenance is announced

2. **Attempt Payment**
   - [ ] Try to initiate payment
   - [ ] **Expected**: Appropriate error message shown
   - [ ] **Expected**: User can retry later

3. **Restore Access**
   - [ ] Remove blocking/wait for service restoration
   - [ ] **Expected**: Payment works normally again

### **Expected Results**:
- ✅ Service unavailability handled gracefully
- ✅ Clear error messages provided to users
- ✅ Recovery works when service restored

---

## Security Testing

### **Test Case 13: URL Parameter Tampering**

### **Objective**: Verify security against parameter manipulation

### **Test Steps**:

1. **Capture Return URL**
   - [ ] Complete payment normally
   - [ ] Copy the return URL with parameters

2. **Modify Parameters**
   - [ ] Change amount parameter in URL
   - [ ] Change response code from failure to success
   - [ ] Change transaction reference

3. **Test Modified URLs**
   - [ ] Access AIMS with modified URLs
   - [ ] **Expected**: Invalid signatures rejected
   - [ ] **Expected**: Tampering detected and blocked

### **Expected Results**:
- ✅ Parameter tampering detected
- ✅ Invalid signatures rejected
- ✅ Security measures working correctly

---

## Data Validation Testing

### **Test Case 14: Order Data Consistency**

### **Objective**: Verify data consistency throughout payment flow

### **Test Steps**:

1. **Record Initial Data**
   - [ ] Order ID: ________________
   - [ ] Order amount: ________________
   - [ ] Customer details: ________________

2. **Verify at Each Stage**
   - [ ] **VNPay page**: Amount matches order
   - [ ] **Payment completion**: Correct order referenced
   - [ ] **Database**: All data consistent

3. **Check Final State**
   - [ ] **Order table**: Status updated correctly
   - [ ] **Transaction table**: All fields populated
   - [ ] **Audit trail**: Complete transaction history

### **Expected Results**:
- ✅ Data remains consistent throughout flow
- ✅ No data corruption or loss
- ✅ Complete audit trail maintained

---

## Test Completion Checklist

### **Environment Verification**
- [ ] VNPay sandbox credentials configured correctly
- [ ] Test database accessible and clean
- [ ] Application deployed and running
- [ ] Test card information available

### **Core Functionality**
- [ ] Successful payment flow working
- [ ] Payment cancellation handling
- [ ] Error scenarios handled appropriately
- [ ] Security features validated

### **Performance & Reliability**
- [ ] Performance targets met
- [ ] Browser compatibility verified
- [ ] Mobile compatibility tested
- [ ] Concurrent access handled

### **Data Integrity**
- [ ] Transaction data accurate
- [ ] Order status updates correct
- [ ] Audit trail complete
- [ ] No data inconsistencies

## Test Results Summary

### **Test Execution Date**: ________________
### **Tester Name**: ________________
### **Environment**: ________________

### **Overall Results**:
- **Total Test Cases**: 14
- **Passed**: ____/14
- **Failed**: ____/14
- **Blocked**: ____/14

### **Critical Issues Found**:
1. ________________________________
2. ________________________________
3. ________________________________

### **Recommendations**:
1. ________________________________
2. ________________________________
3. ________________________________

### **Sign-off**:
- **Tester**: ________________ Date: ________
- **Reviewer**: ________________ Date: ________
- **Approval**: ________________ Date: ________

---

## Appendix

### **Useful URLs for Testing**:
- VNPay Demo: https://sandbox.vnpayment.vn/apis/vnpay-demo/
- VNPay Documentation: https://sandbox.vnpayment.vn/apis/docs/
- Merchant Portal: https://sandbox.vnpayment.vn/merchantv2/

### **Test Card Variations** (if needed):
- **Standard**: 9704198526191432198
- **Alternative formats** may be available in VNPay documentation

### **Common Error Codes**:
- **00**: Success
- **24**: Transaction cancelled by user
- **99**: General failure
- **07**: Pending transaction
- **51**: Insufficient funds

### **Troubleshooting Tips**:
1. Clear browser cache if redirects fail
2. Check system time if signature validation fails
3. Verify network connectivity to sandbox.vnpayment.vn
4. Ensure return URL is accessible from VNPay servers
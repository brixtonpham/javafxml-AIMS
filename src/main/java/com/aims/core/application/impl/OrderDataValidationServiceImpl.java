package com.aims.core.application.impl;

import org.springframework.stereotype.Service;
import com.aims.core.application.services.IOrderDataValidationService;
import com.aims.core.application.services.IOrderDataLoaderService;
import com.aims.core.application.services.ICartDataValidationService;
import com.aims.core.application.services.IDeliveryCalculationService;
import com.aims.core.application.services.IProductService;
import com.aims.core.application.dtos.validation.*;
import com.aims.core.entities.*;
import com.aims.core.enums.OrderStatus;
import com.aims.core.enums.ProductType;
import com.aims.core.shared.exceptions.ValidationException;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Enhanced Order Data Validation Service Implementation
 *
 * Provides comprehensive multi-level validation, detailed error reporting,
 * and recovery mechanisms to ensure order data integrity throughout
 * the customer journey.
 */
@Service
public class OrderDataValidationServiceImpl implements IOrderDataValidationService {
    
    private static final Logger logger = Logger.getLogger(OrderDataValidationServiceImpl.class.getName());
    
    // Dependencies
    private final IOrderDataLoaderService orderDataLoaderService;
    private final ICartDataValidationService cartDataValidationService;
    private final IDeliveryCalculationService deliveryCalculationService;
    private final IProductService productService;
    
    // Validation constants
    private static final float VAT_RATE = 0.10f; // 10% VAT
    private static final float PRICING_TOLERANCE = 0.01f;
    private static final int MAX_ORDER_AGE_DAYS = 30;
    private static final List<String> RUSH_ELIGIBLE_CITIES = List.of("Hanoi", "Ho Chi Minh City", "HCMC");
    
    public OrderDataValidationServiceImpl(IOrderDataLoaderService orderDataLoaderService,
                                        ICartDataValidationService cartDataValidationService,
                                        IDeliveryCalculationService deliveryCalculationService,
                                        IProductService productService) {
        this.orderDataLoaderService = orderDataLoaderService;
        this.cartDataValidationService = cartDataValidationService;
        this.deliveryCalculationService = deliveryCalculationService;
        this.productService = productService;
    }
    
    @Override
    public OrderValidationResult validateOrderComprehensive(OrderEntity order) {
        logger.log(Level.INFO, "Starting comprehensive order validation for order: " + 
                  (order != null ? order.getOrderId() : "null"));
        
        OrderValidationResult.Builder builder = OrderValidationResult.builder();
        
        if (order == null) {
            return builder
                .addCritical("order", "ORDER_NULL", "Order entity is null")
                .addRecoverySuggestion("Please reload the order and try again")
                .build();
        }
        
        builder.addContextInfo("orderId", order.getOrderId())
               .addContextInfo("validationType", "comprehensive");
        
        try {
            // 1. Basic order structure validation
            validateBasicOrderStructure(order, builder);
            
            // 2. Order items validation
            validateOrderItemsComprehensive(order.getOrderItems(), builder);
            
            // 3. Delivery information validation
            validateDeliveryInfoComprehensive(order.getDeliveryInfo(), builder);
            
            // 4. Pricing validation
            validatePricingComprehensive(order, builder);
            
            // 5. Customer information validation
            validateCustomerInfo(order.getUserAccount(), builder);
            
            // 6. Rush delivery validation
            validateRushDeliveryComprehensive(order, builder);
            
            // 7. Business rules validation
            validateBusinessRules(order, builder);
            
            logger.log(Level.INFO, "Comprehensive validation completed for order: " + order.getOrderId() +
                      " - Valid: " + !builder.build().hasErrors());
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error during comprehensive validation for order: " + order.getOrderId(), e);
            builder.addCritical("validation", "VALIDATION_ERROR", 
                              "Validation process failed: " + e.getMessage())
                   .addRecoverySuggestion("Please contact technical support");
        }
        
        return builder.build();
    }
    
    @Override
    public OrderValidationResult validateOrderForDisplay(OrderEntity order) {
        logger.log(Level.INFO, "Validating order for display: " + 
                  (order != null ? order.getOrderId() : "null"));
        
        OrderValidationResult.Builder builder = OrderValidationResult.builder();
        
        if (order == null) {
            return builder
                .addError("order", "ORDER_NULL", "Order not found")
                .addRecoverySuggestion("Please refresh the page and try again")
                .build();
        }
        
        builder.addContextInfo("orderId", order.getOrderId())
               .addContextInfo("validationType", "display");
        
        // Display validation focuses on UI-critical elements
        validateBasicOrderStructure(order, builder);
        validateOrderItemsForDisplay(order.getOrderItems(), builder);
        validateDeliveryInfoForDisplay(order.getDeliveryInfo(), builder);
        validatePricingForDisplay(order, builder);
        
        return builder.build();
    }
    
    @Override
    public OrderValidationResult validateOrderForPayment(OrderEntity order) {
        logger.log(Level.INFO, "Validating order for payment: " + 
                  (order != null ? order.getOrderId() : "null"));
        
        OrderValidationResult.Builder builder = OrderValidationResult.builder();
        
        if (order == null) {
            return builder
                .addCritical("order", "ORDER_NULL", "Order not found for payment")
                .addRecoverySuggestion("Please return to cart and create a new order")
                .build();
        }
        
        builder.addContextInfo("orderId", order.getOrderId())
               .addContextInfo("validationType", "payment");
        
        // Payment validation is stricter
        validatePaymentReadiness(order, builder);
        validateInventoryAvailability(order, builder);
        validatePaymentMethodCompatibility(order, builder);
        validateFinalPricingAccuracy(order, builder);
        
        return builder.build();
    }
    
    @Override
    public OrderValidationResult validateOrderForNavigation(OrderEntity order, String targetScreen) {
        logger.log(Level.INFO, "Validating order for navigation to: " + targetScreen);
        
        OrderValidationResult.Builder builder = OrderValidationResult.builder();
        
        if (order == null) {
            return builder
                .addError("order", "ORDER_NULL", "Order not found")
                .build();
        }
        
        builder.addContextInfo("orderId", order.getOrderId())
               .addContextInfo("validationType", "navigation")
               .addContextInfo("targetScreen", targetScreen);
        
        switch (targetScreen) {
            case "order_summary":
                validateForOrderSummaryNavigation(order, builder);
                break;
            case "payment_method":
                validateForPaymentMethodNavigation(order, builder);
                break;
            case "payment_processing":
                validateForPaymentProcessingNavigation(order, builder);
                break;
            default:
                validateBasicNavigationReadiness(order, builder);
        }
        
        return builder.build();
    }
    
    @Override
    public OrderItemValidationResult validateOrderItems(List<OrderItem> orderItems) {
        logger.log(Level.INFO, "Validating order items: " + 
                  (orderItems != null ? orderItems.size() + " items" : "null"));
        
        OrderItemValidationResult result = new OrderItemValidationResult();
        
        if (orderItems == null || orderItems.isEmpty()) {
            result.addIssue(ValidationIssue.builder()
                .field("orderItems")
                .code("ITEMS_EMPTY")
                .message("Order must contain at least one item")
                .severity(ValidationSeverity.ERROR)
                .userFriendlyMessage("Please add items to your cart before proceeding")
                .possibleFix("Add products to cart")
                .build());
            return result;
        }
        
        result.setTotalItemsValidated(orderItems.size());
        int validItems = 0;
        int invalidItems = 0;
        
        for (int i = 0; i < orderItems.size(); i++) {
            OrderItem item = orderItems.get(i);
            String itemContext = "orderItems[" + i + "]";
            
            boolean itemValid = validateSingleOrderItem(item, itemContext, result);
            if (itemValid) {
                validItems++;
            } else {
                invalidItems++;
            }
        }
        
        result.setValidItemsCount(validItems);
        result.setInvalidItemsCount(invalidItems);
        
        // Add summary suggestions
        if (invalidItems > 0) {
            result.addRecoverySuggestion("Review and fix " + invalidItems + " invalid items");
        }
        if (validItems > 0) {
            result.addRecoverySuggestion(validItems + " items are valid and ready for processing");
        }
        
        return result;
    }
    
    @Override
    public DeliveryValidationResult validateDeliveryInfo(DeliveryInfo deliveryInfo) {
        logger.log(Level.INFO, "Validating delivery information");
        
        DeliveryValidationResult result = new DeliveryValidationResult();
        
        if (deliveryInfo == null) {
            result.addIssue(ValidationIssue.builder()
                .field("deliveryInfo")
                .code("DELIVERY_INFO_NULL")
                .message("Delivery information is required")
                .severity(ValidationSeverity.ERROR)
                .userFriendlyMessage("Please provide delivery information")
                .possibleFix("Complete delivery address form")
                .build());
            return result;
        }
        
        // Set delivery method for context
        result.setDeliveryMethod(deliveryInfo.getDeliveryMethodChosen());
        
        // Validate required fields
        validateDeliveryRequiredFields(deliveryInfo, result);
        
        // Validate address format and completeness
        validateDeliveryAddress(deliveryInfo, result);
        
        // Validate contact information
        validateDeliveryContactInfo(deliveryInfo, result);
        
        // Validate delivery method
        validateDeliveryMethod(deliveryInfo, result);
        
        // Check rush delivery eligibility
        if ("RUSH_DELIVERY".equals(deliveryInfo.getDeliveryMethodChosen())) {
            validateRushDeliveryEligibility(deliveryInfo, result);
        }
        
        return result;
    }
    
    @Override
    public PricingValidationResult validateOrderPricing(OrderEntity order) {
        logger.log(Level.INFO, "Validating order pricing for order: " + 
                  (order != null ? order.getOrderId() : "null"));
        
        PricingValidationResult result = new PricingValidationResult();
        
        if (order == null) {
            result.addIssue(ValidationIssue.builder()
                .field("order")
                .code("ORDER_NULL")
                .message("Order is required for pricing validation")
                .severity(ValidationSeverity.CRITICAL)
                .build());
            return result;
        }
        
        // Calculate expected values
        float calculatedSubtotal = calculateSubtotal(order.getOrderItems());
        float calculatedVAT = calculatedSubtotal * VAT_RATE;
        float calculatedSubtotalWithVAT = calculatedSubtotal + calculatedVAT;
        float calculatedTotal = calculatedSubtotalWithVAT + order.getCalculatedDeliveryFee();
        
        // Set calculated values for reference
        result.setCalculatedSubtotal(calculatedSubtotal);
        result.setCalculatedVAT(calculatedVAT);
        result.setCalculatedDeliveryFee(order.getCalculatedDeliveryFee());
        result.setCalculatedTotal(calculatedTotal);
        
        // Validate pricing calculations
        validateSubtotalCalculation(order, calculatedSubtotal, result);
        validateVATCalculation(order, calculatedSubtotalWithVAT, result);
        validateDeliveryFeeCalculation(order, result);
        validateTotalCalculation(order, calculatedTotal, result);
        
        return result;
    }
    
    @Override
    public RushDeliveryValidationResult validateRushDelivery(OrderEntity order) {
        logger.log(Level.INFO, "Validating rush delivery for order: " + 
                  (order != null ? order.getOrderId() : "null"));
        
        RushDeliveryValidationResult result = new RushDeliveryValidationResult();
        
        if (order == null) {
            result.addIssue(ValidationIssue.builder()
                .field("order")
                .code("ORDER_NULL")
                .message("Order is required for rush delivery validation")
                .severity(ValidationSeverity.ERROR)
                .build());
            return result;
        }
        
        // Check address eligibility
        boolean addressEligible = checkRushDeliveryAddressEligibility(order.getDeliveryInfo(), result);
        result.setAddressEligible(addressEligible);
        
        // Check item eligibility
        boolean itemsEligible = checkRushDeliveryItemEligibility(order.getOrderItems(), result);
        result.setItemsEligible(itemsEligible);
        
        // Determine overall availability
        boolean rushAvailable = addressEligible && itemsEligible;
        result.setRushDeliveryAvailable(rushAvailable);
        
        if (!rushAvailable) {
            if (!addressEligible) {
                result.setEligibilityReason("Address not eligible for rush delivery");
                result.addRecoverySuggestion("Rush delivery is only available for major cities");
            } else if (!itemsEligible) {
                result.setEligibilityReason("No items eligible for rush delivery");
                result.addRecoverySuggestion("Some products cannot be delivered via rush delivery");
            }
        }
        
        return result;
    }
    
    @Override
    public DetailedValidationReport getDetailedValidationReport(OrderEntity order) {
        logger.log(Level.INFO, "Generating detailed validation report for order: " + 
                  (order != null ? order.getOrderId() : "null"));
        
        DetailedValidationReport report = new DetailedValidationReport();
        
        if (order != null) {
            report.setOrderId(order.getOrderId());
        }
        
        // Generate comprehensive validation report
        report.addContextInfo("reportGeneratedAt", LocalDateTime.now());
        report.addContextInfo("validationVersion", "2.0");
        
        // Add section for each validation area
        report.addSection("Order Structure", validateOrderForDisplay(order));
        report.addSection("Order Items", convertToOrderValidationResult(validateOrderItems(order.getOrderItems())));
        report.addSection("Delivery Information", convertToOrderValidationResult(validateDeliveryInfo(order.getDeliveryInfo())));
        report.addSection("Pricing", convertToOrderValidationResult(validateOrderPricing(order)));
        report.addSection("Rush Delivery", convertToOrderValidationResult(validateRushDelivery(order)));
        report.addSection("Payment Readiness", validateOrderForPayment(order));
        
        // Generate summary and recommendations
        report.generateSummary();
        report.generateRecommendations();
        
        return report;
    }
    
    @Override
    public OrderEntity attemptValidationFixes(OrderEntity order) throws ValidationException {
        logger.log(Level.INFO, "Attempting validation fixes for order: " + 
                  (order != null ? order.getOrderId() : "null"));
        
        if (order == null) {
            throw new ValidationException("Cannot fix null order");
        }
        
        OrderEntity fixedOrder = order;
        
        try {
            // Attempt to fix common pricing calculation issues
            fixedOrder = attemptPricingFixes(fixedOrder);
            
            // Attempt to fix delivery information issues
            fixedOrder = attemptDeliveryInfoFixes(fixedOrder);
            
            // Attempt to fix order items issues
            fixedOrder = attemptOrderItemsFixes(fixedOrder);
            
            // Validate the fixed order
            OrderValidationResult validationResult = validateOrderComprehensive(fixedOrder);
            if (!validationResult.isValid() && validationResult.hasCriticalErrors()) {
                throw new ValidationException("Unable to automatically fix critical validation issues: " + 
                                            validationResult.getCriticalErrorSummary());
            }
            
            logger.log(Level.INFO, "Validation fixes completed for order: " + order.getOrderId());
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error during validation fixes for order: " + order.getOrderId(), e);
            throw new ValidationException("Failed to fix validation issues: " + e.getMessage(), e);
        }
        
        return fixedOrder;
    }
    
    // Private helper methods for comprehensive validation
    
    private void validateBasicOrderStructure(OrderEntity order, OrderValidationResult.Builder builder) {
        if (order.getOrderId() == null || order.getOrderId().trim().isEmpty()) {
            builder.addError("orderId", "ORDER_ID_MISSING", "Order ID is required");
        }
        
        if (order.getOrderDate() == null) {
            builder.addError("orderDate", "ORDER_DATE_MISSING", "Order date is required");
        } else {
            // Check if order is too old
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(MAX_ORDER_AGE_DAYS);
            if (order.getOrderDate().isBefore(cutoffDate)) {
                builder.addError("orderDate", "ORDER_TOO_OLD", 
                               "Order is too old to process (created " + order.getOrderDate() + ")")
                       .addRecoverySuggestion("Please create a new order");
            }
            
            // Check if order is in the future
            if (order.getOrderDate().isAfter(LocalDateTime.now().plusMinutes(5))) {
                builder.addWarning("orderDate", "ORDER_FUTURE_DATE", 
                                 "Order date is in the future");
            }
        }
        
        if (order.getOrderStatus() == null) {
            builder.addError("orderStatus", "ORDER_STATUS_MISSING", "Order status is required");
        }
    }
    
    private void validateOrderItemsComprehensive(List<OrderItem> orderItems, OrderValidationResult.Builder builder) {
        if (orderItems == null || orderItems.isEmpty()) {
            builder.addError("orderItems", "ITEMS_EMPTY", "Order must contain at least one item")
                   .addRecoverySuggestion("Add products to your cart");
            return;
        }
        
        for (int i = 0; i < orderItems.size(); i++) {
            OrderItem item = orderItems.get(i);
            String itemContext = "orderItems[" + i + "]";
            
            validateOrderItemComprehensive(item, itemContext, builder);
        }
    }
    
    private void validateOrderItemComprehensive(OrderItem item, String context, OrderValidationResult.Builder builder) {
        if (item == null) {
            builder.addError(context, "ITEM_NULL", "Order item is null");
            return;
        }
        
        // Validate product information
        if (item.getProduct() == null) {
            builder.addError(context + ".product", "PRODUCT_MISSING", "Product information is missing");
        } else {
            validateProductInformation(item.getProduct(), context + ".product", builder);
        }
        
        // Validate quantity
        if (item.getQuantity() <= 0) {
            builder.addError(context + ".quantity", "QUANTITY_INVALID", 
                           "Quantity must be greater than zero")
                   .addRecoverySuggestion("Update item quantity to a positive number");
        }
        
        // Validate pricing
        if (item.getPriceAtTimeOfOrder() <= 0) {
            builder.addError(context + ".price", "PRICE_INVALID", 
                           "Item price must be greater than zero");
        }
    }
    
    private void validateProductInformation(Product product, String context, OrderValidationResult.Builder builder) {
        if (product.getProductId() == null || product.getProductId().trim().isEmpty()) {
            builder.addError(context + ".productId", "PRODUCT_ID_MISSING", "Product ID is required");
        }
        
        if (product.getTitle() == null || product.getTitle().trim().isEmpty()) {
            builder.addWarning(context + ".title", "PRODUCT_TITLE_MISSING", "Product title is missing");
        }
        
        if (product.getPrice() <= 0) {
            builder.addError(context + ".price", "PRODUCT_PRICE_INVALID", "Product price must be greater than zero");
        }
        
        if (product.getQuantityInStock() < 0) {
            builder.addWarning(context + ".stock", "PRODUCT_STOCK_NEGATIVE", "Product stock is negative");
        }
    }
    
    // Additional helper methods will be continued in the next part due to length...
    
    private float calculateSubtotal(List<OrderItem> orderItems) {
        if (orderItems == null) return 0f;
        
        float subtotal = 0f;
        for (OrderItem item : orderItems) {
            subtotal += item.getPriceAtTimeOfOrder() * item.getQuantity();
        }
        return subtotal;
    }
    
    private boolean validateSingleOrderItem(OrderItem item, String context, OrderItemValidationResult result) {
        boolean isValid = true;
        
        if (item == null) {
            result.addIssue(ValidationIssue.builder()
                .field(context)
                .code("ITEM_NULL")
                .message("Order item is null")
                .severity(ValidationSeverity.ERROR)
                .build());
            return false;
        }
        
        if (item.getProduct() == null) {
            result.addIssue(ValidationIssue.builder()
                .field(context + ".product")
                .code("PRODUCT_MISSING")
                .message("Product information is missing")
                .severity(ValidationSeverity.ERROR)
                .build());
            isValid = false;
        }
        
        if (item.getQuantity() <= 0) {
            result.addIssue(ValidationIssue.builder()
                .field(context + ".quantity")
                .code("QUANTITY_INVALID")
                .message("Quantity must be positive")
                .severity(ValidationSeverity.ERROR)
                .actualValue(item.getQuantity())
                .build());
            isValid = false;
        }
        
        if (item.getPriceAtTimeOfOrder() <= 0) {
            result.addIssue(ValidationIssue.builder()
                .field(context + ".price")
                .code("PRICE_INVALID")
                .message("Price must be positive")
                .severity(ValidationSeverity.ERROR)
                .actualValue(item.getPriceAtTimeOfOrder())
                .build());
            isValid = false;
        }
        
        return isValid;
    }
    
    // Helper method to convert specialized validation results to OrderValidationResult
    private OrderValidationResult convertToOrderValidationResult(Object validationResult) {
        OrderValidationResult.Builder builder = OrderValidationResult.builder();
        
        if (validationResult instanceof OrderItemValidationResult) {
            OrderItemValidationResult itemResult = (OrderItemValidationResult) validationResult;
            builder.valid(itemResult.isValid())
                   .severity(itemResult.getSeverity());
            
            for (ValidationIssue issue : itemResult.getIssues()) {
                builder.addIssue(issue);
            }
            for (String suggestion : itemResult.getRecoverySuggestions()) {
                builder.addRecoverySuggestion(suggestion);
            }
        } else if (validationResult instanceof DeliveryValidationResult) {
            DeliveryValidationResult deliveryResult = (DeliveryValidationResult) validationResult;
            builder.valid(deliveryResult.isValid())
                   .severity(deliveryResult.getSeverity());
            
            for (ValidationIssue issue : deliveryResult.getIssues()) {
                builder.addIssue(issue);
            }
            for (String suggestion : deliveryResult.getRecoverySuggestions()) {
                builder.addRecoverySuggestion(suggestion);
            }
        } else if (validationResult instanceof PricingValidationResult) {
            PricingValidationResult pricingResult = (PricingValidationResult) validationResult;
            builder.valid(pricingResult.isValid())
                   .severity(pricingResult.getSeverity());
            
            for (ValidationIssue issue : pricingResult.getIssues()) {
                builder.addIssue(issue);
            }
            for (String suggestion : pricingResult.getRecoverySuggestions()) {
                builder.addRecoverySuggestion(suggestion);
            }
        } else if (validationResult instanceof RushDeliveryValidationResult) {
            RushDeliveryValidationResult rushResult = (RushDeliveryValidationResult) validationResult;
            builder.valid(rushResult.isValid())
                   .severity(rushResult.getSeverity());
            
            for (ValidationIssue issue : rushResult.getIssues()) {
                builder.addIssue(issue);
            }
            for (String suggestion : rushResult.getRecoverySuggestions()) {
                builder.addRecoverySuggestion(suggestion);
            }
        }
        
        return builder.build();
    }
    
    // Placeholder methods that need to be implemented (partial implementation due to length)
    
    private void validateDeliveryInfoComprehensive(DeliveryInfo deliveryInfo, OrderValidationResult.Builder builder) {
        if (deliveryInfo == null) {
            builder.addError("deliveryInfo", "DELIVERY_INFO_MISSING", "Delivery information is required")
                   .addRecoverySuggestion("Complete delivery address form");
            return;
        }
        
        // Validate required fields
        if (deliveryInfo.getRecipientName() == null || deliveryInfo.getRecipientName().trim().isEmpty()) {
            builder.addError("deliveryInfo.recipientName", "RECIPIENT_NAME_MISSING", "Recipient name is required");
        }
        
        if (deliveryInfo.getPhoneNumber() == null || deliveryInfo.getPhoneNumber().trim().isEmpty()) {
            builder.addError("deliveryInfo.phoneNumber", "PHONE_NUMBER_MISSING", "Phone number is required");
        }
        
        if (deliveryInfo.getDeliveryAddress() == null || deliveryInfo.getDeliveryAddress().trim().isEmpty()) {
            builder.addError("deliveryInfo.address", "ADDRESS_MISSING", "Delivery address is required");
        }
        
        if (deliveryInfo.getDeliveryProvinceCity() == null || deliveryInfo.getDeliveryProvinceCity().trim().isEmpty()) {
            builder.addError("deliveryInfo.city", "CITY_MISSING", "City/Province is required");
        }
    }
    
    private void validatePricingComprehensive(OrderEntity order, OrderValidationResult.Builder builder) {
        // Validate product pricing totals
        float calculatedSubtotal = calculateSubtotal(order.getOrderItems());
        
        if (Math.abs(order.getTotalProductPriceExclVAT() - calculatedSubtotal) > PRICING_TOLERANCE) {
            builder.addError("pricing.subtotal", "SUBTOTAL_MISMATCH", 
                           "Calculated subtotal doesn't match stored value")
                   .addRecoverySuggestion("Recalculate order totals");
        }
        
        // Validate VAT calculations
        float expectedVAT = calculatedSubtotal * VAT_RATE;
        float expectedInclVAT = calculatedSubtotal + expectedVAT;
        
        if (Math.abs(order.getTotalProductPriceInclVAT() - expectedInclVAT) > PRICING_TOLERANCE) {
            builder.addError("pricing.vat", "VAT_CALCULATION_ERROR", 
                           "VAT calculation is incorrect")
                   .addRecoverySuggestion("Recalculate VAT amount");
        }
        
        // Validate final total
        float expectedTotal = order.getTotalProductPriceInclVAT() + order.getCalculatedDeliveryFee();
        
        if (Math.abs(order.getTotalAmountPaid() - expectedTotal) > PRICING_TOLERANCE) {
            builder.addError("pricing.total", "TOTAL_CALCULATION_ERROR", 
                           "Total amount calculation is incorrect")
                   .addRecoverySuggestion("Recalculate final total");
        }
    }
    
    private void validateCustomerInfo(UserAccount userAccount, OrderValidationResult.Builder builder) {
        if (userAccount != null) {
            // Registered user validation
            if (userAccount.getUserId() == null || userAccount.getUserId().trim().isEmpty()) {
                builder.addError("customer.userId", "USER_ID_MISSING", "User ID is missing");
            }
            
            if (userAccount.getEmail() == null || userAccount.getEmail().trim().isEmpty()) {
                builder.addWarning("customer.email", "EMAIL_MISSING", "User email is missing");
            }
        } else {
            // Guest order - validate delivery info has contact details
            builder.addInfo("customer", "GUEST_ORDER", "Order is for guest user");
        }
    }
    
    private void validateRushDeliveryComprehensive(OrderEntity order, OrderValidationResult.Builder builder) {
        DeliveryInfo deliveryInfo = order.getDeliveryInfo();
        
        if (deliveryInfo != null && "RUSH_DELIVERY".equals(deliveryInfo.getDeliveryMethodChosen())) {
            // Validate rush delivery requirements
            if (!RUSH_ELIGIBLE_CITIES.contains(deliveryInfo.getDeliveryProvinceCity())) {
                builder.addError("rushDelivery.address", "ADDRESS_NOT_ELIGIBLE", 
                               "Address not eligible for rush delivery")
                       .addRecoverySuggestion("Rush delivery is only available in major cities");
            }
            
            // Check if any items are rush delivery eligible
            boolean hasRushEligibleItems = order.getOrderItems().stream()
                .anyMatch(OrderItem::isEligibleForRushDelivery);
            
            if (!hasRushEligibleItems) {
                builder.addError("rushDelivery.items", "NO_ELIGIBLE_ITEMS", 
                               "No items in order are eligible for rush delivery")
                       .addRecoverySuggestion("Remove rush delivery option or add eligible items");
            }
        }
    }
    
    private void validateBusinessRules(OrderEntity order, OrderValidationResult.Builder builder) {
        // Validate order status transitions
        if (order.getOrderStatus() != null) {
            // Add business rule validations based on order status
            switch (order.getOrderStatus()) {
                case PENDING_PAYMENT:
                    if (order.getDeliveryInfo() == null) {
                        builder.addError("businessRules.paymentStatus", "DELIVERY_INFO_REQUIRED", 
                                       "Delivery information required before payment");
                    }
                    break;
                case CANCELLED:
                    builder.addWarning("businessRules.status", "ORDER_CANCELLED", 
                                     "Order has been cancelled");
                    break;
                // Add more status-specific validations as needed
            }
        }
        
        // Validate total amount is reasonable
        if (order.getTotalAmountPaid() <= 0) {
            builder.addError("businessRules.amount", "INVALID_TOTAL", 
                           "Total amount must be greater than zero");
        }
        
        if (order.getTotalAmountPaid() > 100000000) { // 100 million VND threshold
            builder.addWarning("businessRules.amount", "HIGH_AMOUNT", 
                             "Order amount is unusually high")
                   .addRecoverySuggestion("Please verify the order total is correct");
        }
    }
    
    // Placeholder implementations for remaining methods (to be completed)
    
    private void validateOrderItemsForDisplay(List<OrderItem> orderItems, OrderValidationResult.Builder builder) {
        // Implementation for display validation
    }
    
    private void validateDeliveryInfoForDisplay(DeliveryInfo deliveryInfo, OrderValidationResult.Builder builder) {
        // Implementation for display validation
    }
    
    private void validatePricingForDisplay(OrderEntity order, OrderValidationResult.Builder builder) {
        // Implementation for display validation
    }
    
    private void validatePaymentReadiness(OrderEntity order, OrderValidationResult.Builder builder) {
        // Implementation for payment readiness validation
    }
    
    private void validateInventoryAvailability(OrderEntity order, OrderValidationResult.Builder builder) {
        // Implementation for inventory validation
    }
    
    private void validatePaymentMethodCompatibility(OrderEntity order, OrderValidationResult.Builder builder) {
        // Implementation for payment method validation
    }
    
    private void validateFinalPricingAccuracy(OrderEntity order, OrderValidationResult.Builder builder) {
        // Implementation for final pricing validation
    }
    
    private void validateForOrderSummaryNavigation(OrderEntity order, OrderValidationResult.Builder builder) {
        // Implementation for order summary navigation validation
    }
    
    private void validateForPaymentMethodNavigation(OrderEntity order, OrderValidationResult.Builder builder) {
        // Implementation for payment method navigation validation
    }
    
    private void validateForPaymentProcessingNavigation(OrderEntity order, OrderValidationResult.Builder builder) {
        // Implementation for payment processing navigation validation
    }
    
    private void validateBasicNavigationReadiness(OrderEntity order, OrderValidationResult.Builder builder) {
        // Implementation for basic navigation validation
    }
    
    private void validateDeliveryRequiredFields(DeliveryInfo deliveryInfo, DeliveryValidationResult result) {
        // Implementation for delivery required fields validation
    }
    
    private void validateDeliveryAddress(DeliveryInfo deliveryInfo, DeliveryValidationResult result) {
        // Implementation for delivery address validation
    }
    
    private void validateDeliveryContactInfo(DeliveryInfo deliveryInfo, DeliveryValidationResult result) {
        // Implementation for delivery contact validation
    }
    
    private void validateDeliveryMethod(DeliveryInfo deliveryInfo, DeliveryValidationResult result) {
        // Implementation for delivery method validation
    }
    
    private void validateRushDeliveryEligibility(DeliveryInfo deliveryInfo, DeliveryValidationResult result) {
        // Implementation for rush delivery eligibility validation
    }
    
    private void validateSubtotalCalculation(OrderEntity order, float calculatedSubtotal, PricingValidationResult result) {
        // Implementation for subtotal validation
    }
    
    private void validateVATCalculation(OrderEntity order, float calculatedVAT, PricingValidationResult result) {
        // Implementation for VAT validation
    }
    
    private void validateDeliveryFeeCalculation(OrderEntity order, PricingValidationResult result) {
        // Implementation for delivery fee validation
    }
    
    private void validateTotalCalculation(OrderEntity order, float calculatedTotal, PricingValidationResult result) {
        // Implementation for total calculation validation
    }
    
    private boolean checkRushDeliveryAddressEligibility(DeliveryInfo deliveryInfo, RushDeliveryValidationResult result) {
        // Implementation for rush delivery address eligibility
        return false;
    }
    
    private boolean checkRushDeliveryItemEligibility(List<OrderItem> orderItems, RushDeliveryValidationResult result) {
        // Implementation for rush delivery item eligibility
        return false;
    }
    
    private OrderEntity attemptPricingFixes(OrderEntity order) throws ValidationException {
        // Implementation for pricing fixes
        return order;
    }
    
    private OrderEntity attemptDeliveryInfoFixes(OrderEntity order) throws ValidationException {
        // Implementation for delivery info fixes
        return order;
    }
    
    private OrderEntity attemptOrderItemsFixes(OrderEntity order) throws ValidationException {
        // Implementation for order items fixes
        return order;
    }
}
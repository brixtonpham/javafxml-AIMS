package com.aims.core.application.impl;

import org.springframework.stereotype.Service;
import com.aims.core.application.services.IOrderValidationService;
import com.aims.core.entities.OrderEntity;
import com.aims.core.entities.OrderItem;
import com.aims.core.entities.DeliveryInfo;
import com.aims.core.entities.Product;
import com.aims.core.entities.UserAccount;
import com.aims.core.entities.Invoice;
import com.aims.core.entities.PaymentTransaction;
import com.aims.core.enums.OrderStatus;
import com.aims.core.enums.ProductType;
import com.aims.core.infrastructure.database.dao.IOrderEntityDAO;
import com.aims.core.infrastructure.database.dao.IOrderItemDAO;
import com.aims.core.infrastructure.database.dao.IDeliveryInfoDAO;
import com.aims.core.infrastructure.database.dao.IProductDAO;
import com.aims.core.infrastructure.database.dao.IUserAccountDAO;
import com.aims.core.infrastructure.database.dao.IInvoiceDAO;
import com.aims.core.infrastructure.database.dao.IPaymentTransactionDAO;
import com.aims.core.shared.exceptions.ValidationException;
import com.aims.core.shared.exceptions.ResourceNotFoundException;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * UNIVERSAL ORDER VALIDATION SERVICE - BULLETPROOF FOR ANY CUSTOMER ORDER
 * 
 * This enhanced implementation provides comprehensive validation and loading
 * capabilities that work with ANY customer order scenario:
 * - Guest vs registered user orders
 * - All product types (Books, CDs, DVDs, LPs)
 * - Any delivery method (standard/rush)
 * - Database inconsistencies and edge cases
 * - JPA lazy loading issues resolution
 * 
 * Focus: 100% validation success rate for valid orders regardless of how they were created.
 */
@Service
public class OrderValidationServiceImpl implements IOrderValidationService {

    private static final Logger logger = Logger.getLogger(OrderValidationServiceImpl.class.getName());
    
    // Core DAOs for universal order loading
    private final IOrderEntityDAO orderEntityDAO;
    private final IOrderItemDAO orderItemDAO;
    private final IDeliveryInfoDAO deliveryInfoDAO;
    private final IProductDAO productDAO;
    private final IUserAccountDAO userAccountDAO;
    private final IInvoiceDAO invoiceDAO;
    private final IPaymentTransactionDAO paymentTransactionDAO;

    public OrderValidationServiceImpl(IOrderEntityDAO orderEntityDAO, 
                                    IOrderItemDAO orderItemDAO,
                                    IDeliveryInfoDAO deliveryInfoDAO, 
                                    IProductDAO productDAO,
                                    IUserAccountDAO userAccountDAO,
                                    IInvoiceDAO invoiceDAO,
                                    IPaymentTransactionDAO paymentTransactionDAO) {
        this.orderEntityDAO = orderEntityDAO;
        this.orderItemDAO = orderItemDAO;
        this.deliveryInfoDAO = deliveryInfoDAO;
        this.productDAO = productDAO;
        this.userAccountDAO = userAccountDAO;
        this.invoiceDAO = invoiceDAO;
        this.paymentTransactionDAO = paymentTransactionDAO;
    }

    @Override
    public boolean isOrderReadyForPayment(String orderId) throws SQLException, ResourceNotFoundException {
        logger.log(Level.INFO, "Checking if order is ready for payment: " + orderId);
        
        try {
            OrderEntity order = orderEntityDAO.getById(orderId);
            if (order == null) {
                logger.log(Level.WARNING, "Order not found for payment readiness check: " + orderId);
                throw new ResourceNotFoundException("Order not found with ID: " + orderId);
            }

            // Check order status
            if (order.getOrderStatus() != OrderStatus.PENDING_PAYMENT) {
                logger.log(Level.INFO, "Order not ready for payment due to status: " + order.getOrderStatus() + " for order: " + orderId);
                return false;
            }

            // CRITICAL LAZY LOADING FIX: Check delivery info with explicit loading
            if (order.getDeliveryInfo() == null) {
                logger.log(Level.INFO, "READINESS CHECK: DeliveryInfo is null, checking database for lazy-loaded delivery info for order: " + orderId);
                try {
                    DeliveryInfo deliveryInfo = deliveryInfoDAO.getByOrderId(orderId);
                    if (deliveryInfo != null) {
                        logger.log(Level.INFO, "READINESS CHECK: Found delivery info in database for order: " + orderId);
                        order.setDeliveryInfo(deliveryInfo);
                    } else {
                        logger.log(Level.INFO, "READINESS CHECK: No delivery info found in database for order: " + orderId);
                        return false;
                    }
                } catch (SQLException e) {
                    logger.log(Level.SEVERE, "READINESS CHECK: Database error while loading delivery info for order: " + orderId, e);
                    throw e; // Re-throw SQLException as this method declares it
                }
            }
            
            // Final check - ensure delivery info is now available
            if (order.getDeliveryInfo() == null) {
                logger.log(Level.INFO, "Order not ready for payment - missing delivery info for order: " + orderId);
                return false;
            }

            // Check order has items
            if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
                logger.log(Level.INFO, "Order not ready for payment - no order items for order: " + orderId);
                return false;
            }

            // Check total amount
            if (order.getTotalAmountPaid() <= 0) {
                logger.log(Level.INFO, "Order not ready for payment - invalid total amount for order: " + orderId);
                return false;
            }

            logger.log(Level.INFO, "Order is ready for payment: " + orderId);
            return true;

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error checking order payment readiness for: " + orderId, e);
            throw e;
        }
    }

    @Override
    public OrderEntity getValidatedOrderForPayment(String orderId) throws SQLException, ResourceNotFoundException, ValidationException {
        logger.log(Level.INFO, "UNIVERSAL VALIDATION: Starting comprehensive order validation for payment: " + orderId);
        
        if (orderId == null || orderId.trim().isEmpty()) {
            throw new ValidationException("Order ID is required for payment validation");
        }

        try {
            // PHASE 1: Universal Order Loading with ALL relationships
            OrderEntity order = loadOrderWithAllRelationships(orderId);
            
            // PHASE 2: Universal Order Status Validation
            validateOrderStatusForPayment(order);
            
            // PHASE 3: Universal Customer Validation (Guest vs Registered)
            validateCustomerForPayment(order);
            
            // PHASE 4: Universal Product Validation (All Types)
            validateProductsUniversally(order);
            
            // PHASE 5: Universal Delivery Validation (All Methods)
            validateDeliveryInfoUniversally(order);
            
            // PHASE 6: Universal Business Rules Validation
            validateOrderBusinessRules(order);
            
            // PHASE 7: Universal Data Consistency Check
            validateDataConsistencyUniversally(order);

            logger.log(Level.INFO, "UNIVERSAL VALIDATION: All validation phases completed successfully for order: " + orderId);
            return order;

        } catch (ValidationException | ResourceNotFoundException e) {
            logger.log(Level.WARNING, "UNIVERSAL VALIDATION: Validation failed for order: " + orderId + " - " + e.getMessage());
            throw e;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "UNIVERSAL VALIDATION: Database error during validation for order: " + orderId, e);
            throw new ValidationException("Unable to validate order due to a temporary system error. Please try again.", e);
        }
    }

    /**
     * UNIVERSAL ORDER LOADING - Loads order with ALL relationships regardless of lazy loading
     * Handles ANY customer order scenario including edge cases and database inconsistencies
     */
    private OrderEntity loadOrderWithAllRelationships(String orderId) throws SQLException, ResourceNotFoundException, ValidationException {
        logger.log(Level.INFO, "UNIVERSAL LOADING: Loading order with all relationships for: " + orderId);
        
        // Step 1: Load base order entity
        OrderEntity order = orderEntityDAO.getById(orderId);
        if (order == null) {
            logger.log(Level.SEVERE, "UNIVERSAL LOADING: Order not found in database: " + orderId);
            throw new ResourceNotFoundException("Order with ID '" + orderId + "' was not found. Please verify the order exists and try again.");
        }
        
        logger.log(Level.INFO, "UNIVERSAL LOADING: Base order loaded, status: " + order.getOrderStatus());
        
        // Step 2: Force load ALL relationships to prevent lazy loading issues
        try {
            // Load order items with full product details
            loadOrderItemsUniversally(order);
            
            // Load delivery information
            loadDeliveryInfoUniversally(order);
            
            // Load user account if exists
            loadUserAccountUniversally(order);
            
            // Load invoice if exists
            loadInvoiceUniversally(order);
            
            // Load payment transactions if exist
            loadPaymentTransactionsUniversally(order);
            
            logger.log(Level.INFO, "UNIVERSAL LOADING: All relationships loaded successfully for order: " + orderId);
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "UNIVERSAL LOADING: Error loading relationships for order: " + orderId, e);
            throw new ValidationException("Error loading order details: " + e.getMessage(), e);
        }
        
        return order;
    }

    /**
     * Universal order items loading - handles ALL product types and edge cases
     */
    private void loadOrderItemsUniversally(OrderEntity order) throws SQLException, ValidationException {
        String orderId = order.getOrderId();
        logger.log(Level.INFO, "UNIVERSAL LOADING: Loading order items for order: " + orderId);
        
        try {
            // Force reload order items even if already loaded (to get latest state)
            List<OrderItem> orderItems = orderItemDAO.getItemsByOrderId(orderId);
            
            if (orderItems == null) {
                orderItems = new ArrayList<>();
            }
            
            // Validate and enrich each order item with full product details
            for (OrderItem item : orderItems) {
                if (item.getProduct() == null || item.getProduct().getProductId() == null) {
                    logger.log(Level.WARNING, "UNIVERSAL LOADING: Order item has null product for order: " + orderId);
                    throw new ValidationException("Order contains invalid items. Please contact support.");
                }
                
                // Force reload product details to ensure ALL product types are properly loaded
                Product fullProduct = productDAO.getById(item.getProduct().getProductId());
                if (fullProduct == null) {
                    logger.log(Level.SEVERE, "UNIVERSAL LOADING: Product not found for order item: " + item.getProduct().getProductId());
                    throw new ValidationException("Product '" + item.getProduct().getProductId() + "' is no longer available.");
                }
                
                // Set the full product details on the order item
                item.setProduct(fullProduct);
                
                logger.log(Level.FINE, "UNIVERSAL LOADING: Loaded product " + fullProduct.getProductId() +
                          " of type " + fullProduct.getProductType() + " for order: " + orderId);
            }
            
            order.setOrderItems(orderItems);
            logger.log(Level.INFO, "UNIVERSAL LOADING: Successfully loaded " + orderItems.size() + " order items for order: " + orderId);
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "UNIVERSAL LOADING: Database error loading order items for order: " + orderId, e);
            throw e;
        }
    }

    /**
     * Universal delivery info loading - handles ALL delivery methods and edge cases
     */
    private void loadDeliveryInfoUniversally(OrderEntity order) throws SQLException, ValidationException {
        String orderId = order.getOrderId();
        logger.log(Level.INFO, "UNIVERSAL LOADING: Loading delivery info for order: " + orderId);
        
        try {
            // Force reload delivery info even if already loaded
            DeliveryInfo deliveryInfo = deliveryInfoDAO.getByOrderId(orderId);
            
            if (deliveryInfo == null) {
                logger.log(Level.WARNING, "UNIVERSAL LOADING: No delivery info found for order: " + orderId);
                // For orders in PENDING_PAYMENT status, delivery info MUST exist
                if (order.getOrderStatus() == OrderStatus.PENDING_PAYMENT) {
                    throw new ValidationException("Delivery information is required for payment processing. Please complete delivery details first.");
                }
            } else {
                logger.log(Level.INFO, "UNIVERSAL LOADING: Delivery info loaded for order: " + orderId +
                          " - Method: " + deliveryInfo.getDeliveryMethodChosen());
            }
            
            order.setDeliveryInfo(deliveryInfo);
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "UNIVERSAL LOADING: Database error loading delivery info for order: " + orderId, e);
            throw e;
        }
    }

    /**
     * Universal user account loading - handles both guest and registered users
     */
    private void loadUserAccountUniversally(OrderEntity order) throws SQLException {
        String orderId = order.getOrderId();
        
        if (order.getUserAccount() != null && order.getUserAccount().getUserId() != null) {
            logger.log(Level.INFO, "UNIVERSAL LOADING: Loading user account for registered user order: " + orderId);
            
            try {
                // Force reload user account to get latest details
                UserAccount fullUser = userAccountDAO.getById(order.getUserAccount().getUserId());
                if (fullUser != null) {
                    order.setUserAccount(fullUser);
                    logger.log(Level.INFO, "UNIVERSAL LOADING: User account loaded for order: " + orderId);
                } else {
                    logger.log(Level.WARNING, "UNIVERSAL LOADING: User account not found for order: " + orderId);
                    // Don't fail - order can still be processed as guest order
                    order.setUserAccount(null);
                }
            } catch (SQLException e) {
                logger.log(Level.WARNING, "UNIVERSAL LOADING: Error loading user account for order: " + orderId, e);
                // Don't fail - treat as guest order
                order.setUserAccount(null);
            }
        } else {
            logger.log(Level.INFO, "UNIVERSAL LOADING: Guest order detected: " + orderId);
        }
    }

    /**
     * Universal invoice loading - handles existing invoices
     */
    private void loadInvoiceUniversally(OrderEntity order) throws SQLException {
        String orderId = order.getOrderId();
        
        try {
            Invoice invoice = invoiceDAO.getByOrderId(orderId);
            order.setInvoice(invoice);
            
            if (invoice != null) {
                logger.log(Level.INFO, "UNIVERSAL LOADING: Invoice loaded for order: " + orderId);
            } else {
                logger.log(Level.INFO, "UNIVERSAL LOADING: No invoice found for order: " + orderId);
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "UNIVERSAL LOADING: Error loading invoice for order: " + orderId, e);
            // Don't fail - invoice may not exist yet
        }
    }

    /**
     * Universal payment transactions loading - handles all transaction types
     */
    private void loadPaymentTransactionsUniversally(OrderEntity order) throws SQLException {
        String orderId = order.getOrderId();
        
        try {
            List<PaymentTransaction> transactions = paymentTransactionDAO.getByOrderId(orderId);
            if (transactions == null) {
                transactions = new ArrayList<>();
            }
            order.setPaymentTransactions(transactions);
            
            logger.log(Level.INFO, "UNIVERSAL LOADING: Loaded " + transactions.size() + " payment transactions for order: " + orderId);
        } catch (SQLException e) {
            logger.log(Level.WARNING, "UNIVERSAL LOADING: Error loading payment transactions for order: " + orderId, e);
            // Don't fail - transactions may not exist yet
            order.setPaymentTransactions(new ArrayList<>());
        }
    }

    /**
     * Universal order status validation - handles ALL order states
     */
    private void validateOrderStatusForPayment(OrderEntity order) throws ValidationException {
        logger.log(Level.INFO, "UNIVERSAL VALIDATION: Validating order status for payment: " + order.getOrderId());
        
        if (order.getOrderStatus() == null) {
            throw new ValidationException("Order status is not set. Please contact support.");
        }
        
        if (order.getOrderStatus() != OrderStatus.PENDING_PAYMENT) {
            logger.log(Level.WARNING, "UNIVERSAL VALIDATION: Invalid order status for payment: " + order.getOrderStatus() + " for order: " + order.getOrderId());
            
            // Provide specific guidance based on current status
            String message = "Order is not ready for payment. Current status: " + order.getOrderStatus();
            switch (order.getOrderStatus()) {
                case PENDING_DELIVERY_INFO:
                    message += ". Please complete delivery information first.";
                    break;
                case PENDING_PROCESSING:
                    message += ". Order is awaiting manager approval.";
                    break;
                case APPROVED:
                    message += ". Order has already been approved and paid.";
                    break;
                case CANCELLED:
                    message += ". Order has been cancelled.";
                    break;
                case PAYMENT_FAILED:
                    message += ". Previous payment failed. Please try again.";
                    break;
                default:
                    message += ". Please contact support for assistance.";
            }
            
            throw new ValidationException(message);
        }
        
        logger.log(Level.INFO, "UNIVERSAL VALIDATION: Order status validation passed for order: " + order.getOrderId());
    }

    /**
     * Universal customer validation - handles guest and registered users
     */
    private void validateCustomerForPayment(OrderEntity order) throws ValidationException {
        logger.log(Level.INFO, "UNIVERSAL VALIDATION: Validating customer information for order: " + order.getOrderId());
        
        if (order.getUserAccount() != null) {
            // Registered user order validation
            UserAccount user = order.getUserAccount();
            if (user.getUserId() == null || user.getUserId().trim().isEmpty()) {
                throw new ValidationException("Invalid user account information. Please log in again and try again.");
            }
            
            // Additional user account validations
            if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
                throw new ValidationException("User account is missing email information. Please update your profile.");
            }
            
            logger.log(Level.INFO, "UNIVERSAL VALIDATION: Registered user order validated for user: " + user.getUserId());
        } else {
            // Guest order validation
            logger.log(Level.INFO, "UNIVERSAL VALIDATION: Guest order detected - validating delivery info for contact details");
            
            // For guest orders, delivery info must contain contact information
            if (order.getDeliveryInfo() == null) {
                throw new ValidationException("Guest orders require complete delivery information including contact details.");
            }
            
            DeliveryInfo delivery = order.getDeliveryInfo();
            if (delivery.getRecipientName() == null || delivery.getRecipientName().trim().isEmpty() ||
                delivery.getPhoneNumber() == null || delivery.getPhoneNumber().trim().isEmpty()) {
                throw new ValidationException("Guest orders require recipient name and phone number for contact purposes.");
            }
            
            logger.log(Level.INFO, "UNIVERSAL VALIDATION: Guest order validated with delivery contact info");
        }
    }

    /**
     * Universal product validation - handles ALL product types (Books, CDs, DVDs, LPs)
     */
    private void validateProductsUniversally(OrderEntity order) throws ValidationException, SQLException {
        logger.log(Level.INFO, "UNIVERSAL VALIDATION: Validating products for order: " + order.getOrderId());
        
        List<OrderItem> orderItems = order.getOrderItems();
        if (orderItems == null || orderItems.isEmpty()) {
            throw new ValidationException("Order must contain at least one product for payment processing.");
        }
        
        for (OrderItem item : orderItems) {
            validateOrderItemUniversally(item, order.getOrderId());
        }
        
        logger.log(Level.INFO, "UNIVERSAL VALIDATION: All " + orderItems.size() + " products validated successfully for order: " + order.getOrderId());
    }

    /**
     * Universal order item validation - handles any product type and edge cases
     */
    private void validateOrderItemUniversally(OrderItem item, String orderId) throws ValidationException, SQLException {
        // Validate order item structure
        if (item == null) {
            throw new ValidationException("Order contains invalid items. Please contact support.");
        }
        
        if (item.getQuantity() <= 0) {
            throw new ValidationException("All order items must have positive quantities.");
        }
        
        if (item.getPriceAtTimeOfOrder() <= 0) {
            throw new ValidationException("All order items must have valid prices.");
        }
        
        // Validate product exists and is available
        Product product = item.getProduct();
        if (product == null || product.getProductId() == null) {
            throw new ValidationException("Order contains items with missing product information.");
        }
        
        // Verify product still exists in database with current data
        try {
            Product currentProduct = productDAO.getById(product.getProductId());
            if (currentProduct == null) {
                throw new ValidationException("Product '" + product.getProductId() + "' is no longer available.");
            }
            
            // Validate stock availability
            if (currentProduct.getQuantityInStock() < item.getQuantity()) {
                throw new ValidationException("Insufficient stock for product '" + currentProduct.getTitle() +
                                            "'. Available: " + currentProduct.getQuantityInStock() +
                                            ", Requested: " + item.getQuantity());
            }
            
            // Validate product type specific requirements
            validateProductTypeSpecificRequirements(currentProduct, item);
            
            logger.log(Level.FINE, "UNIVERSAL VALIDATION: Product " + product.getProductId() +
                      " (" + currentProduct.getProductType() + ") validated successfully");
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "UNIVERSAL VALIDATION: Database error validating product: " + product.getProductId(), e);
            throw new ValidationException("Unable to verify product availability. Please try again.");
        }
    }

    /**
     * Product type specific validation for ALL supported types
     */
    private void validateProductTypeSpecificRequirements(Product product, OrderItem item) throws ValidationException {
        if (product.getProductType() == null) {
            throw new ValidationException("Product type information is missing for product: " + product.getProductId());
        }
        
        // Validate based on product type
        switch (product.getProductType()) {
            case BOOK:
                // Books are always eligible for standard delivery
                // No specific restrictions
                logger.log(Level.FINE, "UNIVERSAL VALIDATION: Book product validated: " + product.getProductId());
                break;
                
            case CD:
            case DVD:
            case LP:
                // Media products may have specific handling requirements
                // Validate rush delivery eligibility if applicable
                if (item.isEligibleForRushDelivery()) {
                    logger.log(Level.FINE, "UNIVERSAL VALIDATION: Media product eligible for rush delivery: " + product.getProductId());
                }
                break;
                
            default:
                logger.log(Level.WARNING, "UNIVERSAL VALIDATION: Unknown product type: " + product.getProductType() + " for product: " + product.getProductId());
                // Allow unknown types to proceed - system should be extensible
        }
    }

    /**
     * Universal delivery validation - handles ALL delivery methods
     */
    private void validateDeliveryInfoUniversally(OrderEntity order) throws ValidationException {
        logger.log(Level.INFO, "UNIVERSAL VALIDATION: Validating delivery information for order: " + order.getOrderId());
        
        DeliveryInfo deliveryInfo = order.getDeliveryInfo();
        if (deliveryInfo == null) {
            throw new ValidationException("Delivery information is required for payment processing.");
        }
        
        // Validate required delivery fields
        validateRequiredDeliveryFields(deliveryInfo);
        
        // Validate delivery method
        validateDeliveryMethod(deliveryInfo, order);
        
        // Validate delivery address
        validateDeliveryAddress(deliveryInfo);
        
        logger.log(Level.INFO, "UNIVERSAL VALIDATION: Delivery information validated successfully for order: " + order.getOrderId());
    }

    /**
     * Validate required delivery information fields
     */
    private void validateRequiredDeliveryFields(DeliveryInfo deliveryInfo) throws ValidationException {
        if (deliveryInfo.getRecipientName() == null || deliveryInfo.getRecipientName().trim().isEmpty()) {
            throw new ValidationException("Recipient name is required for delivery.");
        }
        
        if (deliveryInfo.getDeliveryAddress() == null || deliveryInfo.getDeliveryAddress().trim().isEmpty()) {
            throw new ValidationException("Delivery address is required.");
        }
        
        if (deliveryInfo.getPhoneNumber() == null || deliveryInfo.getPhoneNumber().trim().isEmpty()) {
            throw new ValidationException("Phone number is required for delivery contact.");
        }
        
        if (deliveryInfo.getDeliveryProvinceCity() == null || deliveryInfo.getDeliveryProvinceCity().trim().isEmpty()) {
            throw new ValidationException("Province/City is required for delivery.");
        }
    }

    /**
     * Validate delivery method (standard vs rush)
     */
    private void validateDeliveryMethod(DeliveryInfo deliveryInfo, OrderEntity order) throws ValidationException {
        String deliveryMethod = deliveryInfo.getDeliveryMethodChosen();
        if (deliveryMethod == null || deliveryMethod.trim().isEmpty()) {
            // Default to standard if not specified
            deliveryInfo.setDeliveryMethodChosen("STANDARD");
            return;
        }
        
        if ("RUSH_DELIVERY".equals(deliveryMethod)) {
            // Validate rush delivery requirements
            validateRushDeliveryRequirements(deliveryInfo, order);
        } else if (!"STANDARD".equals(deliveryMethod)) {
            logger.log(Level.WARNING, "UNIVERSAL VALIDATION: Unknown delivery method: " + deliveryMethod);
            // Default to standard for unknown methods
            deliveryInfo.setDeliveryMethodChosen("STANDARD");
        }
    }

    /**
     * Validate rush delivery specific requirements
     */
    private void validateRushDeliveryRequirements(DeliveryInfo deliveryInfo, OrderEntity order) throws ValidationException {
        // Validate address eligibility for rush delivery (typically major cities)
        String province = deliveryInfo.getDeliveryProvinceCity();
        if (!"Hanoi".equalsIgnoreCase(province) && !"Ho Chi Minh City".equalsIgnoreCase(province) && !"HCMC".equalsIgnoreCase(province)) {
            throw new ValidationException("Rush delivery is only available for Hanoi and Ho Chi Minh City.");
        }
        
        // Validate that at least one item is eligible for rush delivery
        boolean hasRushEligibleItem = false;
        for (OrderItem item : order.getOrderItems()) {
            if (item.isEligibleForRushDelivery()) {
                hasRushEligibleItem = true;
                break;
            }
        }
        
        if (!hasRushEligibleItem) {
            throw new ValidationException("No items in this order are eligible for rush delivery.");
        }
        
        // Validate rush delivery time if specified
        if (deliveryInfo.getRequestedRushDeliveryTime() != null) {
            // Add time validation if needed (e.g., business hours, future time)
            logger.log(Level.INFO, "UNIVERSAL VALIDATION: Rush delivery time specified: " + deliveryInfo.getRequestedRushDeliveryTime());
        }
    }

    /**
     * Validate delivery address format and completeness
     */
    private void validateDeliveryAddress(DeliveryInfo deliveryInfo) throws ValidationException {
        String address = deliveryInfo.getDeliveryAddress();
        
        // Absolute minimum validation
        if (address.length() < 5) {
            throw new ValidationException("Address too short. Please include: street number, street name, and city/district information.");
        }
        
        // Smart validation for short addresses
        if (address.length() < 10) {
            if (!isValidShortAddress(address)) {
                throw new ValidationException("Please provide more address details. Include: street number, street name, and apartment/unit if applicable. Example: '123 Main Street, Apt 2A'");
            }
        }
        
        // Enhanced phone validation with better error message
        String phone = deliveryInfo.getPhoneNumber();
        if (!phone.matches("^[0-9+\\-\\s()]{10,15}$")) {
            throw new ValidationException("Please provide a valid phone number (10-15 digits). Example: 0123456789 or +84 123 456 789");
        }
    }

    /**
     * Validates short addresses (5-9 characters) for essential components
     * Checks for patterns: numbers, PO Box, street abbreviations, unit designations
     */
    private boolean isValidShortAddress(String address) {
        String addressLower = address.toLowerCase().trim();
        
        return addressLower.matches(".*\\d+.*") ||  // Contains numbers
               addressLower.contains("po box") ||     // PO Box
               addressLower.contains("p.o.") ||       // P.O. Box variation
               addressLower.matches(".*(ave|st|rd|dr|blvd|ln|ct|pl).*") ||  // Street abbreviations
               addressLower.matches(".*(unit|apt|suite|#).*");  // Unit designations
    }

    /**
     * Universal data consistency validation - ensures all data is consistent
     */
    private void validateDataConsistencyUniversally(OrderEntity order) throws ValidationException {
        logger.log(Level.INFO, "UNIVERSAL VALIDATION: Validating data consistency for order: " + order.getOrderId());
        
        // Validate amount calculations
        validateAmountConsistency(order);
        
        // Validate delivery fee consistency
        validateDeliveryFeeConsistency(order);
        
        // Validate total amount consistency
        validateTotalAmountConsistency(order);
        
        // Validate order date reasonableness
        validateOrderDateConsistency(order);
        
        logger.log(Level.INFO, "UNIVERSAL VALIDATION: Data consistency validation passed for order: " + order.getOrderId());
    }

    /**
     * Validate amount calculations consistency
     */
    private void validateAmountConsistency(OrderEntity order) throws ValidationException {
        float calculatedExclVAT = 0f;
        for (OrderItem item : order.getOrderItems()) {
            calculatedExclVAT += item.getPriceAtTimeOfOrder() * item.getQuantity();
        }
        
        float tolerance = 0.01f; // Allow small floating point differences
        if (Math.abs(order.getTotalProductPriceExclVAT() - calculatedExclVAT) > tolerance) {
            throw new ValidationException("Product price calculation inconsistency detected. Expected: " + calculatedExclVAT +
                                        ", but order shows: " + order.getTotalProductPriceExclVAT() +
                                        ". Please refresh your cart and try again.");
        }
        
        // Validate VAT calculation (10% VAT)
        float expectedInclVAT = calculatedExclVAT * 1.10f;
        if (Math.abs(order.getTotalProductPriceInclVAT() - expectedInclVAT) > tolerance) {
            throw new ValidationException("VAT calculation inconsistency detected. Please refresh your cart and try again.");
        }
    }

    /**
     * Validate delivery fee consistency
     */
    private void validateDeliveryFeeConsistency(OrderEntity order) throws ValidationException {
        if (order.getCalculatedDeliveryFee() < 0) {
            throw new ValidationException("Delivery fee cannot be negative.");
        }
        
        // Validate reasonable delivery fee ranges
        if (order.getCalculatedDeliveryFee() > 1000000) { // 1 million VND seems excessive
            throw new ValidationException("Delivery fee appears to be incorrect. Please contact support.");
        }
    }

    /**
     * Validate total amount consistency
     */
    private void validateTotalAmountConsistency(OrderEntity order) throws ValidationException {
        float expectedTotal = order.getTotalProductPriceInclVAT() + order.getCalculatedDeliveryFee();
        float tolerance = 0.01f;
        
        if (Math.abs(order.getTotalAmountPaid() - expectedTotal) > tolerance) {
            throw new ValidationException("Total amount inconsistency detected. Expected: " + expectedTotal +
                                        ", but order shows: " + order.getTotalAmountPaid() +
                                        ". Please refresh your cart and try again.");
        }
        
        if (order.getTotalAmountPaid() <= 0) {
            throw new ValidationException("Total payment amount must be greater than zero.");
        }
    }

    /**
     * Validate order date reasonableness
     */
    private void validateOrderDateConsistency(OrderEntity order) throws ValidationException {
        if (order.getOrderDate() == null) {
            throw new ValidationException("Order date is missing. Please contact support.");
        }
        
        // Validate order is not too old (30 days maximum)
        java.time.LocalDateTime cutoffDate = java.time.LocalDateTime.now().minusDays(30);
        if (order.getOrderDate().isBefore(cutoffDate)) {
            throw new ValidationException("This order is too old to process payment. Please create a new order.");
        }
        
        // Validate order is not in the future
        if (order.getOrderDate().isAfter(java.time.LocalDateTime.now().plusMinutes(5))) {
            throw new ValidationException("Order date cannot be in the future. Please contact support.");
        }
    }

    @Override
    public boolean validateOrderIntegrity(String orderId) throws SQLException, ResourceNotFoundException {
        logger.log(Level.INFO, "Validating order integrity: " + orderId);
        
        try {
            OrderEntity order = orderEntityDAO.getById(orderId);
            if (order == null) {
                throw new ResourceNotFoundException("Order not found for integrity validation: " + orderId);
            }

            // Validate order items exist and have valid quantities
            if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
                logger.log(Level.WARNING, "Order integrity failed - no items: " + orderId);
                return false;
            }

            float calculatedTotal = 0;
            for (OrderItem item : order.getOrderItems()) {
                if (item.getProduct() == null) {
                    logger.log(Level.WARNING, "Order integrity failed - null product in item: " + orderId);
                    return false;
                }
                
                if (item.getQuantity() <= 0) {
                    logger.log(Level.WARNING, "Order integrity failed - invalid quantity for item: " + orderId);
                    return false;
                }

                if (item.getPriceAtTimeOfOrder() <= 0) {
                    logger.log(Level.WARNING, "Order integrity failed - invalid unit price for item: " + orderId);
                    return false;
                }

                // Verify product still exists and has adequate stock
                Product currentProduct = productDAO.getById(item.getProduct().getProductId());
                if (currentProduct == null) {
                    logger.log(Level.WARNING, "Order integrity failed - product no longer exists: " + item.getProduct().getProductId());
                    return false;
                }

                if (currentProduct.getQuantityInStock() < item.getQuantity()) {
                    logger.log(Level.WARNING, "Order integrity failed - insufficient stock for product: " + item.getProduct().getProductId());
                    return false;
                }

                calculatedTotal += item.getPriceAtTimeOfOrder() * item.getQuantity();
            }

            // Validate calculated totals match stored totals (with small tolerance for floating point)
            float tolerance = 0.01f;
            if (Math.abs(order.getTotalProductPriceExclVAT() - calculatedTotal) > tolerance) {
                logger.log(Level.WARNING, "Order integrity failed - total mismatch: " + orderId);
                return false;
            }

            // Validate delivery fee is reasonable (non-negative)
            if (order.getCalculatedDeliveryFee() < 0) {
                logger.log(Level.WARNING, "Order integrity failed - negative delivery fee: " + orderId);
                return false;
            }

            // Validate total amount paid matches calculation
            float expectedTotal = order.getTotalProductPriceInclVAT() + order.getCalculatedDeliveryFee();
            if (Math.abs(order.getTotalAmountPaid() - expectedTotal) > tolerance) {
                logger.log(Level.WARNING, "Order integrity failed - total amount mismatch: " + orderId);
                return false;
            }

            logger.log(Level.INFO, "Order integrity validation passed: " + orderId);
            return true;

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during order integrity validation for: " + orderId, e);
            throw e;
        }
    }

    @Override
    public boolean orderExists(String orderId) throws SQLException {
        if (orderId == null || orderId.trim().isEmpty()) {
            return false;
        }

        try {
            OrderEntity order = orderEntityDAO.getById(orderId);
            boolean exists = order != null;
            logger.log(Level.FINE, "Order existence check for " + orderId + ": " + exists);
            return exists;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error checking order existence for: " + orderId, e);
            throw e;
        }
    }

    @Override
    public void validateOrderBusinessRules(OrderEntity order) throws ValidationException {
        logger.log(Level.FINE, "Validating business rules for order: " + order.getOrderId());
        
        // Validate order has items
        if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
            throw new ValidationException("Order must contain at least one item for payment processing.");
        }

        // Validate all items have positive quantities
        for (OrderItem item : order.getOrderItems()) {
            if (item.getQuantity() <= 0) {
                throw new ValidationException("All order items must have positive quantities. Invalid item: " + 
                                           (item.getProduct() != null ? item.getProduct().getTitle() : "Unknown"));
            }
            
            if (item.getPriceAtTimeOfOrder() <= 0) {
                throw new ValidationException("All order items must have valid prices. Invalid item: " + 
                                           (item.getProduct() != null ? item.getProduct().getTitle() : "Unknown"));
            }
        }

        // Validate amounts are positive
        if (order.getTotalProductPriceExclVAT() <= 0) {
            throw new ValidationException("Order total (excluding VAT) must be greater than zero.");
        }

        if (order.getTotalProductPriceInclVAT() <= 0) {
            throw new ValidationException("Order total (including VAT) must be greater than zero.");
        }

        if (order.getTotalAmountPaid() <= 0) {
            throw new ValidationException("Total payment amount must be greater than zero.");
        }

        // Validate delivery fee is not negative
        if (order.getCalculatedDeliveryFee() < 0) {
            throw new ValidationException("Delivery fee cannot be negative.");
        }

        // Validate total amount calculation
        float expectedTotal = order.getTotalProductPriceInclVAT() + order.getCalculatedDeliveryFee();
        float tolerance = 0.01f; // Allow small floating point differences
        if (Math.abs(order.getTotalAmountPaid() - expectedTotal) > tolerance) {
            throw new ValidationException("Total payment amount does not match calculated total. Expected: " + 
                                       expectedTotal + ", but got: " + order.getTotalAmountPaid());
        }

        // Validate delivery info is present and complete
        DeliveryInfo deliveryInfo = order.getDeliveryInfo();
        if (deliveryInfo == null) {
            throw new ValidationException("Delivery information is required for payment processing.");
        }

        if (deliveryInfo.getRecipientName() == null || deliveryInfo.getRecipientName().trim().isEmpty()) {
            throw new ValidationException("Receiver name is required in delivery information.");
        }

        if (deliveryInfo.getDeliveryAddress() == null || deliveryInfo.getDeliveryAddress().trim().isEmpty()) {
            throw new ValidationException("Receiver address is required in delivery information.");
        }

        if (deliveryInfo.getPhoneNumber() == null || deliveryInfo.getPhoneNumber().trim().isEmpty()) {
            throw new ValidationException("Receiver phone number is required in delivery information.");
        }

        logger.log(Level.FINE, "Business rules validation passed for order: " + order.getOrderId());
    }
}
package com.aims.core.application.impl;

import com.aims.core.application.services.IOrderDataLoaderService;
import com.aims.core.entities.*;
import com.aims.core.application.dtos.*;
import com.aims.core.infrastructure.database.dao.*;
import com.aims.core.shared.exceptions.ResourceNotFoundException;
import com.aims.core.shared.exceptions.ValidationException;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Enhanced Order Data Loading Service Implementation
 * 
 * Provides comprehensive order data loading with proper relationship handling
 * and fallback mechanisms to address lazy loading issues.
 */
public class OrderDataLoaderServiceImpl implements IOrderDataLoaderService {
    
    private static final Logger logger = Logger.getLogger(OrderDataLoaderServiceImpl.class.getName());
    
    // DAOs for complete data loading
    private final IOrderEntityDAO orderEntityDAO;
    private final IOrderItemDAO orderItemDAO;
    private final IDeliveryInfoDAO deliveryInfoDAO;
    private final IInvoiceDAO invoiceDAO;
    private final IPaymentTransactionDAO paymentTransactionDAO;
    private final IUserAccountDAO userAccountDAO;
    private final IProductDAO productDAO;
    
    // VAT rate for calculations
    private static final float VAT_RATE = 0.10f;
    
    public OrderDataLoaderServiceImpl(IOrderEntityDAO orderEntityDAO,
                                    IOrderItemDAO orderItemDAO,
                                    IDeliveryInfoDAO deliveryInfoDAO,
                                    IInvoiceDAO invoiceDAO,
                                    IPaymentTransactionDAO paymentTransactionDAO,
                                    IUserAccountDAO userAccountDAO,
                                    IProductDAO productDAO) {
        this.orderEntityDAO = orderEntityDAO;
        this.orderItemDAO = orderItemDAO;
        this.deliveryInfoDAO = deliveryInfoDAO;
        this.invoiceDAO = invoiceDAO;
        this.paymentTransactionDAO = paymentTransactionDAO;
        this.userAccountDAO = userAccountDAO;
        this.productDAO = productDAO;
    }
    
    @Override
    public OrderEntity loadCompleteOrderData(String orderId) throws ResourceNotFoundException {
        logger.log(Level.INFO, "Loading complete order data for order: " + orderId);
        
        if (orderId == null || orderId.trim().isEmpty()) {
            throw new ResourceNotFoundException("Order ID cannot be null or empty");
        }
        
        try {
            // Step 1: Load base order entity
            OrderEntity order = orderEntityDAO.getById(orderId);
            if (order == null) {
                throw new ResourceNotFoundException("Order with ID " + orderId + " not found");
            }
            
            logger.log(Level.INFO, "Base order loaded: " + orderId);
            
            // Step 2: Explicitly load all relationships
            loadOrderItemsWithValidation(order);
            loadDeliveryInfoWithValidation(order);
            loadInvoiceWithValidation(order);
            loadPaymentTransactionsWithValidation(order);
            loadUserAccountWithValidation(order);
            
            // Step 3: Validate completeness
            if (!validateLazyLoadingInitialization(order)) {
                logger.log(Level.WARNING, "Some relationships may not be properly initialized for order: " + orderId);
            }
            
            logger.log(Level.INFO, "Successfully loaded complete order data for: " + orderId);
            return order;
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error loading complete order data for: " + orderId, e);
            throw new ResourceNotFoundException("Unable to load complete order data: " + e.getMessage());
        }
    }
    
    @Override
    public OrderSummaryDTO createOrderSummaryDTO(OrderEntity order) throws ValidationException {
        logger.log(Level.INFO, "Creating OrderSummaryDTO for order: " + 
                  (order != null ? order.getOrderId() : "null"));
        
        if (order == null) {
            throw new ValidationException("Order entity cannot be null");
        }
        
        // Validate order data completeness first
        if (!validateOrderDataCompleteness(order)) {
            throw new ValidationException("Order data is incomplete for DTO creation");
        }
        
        try {
            // Convert order items to DTOs
            List<OrderItemDTO> orderItemDTOs = convertOrderItemsToDTO(order.getOrderItems());
            
            // Convert delivery info to DTO
            DeliveryInfoDTO deliveryInfoDTO = convertDeliveryInfoToDTO(order.getDeliveryInfo());
            
            // Create rush delivery details if applicable
            RushDeliveryDetailsDTO rushDeliveryDetailsDTO = createRushDeliveryDetailsDTO(order);
            
            // Calculate pricing with validation
            float totalProductPriceExclVAT = validateAndCalculateTotalExclVAT(order);
            float totalProductPriceInclVAT = validateAndCalculateTotalInclVAT(order);
            float deliveryFee = validateAndGetDeliveryFee(order);
            float totalAmountToBePaid = validateAndCalculateTotalAmount(order);
            
            // Create additional DTOs for enhanced order summary
            UserSummaryDTO userSummaryDTO = createUserSummaryDTO(order.getUserAccount());
            PaymentSummaryDTO paymentSummaryDTO = createPaymentSummaryDTO(order);
            
            OrderSummaryDTO dto = new OrderSummaryDTO(
                order.getOrderId(),
                orderItemDTOs,
                totalProductPriceExclVAT,
                totalProductPriceInclVAT,
                deliveryFee,
                totalAmountToBePaid,
                deliveryInfoDTO,
                rushDeliveryDetailsDTO,
                // Enhanced fields
                order.getOrderStatus() != null ? order.getOrderStatus().name() : "UNKNOWN",
                order.getOrderDate(),
                userSummaryDTO,
                paymentSummaryDTO,
                totalProductPriceInclVAT - totalProductPriceExclVAT, // VAT amount
                rushDeliveryDetailsDTO != null, // hasRushDelivery
                paymentSummaryDTO != null && "SUCCESS".equals(paymentSummaryDTO.paymentStatus()), // isPaid
                combineSpecialInstructions(order) // specialInstructions
            );
            
            logger.log(Level.INFO, "Successfully created OrderSummaryDTO for order: " + order.getOrderId());
            return dto;
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error creating OrderSummaryDTO for order: " + 
                      order.getOrderId(), e);
            throw new ValidationException("Failed to create OrderSummaryDTO: " + e.getMessage());
        }
    }
    
    @Override
    public boolean validateOrderDataCompleteness(OrderEntity order) {
        if (order == null) {
            logger.log(Level.WARNING, "Order is null - data is incomplete");
            return false;
        }
        
        logger.log(Level.INFO, "Validating order data completeness for: " + order.getOrderId());
        
        // Check essential fields
        if (order.getOrderId() == null || order.getOrderId().trim().isEmpty()) {
            logger.log(Level.WARNING, "Order ID is missing");
            return false;
        }
        
        if (order.getOrderStatus() == null) {
            logger.log(Level.WARNING, "Order status is missing for order: " + order.getOrderId());
            return false;
        }
        
        if (order.getOrderDate() == null) {
            logger.log(Level.WARNING, "Order date is missing for order: " + order.getOrderId());
            return false;
        }
        
        // Check order items
        if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
            logger.log(Level.WARNING, "Order items are missing for order: " + order.getOrderId());
            return false;
        }
        
        // Validate each order item
        for (OrderItem item : order.getOrderItems()) {
            if (item.getProduct() == null) {
                logger.log(Level.WARNING, "Product is missing for order item in order: " + order.getOrderId());
                return false;
            }
            if (item.getQuantity() <= 0) {
                logger.log(Level.WARNING, "Invalid quantity for order item in order: " + order.getOrderId());
                return false;
            }
        }
        
        // Check pricing consistency
        if (order.getTotalProductPriceExclVAT() < 0 || order.getTotalProductPriceInclVAT() < 0) {
            logger.log(Level.WARNING, "Invalid pricing for order: " + order.getOrderId());
            return false;
        }
        
        logger.log(Level.INFO, "Order data completeness validation passed for: " + order.getOrderId());
        return true;
    }
    
    @Override
    public OrderEntity loadOrderWithFallbacks(String orderId) throws ResourceNotFoundException {
        logger.log(Level.INFO, "Loading order with fallbacks for: " + orderId);
        
        try {
            // Try to load complete order first
            OrderEntity order = loadCompleteOrderData(orderId);
            
            // Apply fallbacks for any missing critical data
            applyOrderDataFallbacks(order);
            
            return order;
            
        } catch (ResourceNotFoundException e) {
            // If complete loading fails, try partial loading with fallbacks
            logger.log(Level.WARNING, "Complete loading failed, attempting partial load with fallbacks for: " + orderId);
            return loadPartialOrderWithFallbacks(orderId);
        }
    }
    
    @Override
    public OrderEntity refreshOrderRelationships(OrderEntity order) throws ResourceNotFoundException {
        if (order == null || order.getOrderId() == null) {
            throw new ResourceNotFoundException("Order or order ID cannot be null");
        }
        
        logger.log(Level.INFO, "Refreshing relationships for order: " + order.getOrderId());
        
        try {
            // Refresh all relationships
            loadOrderItemsWithValidation(order);
            loadDeliveryInfoWithValidation(order);
            loadInvoiceWithValidation(order);
            loadPaymentTransactionsWithValidation(order);
            loadUserAccountWithValidation(order);
            
            logger.log(Level.INFO, "Successfully refreshed relationships for order: " + order.getOrderId());
            return order;
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error refreshing relationships for order: " + order.getOrderId(), e);
            throw new ResourceNotFoundException("Failed to refresh order relationships: " + e.getMessage());
        }
    }
    
    @Override
    public boolean validateLazyLoadingInitialization(OrderEntity order) {
        if (order == null) {
            return false;
        }
        
        logger.log(Level.FINE, "Validating lazy loading initialization for order: " + order.getOrderId());
        
        try {
            // Test access to lazy-loaded relationships
            List<OrderItem> items = order.getOrderItems();
            if (items != null) {
                // Test if we can iterate through items without lazy loading exceptions
                for (OrderItem item : items) {
                    if (item.getProduct() != null) {
                        // Access product properties to ensure it's loaded
                        item.getProduct().getTitle();
                    }
                }
            }
            
            // Test delivery info access
            DeliveryInfo deliveryInfo = order.getDeliveryInfo();
            if (deliveryInfo != null) {
                deliveryInfo.getRecipientName(); // Test access
            }
            
            // Test invoice access
            Invoice invoice = order.getInvoice();
            if (invoice != null) {
                invoice.getInvoiceDate(); // Test access
            }
            
            // Test payment transactions access
            List<PaymentTransaction> transactions = order.getPaymentTransactions();
            if (transactions != null) {
                for (PaymentTransaction transaction : transactions) {
                    transaction.getTransactionStatus(); // Test access
                }
            }
            
            // Test user account access
            UserAccount userAccount = order.getUserAccount();
            if (userAccount != null) {
                userAccount.getUserId(); // Test access
            }
            
            logger.log(Level.FINE, "Lazy loading validation passed for order: " + order.getOrderId());
            return true;
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "Lazy loading validation failed for order: " + 
                      order.getOrderId() + " - " + e.getMessage());
            return false;
        }
    }
    
    // Private helper methods
    
    private void loadOrderItemsWithValidation(OrderEntity order) throws SQLException {
        String orderId = order.getOrderId();
        logger.log(Level.FINE, "Loading order items for order: " + orderId);
        
        if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
            List<OrderItem> items = orderItemDAO.getItemsByOrderId(orderId);
            if (items == null) {
                items = new ArrayList<>(); // Provide empty list as fallback
            }
            order.setOrderItems(items);
            logger.log(Level.FINE, "Loaded " + items.size() + " order items for order: " + orderId);
        }
        
        // Validate that products are loaded for each item
        for (OrderItem item : order.getOrderItems()) {
            if (item.getProduct() == null) {
                logger.log(Level.WARNING, "Product not loaded for order item, attempting to load...");
                // This would require additional logic if products aren't loaded by the DAO
            }
        }
    }
    
    private void loadDeliveryInfoWithValidation(OrderEntity order) throws SQLException {
        String orderId = order.getOrderId();
        logger.log(Level.FINE, "Loading delivery info for order: " + orderId);
        
        if (order.getDeliveryInfo() == null) {
            DeliveryInfo deliveryInfo = deliveryInfoDAO.getByOrderId(orderId);
            order.setDeliveryInfo(deliveryInfo);
            logger.log(Level.FINE, "Loaded delivery info for order: " + orderId + 
                      " (found: " + (deliveryInfo != null) + ")");
        }
    }
    
    private void loadInvoiceWithValidation(OrderEntity order) throws SQLException {
        String orderId = order.getOrderId();
        logger.log(Level.FINE, "Loading invoice for order: " + orderId);
        
        if (order.getInvoice() == null) {
            Invoice invoice = invoiceDAO.getByOrderId(orderId);
            order.setInvoice(invoice);
            logger.log(Level.FINE, "Loaded invoice for order: " + orderId + 
                      " (found: " + (invoice != null) + ")");
        }
    }
    
    private void loadPaymentTransactionsWithValidation(OrderEntity order) throws SQLException {
        String orderId = order.getOrderId();
        logger.log(Level.FINE, "Loading payment transactions for order: " + orderId);
        
        if (order.getPaymentTransactions() == null) {
            List<PaymentTransaction> transactions = paymentTransactionDAO.getByOrderId(orderId);
            if (transactions == null) {
                transactions = new ArrayList<>(); // Provide empty list as fallback
            }
            order.setPaymentTransactions(transactions);
            logger.log(Level.FINE, "Loaded " + transactions.size() + " payment transactions for order: " + orderId);
        }
    }
    
    private void loadUserAccountWithValidation(OrderEntity order) throws SQLException {
        logger.log(Level.FINE, "Loading user account for order: " + order.getOrderId());
        
        if (order.getUserAccount() == null) {
            // This might be a guest order, which is acceptable
            logger.log(Level.FINE, "No user account associated with order: " + order.getOrderId() + " (guest order)");
        } else {
            // Validate that user account is properly loaded
            UserAccount user = order.getUserAccount();
            if (user.getUserId() == null) {
                logger.log(Level.WARNING, "User account appears to be incompletely loaded for order: " + order.getOrderId());
            }
        }
    }
    
    private List<OrderItemDTO> convertOrderItemsToDTO(List<OrderItem> orderItems) {
        if (orderItems == null || orderItems.isEmpty()) {
            return new ArrayList<>();
        }
        
        return orderItems.stream()
                .map(this::convertOrderItemToDTO)
                .collect(Collectors.toList());
    }
    
    private OrderItemDTO convertOrderItemToDTO(OrderItem item) {
        Product product = item.getProduct();
        return new OrderItemDTO(
            product.getProductId(),
            product.getTitle(),
            item.getQuantity(),
            item.getPriceAtTimeOfOrder(),
            product.getImageUrl(),
            item.isEligibleForRushDelivery()
        );
    }
    
    private DeliveryInfoDTO convertDeliveryInfoToDTO(DeliveryInfo deliveryInfo) {
        if (deliveryInfo == null) {
            return null;
        }
        
        DeliveryInfoDTO dto = new DeliveryInfoDTO();
        dto.setRecipientName(deliveryInfo.getRecipientName());
        dto.setEmail(deliveryInfo.getEmail());
        dto.setPhoneNumber(deliveryInfo.getPhoneNumber());
        dto.setDeliveryProvinceCity(deliveryInfo.getDeliveryProvinceCity());
        dto.setDeliveryAddress(deliveryInfo.getDeliveryAddress());
        dto.setDeliveryInstructions(deliveryInfo.getDeliveryInstructions());
        dto.setRushOrder("RUSH_DELIVERY".equals(deliveryInfo.getDeliveryMethodChosen()));
        dto.setRequestedRushDeliveryTime(deliveryInfo.getRequestedRushDeliveryTime());
        
        return dto;
    }
    
    private RushDeliveryDetailsDTO createRushDeliveryDetailsDTO(OrderEntity order) {
        if (order.getDeliveryInfo() == null || 
            !"RUSH_DELIVERY".equals(order.getDeliveryInfo().getDeliveryMethodChosen())) {
            return null;
        }
        
        // Filter items eligible for rush delivery
        List<OrderItemDTO> rushItems = order.getOrderItems().stream()
                .filter(OrderItem::isEligibleForRushDelivery)
                .map(this::convertOrderItemToDTO)
                .collect(Collectors.toList());
        
        if (rushItems.isEmpty()) {
            return null;
        }
        
        // Calculate rush delivery fee (simplified - could be more complex)
        float rushDeliverySubFee = order.getCalculatedDeliveryFee() * 0.5f; // Assume 50% of total delivery fee
        
        return new RushDeliveryDetailsDTO(
            rushItems,
            rushDeliverySubFee,
            "Rush delivery requested for eligible items",
            order.getDeliveryInfo().getRequestedRushDeliveryTime(),
            true, // rushDeliveryAvailable
            "REQUESTED" // rushDeliveryStatus
        );
    }
    
    private float validateAndCalculateTotalExclVAT(OrderEntity order) throws ValidationException {
        float total = order.getTotalProductPriceExclVAT();
        if (total < 0) {
            throw new ValidationException("Invalid total product price excluding VAT: " + total);
        }
        return total;
    }
    
    private float validateAndCalculateTotalInclVAT(OrderEntity order) throws ValidationException {
        float total = order.getTotalProductPriceInclVAT();
        if (total < 0) {
            throw new ValidationException("Invalid total product price including VAT: " + total);
        }
        
        // Validate VAT calculation consistency
        float expectedInclVAT = order.getTotalProductPriceExclVAT() * (1 + VAT_RATE);
        if (Math.abs(total - expectedInclVAT) > 0.01f) { // Allow small floating point differences
            logger.log(Level.WARNING, "VAT calculation inconsistency detected for order: " + order.getOrderId() +
                      " Expected: " + expectedInclVAT + ", Actual: " + total);
        }
        
        return total;
    }
    
    private float validateAndGetDeliveryFee(OrderEntity order) throws ValidationException {
        float fee = order.getCalculatedDeliveryFee();
        if (fee < 0) {
            throw new ValidationException("Invalid delivery fee: " + fee);
        }
        return fee;
    }
    
    private float validateAndCalculateTotalAmount(OrderEntity order) throws ValidationException {
        float total = order.getTotalAmountPaid();
        if (total < 0) {
            throw new ValidationException("Invalid total amount to be paid: " + total);
        }
        
        // Validate total calculation consistency
        float expectedTotal = order.getTotalProductPriceInclVAT() + order.getCalculatedDeliveryFee();
        if (Math.abs(total - expectedTotal) > 0.01f) { // Allow small floating point differences
            logger.log(Level.WARNING, "Total amount calculation inconsistency detected for order: " + order.getOrderId() +
                      " Expected: " + expectedTotal + ", Actual: " + total);
        }
        
        return total;
    }
    
    private void applyOrderDataFallbacks(OrderEntity order) {
        logger.log(Level.INFO, "Applying data fallbacks for order: " + order.getOrderId());
        
        // Fallback for missing order items
        if (order.getOrderItems() == null) {
            order.setOrderItems(new ArrayList<>());
            logger.log(Level.WARNING, "Applied fallback: empty order items list for order: " + order.getOrderId());
        }
        
        // Fallback for missing payment transactions
        if (order.getPaymentTransactions() == null) {
            order.setPaymentTransactions(new ArrayList<>());
            logger.log(Level.WARNING, "Applied fallback: empty payment transactions list for order: " + order.getOrderId());
        }
        
        // Fallback for missing order date
        if (order.getOrderDate() == null) {
            order.setOrderDate(LocalDateTime.now());
            logger.log(Level.WARNING, "Applied fallback: current date for missing order date for order: " + order.getOrderId());
        }
        
        // Fallback for negative pricing
        if (order.getTotalProductPriceExclVAT() < 0) {
            order.setTotalProductPriceExclVAT(0f);
            logger.log(Level.WARNING, "Applied fallback: zero for negative total excl VAT for order: " + order.getOrderId());
        }
        
        if (order.getTotalProductPriceInclVAT() < 0) {
            order.setTotalProductPriceInclVAT(0f);
            logger.log(Level.WARNING, "Applied fallback: zero for negative total incl VAT for order: " + order.getOrderId());
        }
        
        if (order.getCalculatedDeliveryFee() < 0) {
            order.setCalculatedDeliveryFee(0f);
            logger.log(Level.WARNING, "Applied fallback: zero for negative delivery fee for order: " + order.getOrderId());
        }
        
        if (order.getTotalAmountPaid() < 0) {
            order.setTotalAmountPaid(0f);
            logger.log(Level.WARNING, "Applied fallback: zero for negative total amount for order: " + order.getOrderId());
        }
    }
    
    private OrderEntity loadPartialOrderWithFallbacks(String orderId) throws ResourceNotFoundException {
        logger.log(Level.INFO, "Attempting partial order load with fallbacks for: " + orderId);
        
        try {
            // Try to load at least the base order
            OrderEntity order = orderEntityDAO.getById(orderId);
            if (order == null) {
                throw new ResourceNotFoundException("Order with ID " + orderId + " not found");
            }
            
            // Try to load each relationship individually with fallbacks
            try {
                loadOrderItemsWithValidation(order);
            } catch (SQLException e) {
                logger.log(Level.WARNING, "Failed to load order items, using empty list: " + e.getMessage());
                order.setOrderItems(new ArrayList<>());
            }
            
            try {
                loadDeliveryInfoWithValidation(order);
            } catch (SQLException e) {
                logger.log(Level.WARNING, "Failed to load delivery info: " + e.getMessage());
                // Leave as null - acceptable for some order states
            }
            
            try {
                loadInvoiceWithValidation(order);
            } catch (SQLException e) {
                logger.log(Level.WARNING, "Failed to load invoice: " + e.getMessage());
                // Leave as null - acceptable for unpaid orders
            }
            
            try {
                loadPaymentTransactionsWithValidation(order);
            } catch (SQLException e) {
                logger.log(Level.WARNING, "Failed to load payment transactions, using empty list: " + e.getMessage());
                order.setPaymentTransactions(new ArrayList<>());
            }
            
            // Apply general fallbacks
            applyOrderDataFallbacks(order);
            
            logger.log(Level.INFO, "Successfully loaded partial order with fallbacks for: " + orderId);
            return order;
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to load even partial order data for: " + orderId, e);
            throw new ResourceNotFoundException("Unable to load order data: " + e.getMessage());
        }
    }
    
    private UserSummaryDTO createUserSummaryDTO(UserAccount userAccount) {
        if (userAccount == null) {
            return new UserSummaryDTO(null, "Guest User", null, true);
        }
        
        return new UserSummaryDTO(
            userAccount.getUserId(),
            userAccount.getUsername(), // Using username as fullName since getFullName() doesn't exist
            userAccount.getEmail(),
            false
        );
    }
    
    private PaymentSummaryDTO createPaymentSummaryDTO(OrderEntity order) {
        if (order.getPaymentTransactions() == null || order.getPaymentTransactions().isEmpty()) {
            return new PaymentSummaryDTO(
                "UNKNOWN",
                "PENDING",
                null,
                null,
                null,
                false
            );
        }
        
        // Find the latest successful payment transaction
        PaymentTransaction latestPayment = order.getPaymentTransactions().stream()
            .filter(t -> "SUCCESS".equalsIgnoreCase(t.getTransactionStatus()))
            .reduce((first, second) -> second) // Get the last one
            .orElse(order.getPaymentTransactions().get(order.getPaymentTransactions().size() - 1));
        
        String paymentMethodType = "UNKNOWN";
        String lastFourDigits = null;
        
        if (latestPayment.getPaymentMethod() != null) {
            paymentMethodType = latestPayment.getPaymentMethod().getMethodType().name();
            
            // Try to get last four digits from card details if available
            if (latestPayment.getPaymentMethod().getCardDetails() != null) {
                String cardNumberMasked = latestPayment.getPaymentMethod().getCardDetails().getCardNumberMasked();
                if (cardNumberMasked != null && cardNumberMasked.length() >= 4) {
                    // Extract last 4 digits from masked card number (e.g., "************1234")
                    lastFourDigits = cardNumberMasked.substring(cardNumberMasked.length() - 4);
                }
            }
        }
        
        return new PaymentSummaryDTO(
            paymentMethodType,
            latestPayment.getTransactionStatus(),
            lastFourDigits,
            latestPayment.getTransactionDateTime(),
            latestPayment.getTransactionId(),
            "SUCCESS".equalsIgnoreCase(latestPayment.getTransactionStatus())
        );
    }
    
    private String combineSpecialInstructions(OrderEntity order) {
        StringBuilder instructions = new StringBuilder();
        
        // Add delivery instructions
        if (order.getDeliveryInfo() != null &&
            order.getDeliveryInfo().getDeliveryInstructions() != null &&
            !order.getDeliveryInfo().getDeliveryInstructions().trim().isEmpty()) {
            instructions.append("Delivery: ").append(order.getDeliveryInfo().getDeliveryInstructions());
        }
        
        // Add rush delivery info if applicable
        if (order.getDeliveryInfo() != null &&
            "RUSH_DELIVERY".equals(order.getDeliveryInfo().getDeliveryMethodChosen())) {
            if (instructions.length() > 0) {
                instructions.append(" | ");
            }
            instructions.append("Rush delivery requested");
            
            if (order.getDeliveryInfo().getRequestedRushDeliveryTime() != null) {
                instructions.append(" for ").append(order.getDeliveryInfo().getRequestedRushDeliveryTime());
            }
        }
        
        return instructions.length() > 0 ? instructions.toString() : null;
    }
}
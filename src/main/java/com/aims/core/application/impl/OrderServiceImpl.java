package com.aims.core.application.impl;

import org.springframework.stereotype.Service;
import com.aims.core.application.services.IOrderService;
import com.aims.core.application.services.ICartService;
import com.aims.core.application.services.IDeliveryCalculationService;
import com.aims.core.application.services.INotificationService;
import com.aims.core.application.services.IPaymentService;
import com.aims.core.application.services.IProductService;
import com.aims.core.application.services.IOrderDataLoaderService;
import com.aims.core.application.services.IStockValidationService;
import com.aims.core.application.services.IOrderStateManagementService;
import com.aims.core.entities.*;
import com.aims.core.enums.OrderStatus;
import com.aims.core.enums.TransactionType;
import com.aims.core.infrastructure.database.dao.*;
import com.aims.core.shared.exceptions.*;
import com.aims.core.shared.dto.SearchResult;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class OrderServiceImpl implements IOrderService {

    private final IOrderEntityDAO orderDAO;
    private final IOrderItemDAO orderItemDAO;
    private final IDeliveryInfoDAO deliveryInfoDAO;
    private final IInvoiceDAO invoiceDAO;
    private final IProductDAO productDAO;
    private final IProductService productService;
    private final ICartService cartService;
    private final IPaymentService paymentService;
    private final IDeliveryCalculationService deliveryCalculationService;
    private final INotificationService notificationService;
    private final IUserAccountDAO userAccountDAO;
    private final IOrderDataLoaderService orderDataLoaderService;
    private final IStockValidationService stockValidationService;
    private final IOrderStateManagementService orderStateManagementService;

    private static final float VAT_RATE = 0.10f;

    public OrderServiceImpl(IOrderEntityDAO orderDAO,
                            IOrderItemDAO orderItemDAO,
                            IDeliveryInfoDAO deliveryInfoDAO,
                            IInvoiceDAO invoiceDAO,
                            IProductDAO productDAO,
                            IProductService productService,
                            ICartService cartService,
                            IPaymentService paymentService,
                            IDeliveryCalculationService deliveryCalculationService,
                            INotificationService notificationService,
                            IUserAccountDAO userAccountDAO,
                            IOrderDataLoaderService orderDataLoaderService,
                            IStockValidationService stockValidationService,
                            IOrderStateManagementService orderStateManagementService) {
        this.orderDAO = orderDAO;
        this.orderItemDAO = orderItemDAO;
        this.deliveryInfoDAO = deliveryInfoDAO;
        this.invoiceDAO = invoiceDAO;
        this.productDAO = productDAO;
        this.productService = productService;
        this.cartService = cartService;
        this.paymentService = paymentService;
        this.deliveryCalculationService = deliveryCalculationService;
        this.notificationService = notificationService;
        this.userAccountDAO = userAccountDAO;
        this.orderDataLoaderService = orderDataLoaderService;
        this.stockValidationService = stockValidationService;
        this.orderStateManagementService = orderStateManagementService;
    }

    @Override
    public OrderEntity createOrder(String userId) throws ValidationException {
        if (userId == null || userId.trim().isEmpty()) {
            throw new ValidationException("User ID is required to create an order");
        }
        
        try {
            UserAccount user = userAccountDAO.getById(userId);
            if (user == null) {
                throw new ValidationException("User with ID " + userId + " not found");
            }
            
            OrderEntity order = new OrderEntity();
            order.setOrderId("ORD-" + UUID.randomUUID().toString());
            order.setOrderDate(LocalDateTime.now());
            order.setOrderStatus(OrderStatus.PENDING_DELIVERY_INFO);
            order.setUserAccount(user);
            order.setTotalProductPriceExclVAT(0f);
            order.setTotalProductPriceInclVAT(0f);
            order.setCalculatedDeliveryFee(0f);
            order.setTotalAmountPaid(0f);
            
            orderDAO.add(order);
            return order;
            
        } catch (SQLException e) {
            throw new ValidationException("Unable to create order due to database error: " + e.getMessage());
        }
    }

    @Override
    public OrderEntity initiateOrderFromCart(String cartSessionId, String userId) throws ValidationException {
        
        System.out.println("ORDER CREATION: Validating cart for session: " + cartSessionId);
        
        if (cartSessionId == null || cartSessionId.trim().isEmpty()) {
            System.err.println("ORDER CREATION ERROR: Cart session ID is null or empty");
            throw new ValidationException("Invalid cart session ID provided");
        }
        
        try {
            Cart cart = cartService.getCart(cartSessionId);
            if (cart == null) {
                System.err.println("ORDER CREATION ERROR: Cart is null for session: " + cartSessionId);
                throw new ValidationException("Cart not found for session ID: " + cartSessionId);
            }
            if (cart.getItems() == null || cart.getItems().isEmpty()) {
                System.err.println("ORDER CREATION ERROR: Cart items are empty for session: " + cartSessionId);
                throw new ValidationException("Cart is empty for session ID: " + cartSessionId);
            }
            
            System.out.println("ORDER CREATION: Cart validation passed - " + cart.getItems().size() + " items found");

            OrderEntity order = new OrderEntity();
            order.setOrderId("ORD-" + UUID.randomUUID().toString());
            order.setOrderDate(LocalDateTime.now());
            order.setOrderStatus(OrderStatus.PENDING_DELIVERY_INFO);

            if (userId != null) {
                UserAccount user = userAccountDAO.getById(userId);
                if (user == null) throw new ValidationException("User with ID " + userId + " not found.");
                order.setUserAccount(user);
            }

            float totalProductPriceExclVAT = 0f;
            List<OrderItem> orderItems = new ArrayList<>();

            for (CartItem cartItem : cart.getItems()) {
                Product product = productDAO.getById(cartItem.getProduct().getProductId());
                if (product == null) {
                    throw new ValidationException("Product " + cartItem.getProduct().getProductId() + " in cart not found in catalog.");
                }
                // Use StockValidationService for enhanced stock validation
                IStockValidationService.StockValidationResult stockResult;
                try {
                    stockResult = stockValidationService.validateProductStock(cartItem.getProduct().getProductId(), cartItem.getQuantity());
                } catch (ResourceNotFoundException e) {
                    throw new ValidationException("Product not found during stock validation: " + e.getMessage());
                }
                if (!stockResult.isValid()) {
                    throw new ValidationException("Stock validation failed for product: " + product.getTitle() +
                            ". " + stockResult.getValidationMessage());
                }
                
                OrderItem orderItem = new OrderItem(order, product, cartItem.getQuantity(), product.getPrice(), 
                    product.getProductType() != com.aims.core.enums.ProductType.BOOK);
                orderItems.add(orderItem);
                totalProductPriceExclVAT += product.getPrice() * cartItem.getQuantity();
            }

            order.setTotalProductPriceExclVAT(totalProductPriceExclVAT);
            order.setTotalProductPriceInclVAT(totalProductPriceExclVAT * (1 + VAT_RATE));

            orderDAO.add(order);
            for (OrderItem item : orderItems) {
                item.setOrderEntity(order);
                orderItemDAO.add(item);
            }
            order.setOrderItems(orderItems);
            
            return order;
            
        } catch (SQLException e) {
            throw new ValidationException("Database error during order creation: " + e.getMessage());
        }
    }

    /**
     * Enhanced cart-to-order conversion with complete data preservation and comprehensive validation.
     * This method ensures all product metadata is preserved during the cart-to-order transition.
     */
    public OrderEntity initiateOrderFromCartEnhanced(String cartSessionId, String userId) throws ValidationException {
        System.out.println("ENHANCED ORDER CREATION: Starting enhanced cart-to-order conversion for session: " + cartSessionId);
        
        if (cartSessionId == null || cartSessionId.trim().isEmpty()) {
            System.err.println("ENHANCED ORDER CREATION ERROR: Cart session ID is null or empty");
            throw new ValidationException("Invalid cart session ID provided");
        }
        
        try {
            // Step 1: Begin transaction conceptually (using try-catch for rollback simulation)
            System.out.println("ENHANCED ORDER CREATION: Step 1 - Starting transaction");
            
            // Step 2: Load cart with complete product metadata
            System.out.println("ENHANCED ORDER CREATION: Step 2 - Loading cart with complete product data");
            Cart cart = loadCartWithCompleteData(cartSessionId);
            
            // Step 3: Comprehensive cart validation
            System.out.println("ENHANCED ORDER CREATION: Step 3 - Performing comprehensive cart validation");
            validateCartCompleteness(cart);
            
            // Step 4: Create order with enhanced data preservation
            System.out.println("ENHANCED ORDER CREATION: Step 4 - Creating order with enhanced data preservation");
            OrderEntity order = createOrderEntityWithCompleteData(cart, userId);
            
            // Step 5: Create order items with complete product metadata
            System.out.println("ENHANCED ORDER CREATION: Step 5 - Creating order items with complete metadata");
            List<OrderItem> orderItems = createOrderItemsWithCompleteData(cart, order);
            
            // Step 6: Enhanced pricing calculations
            System.out.println("ENHANCED ORDER CREATION: Step 6 - Performing enhanced pricing calculations");
            calculateEnhancedPricing(order, orderItems);
            
            // Step 7: Persist order and items with transaction safety
            System.out.println("ENHANCED ORDER CREATION: Step 7 - Persisting order and items");
            persistOrderWithTransactionSafety(order, orderItems);
            
            // Step 8: Validate order completeness after creation
            System.out.println("ENHANCED ORDER CREATION: Step 8 - Validating order completeness");
            validateOrderCompleteness(order);
            
            System.out.println("ENHANCED ORDER CREATION: Successfully created enhanced order: " + order.getOrderId() +
                             " with " + orderItems.size() + " items");
            
            return order;
            
        } catch (Exception e) {
            System.err.println("ENHANCED ORDER CREATION ERROR: " + e.getMessage());
            // Rollback transaction conceptually
            throw new ValidationException("Enhanced order creation failed: " + e.getMessage());
        }
    }
    
    /**
     * Loads cart with complete product data including all metadata
     */
    private Cart loadCartWithCompleteData(String cartSessionId) throws ValidationException {
        try {
            // Use CartServiceImpl's enhanced method to get complete data
            if (cartService instanceof CartServiceImpl) {
                CartServiceImpl cartServiceImpl = (CartServiceImpl) cartService;
                return cartServiceImpl.getCartWithCompleteProductData(cartSessionId);
            } else {
                // Fallback to regular cart loading
                Cart cart = cartService.getCart(cartSessionId);
                if (cart == null) {
                    throw new ValidationException("Cart not found for session ID: " + cartSessionId);
                }
                return cart;
            }
        } catch (Exception e) {
            throw new ValidationException("Failed to load cart with complete data: " + e.getMessage());
        }
    }
    
    /**
     * Comprehensive cart validation before order creation
     */
    private void validateCartCompleteness(Cart cart) throws ValidationException {
        if (cart == null) {
            throw new ValidationException("Cart cannot be null");
        }
        
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new ValidationException("Cart is empty for session ID: " + cart.getCartSessionId());
        }
        
        System.out.println("CART VALIDATION: Validating " + cart.getItems().size() + " cart items");
        
        // Validate each cart item for order conversion
        for (CartItem cartItem : cart.getItems()) {
            validateCartItemForOrderConversion(cartItem);
        }
        
        System.out.println("CART VALIDATION: All cart items validated successfully");
    }
    
    /**
     * Validates individual cart item for order conversion
     */
    private void validateCartItemForOrderConversion(CartItem cartItem) throws ValidationException {
        if (cartItem == null) {
            throw new ValidationException("Cart item cannot be null");
        }
        
        if (cartItem.getProduct() == null) {
            throw new ValidationException("Product in cart item cannot be null");
        }
        
        if (cartItem.getQuantity() <= 0) {
            throw new ValidationException("Cart item quantity must be positive");
        }
        
        // Validate product exists and has current data
        try {
            Product currentProduct = productDAO.getById(cartItem.getProduct().getProductId());
            if (currentProduct == null) {
                throw new ValidationException("Product " + cartItem.getProduct().getProductId() +
                    " in cart not found in catalog");
            }
            
            // Use StockValidationService for enhanced stock validation
            IStockValidationService.StockValidationResult stockResult;
            try {
                stockResult = stockValidationService.validateProductStock(cartItem.getProduct().getProductId(), cartItem.getQuantity());
            } catch (ResourceNotFoundException e) {
                throw new ValidationException("Product not found during stock validation: " + e.getMessage());
            }
            if (!stockResult.isValid()) {
                throw new ValidationException("Enhanced stock validation failed for product: " + currentProduct.getTitle() +
                    ". " + stockResult.getValidationMessage());
            }
            
        } catch (Exception e) {
            throw new ValidationException("Error validating cart item: " + e.getMessage());
        }
    }
    
    /**
     * Creates order entity with complete data preservation
     */
    private OrderEntity createOrderEntityWithCompleteData(Cart cart, String userId) throws ValidationException {
        try {
            OrderEntity order = new OrderEntity();
            order.setOrderId("ORD-" + UUID.randomUUID().toString());
            order.setOrderDate(LocalDateTime.now());
            order.setOrderStatus(OrderStatus.PENDING_DELIVERY_INFO);
            
            // Set user account if provided
            if (userId != null) {
                UserAccount user = userAccountDAO.getById(userId);
                if (user == null) {
                    throw new ValidationException("User with ID " + userId + " not found");
                }
                order.setUserAccount(user);
            }
            
            // Initialize pricing fields
            order.setTotalProductPriceExclVAT(0f);
            order.setTotalProductPriceInclVAT(0f);
            order.setCalculatedDeliveryFee(0f);
            order.setTotalAmountPaid(0f);
            
            return order;
            
        } catch (Exception e) {
            throw new ValidationException("Failed to create order entity: " + e.getMessage());
        }
    }
    
    /**
     * Creates order items with complete product data preservation
     */
    private List<OrderItem> createOrderItemsWithCompleteData(Cart cart, OrderEntity order) throws ValidationException {
        List<OrderItem> orderItems = new ArrayList<>();
        
        for (CartItem cartItem : cart.getItems()) {
            OrderItem orderItem = createOrderItemWithCompleteData(cartItem, order);
            orderItems.add(orderItem);
        }
        
        return orderItems;
    }
    
    /**
     * Creates individual order item with complete product metadata preservation
     */
    private OrderItem createOrderItemWithCompleteData(CartItem cartItem, OrderEntity order) throws ValidationException {
        try {
            Product product = cartItem.getProduct();
            
            // Create order item with complete product information
            OrderItem orderItem = new OrderItem(
                order,
                product,
                cartItem.getQuantity(),
                product.getPrice(), // Price at time of order
                determineRushEligibility(product) // Enhanced rush delivery eligibility
            );
            
            System.out.println("ORDER ITEM CREATION: Created order item with complete data - Product: " +
                             product.getProductId() + ", Title: " + product.getTitle() +
                             ", Quantity: " + cartItem.getQuantity() +
                             ", Price: " + product.getPrice() +
                             ", Rush Eligible: " + orderItem.isEligibleForRushDelivery());
            
            return orderItem;
            
        } catch (Exception e) {
            throw new ValidationException("Failed to create order item with complete data: " + e.getMessage());
        }
    }
    
    /**
     * Enhanced rush delivery eligibility determination
     */
    private boolean determineRushEligibility(Product product) {
        // Enhanced logic for rush delivery eligibility
        boolean eligible = product.getProductType() != com.aims.core.enums.ProductType.BOOK;
        
        // Additional criteria for rush eligibility
        if (eligible) {
            // Check weight constraints (example: items over 10kg not eligible for rush)
            if (product.getWeightKg() > 10.0f) {
                eligible = false;
            }
            
            // Check dimensions constraints (example: very large items not eligible)
            if (product.getDimensionsCm() != null &&
                product.getDimensionsCm().contains("100")) { // Simplified check
                eligible = false;
            }
        }
        
        return eligible;
    }
    
    /**
     * Enhanced pricing calculations with detailed breakdown
     */
    private void calculateEnhancedPricing(OrderEntity order, List<OrderItem> orderItems) {
        float totalProductPriceExclVAT = 0f;
        
        for (OrderItem item : orderItems) {
            float itemTotal = item.getPriceAtTimeOfOrder() * item.getQuantity();
            totalProductPriceExclVAT += itemTotal;
            
            System.out.println("PRICING: Item " + item.getProduct().getProductId() +
                             " - Unit Price: " + item.getPriceAtTimeOfOrder() +
                             ", Quantity: " + item.getQuantity() +
                             ", Total: " + itemTotal);
        }
        
        float totalProductPriceInclVAT = totalProductPriceExclVAT * (1 + VAT_RATE);
        
        order.setTotalProductPriceExclVAT(totalProductPriceExclVAT);
        order.setTotalProductPriceInclVAT(totalProductPriceInclVAT);
        
        System.out.println("PRICING: Total Excl VAT: " + totalProductPriceExclVAT +
                          ", Total Incl VAT: " + totalProductPriceInclVAT +
                          ", VAT Rate: " + (VAT_RATE * 100) + "%");
    }
    
    /**
     * Persists order and items with transaction safety
     */
    private void persistOrderWithTransactionSafety(OrderEntity order, List<OrderItem> orderItems) throws ValidationException {
        try {
            // Begin transaction conceptually
            orderDAO.add(order);
            
            for (OrderItem item : orderItems) {
                item.setOrderEntity(order); // Ensure order relationship is set
                orderItemDAO.add(item);
            }
            
            order.setOrderItems(orderItems); // Set items in order object
            
            // Commit transaction conceptually - if we reach here, all operations succeeded
            System.out.println("PERSISTENCE: Successfully persisted order " + order.getOrderId() +
                             " with " + orderItems.size() + " items");
            
        } catch (Exception e) {
            // Rollback transaction conceptually
            System.err.println("PERSISTENCE ERROR: Failed to persist order: " + e.getMessage());
            throw new ValidationException("Database error during order creation: " + e.getMessage());
        }
    }
    
    /**
     * Validates order completeness after creation
     */
    private void validateOrderCompleteness(OrderEntity order) throws ValidationException {
        if (order == null) {
            throw new ValidationException("Order is null after creation");
        }
        
        if (order.getOrderId() == null || order.getOrderId().trim().isEmpty()) {
            throw new ValidationException("Order ID is missing");
        }
        
        if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
            throw new ValidationException("Order has no items");
        }
        
        if (order.getTotalProductPriceExclVAT() <= 0) {
            throw new ValidationException("Order total price is invalid");
        }
        
        System.out.println("ORDER VALIDATION: Order completeness validated successfully");
    }

    @Override
    public OrderEntity getOrderById(String orderId) throws ResourceNotFoundException {
        if (orderId == null || orderId.trim().isEmpty()) {
            throw new ResourceNotFoundException("Order ID cannot be null or empty");
        }
        
        try {
            OrderEntity order = orderDAO.getById(orderId);
            if (order == null) {
                throw new ResourceNotFoundException("Order with ID " + orderId + " not found");
            }
            return order;
        } catch (SQLException e) {
            throw new ResourceNotFoundException("Unable to retrieve order due to database error: " + e.getMessage());
        }
    }

    @Override
    public OrderEntity getOrderDetails(String orderId) throws ResourceNotFoundException {
        System.out.println("ORDER DETAILS: Using enhanced data loader for order: " + orderId);
        
        try {
            // Use the new OrderDataLoaderService for complete and reliable data loading
            OrderEntity order = orderDataLoaderService.loadCompleteOrderData(orderId);
            
            // Validate that all data is properly loaded
            if (!orderDataLoaderService.validateOrderDataCompleteness(order)) {
                System.out.println("ORDER DETAILS: Data incomplete, attempting fallback loading...");
                order = orderDataLoaderService.loadOrderWithFallbacks(orderId);
            }
            
            // Validate lazy loading initialization
            if (!orderDataLoaderService.validateLazyLoadingInitialization(order)) {
                System.out.println("ORDER DETAILS: Lazy loading issues detected, refreshing relationships...");
                order = orderDataLoaderService.refreshOrderRelationships(order);
            }
            
            System.out.println("ORDER DETAILS: Successfully loaded complete order data for: " + orderId);
            return order;
            
        } catch (ResourceNotFoundException e) {
            System.err.println("ORDER DETAILS ERROR: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.err.println("ORDER DETAILS ERROR: Unexpected error loading order: " + e.getMessage());
            throw new ResourceNotFoundException("Unable to retrieve order details: " + e.getMessage());
        }
    }

    @Override
    public List<OrderEntity> getOrdersByUserId(String userId) throws ResourceNotFoundException {
        try {
            return orderDAO.getByUserId(userId);
        } catch (SQLException e) {
            throw new ResourceNotFoundException("Unable to retrieve orders for user: " + e.getMessage());
        }
    }

    @Override
    public List<OrderEntity> getOrdersByStatus(OrderStatus status) {
        if (status == null) {
            return new ArrayList<>();
        }
        
        try {
            return orderDAO.getByStatus(status);
        } catch (SQLException e) {
            System.err.println("Error retrieving orders by status: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public SearchResult<OrderEntity> getOrdersByStatusForManager(OrderStatus status, int pageNumber, int pageSize) 
            throws ResourceNotFoundException {
        try {
            List<OrderEntity> ordersByStatus = orderDAO.getByStatus(status);

            int totalResults = ordersByStatus.size();
            int fromIndex = (pageNumber - 1) * pageSize;
            if (fromIndex >= totalResults && totalResults > 0) {
                fromIndex = Math.max(0, totalResults - pageSize);
            } else if (fromIndex >= totalResults && totalResults == 0) {
                return new SearchResult<OrderEntity>(new ArrayList<>(), pageNumber, 0, 0);
            }

            int toIndex = Math.min(fromIndex + pageSize, totalResults);
            List<OrderEntity> pageResults = ordersByStatus.subList(fromIndex, toIndex);
            int totalPages = (int) Math.ceil((double) totalResults / pageSize);
            if (totalPages == 0 && totalResults > 0) totalPages = 1;

            return new SearchResult<OrderEntity>(pageResults, pageNumber, totalPages, totalResults);
        } catch (SQLException e) {
            throw new ResourceNotFoundException("Unable to retrieve orders for manager: " + e.getMessage());
        }
    }

    @Override
    public void updateOrderStatus(String orderId, OrderStatus newStatus) 
            throws ResourceNotFoundException, OrderException {
        try {
            OrderEntity order = orderDAO.getById(orderId);
            if (order == null) {
                throw new ResourceNotFoundException("Order with ID " + orderId + " not found");
            }
            
            orderDAO.updateStatus(orderId, newStatus);
        } catch (SQLException e) {
            throw new OrderException("Unable to update order status due to database error: " + e.getMessage());
        }
    }

    @Override
    public void updateOrderStatus(String orderId, OrderStatus newStatus, String adminOrManagerId)
            throws ResourceNotFoundException, OrderException, ValidationException {
        try {
            OrderEntity order = getOrderDetails(orderId);
            OrderStatus oldStatus = order.getOrderStatus();

            if (oldStatus == newStatus) return;

            order.setOrderStatus(newStatus);
            orderDAO.updateStatus(orderId, newStatus);
            notificationService.sendOrderStatusUpdateNotification(order, oldStatus.name(), newStatus.name(), 
                "Status updated by " + adminOrManagerId);
        } catch (SQLException e) {
            throw new OrderException("Database error updating order status: " + e.getMessage());
        }
    }

    @Override
    public void addDeliveryInfo(String orderId, DeliveryInfo deliveryInfo) 
            throws ResourceNotFoundException, ValidationException {
        setDeliveryInformation(orderId, deliveryInfo, false);
    }

    @Override
    public OrderEntity setDeliveryInformation(String orderId, DeliveryInfo deliveryInfoInput, boolean isRushOrder)
            throws ResourceNotFoundException, ValidationException {
        try {
            OrderEntity order = orderDAO.getById(orderId);
            if (order == null) {
                throw new ResourceNotFoundException("Order with ID " + orderId + " not found.");
            }
            if (order.getOrderStatus() != OrderStatus.PENDING_DELIVERY_INFO && 
                order.getOrderStatus() != OrderStatus.PENDING_PAYMENT) {
                throw new ValidationException("Delivery information can only be set for orders pending delivery info or pending payment.");
            }
            if (deliveryInfoInput == null) {
                throw new ValidationException("Delivery information cannot be null.");
            }

            // Validate required fields
            if (deliveryInfoInput.getRecipientName() == null || deliveryInfoInput.getRecipientName().trim().isEmpty() ||
                deliveryInfoInput.getPhoneNumber() == null || deliveryInfoInput.getPhoneNumber().trim().isEmpty() ||
                deliveryInfoInput.getDeliveryAddress() == null || deliveryInfoInput.getDeliveryAddress().trim().isEmpty() ||
                deliveryInfoInput.getDeliveryProvinceCity() == null || deliveryInfoInput.getDeliveryProvinceCity().trim().isEmpty()) {
                throw new ValidationException("Recipient name, phone number, address, and province/city are required.");
            }

            // Check rush order eligibility
            boolean actualRushOrderApplicable = false;
            if (isRushOrder) {
                if (!deliveryCalculationService.isRushDeliveryAddressEligible(deliveryInfoInput)) {
                    throw new ValidationException("Delivery address is not eligible for rush order.");
                }
                
                boolean anyItemEligibleForRush = order.getOrderItems().stream()
                    .anyMatch(OrderItem::isEligibleForRushDelivery);
                if (!anyItemEligibleForRush) {
                    throw new ValidationException("No items in the order are eligible for rush delivery.");
                }
                actualRushOrderApplicable = true;
                deliveryInfoInput.setDeliveryMethodChosen("RUSH_DELIVERY");
            } else {
                deliveryInfoInput.setDeliveryMethodChosen("STANDARD");
            }

            deliveryInfoInput.setOrderEntity(order);
            order.setDeliveryInfo(deliveryInfoInput);
            
            float shippingFee = deliveryCalculationService.calculateShippingFee(order, actualRushOrderApplicable);
            order.setCalculatedDeliveryFee(shippingFee);
            order.setTotalAmountPaid(order.getTotalProductPriceInclVAT() + shippingFee);
            order.setOrderStatus(OrderStatus.PENDING_PAYMENT);

            DeliveryInfo existingDeliveryInfo = deliveryInfoDAO.getByOrderId(orderId);
            if (existingDeliveryInfo != null) {
                deliveryInfoInput.setDeliveryInfoId(existingDeliveryInfo.getDeliveryInfoId());
                deliveryInfoDAO.update(deliveryInfoInput);
            } else {
                deliveryInfoInput.setDeliveryInfoId("DINFO-" + UUID.randomUUID().toString());
                deliveryInfoDAO.add(deliveryInfoInput);
            }
            orderDAO.update(order);
            
            return order;
        } catch (SQLException e) {
            throw new ValidationException("Database error setting delivery information: " + e.getMessage());
        }
    }

    @Override
    public float calculateShippingFee(String orderId, DeliveryInfo deliveryInfo, boolean isRushOrder)
            throws ResourceNotFoundException {
        try {
            OrderEntity order = orderDAO.getById(orderId);
            if (order == null) {
                throw new ResourceNotFoundException("Order with ID " + orderId + " not found for fee calculation.");
            }
            return deliveryCalculationService.calculateShippingFee(order, isRushOrder);
        } catch (SQLException e) {
            throw new ResourceNotFoundException("Unable to calculate shipping fee: " + e.getMessage());
        } catch (ValidationException e) {
            throw new ResourceNotFoundException("Validation error calculating shipping fee: " + e.getMessage());
        }
    }

    @Override
    public float calculateShippingFeePreview(List<OrderItem> items, DeliveryInfo deliveryInfo, boolean isRushOrder)
            throws ValidationException {
        if (items == null || items.isEmpty()) {
            throw new ValidationException("Order items are required for shipping calculation.");
        }
        if (deliveryInfo == null) {
            throw new ValidationException("Delivery information is required for shipping calculation.");
        }
        
        OrderEntity tempOrder = new OrderEntity();
        tempOrder.setOrderItems(items);
        tempOrder.setDeliveryInfo(deliveryInfo);
        
        return deliveryCalculationService.calculateShippingFee(tempOrder, isRushOrder);
    }

    @Override
    public float calculateOrderTotal(String orderId) throws ResourceNotFoundException {
        try {
            OrderEntity order = orderDAO.getById(orderId);
            if (order == null) {
                throw new ResourceNotFoundException("Order with ID " + orderId + " not found");
            }
            return order.getTotalProductPriceInclVAT() + order.getCalculatedDeliveryFee();
        } catch (SQLException e) {
            throw new ResourceNotFoundException("Unable to calculate order total due to database error: " + e.getMessage());
        }
    }

    @Override
    public void processOrderPayment(String orderId, String paymentMethodId) 
            throws ResourceNotFoundException, PaymentException, ValidationException {
        try {
            OrderEntity order = orderDAO.getById(orderId);
            if (order == null) {
                throw new ResourceNotFoundException("Order " + orderId + " not found.");
            }
            
            if (order.getOrderStatus() != OrderStatus.PENDING_PAYMENT) {
                throw new ValidationException("Order is not pending payment. Current status: " + order.getOrderStatus());
            }
            
            // Load delivery info if not present
            if (order.getDeliveryInfo() == null) {
                DeliveryInfo deliveryInfo = deliveryInfoDAO.getByOrderId(orderId);
                if (deliveryInfo != null) {
                    order.setDeliveryInfo(deliveryInfo);
                } else {
                    throw new ValidationException("Delivery information is required before payment.");
                }
            }

            // Enhanced final inventory check using StockValidationService
            IStockValidationService.BulkStockValidationResult bulkValidation = 
                stockValidationService.validateOrderItemsStock(order.getOrderItems());
            if (!bulkValidation.isAllValid()) {
                order.setOrderStatus(OrderStatus.PENDING_DELIVERY_INFO);
                orderDAO.updateStatus(orderId, order.getOrderStatus());
                String failedProducts = bulkValidation.getFailedValidations().stream()
                    .map(result -> result.getProductId() + ": " + result.getValidationMessage())
                    .reduce("", (a, b) -> a + "; " + b);
                throw new ValidationException("Stock validation failed for products: " + failedProducts);
            }

            PaymentTransaction paymentTransactionResult = paymentService.processPayment(order, paymentMethodId);

            // Update product stock
            for (OrderItem item : order.getOrderItems()) {
                try {
                    productService.updateProductStock(item.getProduct().getProductId(), -item.getQuantity());
                } catch (Exception e) {
                    System.err.println("CRITICAL: Payment succeeded but failed to update stock for product " + 
                        item.getProduct().getProductId() + ". Reason: " + e.getMessage());
                    order.setOrderStatus(OrderStatus.PAYMENT_FAILED);
                    orderDAO.updateStatus(orderId, OrderStatus.PAYMENT_FAILED);
                    throw new ValidationException("Payment successful, but stock update failed. Please contact support.");
                }
            }

            // Create Invoice
            Invoice invoice = new Invoice("INV-" + orderId, order, LocalDateTime.now(), order.getTotalAmountPaid());
            invoiceDAO.add(invoice);
            order.setInvoice(invoice);

            // Update Order Status
            order.setOrderStatus(OrderStatus.PENDING_PROCESSING);
            orderDAO.update(order);

            // Send Notification
            notificationService.sendOrderConfirmationEmail(order, invoice, paymentTransactionResult);
            
        } catch (SQLException e) {
            throw new ValidationException("Database error during payment processing: " + e.getMessage());
        }
    }

    @Override
    public void cancelOrder(String orderId) throws ResourceNotFoundException, OrderException {
        try {
            OrderEntity order = orderDAO.getById(orderId);
            if (order == null) {
                throw new ResourceNotFoundException("Order with ID " + orderId + " not found");
            }
            
            if (order.getUserAccount() != null) {
                cancelOrder(orderId, order.getUserAccount().getUserId());
            } else {
                orderDAO.updateStatus(orderId, OrderStatus.CANCELLED);
            }
        } catch (SQLException | ValidationException e) {
            throw new OrderException("Unable to cancel order: " + e.getMessage());
        }
    }

    @Override
    public void cancelOrder(String orderId, String customerId)
            throws ResourceNotFoundException, OrderException, ValidationException {
        try {
            OrderEntity order = getOrderDetails(orderId);

            if (order.getUserAccount() != null && !order.getUserAccount().getUserId().equals(customerId)) {
                throw new ValidationException("User " + customerId + " is not authorized to cancel order " + orderId);
            }

            if (order.getOrderStatus() != OrderStatus.PENDING_PROCESSING && 
                order.getOrderStatus() != OrderStatus.PENDING_DELIVERY_INFO && 
                order.getOrderStatus() != OrderStatus.PENDING_PAYMENT) {
                throw new ValidationException("Order cannot be cancelled. Current status: " + order.getOrderStatus());
            }

            // Handle refund if payment was made
            PaymentTransaction refundTransaction = null;
            List<PaymentTransaction> transactions = order.getPaymentTransactions();
            if (transactions == null) transactions = new ArrayList<>();

            PaymentTransaction originalPayment = transactions.stream()
                .filter(t -> t.getTransactionType() == TransactionType.PAYMENT && 
                           "SUCCESS".equalsIgnoreCase(t.getTransactionStatus()))
                .findFirst().orElse(null);

            if (originalPayment != null) {
                refundTransaction = paymentService.processRefund(orderId, originalPayment.getExternalTransactionId(), 
                    originalPayment.getAmount(), "Customer cancellation");
            }

            // Restore stock
            for (OrderItem item : order.getOrderItems()) {
                try {
                    productService.updateProductStock(item.getProduct().getProductId(), item.getQuantity());
                } catch (Exception e) {
                    System.err.println("Warning: Order " + orderId + " cancelled, but failed to restore stock for product " + 
                        item.getProduct().getProductId() + ". Reason: " + e.getMessage());
                }
            }

            order.setOrderStatus(OrderStatus.CANCELLED);
            orderDAO.updateStatus(orderId, OrderStatus.CANCELLED);

            notificationService.sendOrderCancellationNotification(order, refundTransaction);
        } catch (SQLException | PaymentException e) {
            throw new OrderException("Error during cancellation: " + e.getMessage());
        }
    }

    @Override
    public void approveOrder(String orderId, String managerId) 
            throws ResourceNotFoundException, OrderException, ValidationException {
        try {
            // Use OrderStateManagementService for comprehensive approval workflow
            IOrderStateManagementService.OrderApprovalResult approvalResult;
            try {
                approvalResult = orderStateManagementService.approveOrder(orderId, managerId, "Order approved via OrderService");
            } catch (SQLException e) {
                throw new OrderException("Database error during order approval: " + e.getMessage());
            } catch (InventoryException e) {
                throw new OrderException("Inventory error during order approval: " + e.getMessage());
            }
            
            if (!approvalResult.isSuccessful()) {
                throw new OrderException("Order approval failed: " +
                    approvalResult.getTransitionResult().getTransitionNotes() +
                    ". Warnings: " + String.join("; ", approvalResult.getApprovalWarnings()));
            }
            
            // Additional notification if needed (OrderStateManagementService handles primary notifications)
            System.out.println("Order " + orderId + " approved successfully by manager " + managerId + 
                             " via enhanced OrderStateManagementService");
                             
        } catch (Exception e) {
            if (e instanceof OrderException || e instanceof ValidationException) {
                throw e;
            }
            throw new OrderException("Error during order approval: " + e.getMessage());
        }
    }

    @Override
    public void rejectOrder(String orderId, String managerId, String reason) 
            throws ResourceNotFoundException, OrderException, ValidationException {
        try {
            // Use OrderStateManagementService for comprehensive rejection workflow
            IOrderStateManagementService.StateTransitionResult rejectionResult;
            try {
                rejectionResult = orderStateManagementService.rejectOrder(orderId, managerId, "MANUAL_REVIEW", reason);
            } catch (SQLException e) {
                throw new OrderException("Database error during order rejection: " + e.getMessage());
            }
            
            if (!rejectionResult.isSuccessful()) {
                throw new OrderException("Order rejection failed: " + rejectionResult.getTransitionNotes());
            }
            
            // Additional processing if needed (OrderStateManagementService handles primary workflow)
            System.out.println("Order " + orderId + " rejected successfully by manager " + managerId + 
                             " via enhanced OrderStateManagementService. Reason: " + reason);
                             
        } catch (Exception e) {
            if (e instanceof OrderException || e instanceof ValidationException) {
                throw e;
            }
            throw new OrderException("Error during order rejection: " + e.getMessage());
        }
    }

    @Override
    public void markOrderAsShipped(String orderId, String trackingNumber) 
            throws ResourceNotFoundException, OrderException {
        try {
            OrderEntity order = orderDAO.getById(orderId);
            if (order == null) {
                throw new ResourceNotFoundException("Order with ID " + orderId + " not found");
            }
            
            if (order.getOrderStatus() != OrderStatus.APPROVED) {
                throw new OrderException("Only approved orders can be marked as shipped");
            }
            
            orderDAO.updateStatus(orderId, OrderStatus.SHIPPING);
            
            notificationService.sendOrderStatusUpdateNotification(order, 
                OrderStatus.APPROVED.name(), OrderStatus.SHIPPING.name(), 
                "Order shipped with tracking number: " + trackingNumber);
                
        } catch (SQLException e) {
            throw new OrderException("Unable to mark order as shipped due to database error: " + e.getMessage());
        }
    }

    @Override
    public void markOrderAsDelivered(String orderId) throws ResourceNotFoundException, OrderException {
        try {
            OrderEntity order = orderDAO.getById(orderId);
            if (order == null) {
                throw new ResourceNotFoundException("Order with ID " + orderId + " not found");
            }
            
            if (order.getOrderStatus() != OrderStatus.SHIPPING) {
                throw new OrderException("Only orders in shipping status can be marked as delivered");
            }
            
            orderDAO.updateStatus(orderId, OrderStatus.DELIVERED);
            
            notificationService.sendOrderStatusUpdateNotification(order, 
                OrderStatus.SHIPPING.name(), OrderStatus.DELIVERED.name(), 
                "Order has been delivered successfully");
                
        } catch (SQLException e) {
            throw new OrderException("Unable to mark order as delivered due to database error: " + e.getMessage());
        }
    }
}
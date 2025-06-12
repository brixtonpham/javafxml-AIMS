package com.aims.core.application.impl; // Or com.aims.core.application.services.impl;

import com.aims.core.application.services.IOrderService;
import com.aims.core.application.services.ICartService;
import com.aims.core.application.services.IDeliveryCalculationService;
import com.aims.core.application.services.INotificationService;
import com.aims.core.application.services.IPaymentService;
import com.aims.core.application.services.IProductService;
import com.aims.core.entities.*;
import com.aims.core.enums.OrderStatus;
import com.aims.core.enums.TransactionType;
import com.aims.core.infrastructure.database.dao.*;
import com.aims.core.shared.exceptions.*;
import com.aims.core.shared.utils.SearchResult;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Map; // For payment parameters

public class OrderServiceImpl implements IOrderService {

    private final IOrderEntityDAO orderDAO;
    private final IOrderItemDAO orderItemDAO;
    private final IDeliveryInfoDAO deliveryInfoDAO;
    private final IInvoiceDAO invoiceDAO;
    private final IProductDAO productDAO; // Direct use for stock, could also go via IProductService
    private final IProductService productService; // For more complex product operations if needed
    private final ICartService cartService;
    private final IPaymentService paymentService;
    private final IDeliveryCalculationService deliveryCalculationService;
    private final INotificationService notificationService;
    private final IUserAccountDAO userAccountDAO; // To fetch UserAccount for order

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
                            IUserAccountDAO userAccountDAO) {
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
    }

    @Override
    public OrderEntity initiateOrderFromCart(String cartSessionId, String userId)
            throws SQLException, ResourceNotFoundException, InventoryException, ValidationException {
        
        // CRITICAL FIX: Enhanced cart validation with detailed logging
        System.out.println("ORDER CREATION: Validating cart for session: " + cartSessionId);
        
        if (cartSessionId == null || cartSessionId.trim().isEmpty()) {
            System.err.println("ORDER CREATION ERROR: Cart session ID is null or empty");
            throw new ValidationException("Invalid cart session ID provided");
        }
        
        Cart cart = cartService.getCart(cartSessionId);
        if (cart == null) {
            System.err.println("ORDER CREATION ERROR: Cart is null for session: " + cartSessionId);
            throw new ResourceNotFoundException("Cart not found for session ID: " + cartSessionId);
        }
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            System.err.println("ORDER CREATION ERROR: Cart items are empty for session: " + cartSessionId);
            throw new ResourceNotFoundException("Cart is empty for session ID: " + cartSessionId);
        }
        
        System.out.println("ORDER CREATION: Cart validation passed - " + cart.getItems().size() + " items found");

        OrderEntity order = new OrderEntity();
        order.setOrderId("ORD-" + UUID.randomUUID().toString());
        order.setOrderDate(LocalDateTime.now());
        order.setOrderStatus(OrderStatus.PENDING_DELIVERY_INFO); // Initial status

        if (userId != null) {
            UserAccount user = userAccountDAO.getById(userId);
            if (user == null) throw new ResourceNotFoundException("User with ID " + userId + " not found.");
            order.setUserAccount(user);
        }

        float totalProductPriceExclVAT = 0f;
        List<OrderItem> orderItems = new ArrayList<>();

        for (CartItem cartItem : cart.getItems()) {
            Product product = productDAO.getById(cartItem.getProduct().getProductId());
            if (product == null) {
                throw new ResourceNotFoundException("Product " + cartItem.getProduct().getProductId() + " in cart not found in catalog.");
            }
            if (product.getQuantityInStock() < cartItem.getQuantity()) {
                throw new InventoryException("Insufficient stock for product: " + product.getTitle() +
                        ". Requested: " + cartItem.getQuantity() + ", Available: " + product.getQuantityInStock());
            }
            // Price from product entity is ex-VAT
            OrderItem orderItem = new OrderItem(order, product, cartItem.getQuantity(), product.getPrice(), product.getProductType() != com.aims.core.enums.ProductType.BOOK); // Example eligibility for rush
            orderItems.add(orderItem);
            totalProductPriceExclVAT += product.getPrice() * cartItem.getQuantity();
        }

        order.setTotalProductPriceExclVAT(totalProductPriceExclVAT);
        order.setTotalProductPriceInclVAT(totalProductPriceExclVAT * (1 + VAT_RATE));
        // Delivery fee and total amount paid will be calculated later

        // // START TRANSACTION (conceptually)
        try {
            orderDAO.add(order);
            for (OrderItem item : orderItems) {
                item.setOrderEntity(order); // Ensure OrderEntity is set before adding
                orderItemDAO.add(item);
            }
            order.setOrderItems(orderItems); // Set items in the order object
            // // COMMIT TRANSACTION (conceptually)
        } catch (SQLException e) {
            // // ROLLBACK TRANSACTION (conceptually)
            throw e;
        }
        return order;
    }

    @Override
    public OrderEntity setDeliveryInformation(String orderId, DeliveryInfo deliveryInfoInput, boolean isRushOrder)
            throws SQLException, ResourceNotFoundException, ValidationException {
        OrderEntity order = orderDAO.getById(orderId);
        if (order == null) {
            throw new ResourceNotFoundException("Order with ID " + orderId + " not found.");
        }
        if (order.getOrderStatus() != OrderStatus.PENDING_DELIVERY_INFO && order.getOrderStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new ValidationException("Delivery information can only be set for orders pending delivery info or pending payment.");
        }
        if (deliveryInfoInput == null) {
            throw new ValidationException("Delivery information cannot be null.");
        }

        // Validate deliveryInfoInput fields (e.g., non-empty recipientName, phone, address, province)
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
            // Check if any items in the order are eligible for rush
            boolean anyItemEligibleForRush = order.getOrderItems().stream().anyMatch(OrderItem::isEligibleForRushDelivery);
            if (!anyItemEligibleForRush) {
                throw new ValidationException("No items in the order are eligible for rush delivery.");
            }
            actualRushOrderApplicable = true;
            deliveryInfoInput.setDeliveryMethodChosen("RUSH_DELIVERY");
            // deliveryInfoInput.setRequestedRushDeliveryTime(...); // This should be set by customer input
        } else {
            deliveryInfoInput.setDeliveryMethodChosen("STANDARD");
        }


        // CRITICAL FIX: Set delivery info on order BEFORE calculating shipping fee
        // This prevents ValidationException "Delivery information is required for shipping calculation"
        deliveryInfoInput.setOrderEntity(order); // Link to order
        order.setDeliveryInfo(deliveryInfoInput); // Set delivery info FIRST
        
        // Now calculate shipping fee with delivery info available
        float shippingFee = deliveryCalculationService.calculateShippingFee(order, actualRushOrderApplicable);
        order.setCalculatedDeliveryFee(shippingFee);
        order.setTotalAmountPaid(order.getTotalProductPriceInclVAT() + shippingFee); // Recalculate total
        order.setOrderStatus(OrderStatus.PENDING_PAYMENT); // Move to next state

        // // START TRANSACTION
        try {
            DeliveryInfo existingDeliveryInfo = deliveryInfoDAO.getByOrderId(orderId);
            if (existingDeliveryInfo != null) {
                deliveryInfoInput.setDeliveryInfoId(existingDeliveryInfo.getDeliveryInfoId()); // Use existing ID for update
                deliveryInfoDAO.update(deliveryInfoInput);
            } else {
                deliveryInfoInput.setDeliveryInfoId("DINFO-" + UUID.randomUUID().toString());
                deliveryInfoDAO.add(deliveryInfoInput);
            }
            orderDAO.update(order); // Update order with new fees and status
            // // COMMIT TRANSACTION
        } catch (SQLException e) {
            // // ROLLBACK TRANSACTION
            throw e;
        }
        return order;
    }


    @Override
    public float calculateShippingFee(String orderId, DeliveryInfo deliveryInfo, boolean isRushOrder)
            throws SQLException, ResourceNotFoundException, ValidationException {
        OrderEntity order = orderDAO.getById(orderId); // Need items from order
        if (order == null) {
            throw new ResourceNotFoundException("Order with ID " + orderId + " not found for fee calculation.");
        }
        return deliveryCalculationService.calculateShippingFee(order, isRushOrder);
    }

    @Override
    public float calculateShippingFeePreview(List<OrderItem> items, DeliveryInfo deliveryInfo, boolean isRushOrder)
            throws ValidationException {
        // Validate input parameters
        if (items == null || items.isEmpty()) {
            throw new ValidationException("Order items are required for shipping calculation.");
        }
        if (deliveryInfo == null) {
            throw new ValidationException("Delivery information is required for shipping calculation.");
        }
        
        // Create temporary order for calculation without persisting
        OrderEntity tempOrder = new OrderEntity();
        tempOrder.setOrderItems(items);
        tempOrder.setDeliveryInfo(deliveryInfo);
        
        // Calculate using the delivery calculation service
        return deliveryCalculationService.calculateShippingFee(tempOrder, isRushOrder);
    }

    @Override
    public OrderEntity processPayment(String orderId, String paymentMethodId /*, PaymentDetailsDTO paymentDetails */)
            throws SQLException, ResourceNotFoundException, ValidationException, PaymentException, InventoryException {
        // // START TRANSACTION
        OrderEntity order = orderDAO.getById(orderId); // Re-fetch to ensure latest state
        if (order == null) {
            throw new ResourceNotFoundException("Order " + orderId + " not found.");
        }
        if (order.getOrderStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new ValidationException("Order is not pending payment. Current status: " + order.getOrderStatus());
        }
        if (order.getDeliveryInfo() == null) {
            throw new ValidationException("Delivery information must be set before payment.");
        }

        // Final inventory check
        for (OrderItem item : order.getOrderItems()) {
            Product product = productDAO.getById(item.getProduct().getProductId());
            if (product == null || product.getQuantityInStock() < item.getQuantity()) {
                order.setOrderStatus(OrderStatus.PENDING_DELIVERY_INFO); // Or a new "AWAITING_STOCK_UPDATE" status
                orderDAO.updateStatus(orderId, order.getOrderStatus());
                // // COMMIT (partial failure) or ROLLBACK
                throw new InventoryException("Stock changed for product " + (product != null ? product.getTitle() : item.getProduct().getProductId()) + ". Please review your cart/order.");
            }
        }

        PaymentTransaction paymentTransactionResult;
        try {
            // paymentParams would be constructed here based on paymentMethodId and order details
             Map<String, Object> paymentParams = Map.of(
                "ipAddress", "127.0.0.1" // Placeholder, get actual client IP
                // Potentially add card details or bank codes if not using saved methods or if required
             );
            paymentTransactionResult = paymentService.processPayment(order, paymentMethodId /*, paymentDetails */);

        } catch (PaymentException e) {
            order.setOrderStatus(OrderStatus.PAYMENT_FAILED);
            orderDAO.updateStatus(orderId, OrderStatus.PAYMENT_FAILED);
            // // COMMIT (to save failed status)
            throw e; // Re-throw to inform caller
        }

        // If payment successful:
        // 1. Update product stock
        for (OrderItem item : order.getOrderItems()) {
            try {
                // Using productService for stock update which might have additional logic
                productService.updateProductStock(item.getProduct().getProductId(), -item.getQuantity());
            } catch (ValidationException | ResourceNotFoundException | InventoryException e) {
                // For now, log and potentially set order to a special error state.
                System.err.println("CRITICAL: Payment succeeded for order " + orderId + " but failed to update stock for product " + item.getProduct().getProductId() + ". Reason: " + e.getMessage());
                order.setOrderStatus(OrderStatus.PAYMENT_FAILED); // Or a more specific error status like ERROR_STOCK_UPDATE_FAILED
                orderDAO.updateStatus(orderId, OrderStatus.PAYMENT_FAILED); // Or a more specific error status like ERROR_STOCK_UPDATE_FAILED
                // // COMMIT
                throw new InventoryException("Payment successful, but stock update failed. Please contact support.", e);
            }
        }

        // 2. Create Invoice
        Invoice invoice = new Invoice("INV-" + orderId, order, LocalDateTime.now(), order.getTotalAmountPaid());
        invoiceDAO.add(invoice);
        order.setInvoice(invoice);

        // 3. Update Order Status
        order.setOrderStatus(OrderStatus.PENDING_PROCESSING); // Or directly to APPROVED if no further manager review for simple orders
        orderDAO.update(order); // Update the full order with new status and invoice link

        // 4. Clear Cart (if associated with a session from which order was placed)
        // This requires knowing the cartSessionId. Assuming it might be passed or inferred.
        // For now, this step is conceptual as cartSessionId is not directly on OrderEntity.
        // If order was created from a cart, that cartSessionId should be used.
        // String cartSessionId = ... ;
        // if(cartSessionId != null) cartService.clearCart(cartSessionId);

        // 5. Send Notification
        notificationService.sendOrderConfirmationEmail(order, invoice, paymentTransactionResult);

        // // COMMIT TRANSACTION
        return order;
    }


    @Override
    public OrderEntity getOrderDetails(String orderId) throws SQLException, ResourceNotFoundException {
        OrderEntity order = orderDAO.getById(orderId); // DAO should load items, deliveryInfo, invoice
        if (order == null) {
            throw new ResourceNotFoundException("Order " + orderId + " not found.");
        }
        // If DAO doesn't load everything, load them here:
        if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
             order.setOrderItems(orderItemDAO.getItemsByOrderId(orderId));
        }
        if (order.getDeliveryInfo() == null) {
            order.setDeliveryInfo(deliveryInfoDAO.getByOrderId(orderId));
        }
        if (order.getInvoice() == null) {
            order.setInvoice(invoiceDAO.getByOrderId(orderId));
        }
        // Load payment transactions too if needed for display
        // order.setPaymentTransactions(paymentTransactionDAO.getByOrderId(orderId));
        return order;
    }

    @Override
    public List<OrderEntity> getOrdersByUserId(String userId) throws SQLException {
        return orderDAO.getByUserId(userId);
    }

    @Override
    public OrderEntity cancelOrder(String orderId, String customerId)
            throws SQLException, ResourceNotFoundException, ValidationException, PaymentException {
        // // START TRANSACTION
        OrderEntity order = getOrderDetails(orderId); // Get full order details

        if (order.getUserAccount() != null && !order.getUserAccount().getUserId().equals(customerId)) {
            // Or if it's a guest order, how to authorize cancellation? (e.g. via a unique link in email)
            // For now, only allow registered users to cancel their own orders.
             throw new ValidationException("User " + customerId + " is not authorized to cancel order " + orderId);
        }

        if (order.getOrderStatus() != OrderStatus.PENDING_PROCESSING && order.getOrderStatus() != OrderStatus.PENDING_DELIVERY_INFO && order.getOrderStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new ValidationException("Order cannot be cancelled. Current status: " + order.getOrderStatus());
        }

        PaymentTransaction refundTransaction = null;
        // Check if there was a successful payment to refund
        List<PaymentTransaction> transactions = order.getPaymentTransactions(); // Assume getOrderDetails loads this
        if (transactions == null) transactions = new ArrayList<>(); // Or fetch IPaymentTransactionDAO().getByOrderId(orderId);

        PaymentTransaction originalPayment = transactions.stream()
            .filter(t -> t.getTransactionType() == TransactionType.PAYMENT && "SUCCESS".equalsIgnoreCase(t.getTransactionStatus())) // Assuming "SUCCESS" status
            .findFirst().orElse(null);

        if (originalPayment != null) {
            refundTransaction = paymentService.processRefund(orderId, originalPayment.getExternalTransactionId(), originalPayment.getAmount(), "Customer cancellation");
        }

        // Restore stock
        for (OrderItem item : order.getOrderItems()) {
            try {
                productService.updateProductStock(item.getProduct().getProductId(), item.getQuantity()); // Add back
            } catch (ValidationException | ResourceNotFoundException | InventoryException e) {
                // Log this issue, as refund might have been processed
                System.err.println("Warning: Order " + orderId + " cancelled, but failed to restore stock for product " + item.getProduct().getProductId() + ". Reason: " + e.getMessage());
            }
        }

        order.setOrderStatus(OrderStatus.CANCELLED);
        orderDAO.updateStatus(orderId, OrderStatus.CANCELLED);

        notificationService.sendOrderCancellationNotification(order, refundTransaction);
        // // COMMIT TRANSACTION
        return order;
    }

    @Override
    public SearchResult<OrderEntity> getOrdersByStatusForManager(OrderStatus status, int pageNumber, int pageSize) throws SQLException {
        // DAO should ideally support pagination. For now, fetch all then paginate in memory.
        List<OrderEntity> ordersByStatus = orderDAO.getByStatus(status); // TODO: Add pagination to DAO method

        int totalResults = ordersByStatus.size();
        int fromIndex = (pageNumber - 1) * pageSize;
        if (fromIndex >= totalResults && totalResults > 0) { // If fromIndex is out of bounds but there are results, return last page
             fromIndex = Math.max(0, totalResults - pageSize); // adjust to last page
        } else if (fromIndex >= totalResults && totalResults == 0) {
             return new SearchResult<OrderEntity>(new ArrayList<>(), pageNumber, 0, 0);
        }


        int toIndex = Math.min(fromIndex + pageSize, totalResults);
        List<OrderEntity> pageResults = ordersByStatus.subList(fromIndex, toIndex);
        int totalPages = (int) Math.ceil((double) totalResults / pageSize);
        if (totalPages == 0 && totalResults > 0) totalPages = 1;

        return new SearchResult<OrderEntity>(pageResults, pageNumber, totalPages, totalResults);
    }

    @Override
    public OrderEntity approveOrder(String orderId, String managerId)
            throws SQLException, ResourceNotFoundException, ValidationException, InventoryException {
        // // START TRANSACTION
        OrderEntity order = getOrderDetails(orderId);
        if (order.getOrderStatus() != OrderStatus.PENDING_PROCESSING) {
            throw new ValidationException("Only orders pending processing can be approved. Current status: " + order.getOrderStatus());
        }

        // Final inventory check by manager (problem statement implies this)
        for (OrderItem item : order.getOrderItems()) {
            Product product = productDAO.getById(item.getProduct().getProductId());
            if (product == null || product.getQuantityInStock() < item.getQuantity()) {
                // Manager might reject or put on hold, but cannot approve if no stock.
                // For simplicity, throw inventory exception, or allow rejection.
                order.setOrderStatus(OrderStatus.REJECTED); // Or a specific on-hold status
                orderDAO.updateStatus(orderId, order.getOrderStatus());
                // // COMMIT
                throw new InventoryException("Cannot approve order " + orderId + ". Insufficient stock for product: " +
                        (product != null ? product.getTitle() : item.getProduct().getProductId()));
            }
        }

        order.setOrderStatus(OrderStatus.APPROVED);
        orderDAO.updateStatus(orderId, OrderStatus.APPROVED);
        notificationService.sendOrderStatusUpdateNotification(order, OrderStatus.PENDING_PROCESSING.name(), OrderStatus.APPROVED.name(), "Order approved by manager " + managerId);
        // // COMMIT TRANSACTION
        return order;
    }

    @Override
    public OrderEntity rejectOrder(String orderId, String managerId, String reason)
            throws SQLException, ResourceNotFoundException, ValidationException, PaymentException {
        // // START TRANSACTION
        OrderEntity order = getOrderDetails(orderId);
        if (order.getOrderStatus() != OrderStatus.PENDING_PROCESSING) {
            throw new ValidationException("Only orders pending processing can be rejected. Current status: " + order.getOrderStatus());
        }

        // If order was paid, a refund might be needed.
        PaymentTransaction refundTransaction = null;
        List<PaymentTransaction> transactions = order.getPaymentTransactions(); // Assume getOrderDetails loads this
         if (transactions == null) transactions = new ArrayList<>();

        PaymentTransaction originalPayment = transactions.stream()
            .filter(t -> t.getTransactionType() == TransactionType.PAYMENT && "SUCCESS".equalsIgnoreCase(t.getTransactionStatus()))
            .findFirst().orElse(null);

        if (originalPayment != null) {
            // paymentService.processRefund might throw PaymentException
            refundTransaction = paymentService.processRefund(orderId, originalPayment.getExternalTransactionId(), originalPayment.getAmount(), "Order rejected by manager: " + reason);
        }

        // If a refund was attempted (even if it failed), or if no payment was made, proceed to reject.
        // If refund failed, the order might go into an error state or still be rejected with a note.
        // For simplicity, we assume rejection proceeds and any refund failure is handled by PaymentException.

        // Restore stock for rejected orders IF it was decremented at payment
        // Stock was decremented in processPayment if successful.
        if (originalPayment != null) { // Implying payment was made and stock likely decremented
             for (OrderItem item : order.getOrderItems()) {
                try {
                    productService.updateProductStock(item.getProduct().getProductId(), item.getQuantity()); // Add back
                } catch (Exception e) { // Catch general exception here as this is a recovery step
                    System.err.println("Warning: Order " + orderId + " rejected, but failed to restore stock for product " + item.getProduct().getProductId() + ". Reason: " + e.getMessage());
                }
            }
        }


        order.setOrderStatus(OrderStatus.REJECTED);
        orderDAO.updateStatus(orderId, OrderStatus.REJECTED);
        notificationService.sendOrderStatusUpdateNotification(order, OrderStatus.PENDING_PROCESSING.name(), OrderStatus.REJECTED.name(), "Order rejected by manager " + managerId + ". Reason: " + reason);
        // // COMMIT TRANSACTION
        return order;
    }

    @Override
    public OrderEntity updateOrderStatus(String orderId, OrderStatus newStatus, String adminOrManagerId)
            throws SQLException, ResourceNotFoundException, ValidationException {
        // // START TRANSACTION
        OrderEntity order = getOrderDetails(orderId);
        OrderStatus oldStatus = order.getOrderStatus();

        // Add validation logic for allowed status transitions if necessary
        // e.g., cannot go from DELIVERED back to PENDING_PAYMENT
        if (oldStatus == newStatus) return order; // No change

        order.setOrderStatus(newStatus);
        orderDAO.updateStatus(orderId, newStatus);
        notificationService.sendOrderStatusUpdateNotification(order, oldStatus.name(), newStatus.name(), "Status updated by " + adminOrManagerId);
        // // COMMIT TRANSACTION
        return order;
    }
}
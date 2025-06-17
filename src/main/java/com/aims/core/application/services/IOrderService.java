package com.aims.core.application.services;

import com.aims.core.entities.DeliveryInfo;
import com.aims.core.entities.OrderEntity;
import com.aims.core.entities.OrderItem;
import com.aims.core.enums.OrderStatus;
import com.aims.core.shared.dto.SearchResult;
import com.aims.core.shared.exceptions.OrderException;
import com.aims.core.shared.exceptions.ValidationException;
import com.aims.core.shared.exceptions.ResourceNotFoundException;
import com.aims.core.shared.exceptions.PaymentException;

import java.util.List;

public interface IOrderService {
    /**
     * Create a new order from the current cart
     */
    OrderEntity createOrder(String userId) throws ValidationException;
    
    /**
     * Initialize an order from a cart session
     */
    OrderEntity initiateOrderFromCart(String cartSessionId, String userId) throws ValidationException;
    
    /**
     * Enhanced cart-to-order conversion with complete data preservation and comprehensive validation.
     * This method ensures all product metadata is preserved during the cart-to-order transition.
     *
     * @param cartSessionId The session ID of the cart from which to create the order
     * @param userId The ID of the user placing the order (can be null for guest checkouts)
     * @return An OrderEntity with complete product metadata and enhanced validation
     * @throws ValidationException If cart validation, data integrity, or conversion fails
     */
    OrderEntity initiateOrderFromCartEnhanced(String cartSessionId, String userId) throws ValidationException;
    
    /**
     * Get an order by its ID
     */
    OrderEntity getOrderById(String orderId) throws ResourceNotFoundException;
    
    /**
     * Get detailed order information
     */
    OrderEntity getOrderDetails(String orderId) throws ResourceNotFoundException;
    
    /**
     * Get orders for a user
     */
    List<OrderEntity> getOrdersByUserId(String userId) throws ResourceNotFoundException;
    
    /**
     * Get orders by status
     */
    List<OrderEntity> getOrdersByStatus(OrderStatus status);
    
    /**
     * Get paginated orders by status for manager review
     */
    SearchResult<OrderEntity> getOrdersByStatusForManager(OrderStatus status, int pageNumber, int pageSize) 
        throws ResourceNotFoundException;
    
    /**
     * Update order status
     */
    void updateOrderStatus(String orderId, OrderStatus newStatus) 
        throws ResourceNotFoundException, OrderException;

    /**
     * Update order status with manager/admin action
     */
    void updateOrderStatus(String orderId, OrderStatus newStatus, String adminOrManagerId)
        throws ResourceNotFoundException, OrderException, ValidationException;
    
    /**
     * Add or update delivery information
     */
    void addDeliveryInfo(String orderId, DeliveryInfo deliveryInfo) 
        throws ResourceNotFoundException, ValidationException;
    
    /**
     * Set delivery information with rush order option
     */
    OrderEntity setDeliveryInformation(String orderId, DeliveryInfo deliveryInfo, boolean isRushOrder)
        throws ResourceNotFoundException, ValidationException;
    
    /**
     * Calculate shipping fee for order
     */
    float calculateShippingFee(String orderId, DeliveryInfo deliveryInfo, boolean isRushOrder)
        throws ResourceNotFoundException;
    
    /**
     * Preview shipping fee for cart items
     */
    float calculateShippingFeePreview(List<OrderItem> items, DeliveryInfo deliveryInfo, boolean isRushOrder)
        throws ValidationException;
    
    /**
     * Calculate total amount for an order
     */
    float calculateOrderTotal(String orderId) throws ResourceNotFoundException;
    
    /**
     * Process order payment
     */
    void processOrderPayment(String orderId, String paymentMethodId) 
        throws ResourceNotFoundException, PaymentException, ValidationException;
    
    /**
     * Cancel an order
     */
    void cancelOrder(String orderId) 
        throws ResourceNotFoundException, OrderException;
    
    /**
     * Cancel an order with customer verification
     */
    void cancelOrder(String orderId, String customerId)
        throws ResourceNotFoundException, OrderException, ValidationException;
    
    /**
     * Approve an order (by product manager)
     */
    void approveOrder(String orderId, String managerId) 
        throws ResourceNotFoundException, OrderException, ValidationException;
    
    /**
     * Reject an order (by product manager)
     */
    void rejectOrder(String orderId, String managerId, String reason) 
        throws ResourceNotFoundException, OrderException, ValidationException;
    
    /**
     * Mark order as shipped
     */
    void markOrderAsShipped(String orderId, String trackingNumber) 
        throws ResourceNotFoundException, OrderException;
    
    /**
     * Mark order as delivered
     */
    void markOrderAsDelivered(String orderId) 
        throws ResourceNotFoundException, OrderException;
}
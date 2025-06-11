package com.aims.core.application.services;

import com.aims.core.entities.OrderEntity;
import com.aims.core.entities.OrderItem;
import com.aims.core.entities.DeliveryInfo; // Assuming direct use, or DTOs
import com.aims.core.entities.PaymentTransaction; // For returning payment result details
import com.aims.core.enums.OrderStatus;
import com.aims.core.shared.exceptions.ValidationException;
import com.aims.core.shared.exceptions.ResourceNotFoundException;
import com.aims.core.shared.exceptions.PaymentException; // For payment related failures
import com.aims.core.shared.exceptions.InventoryException; // For stock issues
import com.aims.core.shared.utils.SearchResult;
// import com.aims.core.dtos.OrderPlacementRequestDTO; // DTO for initial order request
// import com.aims.core.dtos.OrderDetailsDTO; // DTO for returning comprehensive order details
// import com.aims.core.dtos.DeliveryInfoDTO; // DTO for delivery information input
// import com.aims.core.dtos.PaymentDetailsDTO; // DTO for payment input (card info etc.)

import java.sql.SQLException;
import java.util.List;

/**
 * Service interface for orchestrating the order processing lifecycle.
 * This includes validating orders, calculating fees, processing payments,
 * updating statuses, and managing order fulfillment and cancellation.
 */
public interface IOrderService {

    /**
     * Initiates the order placement process from a cart.
     * Checks inventory for all items in the cart.
     *
     * @param cartSessionId The session ID of the cart from which to create the order.
     * @param userId The ID of the user placing the order (can be null for guest checkouts).
     * @return An OrderEntity in a pending state, with initial calculations.
     * @throws SQLException If a database error occurs.
     * @throws ResourceNotFoundException If the cart is not found or is empty.
     * @throws InventoryException If any cart item is out of stock or has insufficient quantity.
     * @throws ValidationException If other cart/user validations fail.
     */
    OrderEntity initiateOrderFromCart(String cartSessionId, String userId) throws SQLException, ResourceNotFoundException, InventoryException, ValidationException;

    /**
     * Sets or updates the delivery information for a pending order.
     * Recalculates shipping fees based on the new information.
     * Validates delivery address and rush order eligibility if requested.
     *
     * @param orderId The ID of the pending order.
     * @param deliveryInfo The delivery information to set/update. (Consider using a DTO)
     * @param isRushOrder If true, attempts to apply rush order logic.
     * @return The updated OrderEntity with new delivery info and potentially recalculated fees.
     * @throws SQLException If a database error occurs.
     * @throws ResourceNotFoundException If the order is not found.
     * @throws ValidationException If delivery information is invalid, or rush order is not possible.
     */
    OrderEntity setDeliveryInformation(String orderId, DeliveryInfo deliveryInfo, boolean isRushOrder) throws SQLException, ResourceNotFoundException, ValidationException;

    /**
     * Calculates shipping fees for an order.
     * This might be called internally by setDeliveryInformation or explicitly.
     *
     * @param orderId The ID of the order.
     * @param deliveryInfo The delivery information (needed for calculation).
     * @param isRushOrder Whether rush order fees should be considered.
     * @return The calculated shipping fee.
     * @throws SQLException If a database error occurs.
     * @throws ResourceNotFoundException If the order is not found.
     * @throws ValidationException If data required for calculation is missing or invalid.
     */
    float calculateShippingFee(String orderId, DeliveryInfo deliveryInfo, boolean isRushOrder) throws SQLException, ResourceNotFoundException, ValidationException;

    /**
     * Calculates shipping fee for preview without modifying order state.
     * Used for real-time calculation in delivery info screen.
     *
     * @param items The list of order items to calculate shipping for.
     * @param deliveryInfo The delivery information for calculation.
     * @param isRushOrder Whether rush order fees should be considered.
     * @return The calculated shipping fee.
     * @throws ValidationException If data required for calculation is missing or invalid.
     */
    float calculateShippingFeePreview(List<OrderItem> items, DeliveryInfo deliveryInfo, boolean isRushOrder)
        throws ValidationException;

    /**
     * Processes the payment for a confirmed order.
     * Interacts with IPaymentService. Updates order status upon successful payment.
     * Creates an invoice and records the payment transaction.
     * Clears the cart after successful payment.
     *
     * @param orderId The ID of the order to pay for.
     * @param paymentMethodId The ID of the chosen payment method (e.g., saved card).
     * @param paymentDetails DTO containing transient payment details like CVV if not saved, or for VNPay interaction.
     * (The exact nature of paymentDetails will depend on IPaymentService and VNPay adapter)
     * @return The updated OrderEntity with status PAID and associated payment transaction info.
     * @throws SQLException If a database error occurs.
     * @throws ResourceNotFoundException If the order or payment method is not found.
     * @throws ValidationException If order is not in a payable state or payment details are invalid.
     * @throws PaymentException If the payment processing itself fails.
     * @throws InventoryException If stock becomes unavailable during final payment attempt.
     */
    OrderEntity processPayment(String orderId, String paymentMethodId /*, PaymentDetailsDTO paymentDetails */) throws SQLException, ResourceNotFoundException, ValidationException, PaymentException, InventoryException;

    /**
     * Retrieves detailed information for a specific order.
     *
     * @param orderId The ID of the order.
     * @return An OrderDetailsDTO or the OrderEntity with all associations loaded.
     * @throws SQLException If a database error occurs.
     * @throws ResourceNotFoundException If the order is not found.
     */
    OrderEntity getOrderDetails(String orderId) throws SQLException, ResourceNotFoundException;

    /**
     * Retrieves all orders placed by a specific user.
     *
     * @param userId The ID of the user.
     * @return A list of OrderEntity objects.
     * @throws SQLException If a database error occurs.
     */
    List<OrderEntity> getOrdersByUserId(String userId) throws SQLException;

    /**
     * Allows a customer to cancel an order if it's in a cancellable state (e.g., PENDING_PROCESSING).
     * Coordinates with IPaymentService for refunds if applicable.
     *
     * @param orderId The ID of the order to cancel.
     * @param customerId The ID of the customer requesting cancellation (for authorization).
     * @return The updated OrderEntity with status CANCELLED.
     * @throws SQLException If a database error occurs.
     * @throws ResourceNotFoundException If the order is not found.
     * @throws ValidationException If the order is not in a cancellable state or user is not authorized.
     * @throws PaymentException If refund processing fails.
     */
    OrderEntity cancelOrder(String orderId, String customerId) throws SQLException, ResourceNotFoundException, ValidationException, PaymentException;

    // --- Product Manager Order Management ---

    /**
     * Retrieves a paginated list of orders with a specific status for product managers to review.
     *
     * @param status The status of orders to retrieve (e.g., PENDING_PROCESSING).
     * @param pageNumber The page number (1-indexed).
     * @param pageSize The number of orders per page (e.g., 30).
     * @return A SearchResult of OrderEntity objects.
     * @throws SQLException If a database error occurs.
     */
    SearchResult<OrderEntity> getOrdersByStatusForManager(OrderStatus status, int pageNumber, int pageSize) throws SQLException;

    /**
     * Allows a product manager to approve a pending order.
     * Updates order status and may trigger further fulfillment processes.
     *
     * @param orderId The ID of the order to approve.
     * @param managerId The ID of the product manager performing the action.
     * @return The updated OrderEntity with status APPROVED.
     * @throws SQLException If a database error occurs.
     * @throws ResourceNotFoundException If the order is not found.
     * @throws ValidationException If the order is not in a state that can be approved.
     * @throws InventoryException If, upon final check, stock is insufficient.
     */
    OrderEntity approveOrder(String orderId, String managerId) throws SQLException, ResourceNotFoundException, ValidationException, InventoryException;

    /**
     * Allows a product manager to reject a pending order.
     * Updates order status. May coordinate with IPaymentService for refunds if payment was pre-authorized.
     *
     * @param orderId The ID of the order to reject.
     * @param managerId The ID of the product manager performing the action.
     * @param reason The reason for rejection.
     * @return The updated OrderEntity with status REJECTED.
     * @throws SQLException If a database error occurs.
     * @throws ResourceNotFoundException If the order is not found.
     * @throws ValidationException If the order is not in a state that can be rejected.
     * @throws PaymentException If an associated refund fails.
     */
    OrderEntity rejectOrder(String orderId, String managerId, String reason) throws SQLException, ResourceNotFoundException, ValidationException, PaymentException;

    /**
     * Updates the status of an order, e.g., to SHIPPING, DELIVERED.
     * Typically used by internal system processes or product managers.
     *
     * @param orderId The ID of the order.
     * @param newStatus The new status for the order.
     * @param adminOrManagerId ID of the user performing the update (for logging/authorization).
     * @return The updated OrderEntity.
     * @throws SQLException If a database error occurs.
     * @throws ResourceNotFoundException If the order is not found.
     * @throws ValidationException If the status transition is invalid.
     */
    OrderEntity updateOrderStatus(String orderId, OrderStatus newStatus, String adminOrManagerId) throws SQLException, ResourceNotFoundException, ValidationException;
}
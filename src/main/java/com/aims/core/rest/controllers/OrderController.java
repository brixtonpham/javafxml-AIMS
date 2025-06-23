package com.aims.core.rest.controllers;

import com.aims.core.application.services.IOrderService;
import com.aims.core.entities.OrderEntity;
import com.aims.core.entities.DeliveryInfo;
import com.aims.core.enums.OrderStatus;
import com.aims.core.shared.ServiceFactory;
import com.aims.core.shared.dto.SearchResult;
import com.aims.core.shared.exceptions.ResourceNotFoundException;
import com.aims.core.shared.exceptions.ValidationException;
import com.aims.core.shared.exceptions.OrderException;
import com.aims.core.shared.exceptions.PaymentException;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * REST controller for order management operations
 * CORS configuration is handled globally by CorsConfig.java
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController extends BaseController {

    private final IOrderService orderService;

    public OrderController() {
        this.orderService = ServiceFactory.getOrderService();
    }

    /**
     * Create order from cart
     */
    @PostMapping("/from-cart")
    public ResponseEntity<ApiResponse<OrderEntity>> createOrderFromCart(@RequestBody CreateOrderRequest request) {
        try {
            OrderEntity order = orderService.initiateOrderFromCartEnhanced(request.getCartSessionId(), request.getUserId());
            return success(order, "Order created successfully from cart");
        } catch (ValidationException e) {
            Map<String, String> errors = new HashMap<>();
            errors.put("validation", e.getMessage());
            return validationError("Order creation failed", errors);
        } catch (Exception e) {
            return error("An error occurred while creating order: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get order by ID
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderEntity>> getOrderById(@PathVariable String orderId) {
        try {
            OrderEntity order = orderService.getOrderDetails(orderId);
            return success(order, "Order retrieved successfully");
        } catch (ResourceNotFoundException e) {
            return error("Order not found: " + e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return error("An error occurred while retrieving order: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get orders for a user
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<OrderEntity>>> getOrdersByUserId(@PathVariable String userId) {
        try {
            List<OrderEntity> orders = orderService.getOrdersByUserId(userId);
            return success(orders, "Orders retrieved successfully");
        } catch (ResourceNotFoundException e) {
            return error("User or orders not found: " + e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return error("An error occurred while retrieving orders: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get orders by status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<OrderEntity>>> getOrdersByStatus(@PathVariable String status) {
        try {
            OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
            List<OrderEntity> orders = orderService.getOrdersByStatus(orderStatus);
            return success(orders, "Orders retrieved successfully");
        } catch (IllegalArgumentException e) {
            return error("Invalid order status: " + status, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return error("An error occurred while retrieving orders: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get paginated orders by status for manager review
     */
    @GetMapping("/manager/status/{status}")
    public ResponseEntity<PaginatedApiResponse<OrderEntity>> getOrdersByStatusForManager(
            @PathVariable String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
            SearchResult<OrderEntity> result = orderService.getOrdersByStatusForManager(orderStatus, page, limit);
            return paginatedSuccess(result.getItems(), page, limit, result.getTotalItems());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Update order status
     */
    @PutMapping("/{orderId}/status")
    public ResponseEntity<ApiResponse<String>> updateOrderStatus(
            @PathVariable String orderId,
            @RequestBody UpdateOrderStatusRequest request) {
        try {
            OrderStatus newStatus = OrderStatus.valueOf(request.getStatus().toUpperCase());
            if (request.getAdminOrManagerId() != null) {
                orderService.updateOrderStatus(orderId, newStatus, request.getAdminOrManagerId());
            } else {
                orderService.updateOrderStatus(orderId, newStatus);
            }
            return success("Status updated", "Order status updated successfully");
        } catch (IllegalArgumentException e) {
            return error("Invalid order status: " + request.getStatus(), HttpStatus.BAD_REQUEST);
        } catch (ResourceNotFoundException e) {
            return error("Order not found: " + e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (OrderException e) {
            return error("Order operation failed: " + e.getMessage(), HttpStatus.CONFLICT);
        } catch (ValidationException e) {
            Map<String, String> errors = new HashMap<>();
            errors.put("validation", e.getMessage());
            return validationError("Status update failed", errors);
        } catch (Exception e) {
            return error("An error occurred while updating order status: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Add delivery information to order
     */
    @PostMapping("/{orderId}/delivery")
    public ResponseEntity<ApiResponse<OrderEntity>> addDeliveryInfo(
            @PathVariable String orderId,
            @RequestBody AddDeliveryInfoRequest request) {
        try {
            OrderEntity order = orderService.setDeliveryInformation(orderId, request.getDeliveryInfo(), request.isRushOrder());
            return success(order, "Delivery information added successfully");
        } catch (ResourceNotFoundException e) {
            return error("Order not found: " + e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (ValidationException e) {
            Map<String, String> errors = new HashMap<>();
            errors.put("validation", e.getMessage());
            return validationError("Delivery information validation failed", errors);
        } catch (Exception e) {
            return error("An error occurred while adding delivery information: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Calculate shipping fee for order
     */
    @PostMapping("/{orderId}/shipping-fee")
    public ResponseEntity<ApiResponse<ShippingFeeResponse>> calculateShippingFee(
            @PathVariable String orderId,
            @RequestBody CalculateShippingRequest request) {
        try {
            float shippingFee = orderService.calculateShippingFee(orderId, request.getDeliveryInfo(), request.isRushOrder());
            ShippingFeeResponse response = new ShippingFeeResponse();
            response.setShippingFee(shippingFee);
            return success(response, "Shipping fee calculated successfully");
        } catch (ResourceNotFoundException e) {
            return error("Order not found: " + e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return error("An error occurred while calculating shipping fee: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Calculate order total
     */
    @GetMapping("/{orderId}/total")
    public ResponseEntity<ApiResponse<OrderTotalResponse>> calculateOrderTotal(@PathVariable String orderId) {
        try {
            float total = orderService.calculateOrderTotal(orderId);
            OrderTotalResponse response = new OrderTotalResponse();
            response.setTotal(total);
            return success(response, "Order total calculated successfully");
        } catch (ResourceNotFoundException e) {
            return error("Order not found: " + e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return error("An error occurred while calculating order total: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Process order payment
     */
    @PostMapping("/{orderId}/payment")
    public ResponseEntity<ApiResponse<String>> processOrderPayment(
            @PathVariable String orderId,
            @RequestBody ProcessPaymentRequest request) {
        try {
            orderService.processOrderPayment(orderId, request.getPaymentMethodId());
            return success("Payment processed", "Order payment processed successfully");
        } catch (ResourceNotFoundException e) {
            return error("Order or payment method not found: " + e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (PaymentException e) {
            return error("Payment processing failed: " + e.getMessage(), HttpStatus.PAYMENT_REQUIRED);
        } catch (ValidationException e) {
            Map<String, String> errors = new HashMap<>();
            errors.put("validation", e.getMessage());
            return validationError("Payment validation failed", errors);
        } catch (Exception e) {
            return error("An error occurred while processing payment: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Cancel order
     */
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<String>> cancelOrder(
            @PathVariable String orderId,
            @RequestBody(required = false) CancelOrderRequest request) {
        try {
            if (request != null && request.getCustomerId() != null) {
                orderService.cancelOrder(orderId, request.getCustomerId());
            } else {
                orderService.cancelOrder(orderId);
            }
            return success("Order cancelled", "Order cancelled successfully");
        } catch (ResourceNotFoundException e) {
            return error("Order not found: " + e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (OrderException e) {
            return error("Order cancellation failed: " + e.getMessage(), HttpStatus.CONFLICT);
        } catch (ValidationException e) {
            Map<String, String> errors = new HashMap<>();
            errors.put("validation", e.getMessage());
            return validationError("Order cancellation validation failed", errors);
        } catch (Exception e) {
            return error("An error occurred while cancelling order: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Approve order (by product manager)
     */
    @PostMapping("/{orderId}/approve")
    public ResponseEntity<ApiResponse<String>> approveOrder(
            @PathVariable String orderId,
            @RequestBody ApproveOrderRequest request) {
        try {
            orderService.approveOrder(orderId, request.getManagerId());
            return success("Order approved", "Order approved successfully");
        } catch (ResourceNotFoundException e) {
            return error("Order not found: " + e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (OrderException e) {
            return error("Order approval failed: " + e.getMessage(), HttpStatus.CONFLICT);
        } catch (ValidationException e) {
            Map<String, String> errors = new HashMap<>();
            errors.put("validation", e.getMessage());
            return validationError("Order approval validation failed", errors);
        } catch (Exception e) {
            return error("An error occurred while approving order: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Reject order (by product manager)
     */
    @PostMapping("/{orderId}/reject")
    public ResponseEntity<ApiResponse<String>> rejectOrder(
            @PathVariable String orderId,
            @RequestBody RejectOrderRequest request) {
        try {
            orderService.rejectOrder(orderId, request.getManagerId(), request.getReason());
            return success("Order rejected", "Order rejected successfully");
        } catch (ResourceNotFoundException e) {
            return error("Order not found: " + e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (OrderException e) {
            return error("Order rejection failed: " + e.getMessage(), HttpStatus.CONFLICT);
        } catch (ValidationException e) {
            Map<String, String> errors = new HashMap<>();
            errors.put("validation", e.getMessage());
            return validationError("Order rejection validation failed", errors);
        } catch (Exception e) {
            return error("An error occurred while rejecting order: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Mark order as shipped
     */
    @PostMapping("/{orderId}/ship")
    public ResponseEntity<ApiResponse<String>> markOrderAsShipped(
            @PathVariable String orderId,
            @RequestBody ShipOrderRequest request) {
        try {
            orderService.markOrderAsShipped(orderId, request.getTrackingNumber());
            return success("Order shipped", "Order marked as shipped successfully");
        } catch (ResourceNotFoundException e) {
            return error("Order not found: " + e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (OrderException e) {
            return error("Order shipping failed: " + e.getMessage(), HttpStatus.CONFLICT);
        } catch (Exception e) {
            return error("An error occurred while marking order as shipped: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Mark order as delivered
     */
    @PostMapping("/{orderId}/deliver")
    public ResponseEntity<ApiResponse<String>> markOrderAsDelivered(@PathVariable String orderId) {
        try {
            orderService.markOrderAsDelivered(orderId);
            return success("Order delivered", "Order marked as delivered successfully");
        } catch (ResourceNotFoundException e) {
            return error("Order not found: " + e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (OrderException e) {
            return error("Order delivery failed: " + e.getMessage(), HttpStatus.CONFLICT);
        } catch (Exception e) {
            return error("An error occurred while marking order as delivered: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Request and Response DTOs
    public static class CreateOrderRequest {
        private String cartSessionId;
        private String userId;

        public String getCartSessionId() { return cartSessionId; }
        public void setCartSessionId(String cartSessionId) { this.cartSessionId = cartSessionId; }

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
    }

    public static class UpdateOrderStatusRequest {
        private String status;
        private String adminOrManagerId;

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getAdminOrManagerId() { return adminOrManagerId; }
        public void setAdminOrManagerId(String adminOrManagerId) { this.adminOrManagerId = adminOrManagerId; }
    }

    public static class AddDeliveryInfoRequest {
        private DeliveryInfo deliveryInfo;
        private boolean rushOrder;

        public DeliveryInfo getDeliveryInfo() { return deliveryInfo; }
        public void setDeliveryInfo(DeliveryInfo deliveryInfo) { this.deliveryInfo = deliveryInfo; }

        public boolean isRushOrder() { return rushOrder; }
        public void setRushOrder(boolean rushOrder) { this.rushOrder = rushOrder; }
    }

    public static class CalculateShippingRequest {
        private DeliveryInfo deliveryInfo;
        private boolean rushOrder;

        public DeliveryInfo getDeliveryInfo() { return deliveryInfo; }
        public void setDeliveryInfo(DeliveryInfo deliveryInfo) { this.deliveryInfo = deliveryInfo; }

        public boolean isRushOrder() { return rushOrder; }
        public void setRushOrder(boolean rushOrder) { this.rushOrder = rushOrder; }
    }

    public static class ProcessPaymentRequest {
        private String paymentMethodId;

        public String getPaymentMethodId() { return paymentMethodId; }
        public void setPaymentMethodId(String paymentMethodId) { this.paymentMethodId = paymentMethodId; }
    }

    public static class CancelOrderRequest {
        private String customerId;

        public String getCustomerId() { return customerId; }
        public void setCustomerId(String customerId) { this.customerId = customerId; }
    }

    public static class ApproveOrderRequest {
        private String managerId;

        public String getManagerId() { return managerId; }
        public void setManagerId(String managerId) { this.managerId = managerId; }
    }

    public static class RejectOrderRequest {
        private String managerId;
        private String reason;

        public String getManagerId() { return managerId; }
        public void setManagerId(String managerId) { this.managerId = managerId; }

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public static class ShipOrderRequest {
        private String trackingNumber;

        public String getTrackingNumber() { return trackingNumber; }
        public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }
    }

    public static class ShippingFeeResponse {
        private float shippingFee;

        public float getShippingFee() { return shippingFee; }
        public void setShippingFee(float shippingFee) { this.shippingFee = shippingFee; }
    }

    public static class OrderTotalResponse {
        private float total;

        public float getTotal() { return total; }
        public void setTotal(float total) { this.total = total; }
    }
}
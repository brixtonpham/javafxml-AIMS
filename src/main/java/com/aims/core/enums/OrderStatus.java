package com.aims.core.enums;

public enum OrderStatus {
    PENDING_DELIVERY_INFO, // Order initiated, awaiting delivery details
    PENDING_PAYMENT,       // Delivery details provided, awaiting payment
    PAYMENT_FAILED,        // Payment attempt failed
    PENDING_PROCESSING,    // Chờ xử lý
    APPROVED,           // Đã duyệt
    REJECTED,           // Đã từ chối
    SHIPPING,           // Đang giao hàng
    DELIVERED,          // Đã giao thành công
    CANCELLED,          // Đã hủy
    REFUNDED,           // Đã hoàn tiền
    ERROR_STOCK_UPDATE_FAILED // Error occurred during stock update
}
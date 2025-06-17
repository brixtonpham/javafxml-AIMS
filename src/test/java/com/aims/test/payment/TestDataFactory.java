package com.aims.test.payment;

import com.aims.core.entities.OrderEntity;
import com.aims.core.entities.OrderItem;
import com.aims.core.entities.Product;
import com.aims.core.enums.OrderStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TestDataFactory {
    
    public static OrderEntity createTestOrder() {
        OrderEntity order = new OrderEntity();
        order.setOrderId(UUID.randomUUID().toString());
        order.setOrderDate(LocalDateTime.now());
        order.setTotalAmountPaid(100.0f);
        order.setOrderStatus(OrderStatus.PENDING_PAYMENT);
        order.setOrderItems(createTestOrderItems());
        return order;
    }
    
    private static List<OrderItem> createTestOrderItems() {
        List<OrderItem> items = new ArrayList<>();
        
        Product testProduct = new Product();
        testProduct.setProductId(UUID.randomUUID().toString());
        testProduct.setPrice(50.0f);
        testProduct.setTitle("Test Product");
        
        OrderItem item = new OrderItem();
        item.setProduct(testProduct);
        item.setQuantity(2);
        item.setPriceAtTimeOfOrder(testProduct.getPrice());
        item.setEligibleForRushDelivery(true);
        
        items.add(item);
        return items;
    }
}
package com.aims.core.application.services;

import com.aims.core.application.dtos.validation.*;
import com.aims.core.entities.OrderEntity;
import com.aims.core.entities.OrderItem;
import com.aims.core.entities.DeliveryInfo;
import com.aims.core.shared.exceptions.ValidationException;

import java.util.List;

/**
 * Enhanced Order Data Validation Service Interface
 * 
 * Provides comprehensive multi-level validation, detailed error reporting, 
 * and recovery mechanisms to ensure order data integrity throughout 
 * the customer journey.
 */
public interface IOrderDataValidationService {
    
    /**
     * Comprehensive order validation for all scenarios
     * 
     * @param order The order entity to validate
     * @return OrderValidationResult with detailed validation information
     */
    OrderValidationResult validateOrderComprehensive(OrderEntity order);
    
    /**
     * Validates order data completeness for UI display
     * 
     * @param order The order entity to validate
     * @return OrderValidationResult with UI-specific validation
     */
    OrderValidationResult validateOrderForDisplay(OrderEntity order);
    
    /**
     * Validates order readiness for payment processing
     * 
     * @param order The order entity to validate
     * @return OrderValidationResult with payment-specific validation
     */
    OrderValidationResult validateOrderForPayment(OrderEntity order);
    
    /**
     * Validates order data before navigation between screens
     * 
     * @param order The order entity to validate
     * @param targetScreen The target screen name
     * @return OrderValidationResult with navigation-specific validation
     */
    OrderValidationResult validateOrderForNavigation(OrderEntity order, String targetScreen);
    
    /**
     * Validates order items completeness and consistency
     * 
     * @param orderItems The list of order items to validate
     * @return OrderItemValidationResult with item-specific validation
     */
    OrderItemValidationResult validateOrderItems(List<OrderItem> orderItems);
    
    /**
     * Validates delivery information completeness
     * 
     * @param deliveryInfo The delivery information to validate
     * @return DeliveryValidationResult with delivery-specific validation
     */
    DeliveryValidationResult validateDeliveryInfo(DeliveryInfo deliveryInfo);
    
    /**
     * Validates pricing calculations and totals
     * 
     * @param order The order entity to validate pricing for
     * @return PricingValidationResult with pricing-specific validation
     */
    PricingValidationResult validateOrderPricing(OrderEntity order);
    
    /**
     * Validates rush delivery configuration
     * 
     * @param order The order entity to validate rush delivery for
     * @return RushDeliveryValidationResult with rush delivery validation
     */
    RushDeliveryValidationResult validateRushDelivery(OrderEntity order);
    
    /**
     * Provides detailed validation report with recovery suggestions
     * 
     * @param order The order entity to generate report for
     * @return DetailedValidationReport with comprehensive analysis
     */
    DetailedValidationReport getDetailedValidationReport(OrderEntity order);
    
    /**
     * Attempts to fix common validation issues automatically
     * 
     * @param order The order entity to fix
     * @return Fixed OrderEntity
     * @throws ValidationException If critical issues cannot be fixed
     */
    OrderEntity attemptValidationFixes(OrderEntity order) throws ValidationException;
}
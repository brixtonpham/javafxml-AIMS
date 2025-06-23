package com.aims.core.application.impl; // Or com.aims.core.application.services.impl;

import com.aims.core.application.services.IDeliveryCalculationService;
import com.aims.core.entities.OrderEntity;
import com.aims.core.entities.OrderItem;
import com.aims.core.entities.Product;
import com.aims.core.entities.DeliveryInfo;
import com.aims.core.shared.exceptions.ValidationException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DeliveryCalculationServiceImpl implements IDeliveryCalculationService {

    // Enhanced delivery calculation constants per specification lines 114-129
    private static final float HANOI_HCM_BASE_FEE_FIRST_3KG = 22000f;
    private static final float OTHER_PLACES_BASE_FEE_FIRST_0_5KG = 30000f;
    private static final float ADDITIONAL_FEE_PER_0_5KG = 2500f;
    private static final float RUSH_DELIVERY_SURCHARGE_PER_ITEM = 10000f;
    private static final float FREE_SHIPPING_THRESHOLD_VALUE = 100000f;
    private static final float MAX_FREE_SHIPPING_DISCOUNT = 25000f;
    private static final int DIMENSIONAL_WEIGHT_FACTOR = 6000; // cm^3/kg
    
    // Enhanced region-specific pricing constants
    private static final float HANOI_INNER_DISTRICTS_RATE_MULTIPLIER = 1.0f;
    private static final float HANOI_OUTER_DISTRICTS_RATE_MULTIPLIER = 1.15f;
    private static final float HCM_INNER_DISTRICTS_RATE_MULTIPLIER = 1.0f;
    private static final float HCM_OUTER_DISTRICTS_RATE_MULTIPLIER = 1.1f;
    private static final float NORTHERN_PROVINCES_RATE_MULTIPLIER = 1.2f;
    private static final float SOUTHERN_PROVINCES_RATE_MULTIPLIER = 1.25f;
    private static final float CENTRAL_PROVINCES_RATE_MULTIPLIER = 1.3f;
    private static final float REMOTE_AREAS_RATE_MULTIPLIER = 1.5f;

    // Enhanced region classification for more accurate delivery calculation
    private static final List<String> HANOI_INNER_CITY_DISTRICTS = List.of(
            "hoan kiem", "ba dinh", "dong da", "hai ba trung", "cau giay",
            "thanh xuan", "tay ho", "hoang mai", "long bien", "nam tu liem", "bac tu liem"
    );
    
    private static final List<String> HANOI_OUTER_DISTRICTS = List.of(
            "ha dong", "son tay", "ba vi", "chuong my", "dan phuong", "dong anh",
            "gia lam", "hoai duc", "me linh", "my duc", "phu xuyen", "quoc oai",
            "soc son", "thach that", "thanh oai", "thuong tin", "ung hoa"
    );
    
    private static final List<String> HCM_INNER_DISTRICTS = List.of(
            "district 1", "district 2", "district 3", "district 4", "district 5",
            "district 6", "district 7", "district 8", "district 10", "district 11",
            "binh thanh", "phu nhuan", "tan binh", "tan phu", "go vap", "thu duc"
    );
    
    private static final List<String> HCM_OUTER_DISTRICTS = List.of(
            "binh chanh", "can gio", "cu chi", "hoc mon", "nha be"
    );
    
    private static final List<String> NORTHERN_PROVINCES = List.of(
            "hai phong", "quang ninh", "bac giang", "bac kan", "bac ninh", "cao bang",
            "dien bien", "ha giang", "ha nam", "hai duong", "hoa binh", "hung yen",
            "lai chau", "lang son", "lao cai", "nam dinh", "ninh binh", "phu tho",
            "son la", "thai binh", "thai nguyen", "tuyen quang", "vinh phuc", "yen bai"
    );
    
    private static final List<String> CENTRAL_PROVINCES = List.of(
            "nghe an", "ha tinh", "quang binh", "quang tri", "thua thien hue",
            "da nang", "quang nam", "quang ngai", "binh dinh", "phu yen",
            "khanh hoa", "ninh thuan", "binh thuan", "kon tum", "gia lai", "dak lak", "dak nong"
    );
    
    private static final List<String> SOUTHERN_PROVINCES = List.of(
            "binh duong", "binh phuoc", "dong nai", "tay ninh", "ba ria vung tau",
            "long an", "dong thap", "an giang", "ben tre", "can tho", "hau giang",
            "kien giang", "soc trang", "tra vinh", "vinh long", "ca mau", "bac lieu"
    );
    
    private static final List<String> REMOTE_AREAS = List.of(
            "lai chau", "dien bien", "son la", "cao bang", "ha giang", "bac kan",
            "con dao", "phu quoc", "bach long vi"
    );

    public DeliveryCalculationServiceImpl() {
        // Constructor
    }
    
    /**
     * Enhanced method to determine the region type for more accurate pricing
     */
    private RegionType determineRegionType(DeliveryInfo deliveryInfo) {
        if (deliveryInfo == null || deliveryInfo.getDeliveryProvinceCity() == null || deliveryInfo.getDeliveryAddress() == null) {
            return RegionType.OTHER_PROVINCES;
        }
        
        String province = deliveryInfo.getDeliveryProvinceCity().trim().toLowerCase();
        String address = deliveryInfo.getDeliveryAddress().trim().toLowerCase();
        
        // Check for Hanoi regions
        if (province.contains("hanoi")) {
            if (HANOI_INNER_CITY_DISTRICTS.stream().anyMatch(district -> address.contains(district.toLowerCase()))) {
                return RegionType.HANOI_INNER;
            } else if (HANOI_OUTER_DISTRICTS.stream().anyMatch(district -> address.contains(district.toLowerCase()))) {
                return RegionType.HANOI_OUTER;
            } else {
                return RegionType.HANOI_INNER; // Default to inner for safety
            }
        }
        
        // Check for Ho Chi Minh City regions
        if (province.contains("ho chi minh") || province.contains("hcm") || province.contains("saigon")) {
            if (HCM_INNER_DISTRICTS.stream().anyMatch(district -> address.contains(district.toLowerCase()))) {
                return RegionType.HCM_INNER;
            } else if (HCM_OUTER_DISTRICTS.stream().anyMatch(district -> address.contains(district.toLowerCase()))) {
                return RegionType.HCM_OUTER;
            } else {
                return RegionType.HCM_INNER; // Default to inner for safety
            }
        }
        
        // Check for remote areas first (highest priority)
        if (REMOTE_AREAS.stream().anyMatch(area -> province.contains(area.toLowerCase()))) {
            return RegionType.REMOTE_AREAS;
        }
        
        // Check for regional provinces
        if (NORTHERN_PROVINCES.stream().anyMatch(prov -> province.contains(prov.toLowerCase()))) {
            return RegionType.NORTHERN_PROVINCES;
        }
        
        if (CENTRAL_PROVINCES.stream().anyMatch(prov -> province.contains(prov.toLowerCase()))) {
            return RegionType.CENTRAL_PROVINCES;
        }
        
        if (SOUTHERN_PROVINCES.stream().anyMatch(prov -> province.contains(prov.toLowerCase()))) {
            return RegionType.SOUTHERN_PROVINCES;
        }
        
        return RegionType.OTHER_PROVINCES; // Default fallback
    }
    
    /**
     * Get the rate multiplier based on region type for enhanced pricing
     */
    private float getRegionRateMultiplier(RegionType regionType) {
        switch (regionType) {
            case HANOI_INNER:
                return HANOI_INNER_DISTRICTS_RATE_MULTIPLIER;
            case HANOI_OUTER:
                return HANOI_OUTER_DISTRICTS_RATE_MULTIPLIER;
            case HCM_INNER:
                return HCM_INNER_DISTRICTS_RATE_MULTIPLIER;
            case HCM_OUTER:
                return HCM_OUTER_DISTRICTS_RATE_MULTIPLIER;
            case NORTHERN_PROVINCES:
                return NORTHERN_PROVINCES_RATE_MULTIPLIER;
            case SOUTHERN_PROVINCES:
                return SOUTHERN_PROVINCES_RATE_MULTIPLIER;
            case CENTRAL_PROVINCES:
                return CENTRAL_PROVINCES_RATE_MULTIPLIER;
            case REMOTE_AREAS:
                return REMOTE_AREAS_RATE_MULTIPLIER;
            default:
                return 1.0f; // Default multiplier for other provinces
        }
    }
    
    /**
     * Enhanced method to calculate rush delivery fee separately
     */
    private float calculateRushDeliveryFee(List<OrderItem> rushItems) {
        if (rushItems == null || rushItems.isEmpty()) {
            return 0f;
        }
        return rushItems.size() * RUSH_DELIVERY_SURCHARGE_PER_ITEM;
    }
    
    /**
     * Enhanced enum for region classification
     */
    private enum RegionType {
        HANOI_INNER,
        HANOI_OUTER,
        HCM_INNER,
        HCM_OUTER,
        NORTHERN_PROVINCES,
        CENTRAL_PROVINCES,
        SOUTHERN_PROVINCES,
        REMOTE_AREAS,
        OTHER_PROVINCES
    }

    @Override
    public float calculateShippingFee(OrderEntity order, boolean isRushOrderRequested) throws ValidationException {
        // ENHANCED: Comprehensive validation with detailed debugging output
        System.out.println("DELIVERY_CALC: calculateShippingFee called - Order: " +
                          (order != null ? "Order ID " + order.getOrderId() : "NULL ORDER"));
        
        if (order == null) {
            System.err.println("DELIVERY_CALC_ERROR: Order is null - this indicates a critical navigation/data passing issue");
            System.err.println("DELIVERY_CALC_ERROR: Stack trace for debugging:");
            Thread.dumpStack();
            throw new ValidationException("CRITICAL ERROR: Order is null. This indicates a problem in the navigation chain " +
                                        "where order data was not properly passed between screens. " +
                                        "Please restart the order process and contact support if the issue persists.");
        }
        
        String orderId = order.getOrderId();
        System.out.println("DELIVERY_CALC: Processing Order ID: " + orderId);
        
        if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
            System.err.println("DELIVERY_CALC_ERROR: No order items found for Order " + orderId);
            throw new ValidationException("Order must contain items for shipping calculation. Order ID: " + orderId +
                                        ". Please add items to your cart before proceeding.");
        }
        
        if (order.getDeliveryInfo() == null) {
            System.err.println("DELIVERY_CALC_ERROR: No delivery info found for Order " + orderId);
            throw new ValidationException("Delivery information is required for shipping calculation. " +
                                        "Please complete delivery details before calculating shipping. Order ID: " + orderId);
        }
        
        DeliveryInfo deliveryInfo = order.getDeliveryInfo();
        System.out.println("DELIVERY_CALC: Delivery info found - Province: " + deliveryInfo.getDeliveryProvinceCity());
        
        // ENHANCED: Validate delivery info fields with more specific context
        if (deliveryInfo.getDeliveryProvinceCity() == null || deliveryInfo.getDeliveryProvinceCity().trim().isEmpty()) {
            System.err.println("DELIVERY_CALC_ERROR: Province/city missing for Order " + orderId);
            throw new ValidationException("Delivery province/city is required for shipping calculation. " +
                                        "Please select a valid province/city from the dropdown. Order ID: " + orderId);
        }
        if (deliveryInfo.getDeliveryAddress() == null || deliveryInfo.getDeliveryAddress().trim().isEmpty()) {
            System.err.println("DELIVERY_CALC_ERROR: Address missing for Order " + orderId);
            throw new ValidationException("Delivery address is required for shipping calculation. " +
                                        "Please enter a complete delivery address. Order ID: " + orderId);
        }
        
        // ENHANCED: Add comprehensive logging for debugging shipping calculations
        System.out.println("DELIVERY_CALC: Starting calculation for Order " + orderId +
                          " | Rush requested: " + isRushOrderRequested +
                          " | Province: " + deliveryInfo.getDeliveryProvinceCity() +
                          " | Address: " + deliveryInfo.getDeliveryAddress() +
                          " | Item count: " + order.getOrderItems().size());
        List<OrderItem> allItems = order.getOrderItems();
        float totalShippingFee = 0f;

        List<OrderItem> standardDeliveryItems = new ArrayList<>();
        List<OrderItem> rushDeliveryItems = new ArrayList<>();

        boolean isRushAddressEligible = isRushDeliveryAddressEligible(deliveryInfo);

        if (isRushOrderRequested) {
            if (!isRushAddressEligible) {
                throw new ValidationException("Delivery address is not eligible for rush order. " +
                                            "Rush order is only available for inner city Hanoi districts.");
            }
            for (OrderItem item : allItems) {
                // Assuming OrderItem has a way to know if the product itself is eligible for rush.
                // For now, let's use the flag on OrderItem set during order initiation.
                if (item.isEligibleForRushDelivery()) { // This flag should be set based on product properties
                    rushDeliveryItems.add(item);
                } else {
                    standardDeliveryItems.add(item);
                }
            }
            if (rushDeliveryItems.isEmpty() && !allItems.isEmpty()) {
                 throw new ValidationException("Rush order requested, but no items in the order are eligible for rush delivery.");
            }
        } else {
            standardDeliveryItems.addAll(allItems);
        }

        // Calculate fee for standard delivery items
        if (!standardDeliveryItems.isEmpty()) {
            float standardItemsFee = calculateFeeForItemGroup(standardDeliveryItems, deliveryInfo, false);
            float standardItemsValueExclVAT = 0;
            for(OrderItem item : standardDeliveryItems) {
                standardItemsValueExclVAT += item.getPriceAtTimeOfOrder() * item.getQuantity();
            }
            float discount = getFreeShippingDiscount(standardItemsValueExclVAT);
            totalShippingFee += Math.max(0, standardItemsFee - discount);
        }

        // Enhanced: Calculate fee for rush delivery items with separate rush fee calculation
        if (!rushDeliveryItems.isEmpty()) {
            // Calculate base shipping fee for rush items (without rush surcharge)
            float rushItemsBaseFee = calculateFeeForItemGroup(rushDeliveryItems, deliveryInfo, false);
            // Calculate rush delivery surcharge separately as per specification
            float rushDeliveryFee = calculateRushDeliveryFee(rushDeliveryItems);
            totalShippingFee += rushItemsBaseFee + rushDeliveryFee;
            
            System.out.println("DELIVERY_CALC: Rush items - Base fee: " + rushItemsBaseFee +
                              ", Rush surcharge: " + rushDeliveryFee +
                              ", Items count: " + rushDeliveryItems.size());
        }

        System.out.println("DELIVERY_CALC: Total shipping fee calculated: " + totalShippingFee +
                          " for Order " + orderId);
        return totalShippingFee;
    }

    private float calculateFeeForItemGroup(List<OrderItem> items, DeliveryInfo deliveryInfo, boolean isRushGroup) throws ValidationException {
        if (items.isEmpty()) {
            return 0f;
        }

        float totalWeightKg = 0f;
        for (OrderItem item : items) {
            Product product = item.getProduct();
            if (product == null) {
                throw new ValidationException("Product details missing for an order item during fee calculation.");
            }
            // Using actual weight for now. Volumetric logic would be integrated here if active.
            // For the future requirement, you'd call calculateFeeWithDimensionalWeight for each item's package
            // or for the combined package. The problem statement implies per-item calculation of chargeable weight.
            totalWeightKg += product.getWeightKg() * item.getQuantity();
        }

        if (totalWeightKg <= 0 && !items.isEmpty()) {
             // Or handle as free if it's a digital product with no weight, though problem says physical media
            System.err.println("Warning: Total weight for item group is zero or negative. Fee might be incorrect.");
            return 0f;
        }


        // Enhanced: Use new region-based pricing calculation
        RegionType regionType = determineRegionType(deliveryInfo);
        float regionMultiplier = getRegionRateMultiplier(regionType);
        
        float baseFee;
        
        // Determine if this is a major city (Hanoi/HCM) for weight tier calculation
        boolean isMajorCity = (regionType == RegionType.HANOI_INNER || regionType == RegionType.HANOI_OUTER ||
                              regionType == RegionType.HCM_INNER || regionType == RegionType.HCM_OUTER);

        if (isMajorCity) {
            // Major cities: 3kg base tier
            baseFee = HANOI_HCM_BASE_FEE_FIRST_3KG;
            if (totalWeightKg > 3f) {
                float additionalWeight = totalWeightKg - 3f;
                baseFee += Math.ceil(additionalWeight / 0.5f) * ADDITIONAL_FEE_PER_0_5KG;
            }
        } else {
            // Other provinces: 0.5kg base tier
            baseFee = OTHER_PLACES_BASE_FEE_FIRST_0_5KG;
            if (totalWeightKg > 0.5f) {
                float additionalWeight = totalWeightKg - 0.5f;
                baseFee += Math.ceil(additionalWeight / 0.5f) * ADDITIONAL_FEE_PER_0_5KG;
            }
        }

        // Apply region-specific multiplier for enhanced pricing
        baseFee *= regionMultiplier;
        
        System.out.println("DELIVERY_CALC: Fee calculation - Region: " + regionType +
                          ", Multiplier: " + regionMultiplier +
                          ", Weight: " + totalWeightKg + "kg" +
                          ", Base fee: " + baseFee +
                          ", Rush group: " + isRushGroup);

        if (isRushGroup) {
            // Add rush surcharge per item line in the rush group
            baseFee += items.size() * RUSH_DELIVERY_SURCHARGE_PER_ITEM;
        }
        return baseFee;
    }


    @Override
    public float getFreeShippingDiscount(float totalValueNonRushItemsExclVAT) {
        if (totalValueNonRushItemsExclVAT > FREE_SHIPPING_THRESHOLD_VALUE) {
            return MAX_FREE_SHIPPING_DISCOUNT; // Max discount is 25,000 VND
        }
        return 0f;
    }

    @Override
    public boolean isRushDeliveryAddressEligible(DeliveryInfo deliveryInfo) {
        if (deliveryInfo == null || deliveryInfo.getDeliveryProvinceCity() == null || deliveryInfo.getDeliveryAddress() == null) {
            return false;
        }
        String province = deliveryInfo.getDeliveryProvinceCity().trim().toLowerCase();
        String address = deliveryInfo.getDeliveryAddress().trim().toLowerCase();

        if (province.contains("hanoi")) {
            // Check against predefined list of inner city districts
            return HANOI_INNER_CITY_DISTRICTS.stream().anyMatch(district -> address.contains(district.toLowerCase()));
        }
        return false;
    }

    @Override
    public float calculateFeeWithDimensionalWeight(
            float actualWeightKg,
            float lengthCm,
            float widthCm,
            float heightCm,
            String deliveryProvinceCity,
            boolean isInnerCityHanoiOrHCM // This boolean simplifies location check for this specific method
    ) throws ValidationException {

        if (actualWeightKg <= 0 || lengthCm <= 0 || widthCm <= 0 || heightCm <= 0) {
            throw new ValidationException("Actual weight and dimensions must be positive for dimensional weight calculation.");
        }
        if (deliveryProvinceCity == null || deliveryProvinceCity.trim().isEmpty()){
            throw new ValidationException("Delivery province/city is required.");
        }

        float dimensionalWeightKg = (lengthCm * widthCm * heightCm) / DIMENSIONAL_WEIGHT_FACTOR;
        float chargeableWeightKg = Math.max(actualWeightKg, dimensionalWeightKg);

        float shippingFee;
        if (isInnerCityHanoiOrHCM) {
            shippingFee = HANOI_HCM_BASE_FEE_FIRST_3KG;
            if (chargeableWeightKg > 3f) {
                float additionalWeight = chargeableWeightKg - 3f;
                shippingFee += Math.ceil(additionalWeight / 0.5f) * ADDITIONAL_FEE_PER_0_5KG;
            }
        } else {
            shippingFee = OTHER_PLACES_BASE_FEE_FIRST_0_5KG; // For the first 0.5kg
            if (chargeableWeightKg > 0.5f) {
                float additionalWeight = chargeableWeightKg - 0.5f;
                shippingFee += Math.ceil(additionalWeight / 0.5f) * ADDITIONAL_FEE_PER_0_5KG;
            }
        }
        return shippingFee;
    }

    /**
     * Enhanced implementation of detailed delivery fee breakdown
     */
    @Override
    public DeliveryFeeBreakdown calculateDeliveryFeeBreakdown(OrderEntity order, boolean isRushOrder) throws ValidationException {
        // Validate inputs similar to main calculation
        if (order == null || order.getOrderItems() == null || order.getOrderItems().isEmpty() || order.getDeliveryInfo() == null) {
            throw new ValidationException("Order, order items, and delivery information are required for fee breakdown calculation.");
        }

        List<OrderItem> allItems = order.getOrderItems();
        DeliveryInfo deliveryInfo = order.getDeliveryInfo();
        
        List<OrderItem> standardItems = new ArrayList<>();
        List<OrderItem> rushItems = new ArrayList<>();
        
        // Separate items by delivery type
        if (isRushOrder && isRushDeliveryAddressEligible(deliveryInfo)) {
            for (OrderItem item : allItems) {
                if (item.isEligibleForRushDelivery()) {
                    rushItems.add(item);
                } else {
                    standardItems.add(item);
                }
            }
        } else {
            standardItems.addAll(allItems);
        }
        
        float standardBaseFee = 0f;
        float rushBaseFee = 0f;
        float rushSurcharge = 0f;
        float freeShippingDiscount = 0f;
        float regionalAdjustment = 0f;
        
        // Calculate standard items
        if (!standardItems.isEmpty()) {
            standardBaseFee = calculateFeeForItemGroup(standardItems, deliveryInfo, false);
            
            // Calculate free shipping discount
            float standardValue = 0f;
            for (OrderItem item : standardItems) {
                standardValue += item.getPriceAtTimeOfOrder() * item.getQuantity();
            }
            freeShippingDiscount = getFreeShippingDiscount(standardValue);
        }
        
        // Calculate rush items
        if (!rushItems.isEmpty()) {
            rushBaseFee = calculateFeeForItemGroup(rushItems, deliveryInfo, false);
            rushSurcharge = calculateRushDeliveryFee(rushItems);
        }
        
        // Calculate regional adjustment (difference from base rate)
        RegionType regionType = determineRegionType(deliveryInfo);
        float regionMultiplier = getRegionRateMultiplier(regionType);
        if (regionMultiplier != 1.0f) {
            regionalAdjustment = (standardBaseFee + rushBaseFee) * (regionMultiplier - 1.0f);
        }
        
        float totalFee = Math.max(0, standardBaseFee - freeShippingDiscount) + rushBaseFee + rushSurcharge;
        
        return new DeliveryFeeBreakdown(totalFee, standardBaseFee + rushBaseFee,
                                       regionalAdjustment, rushSurcharge, freeShippingDiscount);
    }

    /**
     * Enhanced implementation of delivery time estimation
     */
    @Override
    public int getEstimatedDeliveryDays(DeliveryInfo deliveryInfo, boolean isRushOrder) {
        if (isRushOrder && isRushDeliveryAddressEligible(deliveryInfo)) {
            return 1; // Rush delivery: next day for eligible addresses
        }
        
        if (deliveryInfo != null && deliveryInfo.getDeliveryProvinceCity() != null) {
            RegionType regionType = determineRegionType(deliveryInfo);
            
            switch (regionType) {
                case HANOI_INNER:
                case HCM_INNER:
                    return 2; // Major city inner districts: 2 days
                case HANOI_OUTER:
                case HCM_OUTER:
                    return 3; // Major city outer districts: 3 days
                case NORTHERN_PROVINCES:
                case SOUTHERN_PROVINCES:
                    return 4; // Regional provinces: 4 days
                case CENTRAL_PROVINCES:
                    return 5; // Central provinces: 5 days
                case REMOTE_AREAS:
                    return 7; // Remote areas: 7 days
                default:
                    return 4; // Default for other provinces
            }
        }
        
        return 4; // Default delivery time
    }
}
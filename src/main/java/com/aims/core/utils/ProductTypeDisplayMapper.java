package com.aims.core.utils;

import com.aims.core.enums.ProductType;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility class for mapping between ProductType enum values and their display names.
 * Provides centralized mapping logic for UI display of product types.
 */
public class ProductTypeDisplayMapper {
    
    /**
     * Mapping from ProductType enum to user-friendly display names
     */
    private static final Map<ProductType, String> DISPLAY_NAMES = Map.of(
        ProductType.BOOK, "Books",
        ProductType.CD, "CDs", 
        ProductType.DVD, "DVDs",
        ProductType.LP, "LP Records"
    );
    
    /**
     * Reverse mapping from display names back to ProductType enum
     */
    private static final Map<String, ProductType> REVERSE_MAPPING = 
        DISPLAY_NAMES.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
    
    /**
     * Gets all display names for product types, excluding OTHER type.
     * Used to populate UI dropdowns and filters.
     * 
     * @return List of user-friendly product type display names
     */
    public static List<String> getAllDisplayNames() {
        return Arrays.stream(ProductType.values())
            .filter(type -> type != ProductType.OTHER)
            .map(DISPLAY_NAMES::get)
            .filter(displayName -> displayName != null)
            .collect(Collectors.toList());
    }
    
    /**
     * Gets the display name for a given ProductType.
     * 
     * @param type The ProductType enum value
     * @return The user-friendly display name, or the enum name if no mapping exists
     */
    public static String getDisplayName(ProductType type) {
        return DISPLAY_NAMES.getOrDefault(type, type.name());
    }
    
    /**
     * Gets the ProductType enum from a display name.
     * 
     * @param displayName The user-friendly display name
     * @return The corresponding ProductType enum, or null if no mapping exists
     */
    public static ProductType fromDisplayName(String displayName) {
        return REVERSE_MAPPING.get(displayName);
    }
    
    /**
     * Checks if a given string is a valid product type display name.
     * 
     * @param displayName The display name to check
     * @return true if it's a valid product type display name, false otherwise
     */
    public static boolean isValidDisplayName(String displayName) {
        return REVERSE_MAPPING.containsKey(displayName);
    }
}
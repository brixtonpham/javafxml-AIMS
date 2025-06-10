package com.aims.core.presentation.utils;

import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * ResponsiveLayoutManager - Utility class for managing responsive layout behavior
 * Provides screen size detection, optimal layout calculations, and responsive adaptation
 */
public class ResponsiveLayoutManager {
    
    public enum ScreenSize {
        MOBILE(768),
        TABLET(1024),
        DESKTOP(1440),
        LARGE_DESKTOP(1920),
        ULTRAWIDE(2560);
        
        private final double minWidth;
        
        ScreenSize(double minWidth) {
            this.minWidth = minWidth;
        }
        
        public double getMinWidth() {
            return minWidth;
        }
    }
    
    /**
     * Enhanced screen size detection based on window width
     */
    public static ScreenSize detectScreenSize(double width) {
        if (width >= ScreenSize.ULTRAWIDE.getMinWidth()) {
            return ScreenSize.ULTRAWIDE;
        } else if (width >= ScreenSize.LARGE_DESKTOP.getMinWidth()) {
            return ScreenSize.LARGE_DESKTOP;
        } else if (width >= ScreenSize.DESKTOP.getMinWidth()) {
            return ScreenSize.DESKTOP;
        } else if (width >= ScreenSize.TABLET.getMinWidth()) {
            return ScreenSize.TABLET;
        } else {
            return ScreenSize.MOBILE;
        }
    }
    
    /**
     * Enhanced CSS class mapping for screen sizes
     */
    public static String getResponsiveStyleClass(ScreenSize screenSize) {
        switch (screenSize) {
            case ULTRAWIDE:
                return "responsive-ultrawide";
            case LARGE_DESKTOP:
                return "responsive-large-desktop";
            case DESKTOP:
                return "responsive-desktop";
            case TABLET:
                return "responsive-tablet";
            case MOBILE:
                return "responsive-mobile";
            default:
                return "responsive-desktop";
        }
    }
    
    /**
     * Calculate optimal number of product columns based on container width
     */
    public static int getOptimalProductColumns(double containerWidth, double cardWidth, double spacing) {
        double availableWidth = containerWidth - 60; // Account for padding
        double cardWithSpacing = cardWidth + spacing;
        int columns = Math.max(1, (int) Math.floor(availableWidth / cardWithSpacing));
        return Math.min(columns, 6); // Maximum 6 columns
    }
    
    /**
     * Enhanced optimal font size calculation based on screen size
     */
    public static double getOptimalFontSize(ScreenSize screenSize, double baseFontSize) {
        switch (screenSize) {
            case ULTRAWIDE:
                return baseFontSize * 1.25;
            case LARGE_DESKTOP:
                return baseFontSize * 1.15;
            case DESKTOP:
                return baseFontSize;
            case TABLET:
                return baseFontSize * 0.95;
            case MOBILE:
                return baseFontSize * 0.9;
            default:
                return baseFontSize;
        }
    }
    
    /**
     * Enhanced optimal product card dimensions based on screen size
     */
    public static CardDimensions getOptimalCardDimensions(ScreenSize screenSize) {
        switch (screenSize) {
            case ULTRAWIDE:
                return new CardDimensions(350, 35);
            case LARGE_DESKTOP:
                return new CardDimensions(320, 30);
            case DESKTOP:
                return new CardDimensions(300, 25);
            case TABLET:
                return new CardDimensions(280, 20);
            case MOBILE:
                return new CardDimensions(250, 15);
            default:
                return new CardDimensions(300, 25);
        }
    }
    
    /**
     * Data class for card dimensions
     */
    public static class CardDimensions {
        private final double width;
        private final double spacing;
        
        public CardDimensions(double width, double spacing) {
            this.width = width;
            this.spacing = spacing;
        }
        
        public double getWidth() { return width; }
        public double getSpacing() { return spacing; }
    }
    
    /**
     * Apply responsive layout to a container based on current screen size
     */
    public static void applyResponsiveLayout(javafx.scene.layout.Region container, double currentWidth) {
        ScreenSize screenSize = detectScreenSize(currentWidth);
        String responsiveClass = getResponsiveStyleClass(screenSize);
        
        // Remove existing responsive classes
        container.getStyleClass().removeIf(styleClass -> 
            styleClass.startsWith("responsive-desktop-") || styleClass.equals("responsive-mobile"));
        
        // Apply new responsive class
        if (!container.getStyleClass().contains(responsiveClass)) {
            container.getStyleClass().add(responsiveClass);
        }
        
        System.out.println("Applied responsive class: " + responsiveClass + " for width: " + currentWidth);
    }
    
    /**
     * Setup responsive behavior for a scene with automatic layout adaptation
     */
    public static void setupResponsiveBehavior(Scene scene, javafx.scene.layout.Region mainContainer) {
        if (scene != null && mainContainer != null) {
            // Initial responsive setup
            applyResponsiveLayout(mainContainer, scene.getWidth());
            
            // Add listener for window resize
            scene.widthProperty().addListener((observable, oldValue, newValue) -> {
                applyResponsiveLayout(mainContainer, newValue.doubleValue());
            });
            
            System.out.println("ResponsiveLayoutManager: Responsive behavior setup completed");
        }
    }
}
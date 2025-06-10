package com.aims.core.presentation.utils;

import javafx.stage.Screen;
import javafx.geometry.Rectangle2D;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ScreenDetectionService - Advanced screen detection and DPI handling
 * Provides comprehensive screen analysis and optimal display configuration
 */
public class ScreenDetectionService {
    
    public static class ScreenInfo {
        private final double width;
        private final double height;
        private final double dpi;
        private final boolean isPrimary;
        private final ResponsiveLayoutManager.ScreenSize sizeCategory;
        
        public ScreenInfo(double width, double height, double dpi, boolean isPrimary) {
            this.width = width;
            this.height = height;
            this.dpi = dpi;
            this.isPrimary = isPrimary;
            this.sizeCategory = ResponsiveLayoutManager.detectScreenSize(width);
        }
        
        // Getters
        public double getWidth() { return width; }
        public double getHeight() { return height; }
        public double getDpi() { return dpi; }
        public boolean isPrimary() { return isPrimary; }
        public ResponsiveLayoutManager.ScreenSize getSizeCategory() { return sizeCategory; }
        
        /**
         * Calculate DPI-based scale factor
         */
        public double getScaleFactor() {
            if (dpi >= 192) return 1.5; // High DPI (4K, Retina)
            if (dpi >= 144) return 1.25; // Medium DPI (QHD)
            return 1.0; // Standard DPI (Full HD and below)
        }
        
        /**
         * Check if this is a high DPI display
         */
        public boolean isHighDPI() {
            return dpi >= 144;
        }
        
        /**
         * Check if this is an ultra-wide display (21:9 or wider)
         */
        public boolean isUltraWide() {
            return width / height >= 2.3; // Typical ultra-wide ratio
        }
        
        /**
         * Get optimal window size as percentage of screen
         */
        public Rectangle2D getOptimalWindowSize() {
            double optimalWidth = width * 0.9;
            double optimalHeight = height * 0.9;
            
            // Ensure minimum sizes
            optimalWidth = Math.max(optimalWidth, 1200);
            optimalHeight = Math.max(optimalHeight, 720);
            
            // Ensure maximum sizes for ultra-wide displays
            if (isUltraWide()) {
                optimalWidth = Math.min(optimalWidth, 1800);
            }
            
            return new Rectangle2D(0, 0, optimalWidth, optimalHeight);
        }
        
        @Override
        public String toString() {
            return String.format("ScreenInfo{width=%.0f, height=%.0f, dpi=%.0f, category=%s, primary=%b, ultraWide=%b, highDPI=%b}", 
                               width, height, dpi, sizeCategory, isPrimary, isUltraWide(), isHighDPI());
        }
    }
    
    /**
     * Get information about the primary screen
     */
    public static ScreenInfo getPrimaryScreenInfo() {
        Screen primaryScreen = Screen.getPrimary();
        Rectangle2D bounds = primaryScreen.getBounds();
        double dpi = primaryScreen.getDpi();
        
        return new ScreenInfo(bounds.getWidth(), bounds.getHeight(), dpi, true);
    }
    
    /**
     * Get information about all available screens
     */
    public static List<ScreenInfo> getAllScreensInfo() {
        List<Screen> screens = Screen.getScreens();
        Screen primary = Screen.getPrimary();
        
        return screens.stream()
                .map(screen -> {
                    Rectangle2D bounds = screen.getBounds();
                    double dpi = screen.getDpi();
                    boolean isPrimary = screen.equals(primary);
                    return new ScreenInfo(bounds.getWidth(), bounds.getHeight(), dpi, isPrimary);
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Get the optimal CSS class for the given screen configuration
     */
    public static String getOptimalCSSClass(ScreenInfo screenInfo) {
        if (screenInfo.isUltraWide()) {
            return "responsive-ultrawide";
        }
        return ResponsiveLayoutManager.getResponsiveStyleClass(screenInfo.getSizeCategory());
    }
    
    /**
     * Calculate optimal font size considering DPI scaling
     */
    public static double getOptimalFontSize(ScreenInfo screenInfo, double baseFontSize) {
        double scaledSize = ResponsiveLayoutManager.getOptimalFontSize(screenInfo.getSizeCategory(), baseFontSize);
        return scaledSize * screenInfo.getScaleFactor();
    }
    
    /**
     * Get optimal product grid configuration for the screen
     */
    public static GridConfiguration getOptimalGridConfiguration(ScreenInfo screenInfo) {
        ResponsiveLayoutManager.CardDimensions cardDims = ResponsiveLayoutManager.getOptimalCardDimensions(screenInfo.getSizeCategory());
        
        // Apply DPI scaling
        double scaledCardWidth = cardDims.getWidth() * screenInfo.getScaleFactor();
        double scaledSpacing = cardDims.getSpacing() * screenInfo.getScaleFactor();
        
        // Calculate optimal columns
        int columns = ResponsiveLayoutManager.getOptimalProductColumns(screenInfo.getWidth(), scaledCardWidth, scaledSpacing);
        
        // Apply ultra-wide adjustments
        if (screenInfo.isUltraWide()) {
            columns = Math.min(columns + 2, 8); // Add 2 more columns for ultra-wide, max 8
        }
        
        return new GridConfiguration(columns, scaledCardWidth, scaledSpacing);
    }
    
    /**
     * Configuration data for product grid layout
     */
    public static class GridConfiguration {
        private final int columns;
        private final double cardWidth;
        private final double spacing;
        
        public GridConfiguration(int columns, double cardWidth, double spacing) {
            this.columns = columns;
            this.cardWidth = cardWidth;
            this.spacing = spacing;
        }
        
        public int getColumns() { return columns; }
        public double getCardWidth() { return cardWidth; }
        public double getSpacing() { return spacing; }
        
        @Override
        public String toString() {
            return String.format("GridConfiguration{columns=%d, cardWidth=%.0f, spacing=%.0f}", 
                               columns, cardWidth, spacing);
        }
    }
    
    /**
     * Configure application window for optimal display on current screen
     */
    public static void configureWindowForScreen(javafx.stage.Stage stage, javafx.scene.Scene scene) {
        ScreenInfo screenInfo = getPrimaryScreenInfo();
        
        System.out.println("Configuring window for screen: " + screenInfo);
        
        // Apply screen-specific configurations
        if (screenInfo.isHighDPI()) {
            // Add high DPI CSS if available
            try {
                String highDpiCss = "/styles/high-dpi.css";
                if (ScreenDetectionService.class.getResource(highDpiCss) != null) {
                    scene.getStylesheets().add(ScreenDetectionService.class.getResource(highDpiCss).toExternalForm());
                    System.out.println("Applied high DPI styles");
                }
            } catch (Exception e) {
                System.out.println("High DPI CSS not available, using default scaling");
            }
        }
        
        if (screenInfo.isUltraWide()) {
            scene.getRoot().getStyleClass().add("responsive-ultrawide");
            System.out.println("Applied ultra-wide layout");
        }
        
        // Configure stage size based on screen
        Rectangle2D optimalSize = screenInfo.getOptimalWindowSize();
        stage.setWidth(optimalSize.getWidth());
        stage.setHeight(optimalSize.getHeight());
        
        // Center on screen
        stage.setX((screenInfo.getWidth() - optimalSize.getWidth()) / 2);
        stage.setY((screenInfo.getHeight() - optimalSize.getHeight()) / 2);
        
        System.out.println("Configured window size: " + optimalSize.getWidth() + "x" + optimalSize.getHeight());
    }
    
    /**
     * Detect if the application is running on a touch-enabled device
     */
    public static boolean isTouchEnabled() {
        try {
            // Check for touch capability (this is a simplified check)
            return System.getProperty("javafx.platform").contains("touch") ||
                   System.getProperty("os.name").toLowerCase().contains("android") ||
                   System.getProperty("os.name").toLowerCase().contains("ios");
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get system theme preference (light/dark)
     */
    public static String getSystemTheme() {
        try {
            String osName = System.getProperty("os.name").toLowerCase();
            if (osName.contains("windows")) {
                // On Windows, check registry for dark mode (simplified)
                return "light"; // Default to light theme
            } else if (osName.contains("mac")) {
                // On macOS, check system preferences (simplified)
                return "light"; // Default to light theme
            }
        } catch (Exception e) {
            System.out.println("Could not detect system theme, using default");
        }
        return "light";
    }
}
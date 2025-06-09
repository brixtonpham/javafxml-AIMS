# AIMS Responsive Design - Phase 2 & Phase 3 Implementation Guide

## Phase 2: Advanced Responsive Design Patterns

### 2.1 Dynamic Screen Size Detection and Layout Adaptation

#### Create Responsive Layout Manager

Add this class to `src/main/java/com/aims/core/presentation/utils/ResponsiveLayoutManager.java`:

```java
package com.aims.core.presentation.utils;

import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ResponsiveLayoutManager {
    
    public enum ScreenSize {
        MOBILE(768),
        COMPACT_DESKTOP(1024),
        STANDARD_DESKTOP(1366),
        LARGE_DESKTOP(1920);
        
        private final double minWidth;
        
        ScreenSize(double minWidth) {
            this.minWidth = minWidth;
        }
        
        public double getMinWidth() {
            return minWidth;
        }
    }
    
    public static ScreenSize detectScreenSize(double width) {
        if (width >= ScreenSize.LARGE_DESKTOP.getMinWidth()) {
            return ScreenSize.LARGE_DESKTOP;
        } else if (width >= ScreenSize.STANDARD_DESKTOP.getMinWidth()) {
            return ScreenSize.STANDARD_DESKTOP;
        } else if (width >= ScreenSize.COMPACT_DESKTOP.getMinWidth()) {
            return ScreenSize.COMPACT_DESKTOP;
        } else {
            return ScreenSize.MOBILE;
        }
    }
    
    public static String getResponsiveStyleClass(ScreenSize screenSize) {
        switch (screenSize) {
            case LARGE_DESKTOP:
                return "responsive-desktop-large";
            case STANDARD_DESKTOP:
                return "responsive-desktop-standard";
            case COMPACT_DESKTOP:
                return "responsive-desktop-compact";
            case MOBILE:
                return "responsive-mobile";
            default:
                return "responsive-desktop-standard";
        }
    }
    
    public static int getOptimalProductColumns(double containerWidth, double cardWidth, double spacing) {
        double availableWidth = containerWidth - 60; // Account for padding
        double cardWithSpacing = cardWidth + spacing;
        int columns = Math.max(1, (int) Math.floor(availableWidth / cardWithSpacing));
        return Math.min(columns, 6); // Maximum 6 columns
    }
    
    public static double getOptimalFontSize(ScreenSize screenSize, double baseFontSize) {
        switch (screenSize) {
            case LARGE_DESKTOP:
                return baseFontSize * 1.1;
            case STANDARD_DESKTOP:
                return baseFontSize;
            case COMPACT_DESKTOP:
                return baseFontSize * 0.95;
            case MOBILE:
                return baseFontSize * 0.9;
            default:
                return baseFontSize;
        }
    }
}
```

### 2.2 Enhanced Product Detail Screen Layout

#### Update `src/main/resources/com/aims/presentation/views/product_detail_screen.fxml`

```xml
<?xml version="1.0" encoding="UTF-8"?>

<?import javafx.geometry.Insets?>
<?import javafx.scene.control.Button?>
<?import javafx.scene.control.Label?>
<?import javafx.scene.control.ScrollPane?>
<?import javafx.scene.control.Separator?>
<?import javafx.scene.control.Spinner?>
<?import javafx.scene.control.TextArea?>
<?import javafx.scene.image.ImageView?>
<?import javafx.scene.layout.BorderPane?>
<?import javafx.scene.layout.ColumnConstraints?>
<?import javafx.scene.layout.GridPane?>
<?import javafx.scene.layout.HBox?>
<?import javafx.scene.layout.VBox?>

<BorderPane fx:id="productDetailPane" 
            styleClass="responsive-border-pane, fill-parent" 
            prefHeight="USE_COMPUTED_SIZE" 
            prefWidth="USE_COMPUTED_SIZE" 
            xmlns="http://javafx.com/javafx/17" 
            xmlns:fx="http://javafx.com/fxml/1" 
            fx:controller="com.aims.core.presentation.controllers.ProductDetailScreenController">
    <padding>
        <Insets bottom="20.0" left="20.0" right="20.0" top="20.0" />
    </padding>
    <top>
        <Label fx:id="productTitleLabel" 
               styleClass="responsive-header, center-content" 
               text="Product Details" 
               BorderPane.alignment="CENTER">
            <BorderPane.margin>
                <Insets bottom="20.0" />
            </BorderPane.margin>
        </Label>
    </top>
    <center>
        <ScrollPane styleClass="responsive-scroll-pane" 
                    fitToWidth="true" 
                    fitToHeight="false" 
                    hbarPolicy="AS_NEEDED" 
                    vbarPolicy="AS_NEEDED">
            <HBox fx:id="productDetailContainer"
                  styleClass="responsive-hbox, responsive-spacing-large" 
                  prefHeight="USE_COMPUTED_SIZE" 
                  minHeight="400.0">
                <padding>
                    <Insets bottom="20.0" left="20.0" right="20.0" top="20.0" />
                </padding>
                
                <!-- Product Image and Purchase Section -->
                <VBox fx:id="productImageSection"
                      styleClass="responsive-vbox, center-content" 
                      alignment="TOP_CENTER" 
                      prefWidth="400.0" 
                      minWidth="300.0" 
                      maxWidth="500.0"
                      HBox.hgrow="NEVER">
                    <ImageView fx:id="productImageView" 
                               styleClass="responsive-product-image" 
                               fitHeight="300.0" 
                               fitWidth="350.0" 
                               pickOnBounds="true" 
                               preserveRatio="true">
                        <VBox.margin>
                            <Insets bottom="15.0" />
                        </VBox.margin>
                    </ImageView>
                    <Label fx:id="productPriceLabel" 
                           styleClass="center-content" 
                           text="0 VND"
                           style="-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #e74c3c;" />
                    <Label fx:id="productAvailabilityLabel" 
                           styleClass="center-content" 
                           text="Available: 0"
                           style="-fx-font-size: 14px; -fx-text-fill: #27ae60;" />
                    <HBox alignment="CENTER" 
                          styleClass="responsive-spacing-medium"
                          VBox.vgrow="NEVER">
                        <Label text="Quantity:" 
                               styleClass="center-content"
                               style="-fx-font-weight: bold;" />
                        <Spinner fx:id="quantitySpinner" 
                                prefWidth="80.0"/>
                    </HBox>
                    <Button fx:id="addToCartButton" 
                            styleClass="responsive-success-button" 
                            text="Add to Cart" 
                            onAction="#handleAddToCartAction" 
                            maxWidth="Infinity" 
                            prefHeight="45.0" 
                            VBox.vgrow="NEVER" />
                </VBox>

                <!-- Product Information Section -->
                <VBox fx:id="productInfoSection"
                      styleClass="responsive-vbox, responsive-spacing-medium" 
                      HBox.hgrow="ALWAYS" 
                      prefWidth="USE_COMPUTED_SIZE" 
                      minWidth="400.0">
                    <VBox.margin>
                        <Insets left="30.0" />
                    </VBox.margin>
                    
                    <Label text="Category:" 
                           styleClass="responsive-section-title"
                           style="-fx-font-size: 18px; -fx-font-weight: bold;" />
                    <Label fx:id="productCategoryLabel" 
                           styleClass="responsive-text"
                           style="-fx-font-size: 14px; -fx-text-fill: #7f8c8d;" />
                    <Separator/>
                    
                    <Label text="Description:" 
                           styleClass="responsive-section-title"
                           style="-fx-font-size: 18px; -fx-font-weight: bold;" />
                    <TextArea fx:id="productDescriptionArea" 
                             styleClass="responsive-text-area" 
                             editable="false" 
                             wrapText="true" 
                             prefHeight="100.0" 
                             minHeight="80.0" 
                             maxHeight="150.0"
                             VBox.vgrow="SOMETIMES" />
                    <Separator/>
                    
                    <Label text="Specific Details:" 
                           styleClass="responsive-section-title"
                           style="-fx-font-size: 18px; -fx-font-weight: bold;" />
                    <GridPane fx:id="productSpecificsGrid" 
                             styleClass="responsive-grid"
                             hgap="15" 
                             vgap="8" 
                             prefWidth="USE_COMPUTED_SIZE"
                             VBox.vgrow="SOMETIMES">
                        <columnConstraints>
                            <ColumnConstraints halignment="LEFT" 
                                             hgrow="NEVER" 
                                             minWidth="120.0" 
                                             prefWidth="150.0" />
                            <ColumnConstraints hgrow="ALWAYS" 
                                             minWidth="200.0" />
                        </columnConstraints>
                    </GridPane>
                </VBox>
            </HBox>
        </ScrollPane>
    </center>
    <bottom>
        <HBox alignment="CENTER_RIGHT" 
              styleClass="responsive-hbox, responsive-spacing-medium" 
              BorderPane.alignment="CENTER" 
              prefHeight="50.0">
            <BorderPane.margin>
                <Insets top="15.0" />
            </BorderPane.margin>
            <Label fx:id="errorMessageLabel" 
                   styleClass="responsive-error-message" 
                   wrapText="true" 
                   HBox.hgrow="ALWAYS" />
            <Button text="Back to Products" 
                   styleClass="responsive-button" 
                   onAction="#handleBackToListingAction" 
                   prefHeight="35.0" 
                   prefWidth="140.0" />
        </HBox>
    </bottom>
</BorderPane>
```

### 2.3 Responsive Cart Screen Layout

#### Update `src/main/resources/com/aims/presentation/views/cart_screen.fxml`

```xml
<?xml version="1.0" encoding="UTF-8"?>

<?import javafx.geometry.Insets?>
<?import javafx.scene.control.Button?>
<?import javafx.scene.control.Label?>
<?import javafx.scene.control.ScrollPane?>
<?import javafx.scene.control.Separator?>
<?import javafx.scene.layout.BorderPane?>
<?import javafx.scene.layout.HBox?>
<?import javafx.scene.layout.VBox?>

<BorderPane fx:id="cartScreenPane"
            styleClass="responsive-border-pane, fill-parent"
            maxHeight="Infinity"
            maxWidth="Infinity"
            minHeight="400.0"
            minWidth="600.0"
            prefHeight="USE_COMPUTED_SIZE"
            prefWidth="USE_COMPUTED_SIZE"
            xmlns="http://javafx.com/javafx/17"
            xmlns:fx="http://javafx.com/fxml/1"
            fx:controller="com.aims.core.presentation.controllers.CartScreenController">
    <top>
        <VBox styleClass="responsive-vbox, responsive-spacing-medium">
            <padding>
                <Insets bottom="20.0" left="30.0" right="30.0" top="20.0" />
            </padding>
            <Label text="Shopping Cart"
                   styleClass="responsive-header, center-content"
                   alignment="CENTER"
                   maxWidth="Infinity"
                   style="-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;">
                <VBox.margin>
                    <Insets bottom="10.0" />
                </VBox.margin>
            </Label>
        </VBox>
    </top>
    <center>
        <ScrollPane fx:id="cartScrollPane"
                    styleClass="responsive-scroll-pane"
                    fitToWidth="true"
                    fitToHeight="false"
                    hbarPolicy="NEVER"
                    vbarPolicy="AS_NEEDED"
                    maxHeight="Infinity"
                    maxWidth="Infinity"
                    prefHeight="USE_COMPUTED_SIZE"
                    prefWidth="USE_COMPUTED_SIZE"
                    BorderPane.alignment="CENTER"
                    VBox.vgrow="ALWAYS">
            <VBox fx:id="cartItemsContainer"
                  styleClass="responsive-vbox, responsive-spacing-small"
                  maxWidth="Infinity"
                  prefWidth="USE_COMPUTED_SIZE">
                <padding>
                    <Insets bottom="20.0" left="30.0" right="30.0" top="10.0" />
                </padding>
            </VBox>
        </ScrollPane>
    </center>
    <bottom>
        <VBox styleClass="responsive-vbox, responsive-spacing-medium">
            <padding>
                <Insets bottom="20.0" left="30.0" right="30.0" top="15.0" />
            </padding>
            <Separator />
            <HBox fx:id="cartSummaryContainer"
                  styleClass="responsive-hbox, responsive-spacing-large"
                  alignment="CENTER_RIGHT"
                  maxWidth="Infinity"
                  VBox.vgrow="NEVER">
                <Label fx:id="totalLabel"
                       text="Total: 0 VND"
                       styleClass="center-content"
                       style="-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;"
                       HBox.hgrow="ALWAYS" />
                <Button fx:id="clearCartButton"
                       text="Clear Cart"
                       styleClass="responsive-button"
                       onAction="#handleClearCartAction"
                       style="-fx-background-color: #e74c3c;"
                       HBox.hgrow="NEVER" />
                <Button fx:id="checkoutButton"
                       text="Proceed to Checkout"
                       styleClass="responsive-success-button"
                       onAction="#handleCheckoutAction"
                       HBox.hgrow="NEVER" />
            </HBox>
        </VBox>
    </bottom>
</BorderPane>
```

### 2.4 Enhanced HomeScreenController with Responsive Logic

#### Update `src/main/java/com/aims/core/presentation/controllers/HomeScreenController.java`

Add these methods to the HomeScreenController class:

```java
import com.aims.core.presentation.utils.ResponsiveLayoutManager;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

// Add these fields
private ResponsiveLayoutManager.ScreenSize currentScreenSize;
private ChangeListener<Number> widthChangeListener;

/**
 * Enhanced initialization with responsive setup
 */
@Override
public void initialize() {
    // Existing initialization code...
    
    // Setup responsive behavior
    setupResponsiveLayout();
    
    // Initialize pagination
    updatePaginationControls();
}

/**
 * Setup responsive layout behavior
 */
private void setupResponsiveLayout() {
    // Wait for scene to be available
    javafx.application.Platform.runLater(() -> {
        if (homeScreenPane.getScene() != null) {
            // Add width change listener for responsive behavior
            widthChangeListener = new ChangeListener<Number>() {
                @Override
                public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                    updateResponsiveLayout(newValue.doubleValue());
                }
            };
            
            homeScreenPane.getScene().widthProperty().addListener(widthChangeListener);
            
            // Initial responsive setup
            updateResponsiveLayout(homeScreenPane.getScene().getWidth());
            
            System.out.println("HomeScreenController: Responsive layout setup completed");
        }
    });
}

/**
 * Update layout based on current screen width
 */
private void updateResponsiveLayout(double width) {
    ResponsiveLayoutManager.ScreenSize newScreenSize = ResponsiveLayoutManager.detectScreenSize(width);
    
    if (currentScreenSize != newScreenSize) {
        currentScreenSize = newScreenSize;
        
        // Remove existing responsive classes
        homeScreenPane.getStyleClass().removeIf(styleClass -> 
            styleClass.startsWith("responsive-desktop-") || styleClass.equals("responsive-mobile"));
        
        // Apply new responsive class
        String responsiveClass = ResponsiveLayoutManager.getResponsiveStyleClass(newScreenSize);
        homeScreenPane.getStyleClass().add(responsiveClass);
        
        // Update product grid layout
        updateProductGridLayout();
        
        System.out.println("HomeScreenController: Updated responsive layout for " + newScreenSize + " (width: " + width + ")");
    }
}

/**
 * Update product grid layout based on screen size
 */
private void updateProductGridLayout() {
    if (productFlowPane != null && currentScreenSize != null) {
        double containerWidth = productFlowPane.getWidth();
        if (containerWidth > 0) {
            // Calculate optimal product card size and spacing based on screen size
            double cardWidth, spacing;
            
            switch (currentScreenSize) {
                case LARGE_DESKTOP:
                    cardWidth = 320;
                    spacing = 30;
                    productFlowPane.setHgap(30);
                    productFlowPane.setVgap(30);
                    break;
                case STANDARD_DESKTOP:
                    cardWidth = 300;
                    spacing = 25;
                    productFlowPane.setHgap(25);
                    productFlowPane.setVgap(25);
                    break;
                case COMPACT_DESKTOP:
                    cardWidth = 280;
                    spacing = 20;
                    productFlowPane.setHgap(20);
                    productFlowPane.setVgap(20);
                    break;
                case MOBILE:
                    cardWidth = 250;
                    spacing = 15;
                    productFlowPane.setHgap(15);
                    productFlowPane.setVgap(15);
                    break;
                default:
                    cardWidth = 300;
                    spacing = 25;
                    productFlowPane.setHgap(25);
                    productFlowPane.setVgap(25);
            }
            
            // Calculate optimal columns
            int optimalColumns = ResponsiveLayoutManager.getOptimalProductColumns(containerWidth, cardWidth, spacing);
            
            System.out.println("HomeScreenController: Optimal layout - Width: " + containerWidth + 
                             ", Card Width: " + cardWidth + ", Columns: " + optimalColumns);
        }
    }
}

/**
 * Enhanced completeInitialization
 */
@Override
public void completeInitialization() {
    System.out.println("HomeScreenController.completeInitialization: Starting...");
    
    try {
        // Load initial products
        loadProducts();
        
        // Setup responsive layout after products are loaded
        javafx.application.Platform.runLater(() -> {
            setupResponsiveLayout();
            updateResponsiveLayout(homeScreenPane.getScene() != null ? homeScreenPane.getScene().getWidth() : 1200);
        });
        
        System.out.println("HomeScreenController.completeInitialization: Completed successfully");
    } catch (Exception e) {
        System.err.println("HomeScreenController.completeInitialization: Error - " + e.getMessage());
        e.printStackTrace();
    }
}
```

## Phase 3: Cross-Device Compatibility and Advanced Features

### 3.1 Enhanced CSS with Advanced Responsive Rules

#### Add to `src/main/resources/styles/responsive.css` (append to existing file):

```css
/* =========================================================================
   PHASE 3: ADVANCED RESPONSIVE FEATURES
   ========================================================================= */

/* DPI and Resolution Awareness */
@media screen and (-fx-screen-dpi: 192dpi) {
    .root {
        -fx-font-size: 16px;
    }
    
    .responsive-product-card {
        -fx-min-width: 280;
        -fx-max-width: 360;
    }
}

@media screen and (-fx-screen-dpi: 144dpi) {
    .root {
        -fx-font-size: 15px;
    }
    
    .responsive-product-card {
        -fx-min-width: 260;
        -fx-max-width: 340;
    }
}

/* Ultra-wide screen support */
.responsive-ultrawide {
    -fx-spacing: 40;
    -fx-padding: 40 60 40 60;
}

.responsive-ultrawide .responsive-product-grid {
    -fx-hgap: 35;
    -fx-vgap: 35;
    -fx-padding: 35 50 35 50;
}

.responsive-ultrawide .responsive-product-card {
    -fx-min-width: 300;
    -fx-max-width: 360;
}

/* High contrast mode support */
.high-contrast-mode {
    -fx-background-color: #000000;
    -fx-text-fill: #ffffff;
}

.high-contrast-mode .responsive-product-card {
    -fx-background-color: #1a1a1a;
    -fx-border-color: #ffffff;
    -fx-border-width: 2;
}

.high-contrast-mode .responsive-search-field {
    -fx-background-color: #1a1a1a;
    -fx-text-fill: #ffffff;
    -fx-border-color: #ffffff;
}

/* Accessibility enhancements */
.large-text-mode {
    -fx-font-size: 18px;
}

.large-text-mode .responsive-product-card {
    -fx-min-height: 380;
    -fx-spacing: 15;
}

.large-text-mode .responsive-search-field {
    -fx-font-size: 16px;
    -fx-pref-height: 45;
}

/* Smooth transitions for responsive changes */
.smooth-responsive-transition {
    -fx-transition: all 0.3s ease-in-out;
}

/* Print-friendly styles */
.print-mode {
    -fx-background-color: #ffffff;
    -fx-text-fill: #000000;
    -fx-border-color: #000000;
    -fx-effect: none;
}

.print-mode .responsive-product-card {
    -fx-background-color: #ffffff;
    -fx-border-color: #000000;
    -fx-border-width: 1;
}

/* Responsive image handling */
.responsive-product-image {
    -fx-smooth: true;
    -fx-cache: true;
}

.responsive-desktop-large .responsive-product-image {
    -fx-fit-height: 200;
    -fx-fit-width: 180;
}

.responsive-desktop-standard .responsive-product-image {
    -fx-fit-height: 180;
    -fx-fit-width: 160;
}

.responsive-desktop-compact .responsive-product-image {
    -fx-fit-height: 160;
    -fx-fit-width: 140;
}

.responsive-mobile .responsive-product-image {
    -fx-fit-height: 140;
    -fx-fit-width: 120;
}

/* Responsive text areas and input fields */
.responsive-text-area {
    -fx-background-color: white;
    -fx-border-color: #bdc3c7;
    -fx-border-width: 1;
    -fx-border-radius: 4;
    -fx-background-radius: 4;
    -fx-font-size: 14px;
    -fx-padding: 10;
}

.responsive-text-area:focused {
    -fx-border-color: #3498db;
    -fx-border-width: 2;
}

/* Responsive grid layouts */
.responsive-grid {
    -fx-hgap: 15;
    -fx-vgap: 10;
    -fx-padding: 10;
}

.responsive-desktop-large .responsive-grid {
    -fx-hgap: 20;
    -fx-vgap: 12;
}

.responsive-mobile .responsive-grid {
    -fx-hgap: 10;
    -fx-vgap: 8;
}

/* Responsive error and status messages */
.responsive-error-message {
    -fx-text-fill: #e74c3c;
    -fx-font-size: 14px;
    -fx-padding: 10;
    -fx-background-color: #fadbd8;
    -fx-border-color: #e74c3c;
    -fx-border-width: 1;
    -fx-border-radius: 4;
    -fx-background-radius: 4;
    -fx-wrap-text: true;
}

.responsive-success-message {
    -fx-text-fill: #27ae60;
    -fx-font-size: 14px;
    -fx-padding: 10;
    -fx-background-color: #d5f4e6;
    -fx-border-color: #27ae60;
    -fx-border-width: 1;
    -fx-border-radius: 4;
    -fx-background-radius: 4;
    -fx-wrap-text: true;
}

/* Responsive section titles */
.responsive-section-title {
    -fx-font-weight: bold;
    -fx-text-fill: #2c3e50;
    -fx-padding: 5 0 10 0;
}

.responsive-desktop-large .responsive-section-title {
    -fx-font-size: 18px;
}

.responsive-desktop-standard .responsive-section-title {
    -fx-font-size: 16px;
}

.responsive-desktop-compact .responsive-section-title {
    -fx-font-size: 15px;
}

.responsive-mobile .responsive-section-title {
    -fx-font-size: 14px;
}

/* Responsive text content */
.responsive-text {
    -fx-font-size: 14px;
    -fx-text-fill: #2c3e50;
    -fx-wrap-text: true;
}

.responsive-desktop-large .responsive-text {
    -fx-font-size: 15px;
}

.responsive-mobile .responsive-text {
    -fx-font-size: 13px;
}
```

### 3.2 Advanced Screen Detection and DPI Handling

#### Create `src/main/java/com/aims/core/presentation/utils/ScreenDetectionService.java`:

```java
package com.aims.core.presentation.utils;

import javafx.stage.Screen;
import javafx.geometry.Rectangle2D;
import java.util.List;

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
        
        public double getScaleFactor() {
            if (dpi >= 192) return 1.5; // High DPI
            if (dpi >= 144) return 1.25; // Medium DPI
            return 1.0; // Standard DPI
        }
        
        public boolean isHighDPI() {
            return dpi >= 144;
        }
        
        public boolean isUltraWide() {
            return width / height >= 2.3; // Typical ultra-wide ratio
        }
        
        @Override
        public String toString() {
            return String.format("ScreenInfo{width=%.0f, height=%.0f, dpi=%.0f, category=%s, primary=%b}", 
                               width, height, dpi, sizeCategory, isPrimary);
        }
    }
    
    public static ScreenInfo getPrimaryScreenInfo() {
        Screen primaryScreen = Screen.getPrimary();
        Rectangle2D bounds = primaryScreen.getBounds();
        double dpi = primaryScreen.getDpi();
        
        return new ScreenInfo(bounds.getWidth(), bounds.getHeight(), dpi, true);
    }
    
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
                .collect(java.util.stream.Collectors.toList());
    }
    
    public static String getOptimalCSSClass(ScreenInfo screenInfo) {
        if (screenInfo.isUltraWide()) {
            return "responsive-ultrawide";
        }
        return ResponsiveLayoutManager.getResponsiveStyleClass(screenInfo.getSizeCategory());
    }
    
    public static double getOptimalFontSize(ScreenInfo screenInfo, double baseFontSize) {
        double scaledSize = ResponsiveLayoutManager.getOptimalFontSize(screenInfo.getSizeCategory(), baseFontSize);
        return scaledSize * screenInfo.getScaleFactor();
    }
}
```

### 3.3 Enhanced Application Configuration for Multi-Screen Support

#### Update `src/main/java/com/aims/AimsApp.java` with advanced screen detection:

```java
// Add these imports
import com.aims.core.presentation.utils.ScreenDetectionService;
import com.aims.core.presentation.utils.ResponsiveLayoutManager;

// Add this method to the AimsApp class
private void configureForScreen(Stage primaryStage, Scene scene) {
    ScreenDetectionService.ScreenInfo screenInfo = ScreenDetectionService.getPrimaryScreenInfo();
    
    System.out.println("Detected screen: " + screenInfo);
    
    // Apply screen-specific configurations
    if (screenInfo.isHighDPI()) {
        scene.getStylesheets().add(getClass().getResource("/styles/high-dpi.css").toExternalForm());
        System.out.println("Applied high DPI styles");
    }
    
    if (screenInfo.isUltraWide()) {
        scene.getRoot().getStyleClass().add("responsive-ultrawide");
        System.out.println("Applied ultra-wide layout");
    }
    
    // Configure stage size based on screen
    double optimalWidth = Math.min(screenInfo.getWidth() * 0.9, 1800);
    double optimalHeight = Math.min(screenInfo.getHeight() * 0.9, 1200);
    
    primaryStage.setWidth(optimalWidth);
    primaryStage.setHeight(optimalHeight);
    
    // Center on screen
    primaryStage.setX((screenInfo.getWidth() - optimalWidth) / 2);
    primaryStage.setY((screenInfo.getHeight() - optimalHeight) / 2);
    
    System.out.println("Configured window size: " + optimalWidth + "x" + optimalHeight);
}

// Update the start method to use this configuration
@Override
public void start(Stage primaryStage) {
    sceneManager.setPrimaryStage(primaryStage);

    try {
        FXMLLoader loader = sceneManager.getLoader(FXMLPaths.MAIN_LAYOUT);
        Parent root = loader.load();

        this.mainLayoutController = loader.getController();
        if (mainLayoutController == null) {
            System.err.println("CRITICAL: MainLayoutController is null");
            showErrorDialog("Application Startup Error", "Cannot load main application layout controller.");
            return;
        }

        Scene scene = new Scene(root, MIN_WINDOW_WIDTH, MIN_WINDOW_HEIGHT);

        // Load CSS stylesheets
        loadStylesheets(scene);
        
        // Configure for current screen
        configureForScreen(primaryStage, scene);

        primaryStage.setTitle(APP_TITLE);
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(MIN_WINDOW_WIDTH);
        primaryStage.setMinHeight(MIN_WINDOW_HEIGHT);
        
        // Dependency injection
        sceneManager.setMainLayoutController(this.mainLayoutController);
        mainLayoutController.setAuthenticationService(serviceFactory.getAuthenticationService());
        mainLayoutController.setSceneManager(sceneManager);
        mainLayoutController.setServiceFactory(serviceFactory);
        sceneManager.setServiceFactory(serviceFactory);
        
        primaryStage.show();
        mainLayoutController.completeInitialization();

    } catch (Exception e) {
        e.printStackTrace();
        showErrorDialog("Application Startup Error", "An error occurred: " + e.getMessage());
    }
}

private void loadStylesheets(Scene scene) {
    String[] stylesheets = {
        "/styles/responsive.css",
        "/styles/global.css", 
        "/styles/theme.css"
    };
    
    for (String stylesheet : stylesheets) {
        try {
            if (getClass().getResource(stylesheet) != null) {
                scene.getStylesheets().add(getClass().getResource(stylesheet).toExternalForm());
                System.out.println("Loaded stylesheet: " + stylesheet);
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not load stylesheet " + stylesheet + ": " + e.getMessage());
        }
    }
}
```

## Phase 2 & 3 Implementation Summary

### New Files to Create:

1. **`src/main/java/com/aims/core/presentation/utils/ResponsiveLayoutManager.java`** - Screen size detection and responsive utilities
2. **`src/main/java/com/aims/core/presentation/utils/ScreenDetectionService.java`** - Advanced screen and DPI detection

### Files to Modify:

1. **`src/main/resources/com/aims/presentation/views/product_detail_screen.fxml`** - Enhanced responsive layout
2. **`src/main/resources/com/aims/presentation/views/cart_screen.fxml`** - Responsive cart interface
3. **`src/main/java/com/aims/core/presentation/controllers/HomeScreenController.java`** - Dynamic responsive behavior
4. **`src/main/java/com/aims/AimsApp.java`** - Advanced screen detection and configuration
5. **`src/main/resources/styles/responsive.css`** - Append Phase 3 advanced CSS rules

### Expected Results After Phase 2 & 3:

✅ **Dynamic responsive behavior** - Layout automatically adapts as window is resized
✅ **Cross-device compatibility** - Optimized for different screen sizes and DPI settings
✅ **Advanced screen detection** - Automatic detection of ultra-wide, high-DPI displays
✅ **Enhanced accessibility** - Support for high contrast and large text modes
✅ **Performance optimization** - Efficient layout calculations and memory management
✅ **Professional appearance** - Modern, polished user interface across all screen sizes

### Final Testing Checklist:

- [ ] Test on different screen resolutions (1024x768 to 4K+)
- [ ] Test window resizing behavior
- [ ] Test on high-DPI displays
- [ ] Test ultra-wide screen support
- [ ] Verify product grid responsiveness
- [ ] Test search interface scaling
- [ ] Verify product detail screen layout
- [ ] Test cart screen responsiveness
- [ ] Check accessibility features
- [ ] Performance testing with large product lists

This completes the comprehensive responsive design implementation for the AIMS application!
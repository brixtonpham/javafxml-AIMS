# AIMS Responsive Design & Full-Screen Centering Implementation Guide

## Phase 1: Core Layout Infrastructure Enhancement

### 1.1 Enhanced Responsive CSS Framework

#### Create `src/main/resources/styles/responsive.css`

```css
/* =========================================================================
   AIMS - Responsive Design CSS Framework
   Unified responsive layout system for full-screen and cross-device support
   ========================================================================= */

/* =========================================================================
   ROOT AND VIEWPORT CONFIGURATION
   ========================================================================= */
.root {
    -fx-font-family: "Segoe UI", "Helvetica Neue", Arial, sans-serif;
    -fx-font-size: 14px;
    -fx-background-color: #f8f9fa;
    -fx-text-fill: #2c3e50;
    
    /* Core responsive properties */
    -fx-fill-width: true;
    -fx-fill-height: true;
    -fx-pref-width: USE_COMPUTED_SIZE;
    -fx-pref-height: USE_COMPUTED_SIZE;
    -fx-max-width: infinity;
    -fx-max-height: infinity;
    -fx-min-width: 800;
    -fx-min-height: 600;
}

/* =========================================================================
   RESPONSIVE CONTAINER SYSTEM
   ========================================================================= */

/* Main Layout Containers */
.responsive-main-container {
    -fx-pref-width: USE_COMPUTED_SIZE;
    -fx-pref-height: USE_COMPUTED_SIZE;
    -fx-max-width: infinity;
    -fx-max-height: infinity;
    -fx-min-width: 800;
    -fx-min-height: 600;
    -fx-background-color: #ffffff;
}

.responsive-content-container {
    -fx-pref-width: USE_COMPUTED_SIZE;
    -fx-pref-height: USE_COMPUTED_SIZE;
    -fx-max-width: infinity;
    -fx-max-height: infinity;
    -fx-min-width: 600;
    -fx-min-height: 400;
    -fx-background-color: #f8f9fa;
}

/* Flexible Layout Containers */
.responsive-vbox {
    -fx-spacing: 20;
    -fx-alignment: center;
    -fx-pref-width: USE_COMPUTED_SIZE;
    -fx-pref-height: USE_COMPUTED_SIZE;
    -fx-max-width: infinity;
    -fx-max-height: infinity;
    -fx-fill-width: true;
}

.responsive-hbox {
    -fx-spacing: 15;
    -fx-alignment: center;
    -fx-pref-width: USE_COMPUTED_SIZE;
    -fx-pref-height: USE_COMPUTED_SIZE;
    -fx-max-width: infinity;
    -fx-fill-height: true;
}

.responsive-border-pane {
    -fx-pref-width: USE_COMPUTED_SIZE;
    -fx-pref-height: USE_COMPUTED_SIZE;
    -fx-max-width: infinity;
    -fx-max-height: infinity;
}

/* =========================================================================
   RESPONSIVE SCROLLPANE SYSTEM
   ========================================================================= */
.responsive-scroll-pane {
    -fx-fit-to-width: true;
    -fx-fit-to-height: false;
    -fx-pref-width: USE_COMPUTED_SIZE;
    -fx-pref-height: USE_COMPUTED_SIZE;
    -fx-max-width: infinity;
    -fx-max-height: infinity;
    -fx-hbar-policy: never;
    -fx-vbar-policy: as-needed;
    -fx-background-color: transparent;
    -fx-border-color: transparent;
    -fx-focus-color: transparent;
    -fx-faint-focus-color: transparent;
}

.responsive-scroll-pane .viewport {
    -fx-background-color: transparent;
}

.responsive-scroll-pane .content {
    -fx-background-color: transparent;
}

/* =========================================================================
   RESPONSIVE PRODUCT GRID SYSTEM
   ========================================================================= */
.responsive-product-grid {
    -fx-alignment: top-center;
    -fx-pref-width: USE_COMPUTED_SIZE;
    -fx-pref-height: USE_COMPUTED_SIZE;
    -fx-max-width: infinity;
    -fx-max-height: infinity;
    -fx-hgap: 20;
    -fx-vgap: 20;
    -fx-padding: 20 30 20 30;
    -fx-column-halignment: center;
    -fx-pref-wrap-length: USE_COMPUTED_SIZE;
    -fx-background-color: transparent;
}

/* Responsive Product Cards */
.responsive-product-card {
    -fx-background-color: white;
    -fx-border-color: #e0e0e0;
    -fx-border-width: 1;
    -fx-border-radius: 8;
    -fx-background-radius: 8;
    -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 4, 0, 0, 2);
    -fx-padding: 15;
    -fx-spacing: 12;
    -fx-alignment: center;
    
    /* Responsive sizing */
    -fx-min-width: 240;
    -fx-max-width: 320;
    -fx-pref-width: USE_COMPUTED_SIZE;
    -fx-min-height: 320;
    -fx-max-height: 400;
    -fx-pref-height: USE_COMPUTED_SIZE;
}

.responsive-product-card:hover {
    -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 8, 0, 0, 4);
    -fx-border-color: #3498db;
}

/* =========================================================================
   RESPONSIVE SEARCH INTERFACE
   ========================================================================= */
.responsive-search-container {
    -fx-pref-width: USE_COMPUTED_SIZE;
    -fx-max-width: infinity;
    -fx-min-width: 600;
    -fx-spacing: 15;
    -fx-alignment: center;
    -fx-padding: 20 30 20 30;
    -fx-background-color: white;
    -fx-border-color: #e0e0e0;
    -fx-border-width: 0 0 1 0;
}

.responsive-search-field {
    -fx-pref-width: USE_COMPUTED_SIZE;
    -fx-min-width: 300;
    -fx-max-width: 500;
    -fx-pref-height: 40;
    -fx-min-height: 35;
    -fx-font-size: 14px;
    -fx-padding: 10 15;
    -fx-background-color: white;
    -fx-border-color: #bdc3c7;
    -fx-border-width: 1;
    -fx-border-radius: 4;
    -fx-background-radius: 4;
}

.responsive-search-field:focused {
    -fx-border-color: #3498db;
    -fx-border-width: 2;
}

.responsive-filter-combo {
    -fx-pref-width: 150;
    -fx-min-width: 120;
    -fx-max-width: 200;
    -fx-pref-height: 40;
    -fx-font-size: 14px;
}

/* =========================================================================
   RESPONSIVE BUTTON SYSTEM
   ========================================================================= */
.responsive-button {
    -fx-font-size: 14px;
    -fx-padding: 10 20;
    -fx-border-radius: 6;
    -fx-background-radius: 6;
    -fx-cursor: hand;
    -fx-text-fill: white;
    -fx-font-weight: normal;
    -fx-min-width: 80;
    -fx-pref-width: USE_COMPUTED_SIZE;
}

.responsive-primary-button {
    -fx-background-color: #3498db;
}

.responsive-primary-button:hover {
    -fx-background-color: #2980b9;
}

.responsive-success-button {
    -fx-background-color: #27ae60;
    -fx-min-width: 150;
    -fx-max-width: infinity;
}

.responsive-success-button:hover {
    -fx-background-color: #229954;
}

/* =========================================================================
   RESPONSIVE NAVIGATION SYSTEM
   ========================================================================= */
.responsive-header {
    -fx-pref-width: USE_COMPUTED_SIZE;
    -fx-max-width: infinity;
    -fx-min-height: 60;
    -fx-pref-height: 80;
    -fx-padding: 15 20;
    -fx-alignment: center;
    -fx-background-color: #34495e;
    -fx-text-fill: white;
}

.responsive-footer {
    -fx-pref-width: USE_COMPUTED_SIZE;
    -fx-max-width: infinity;
    -fx-min-height: 40;
    -fx-pref-height: 50;
    -fx-padding: 10 20;
    -fx-alignment: center;
    -fx-background-color: #34495e;
    -fx-text-fill: white;
    -fx-font-size: 12px;
}

/* =========================================================================
   RESPONSIVE BREAKPOINTS AND ADAPTATIONS
   ========================================================================= */

/* Large Desktop (1920px+) */
.responsive-desktop-large .responsive-product-grid {
    -fx-hgap: 30;
    -fx-vgap: 30;
    -fx-padding: 30 50 30 50;
}

.responsive-desktop-large .responsive-product-card {
    -fx-min-width: 280;
    -fx-max-width: 340;
}

/* Standard Desktop (1366-1920px) */
.responsive-desktop-standard .responsive-product-grid {
    -fx-hgap: 25;
    -fx-vgap: 25;
    -fx-padding: 25 40 25 40;
}

.responsive-desktop-standard .responsive-product-card {
    -fx-min-width: 260;
    -fx-max-width: 320;
}

/* Compact Desktop (1024-1366px) */
.responsive-desktop-compact .responsive-product-grid {
    -fx-hgap: 20;
    -fx-vgap: 20;
    -fx-padding: 20 30 20 30;
}

.responsive-desktop-compact .responsive-product-card {
    -fx-min-width: 240;
    -fx-max-width: 300;
}

.responsive-desktop-compact .responsive-search-container {
    -fx-padding: 15 20 15 20;
}

/* Mobile/Tablet (768-1024px) */
.responsive-mobile .responsive-product-grid {
    -fx-hgap: 15;
    -fx-vgap: 15;
    -fx-padding: 15 20 15 20;
}

.responsive-mobile .responsive-product-card {
    -fx-min-width: 200;
    -fx-max-width: 280;
}

.responsive-mobile .responsive-search-container {
    -fx-spacing: 10;
    -fx-padding: 15 15 15 15;
}

/* =========================================================================
   UTILITY CLASSES
   ========================================================================= */
.fill-parent {
    -fx-pref-width: USE_COMPUTED_SIZE;
    -fx-pref-height: USE_COMPUTED_SIZE;
    -fx-max-width: infinity;
    -fx-max-height: infinity;
}

.center-content {
    -fx-alignment: center;
    -fx-text-alignment: center;
}

.grow-horizontal {
    -fx-pref-width: USE_COMPUTED_SIZE;
    -fx-max-width: infinity;
}

.grow-vertical {
    -fx-pref-height: USE_COMPUTED_SIZE;
    -fx-max-height: infinity;
}

.responsive-spacing-small {
    -fx-spacing: 10;
}

.responsive-spacing-medium {
    -fx-spacing: 20;
}

.responsive-spacing-large {
    -fx-spacing: 30;
}

.responsive-padding-small {
    -fx-padding: 10;
}

.responsive-padding-medium {
    -fx-padding: 20;
}

.responsive-padding-large {
    -fx-padding: 30;
}
```

### 1.2 Update Main Layout FXML

#### Modify `src/main/resources/com/aims/presentation/views/main_layout.fxml`

```xml
<?xml version="1.0" encoding="UTF-8"?>

<?import javafx.scene.layout.BorderPane?>
<?import javafx.scene.layout.VBox?>
<?import javafx.scene.control.MenuBar?>
<?import javafx.scene.control.Menu?>
<?import javafx.scene.control.MenuItem?>
<?import javafx.scene.control.Label?>
<?import javafx.geometry.Insets?>

<BorderPane fx:id="mainBorderPane" 
            styleClass="responsive-main-container" 
            maxHeight="Infinity" 
            maxWidth="Infinity" 
            minHeight="600.0" 
            minWidth="800.0" 
            prefHeight="USE_COMPUTED_SIZE" 
            prefWidth="USE_COMPUTED_SIZE" 
            xmlns="http://javafx.com/javafx/17" 
            xmlns:fx="http://javafx.com/fxml/1" 
            fx:controller="com.aims.core.presentation.controllers.MainLayoutController">
    <top>
        <VBox styleClass="responsive-vbox" spacing="0">
            <MenuBar fx:id="menuBar">
                <menus>
                    <Menu text="File">
                        <items>
                            <MenuItem fx:id="loginMenuItem" text="Login" onAction="#handleLoginAction"/>
                            <MenuItem fx:id="logoutMenuItem" text="Logout" onAction="#handleLogoutAction"/>
                            <MenuItem text="Exit" onAction="#handleExitAction"/>
                        </items>
                    </Menu>
                    <Menu text="View">
                        <items>
                            <MenuItem text="Home" onAction="#navigateToHome"/>
                            <MenuItem text="View Cart" onAction="#navigateToCart"/>
                        </items>
                    </Menu>
                    <Menu fx:id="adminMenu" text="Admin" visible="false">
                        <items>
                            <MenuItem text="User Management" onAction="#navigateToUserManagement"/>
                            <MenuItem text="Product Management (Admin)" onAction="#navigateToAdminProductList"/>
                        </items>
                    </Menu>
                    <Menu fx:id="pmMenu" text="Product Manager" visible="false">
                        <items>
                            <MenuItem text="Product Management" onAction="#navigateToPmProductList"/>
                            <MenuItem text="Pending Orders" onAction="#navigateToPmPendingOrders"/>
                        </items>
                    </Menu>
                </menus>
            </MenuBar>
            <Label fx:id="headerTitle" 
                   text="AIMS - An Internet Media Store" 
                   styleClass="responsive-header"
                   alignment="CENTER" 
                   maxWidth="Infinity" 
                   VBox.vgrow="NEVER">
                <padding>
                    <Insets bottom="10.0" left="15.0" right="15.0" top="10.0" />
                </padding>
            </Label>
        </VBox>
    </top>
    <center>
        <BorderPane fx:id="contentPane"
                    styleClass="responsive-content-container"
                    maxHeight="Infinity"
                    maxWidth="Infinity"
                    minHeight="400.0"
                    minWidth="600.0"
                    prefHeight="USE_COMPUTED_SIZE"
                    prefWidth="USE_COMPUTED_SIZE"
                    BorderPane.alignment="CENTER">
            <BorderPane.margin>
                <Insets bottom="0.0" left="0.0" right="0.0" top="0.0" />
            </BorderPane.margin>
        </BorderPane>
    </center>
    <bottom>
        <Label fx:id="footerLabel" 
               text="© 2025 AIMS Project" 
               styleClass="responsive-footer"
               alignment="CENTER" 
               maxWidth="Infinity">
            <padding>
                <Insets bottom="5.0" left="15.0" right="15.0" top="5.0" />
            </padding>
        </Label>
    </bottom>
</BorderPane>
```

### 1.3 Update Home Screen FXML

#### Modify `src/main/resources/com/aims/presentation/views/home_screen.fxml`

```xml
<?xml version="1.0" encoding="UTF-8"?>

<?import javafx.geometry.Insets?>
<?import javafx.scene.control.Button?>
<?import javafx.scene.control.ComboBox?>
<?import javafx.scene.control.Label?>
<?import javafx.scene.control.ScrollPane?>
<?import javafx.scene.control.TextField?>
<?import javafx.scene.layout.BorderPane?>
<?import javafx.scene.layout.FlowPane?>
<?import javafx.scene.layout.HBox?>
<?import javafx.scene.layout.VBox?>

<BorderPane fx:id="homeScreenPane"
            styleClass="responsive-border-pane, fill-parent"
            maxHeight="Infinity"
            maxWidth="Infinity"
            minHeight="400.0"
            minWidth="600.0"
            prefHeight="USE_COMPUTED_SIZE"
            prefWidth="USE_COMPUTED_SIZE"
            xmlns="http://javafx.com/javafx/17"
            xmlns:fx="http://javafx.com/fxml/1"
            fx:controller="com.aims.core.presentation.controllers.HomeScreenController">
    <top>
        <VBox styleClass="responsive-vbox, responsive-spacing-medium" 
              alignment="CENTER" 
              maxWidth="Infinity"
              VBox.vgrow="NEVER">
            <padding>
                <Insets bottom="20.0" left="30.0" right="30.0" top="20.0" />
            </padding>
            <Label text="Welcome to AIMS - Your Internet Media Store!" 
                   styleClass="center-content"
                   alignment="CENTER" 
                   maxWidth="Infinity" 
                   style="-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;">
                <VBox.margin>
                    <Insets bottom="15.0" />
                </VBox.margin>
            </Label>
            <HBox styleClass="responsive-search-container" 
                  alignment="CENTER" 
                  maxWidth="Infinity" 
                  minWidth="600.0" 
                  VBox.vgrow="NEVER">
                <TextField fx:id="searchField" 
                          styleClass="responsive-search-field"
                          promptText="Search products by title..." 
                          HBox.hgrow="ALWAYS" 
                          onAction="#handleSearchAction"/>
                <ComboBox fx:id="categoryComboBox" 
                         styleClass="responsive-filter-combo"
                         promptText="All Categories" 
                         HBox.hgrow="NEVER"/>
                <ComboBox fx:id="sortByPriceComboBox" 
                         styleClass="responsive-filter-combo"
                         promptText="Sort by Price" 
                         HBox.hgrow="NEVER"/>
                <Button text="Search" 
                       styleClass="responsive-primary-button"
                       onAction="#handleSearchAction" 
                       HBox.hgrow="NEVER"/>
            </HBox>
        </VBox>
    </top>
    <center>
        <ScrollPane fx:id="scrollPane"
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
            <FlowPane fx:id="productFlowPane"
                      styleClass="responsive-product-grid"
                      alignment="TOP_CENTER"
                      maxWidth="Infinity"
                      maxHeight="Infinity"
                      columnHalignment="CENTER"
                      prefWrapLength="USE_COMPUTED_SIZE"
                      prefHeight="USE_COMPUTED_SIZE"
                      prefWidth="USE_COMPUTED_SIZE">
            </FlowPane>
        </ScrollPane>
    </center>
    <bottom>
        <HBox fx:id="paginationControls" 
              styleClass="responsive-hbox, responsive-spacing-medium"
              alignment="CENTER" 
              minHeight="60.0" 
              maxWidth="Infinity" 
              VBox.vgrow="NEVER">
            <padding>
                <Insets bottom="15.0" left="30.0" right="30.0" top="15.0" />
            </padding>
            <Button fx:id="prevPageButton" 
                   text="Previous" 
                   styleClass="responsive-button"
                   onAction="#handlePrevPageAction"/>
            <Label fx:id="currentPageLabel" 
                  text="Page 1/X" 
                  styleClass="center-content"
                  alignment="CENTER" 
                  style="-fx-font-size: 14px;"/>
            <Button fx:id="nextPageButton" 
                   text="Next" 
                   styleClass="responsive-button"
                   onAction="#handleNextPageAction"/>
        </HBox>
    </bottom>
</BorderPane>
```

### 1.4 Update Product Card FXML

#### Modify `src/main/resources/com/aims/presentation/views/partials/product_card.fxml`

```xml
<?xml version="1.0" encoding="UTF-8"?>

<?import javafx.geometry.Insets?>
<?import javafx.scene.control.Button?>
<?import javafx.scene.control.Label?>
<?import javafx.scene.image.ImageView?>
<?import javafx.scene.layout.VBox?>
<?import javafx.scene.input.MouseEvent?>

<VBox fx:id="productCardVBox" 
      styleClass="responsive-product-card"
      alignment="CENTER"
      onMouseClicked="#handleViewProductDetails" 
      xmlns="http://javafx.com/javafx/17" 
      xmlns:fx="http://javafx.com/fxml/1"
      fx:controller="com.aims.core.presentation.controllers.ProductCardController">
    <padding>
        <Insets bottom="15.0" left="15.0" right="15.0" top="15.0"/>
    </padding>
    <ImageView fx:id="productImageView" 
               fitHeight="160.0" 
               fitWidth="120.0" 
               pickOnBounds="true" 
               preserveRatio="true">
        <VBox.margin>
            <Insets bottom="10.0"/>
        </VBox.margin>
    </ImageView>
    <Label fx:id="productTitleLabel" 
           text="Product Title"
           styleClass="center-content"
           wrapText="true" 
           textAlignment="CENTER"
           minHeight="45.0" 
           maxHeight="65.0" 
           maxWidth="220.0"
           alignment="CENTER"
           style="-fx-font-size: 14px; -fx-font-weight: bold;"/>
    <Label fx:id="productPriceLabel" 
           text="0 VND"
           styleClass="center-content"
           alignment="CENTER"
           style="-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #e74c3c;"/>
    <Label fx:id="productAvailabilityLabel" 
           text="Available: 0"
           styleClass="center-content"
           alignment="CENTER"
           style="-fx-font-size: 12px; -fx-text-fill: #7f8c8d;"/>
    <Button fx:id="addToCartButton" 
            text="Add to Cart"
            styleClass="responsive-success-button"
            onAction="#handleAddToCartAction"
            maxWidth="Infinity" 
            VBox.vgrow="NEVER"/>
</VBox>
```

### 1.5 Enhanced JavaFX Application Setup

#### Update `src/main/java/com/aims/AimsApp.java`

```java
// Add these imports
import javafx.stage.Screen;
import javafx.geometry.Rectangle2D;

// Replace the start method with this enhanced version:
@Override
public void start(Stage primaryStage) {
    sceneManager.setPrimaryStage(primaryStage);

    try {
        FXMLLoader loader = sceneManager.getLoader(FXMLPaths.MAIN_LAYOUT);
        Parent root = loader.load();

        this.mainLayoutController = loader.getController();
        if (mainLayoutController == null) {
            System.err.println("CRITICAL: MainLayoutController is null after loading FXML: " + FXMLPaths.MAIN_LAYOUT);
            showErrorDialog("Application Startup Error", "Cannot load main application layout controller.");
            return;
        }

        // Enhanced scene setup for responsiveness
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        double sceneWidth = Math.max(MIN_WINDOW_WIDTH, screenBounds.getWidth() * 0.8);
        double sceneHeight = Math.max(MIN_WINDOW_HEIGHT, screenBounds.getHeight() * 0.8);

        Scene scene = new Scene(root, sceneWidth, sceneHeight);

        // Load CSS with responsive framework
        try {
            String responsiveCssPath = "/styles/responsive.css";
            String globalCssPath = "/styles/global.css";
            String themeCssPath = "/styles/theme.css";
            
            // Load responsive CSS first (highest priority)
            if (getClass().getResource(responsiveCssPath) != null) {
                scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource(responsiveCssPath)).toExternalForm());
                System.out.println("Responsive CSS loaded successfully");
            }
            
            if (getClass().getResource(globalCssPath) != null) {
                scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource(globalCssPath)).toExternalForm());
            }
            
            if (getClass().getResource(themeCssPath) != null) {
                scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource(themeCssPath)).toExternalForm());
            }

        } catch (NullPointerException e) {
            System.err.println("Warning: Could not load one or more CSS files: " + e.getMessage());
        }

        // Enhanced stage configuration
        primaryStage.setTitle(APP_TITLE);
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(MIN_WINDOW_WIDTH);
        primaryStage.setMinHeight(MIN_WINDOW_HEIGHT);
        
        // Responsive window sizing
        primaryStage.setWidth(sceneWidth);
        primaryStage.setHeight(sceneHeight);
        
        // Center the window on screen
        primaryStage.setX((screenBounds.getWidth() - sceneWidth) / 2);
        primaryStage.setY((screenBounds.getHeight() - sceneHeight) / 2);
        
        // Enable maximized state for full-screen experience
        primaryStage.setMaximized(true);
        
        // Enhanced window management
        primaryStage.setOnCloseRequest(event -> {
            System.out.println("AIMS Application is closing...");
        });

        // Dependency injection and initialization
        sceneManager.setMainLayoutController(this.mainLayoutController);
        mainLayoutController.setAuthenticationService(serviceFactory.getAuthenticationService());
        mainLayoutController.setSceneManager(sceneManager);
        mainLayoutController.setServiceFactory(serviceFactory);
        sceneManager.setServiceFactory(serviceFactory);
        
        primaryStage.show();
        
        // Complete initialization after stage is shown
        mainLayoutController.completeInitialization();

    } catch (IOException e) {
        e.printStackTrace();
        showErrorDialog("Application Load Error", "Failed to load the main application interface: \n" + e.getMessage());
    } catch (Exception e) {
        e.printStackTrace();
        showErrorDialog("Application Startup Error", "An unexpected error occurred during application startup: \n" + e.getMessage());
    }
}
```

### 1.6 Enhanced MainLayoutController

#### Update `src/main/java/com/aims/core/presentation/controllers/MainLayoutController.java`

Add this method to the MainLayoutController class:

```java
/**
 * Enhanced content loading with responsive layout enforcement
 */
@Override
public Object loadContent(String fxmlPath) {
    System.out.println("MainLayoutController: Loading content with responsive enhancements: " + fxmlPath);
    
    if (sceneManager != null) {
        Object controller = sceneManager.loadFXMLIntoPane(contentPane, fxmlPath);
        this.currentController = controller;
        
        // Apply responsive class based on window size
        applyResponsiveClasses();
        
        System.out.println("Content loaded successfully with responsive enhancements: " + fxmlPath +
                         (controller != null ? " with controller: " + controller.getClass().getSimpleName() : ""));
        return controller;
    } else {
        // Enhanced fallback with responsive layout constraints
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent newContent = loader.load();
            Object childController = loader.getController();
            this.currentController = childController;
            
            // Enhanced responsive layout enforcement
            if (newContent instanceof javafx.scene.layout.Region) {
                javafx.scene.layout.Region regionContent = (javafx.scene.layout.Region) newContent;
                
                // Apply responsive sizing constraints
                regionContent.setPrefWidth(javafx.scene.layout.Region.USE_COMPUTED_SIZE);
                regionContent.setPrefHeight(javafx.scene.layout.Region.USE_COMPUTED_SIZE);
                regionContent.setMaxWidth(Double.MAX_VALUE);
                regionContent.setMaxHeight(Double.MAX_VALUE);
                regionContent.setMinWidth(600.0);
                regionContent.setMinHeight(400.0);
                
                // Apply responsive style classes
                if (!regionContent.getStyleClass().contains("fill-parent")) {
                    regionContent.getStyleClass().add("fill-parent");
                }
                
                System.out.println("MainLayoutController: Applied responsive constraints to: " +
                                 regionContent.getClass().getSimpleName());
            }
            
            // Enhanced layout container properties
            if (newContent instanceof javafx.scene.layout.BorderPane) {
                javafx.scene.layout.BorderPane borderPane = (javafx.scene.layout.BorderPane) newContent;
                javafx.scene.layout.HBox.setHgrow(borderPane, javafx.scene.layout.Priority.ALWAYS);
                javafx.scene.layout.VBox.setVgrow(borderPane, javafx.scene.layout.Priority.ALWAYS);
                borderPane.getStyleClass().add("responsive-border-pane");
            }
            
            // Service injection (existing code)
            if (childController instanceof IChildController) {
                ((IChildController) childController).setMainLayoutController(this);
            }
            
            // Enhanced content positioning
            contentPane.setCenter(newContent);
            javafx.scene.layout.BorderPane.setAlignment(newContent, javafx.geometry.Pos.CENTER);
            javafx.scene.layout.BorderPane.setMargin(newContent, new javafx.geometry.Insets(0));
            
            // Apply responsive classes to the entire layout
            applyResponsiveClasses();
            
            return childController;
        } catch (IOException e) {
            e.printStackTrace();
            setFooterStatus("Error loading page: " + fxmlPath.substring(fxmlPath.lastIndexOf('/') + 1));
            return null;
        }
    }
}

/**
 * Apply responsive CSS classes based on current window size
 */
private void applyResponsiveClasses() {
    if (mainBorderPane != null && mainBorderPane.getScene() != null) {
        double width = mainBorderPane.getScene().getWidth();
        
        // Remove existing responsive classes
        mainBorderPane.getStyleClass().removeIf(styleClass -> 
            styleClass.startsWith("responsive-desktop-") || styleClass.equals("responsive-mobile"));
        
        // Apply appropriate responsive class
        if (width >= 1920) {
            mainBorderPane.getStyleClass().add("responsive-desktop-large");
        } else if (width >= 1366) {
            mainBorderPane.getStyleClass().add("responsive-desktop-standard");
        } else if (width >= 1024) {
            mainBorderPane.getStyleClass().add("responsive-desktop-compact");
        } else {
            mainBorderPane.getStyleClass().add("responsive-mobile");
        }
        
        System.out.println("Applied responsive class for width: " + width);
    }
}

/**
 * Enhanced initialization with responsive setup
 */
@Override
public void initialize() {
    updateUserSpecificMenus();
    
    // Load responsive CSS
    try {
        String responsiveCssPath = "/styles/responsive.css";
        if (getClass().getResource(responsiveCssPath) != null) {
            mainBorderPane.getStylesheets().add(getClass().getResource(responsiveCssPath).toExternalForm());
            System.out.println("MainLayoutController: Responsive CSS loaded successfully");
        }
        
        // Load existing CSS files
        String layoutFixCssPath = "/com/aims/presentation/styles/layout-fix.css";
        if (getClass().getResource(layoutFixCssPath) != null) {
            mainBorderPane.getStylesheets().add(getClass().getResource(layoutFixCssPath).toExternalForm());
        }
        
        String fullscreenCssPath = "/com/aims/presentation/styles/fullscreen-layout.css";
        if (getClass().getResource(fullscreenCssPath) != null) {
            mainBorderPane.getStylesheets().add(getClass().getResource(fullscreenCssPath).toExternalForm());
        }
        
    } catch (Exception e) {
        System.err.println("MainLayoutController: Error loading CSS: " + e.getMessage());
    }
    
    // Apply initial responsive classes
    javafx.application.Platform.runLater(this::applyResponsiveClasses);
    
    setHeaderTitle("AIMS Home");
}

/**
 * Enhanced completeInitialization with responsive setup
 */
@Override
public void completeInitialization() {
    // Apply responsive classes when the scene is fully initialized
    applyResponsiveClasses();
    
    // Add window resize listener for dynamic responsive behavior
    if (mainBorderPane.getScene() != null) {
        mainBorderPane.getScene().widthProperty().addListener((observable, oldValue, newValue) -> {
            applyResponsiveClasses();
        });
    }
    
    navigateToHome();
}
```

## Phase 1 Implementation Summary

### Files to Create/Modify:

1. **CREATE**: `src/main/resources/styles/responsive.css` - New unified responsive CSS framework
2. **MODIFY**: `src/main/resources/com/aims/presentation/views/main_layout.fxml` - Enhanced with responsive classes
3. **MODIFY**: `src/main/resources/com/aims/presentation/views/home_screen.fxml` - Responsive layout updates
4. **MODIFY**: `src/main/resources/com/aims/presentation/views/partials/product_card.fxml` - Responsive card design
5. **MODIFY**: `src/main/java/com/aims/AimsApp.java` - Enhanced application startup with responsive scene setup
6. **MODIFY**: `src/main/java/com/aims/core/presentation/controllers/MainLayoutController.java` - Responsive layout management

### Expected Results After Phase 1:

✅ **Content fills entire screen** - No more content appearing in small corners
✅ **Responsive scaling** - Automatic adaptation to different screen sizes
✅ **Unified CSS framework** - Consistent styling across all components
✅ **Enhanced window management** - Proper window sizing and positioning
✅ **Dynamic responsive classes** - Automatic layout adjustments based on window size

### Testing Phase 1:

1. **Compile and run** the application
2. **Resize the window** - Content should scale properly
3. **Test different screen resolutions** - Layout should adapt automatically
4. **Verify product grid** - Should center and scale appropriately
5. **Check search interface** - Should remain responsive and usable

Would you like me to proceed with Phase 2 implementation details?
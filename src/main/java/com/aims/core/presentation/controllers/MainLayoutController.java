package com.aims.core.presentation.controllers;

import com.aims.core.application.services.IAuthenticationService; // For logout
import com.aims.core.entities.UserAccount; // To store current user
import com.aims.core.presentation.utils.FXMLSceneManager; // Your scene management utility
import com.aims.core.presentation.utils.ResponsiveLayoutManager;
import com.aims.core.presentation.utils.ScreenDetectionService;
import com.aims.core.shared.ServiceFactory;
import com.aims.core.presentation.utils.AlertHelper; // Added import
// import com.aims.shared.constants.FXMLPaths; // Your FXML paths constants

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage; // To close the application

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

public class MainLayoutController { // This could be your BaseScreenController or a specific controller for the main layout

    @FXML
    private BorderPane mainBorderPane;

    @FXML
    private BorderPane contentPane; // This is where sub-screens will be loaded

    @FXML
    private Menu adminMenu;

    @FXML
    private Menu pmMenu; // Product Manager Menu

    @FXML
    private MenuItem loginMenuItem;

    @FXML
    private MenuItem logoutMenuItem;

    @FXML
    private Label headerTitle; // Optional: For displaying current screen title

    @FXML
    private Label footerLabel; // Optional: For status messages

    // --- Service Dependencies (to be injected) ---
    private IAuthenticationService authenticationService;
    private FXMLSceneManager sceneManager; // Your utility for loading FXML
    private ServiceFactory serviceFactory; // For dependency injection

    private UserAccount currentUser;
    private String currentSessionId; // If you manage sessions with IDs
    private Object currentController; // Store the currently loaded controller
    
    // Enhanced responsive layout management
    private ResponsiveLayoutManager.ScreenSize currentScreenSize;
    private ScreenDetectionService.ScreenInfo currentScreenInfo;
    
    // Enhanced full-screen state tracking
    private boolean isFullScreenMode = false;
    private double lastKnownWidth = 0;
    private double lastKnownHeight = 0;

    public MainLayoutController() {
        // Initialize services via DI in a real app
        // For example:
        // this.authenticationService = ServiceFactory.getAuthenticationService();
        // this.sceneManager = FXMLSceneManager.getInstance(this); // Pass this controller to scene manager if needed
    }

    public void initialize() {
        updateUserSpecificMenus();
        
        // Initialize responsive layout system
        initializeResponsiveLayout();
        
        // Load responsive CSS
        loadResponsiveCSS();
        
        // Apply initial responsive classes
        javafx.application.Platform.runLater(this::setupResponsiveBehavior);
        
        setHeaderTitle("AIMS Home");
    }
    
    /**
     * Initialize the responsive layout system with screen detection
     */
    private void initializeResponsiveLayout() {
        try {
            // Get current screen information
            currentScreenInfo = ScreenDetectionService.getPrimaryScreenInfo();
            currentScreenSize = currentScreenInfo.getSizeCategory();
            
            System.out.println("MainLayoutController: Detected screen - " + currentScreenInfo);
            System.out.println("MainLayoutController: Initial screen size category - " + currentScreenSize);
        } catch (Exception e) {
            System.err.println("MainLayoutController: Error initializing responsive layout - " + e.getMessage());
            // Fallback to desktop if detection fails
            currentScreenSize = ResponsiveLayoutManager.ScreenSize.DESKTOP;
        }
    }
    
    /**
     * Load all responsive CSS frameworks
     */
    private void loadResponsiveCSS() {
        try {
            // Load primary responsive CSS framework
            String responsiveCssPath = "/styles/responsive.css";
            if (getClass().getResource(responsiveCssPath) != null) {
                mainBorderPane.getStylesheets().add(getClass().getResource(responsiveCssPath).toExternalForm());
                System.out.println("MainLayoutController: Primary responsive CSS loaded successfully");
            }
            
            // Load additional responsive CSS files
            String[] additionalCssFiles = {
                "/styles/product-detail-responsive.css",
                "/styles/global.css",
                "/styles/theme.css",
                "/com/aims/presentation/styles/layout-fix.css",
                "/com/aims/presentation/styles/fullscreen-layout.css"
            };
            
            for (String cssPath : additionalCssFiles) {
                try {
                    if (getClass().getResource(cssPath) != null) {
                        mainBorderPane.getStylesheets().add(getClass().getResource(cssPath).toExternalForm());
                        System.out.println("MainLayoutController: Loaded additional CSS - " + cssPath);
                    }
                } catch (Exception e) {
                    System.out.println("MainLayoutController: Could not load optional CSS - " + cssPath);
                }
            }
            
        } catch (Exception e) {
            System.err.println("MainLayoutController: Error loading responsive CSS: " + e.getMessage());
        }
    }
    
    /**
     * Enhanced responsive behavior setup with full-screen optimization
     */
    private void setupResponsiveBehavior() {
        if (mainBorderPane != null && mainBorderPane.getScene() != null) {
            System.out.println("MainLayoutController.setupResponsiveBehavior: Setting up enhanced responsive behavior");
            
            // Setup responsive behavior using ResponsiveLayoutManager
            ResponsiveLayoutManager.setupResponsiveBehavior(mainBorderPane.getScene(), mainBorderPane);
            
            // Add enhanced window resize listener for full-screen behavior
            mainBorderPane.getScene().widthProperty().addListener((observable, oldValue, newValue) -> {
                updateFullScreenResponsiveLayout(newValue.doubleValue(), mainBorderPane.getScene().getHeight());
            });
            
            mainBorderPane.getScene().heightProperty().addListener((observable, oldValue, newValue) -> {
                updateFullScreenResponsiveLayout(mainBorderPane.getScene().getWidth(), newValue.doubleValue());
            });
            
            // Apply initial full-screen responsive layout
            applyFullScreenResponsiveLayout();
            
            System.out.println("MainLayoutController.setupResponsiveBehavior: Enhanced responsive behavior setup completed");
        }
    }
    
    /**
     * Update responsive layout based on current window width
     */
    private void updateResponsiveLayout(double width) {
        ResponsiveLayoutManager.ScreenSize newScreenSize = ResponsiveLayoutManager.detectScreenSize(width);
        
        if (currentScreenSize != newScreenSize) {
            currentScreenSize = newScreenSize;
            
            // Apply responsive layout using ResponsiveLayoutManager
            ResponsiveLayoutManager.applyResponsiveLayout(mainBorderPane, width);
            
            // Apply screen-specific optimizations
            applyScreenSpecificOptimizations();
            
            System.out.println("MainLayoutController: Updated responsive layout for " + newScreenSize + " (width: " + width + ")");
        }
    }
    
    /**
     * Apply screen-specific optimizations based on current screen info
     */
    private void applyScreenSpecificOptimizations() {
        if (currentScreenInfo != null) {
            // Apply ultra-wide specific styles
            if (currentScreenInfo.isUltraWide()) {
                if (!mainBorderPane.getStyleClass().contains("responsive-ultrawide")) {
                    mainBorderPane.getStyleClass().add("responsive-ultrawide");
                }
            } else {
                mainBorderPane.getStyleClass().remove("responsive-ultrawide");
            }
            
            // Apply high-DPI specific styles
            if (currentScreenInfo.isHighDPI()) {
                if (!mainBorderPane.getStyleClass().contains("high-dpi")) {
                    mainBorderPane.getStyleClass().add("high-dpi");
                }
            } else {
                mainBorderPane.getStyleClass().remove("high-dpi");
            }
        }
    }
    
    /**
     * Enhanced completeInitialization with comprehensive full-screen responsive setup
     * Following Vietnamese guide specifications exactly
     */
    public void completeInitialization() {
        System.out.println("MainLayoutController.completeInitialization: Starting FULL-SCREEN enhanced initialization");
        
        // Configure window for optimal display
        if (mainBorderPane.getScene() != null && mainBorderPane.getScene().getWindow() instanceof Stage) {
            Stage stage = (Stage) mainBorderPane.getScene().getWindow();
            ScreenDetectionService.configureWindowForScreen(stage, mainBorderPane.getScene());
        }
        
        // Setup enhanced responsive behavior with full-screen optimization
        setupResponsiveBehavior();
        
        // Apply comprehensive full-screen responsive layout (Vietnamese guide requirement)
        applyFullScreenResponsiveLayout();
        
        // Navigate to home screen with enhanced full-screen responsive support
        navigateToHome();
        
        System.out.println("MainLayoutController.completeInitialization: FULL-SCREEN enhanced initialization completed");
    }

    /**
     * Injects the FXMLSceneManager.
     * @param sceneManager The scene manager utility.
     */
    public void setSceneManager(FXMLSceneManager sceneManager) {
        this.sceneManager = sceneManager;
        System.out.println("MainLayoutController: SceneManager injected successfully");
    }

    /**
     * Injects the ServiceFactory.
     * @param serviceFactory The service factory for dependency injection.
     */
    public void setServiceFactory(ServiceFactory serviceFactory) {
        this.serviceFactory = serviceFactory;
    }

    /**
     * Injects the AuthenticationService.
     * @param authenticationService The authentication service.
     */
    public void setAuthenticationService(IAuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }


    /**
     * Sets the currently logged-in user and updates the UI accordingly.
     * This method would be called by the LoginScreenController after a successful login.
     * @param user The authenticated user account.
     * @param sessionId The session identifier, if applicable.
     */
    public void setCurrentUser(UserAccount user, String sessionId) {
        this.currentUser = user;
        this.currentSessionId = sessionId;
        updateUserSpecificMenus();
        if (user != null) {
            // Navigate to appropriate dashboard based on role
            Set<String> roleIds = user.getRoleAssignments().stream()
                                    .map(ura -> ura.getRole().getRoleId()) // Assuming UserRoleAssignment has getRole() -> Role -> getRoleId()
                                    .collect(Collectors.toSet());
            if (roleIds.contains("ADMIN")) {
                navigateToAdminDashboard();
            } else if (roleIds.contains("PRODUCT_MANAGER")) {
                navigateToProductManagerDashboard();
            } else {
                navigateToHome(); // Fallback or regular user dashboard if exists
            }
            setFooterStatus("Logged in as: " + user.getUsername());
        } else {
            navigateToHome(); // If user is null (logout)
            setFooterStatus("Not logged in.");
        }
    }

    public UserAccount getCurrentUser() {
        return currentUser;
    }

    public String getCurrentSessionId() {
        return currentSessionId;
    }

    /**
     * Returns the currently loaded controller.
     * @return The current controller object or null if none is loaded.
     */
    public Object getCurrentController() {
        return currentController;
    }


    private void updateUserSpecificMenus() {
        if (currentUser != null) {
            loginMenuItem.setVisible(false);
            logoutMenuItem.setVisible(true);

            // Assuming UserAccount entity has a way to get roles (e.g., through UserRoleAssignment)
            // This part needs to be adapted to how you retrieve roles for a UserAccount.
            // For demonstration, assuming a method like currentUser.getRoles() returning Set<Role>
            Set<String> roleIds = currentUser.getRoleAssignments().stream()
                                       .map(ura -> ura.getRole().getRoleId())
                                       .collect(Collectors.toSet());

            adminMenu.setVisible(roleIds.contains("ADMIN")); // Assuming "ADMIN" is the roleId for Admin

            pmMenu.setVisible(roleIds.contains("PRODUCT_MANAGER") || roleIds.contains("ADMIN")); // Admin can also see PM menu

        } else {
            loginMenuItem.setVisible(true);
            logoutMenuItem.setVisible(false);
            adminMenu.setVisible(false);
            pmMenu.setVisible(false);
        }
    }

    /**
     * Loads content with navigation history support.
     * This method delegates to FXMLSceneManager for history-aware navigation.
     *
     * @param fxmlPath The path to the FXML file
     * @param title The title for the screen (used in navigation context)
     * @return The loaded controller object
     */
    public Object loadContentWithHistory(String fxmlPath, String title) {
        System.out.println("MainLayoutController.loadContentWithHistory: Loading " + fxmlPath + " with title: " + title);
        
        if (sceneManager != null) {
            // Use FXMLSceneManager's history-aware loading
            Object controller = sceneManager.loadContentWithHistory(contentPane, fxmlPath, title);
            this.currentController = controller; // Store the loaded controller
            
            // Apply responsive layout after content is loaded
            applyResponsiveClassesToContent();
            
            System.out.println("MainLayoutController.loadContentWithHistory: Content loaded successfully: " + fxmlPath +
                             (controller != null ? " with controller: " + controller.getClass().getSimpleName() : ""));
            return controller;
        } else {
            System.err.println("MainLayoutController.loadContentWithHistory: SceneManager not available, falling back to regular loadContent");
            return loadContent(fxmlPath);
        }
    }

    /**
     * Loads content with navigation history support and custom navigation context.
     * This method delegates to FXMLSceneManager for history-aware navigation.
     *
     * @param fxmlPath The path to the FXML file
     * @param title The title for the screen (used in navigation context)
     * @param context Custom navigation context (for search state, etc.)
     * @return The loaded controller object
     */
    public Object loadContentWithHistory(String fxmlPath, String title, com.aims.core.presentation.utils.NavigationContext context) {
        System.out.println("MainLayoutController.loadContentWithHistory: Loading " + fxmlPath + " with title: " + title + " and custom context");
        
        if (sceneManager != null) {
            // Use FXMLSceneManager's history-aware loading with custom context
            Object controller = sceneManager.loadContentWithHistory(contentPane, fxmlPath, title, context);
            this.currentController = controller; // Store the loaded controller
            
            // Apply responsive layout after content is loaded
            applyResponsiveClassesToContent();
            
            System.out.println("MainLayoutController.loadContentWithHistory: Content loaded successfully: " + fxmlPath +
                             (controller != null ? " with controller: " + controller.getClass().getSimpleName() : ""));
            return controller;
        } else {
            System.err.println("MainLayoutController.loadContentWithHistory: SceneManager not available, falling back to regular loadContent");
            return loadContent(fxmlPath);
        }
    }

    /**
     * Attempts to navigate back using navigation history.
     * This method delegates to FXMLSceneManager for smart back navigation.
     *
     * @return true if navigation was successful, false otherwise
     */
    public boolean navigateBack() {
        System.out.println("MainLayoutController.navigateBack: Attempting back navigation");
        
        if (sceneManager != null) {
            boolean success = sceneManager.navigateBack();
            if (success) {
                // Apply responsive layout after navigation
                applyResponsiveClassesToContent();
                System.out.println("MainLayoutController.navigateBack: Back navigation successful");
            } else {
                System.out.println("MainLayoutController.navigateBack: Back navigation failed or no history available");
            }
            return success;
        } else {
            System.err.println("MainLayoutController.navigateBack: SceneManager not available, cannot perform back navigation");
            return false;
        }
    }

    /**
     * Returns the content pane for direct access by FXMLSceneManager.
     * This is needed for history-aware navigation.
     *
     * @return The content pane where screens are loaded
     */
    public javafx.scene.layout.BorderPane getContentPane() {
        return contentPane;
    }

    /**
     * Enhanced content loading with comprehensive responsive layout enforcement
     * Following Vietnamese guide specifications exactly
     */
    public Object loadContent(String fxmlPath) {
        System.out.println("MainLayoutController.loadContent: Loading content with FULL-SCREEN responsive framework: " + fxmlPath);
        
        if (sceneManager != null) {
            Object controller = sceneManager.loadFXMLIntoPane(contentPane, fxmlPath);
            this.currentController = controller;
            
            // Apply comprehensive responsive layout
            applyResponsiveClassesToContent();
            
            System.out.println("MainLayoutController.loadContent: Content loaded successfully with responsive enhancements: " + fxmlPath +
                             (controller != null ? " with controller: " + controller.getClass().getSimpleName() : ""));
            return controller;
        } else {
            // Enhanced fallback with comprehensive full-screen responsive layout constraints
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                Parent newContent = loader.load();
                Object childController = loader.getController();
                this.currentController = childController;
                
                // Step 1: Apply full-screen constraints enforcement (Vietnamese guide requirement)
                enforceFullScreenConstraints(newContent);
                
                // Step 2: Bind content dimensions to parent container (Vietnamese guide requirement)
                bindContentToParentSize(newContent);
                
                // Service injection (existing code)
                if (childController instanceof IChildController) {
                    ((IChildController) childController).setMainLayoutController(this);
                }
                
                // Enhanced service injection for known controllers
                performEnhancedServiceInjection(childController);

                // Enhanced content positioning with full-screen optimization
                contentPane.setCenter(newContent);
                javafx.scene.layout.BorderPane.setAlignment(newContent, javafx.geometry.Pos.CENTER);
                javafx.scene.layout.BorderPane.setMargin(newContent, new javafx.geometry.Insets(0));
                
                // Step 3: Apply comprehensive full-screen responsive layout (Vietnamese guide requirement)
                applyFullScreenResponsiveLayout();
                
                // Apply responsive classes to loaded content
                applyResponsiveClassesToContent();
                
                System.out.println("MainLayoutController.loadContent: FULL-SCREEN layout enforcement completed for: " + fxmlPath);
                
                return childController;
            } catch (IOException | RuntimeException e) { // Catch RuntimeException for controller instantiation issues
                e.printStackTrace();
                String screenName = fxmlPath.substring(fxmlPath.lastIndexOf('/') + 1);
                AlertHelper.showErrorDialog("Navigation Error", "Could not load page: " + screenName, "An error occurred while trying to display the page.", e);
                setFooterStatus("Error loading page: " + screenName);
                return null;
            }
        }
    }
    
    /**
     * Apply responsive constraints to loaded content
     */
    private void applyResponsiveConstraintsToContent(Parent content) {
        if (content instanceof javafx.scene.layout.Region) {
            javafx.scene.layout.Region regionContent = (javafx.scene.layout.Region) content;
            
            // Apply responsive sizing constraints using ResponsiveLayoutManager settings
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
            
            // Apply screen-size specific classes
            String responsiveClass = ResponsiveLayoutManager.getResponsiveStyleClass(currentScreenSize);
            if (!regionContent.getStyleClass().contains(responsiveClass)) {
                regionContent.getStyleClass().add(responsiveClass);
            }
            
            System.out.println("MainLayoutController: Applied enhanced responsive constraints to: " +
                             regionContent.getClass().getSimpleName());
        }
        
        // Enhanced layout container properties
        if (content instanceof javafx.scene.layout.BorderPane) {
            javafx.scene.layout.BorderPane borderPane = (javafx.scene.layout.BorderPane) content;
            javafx.scene.layout.HBox.setHgrow(borderPane, javafx.scene.layout.Priority.ALWAYS);
            javafx.scene.layout.VBox.setVgrow(borderPane, javafx.scene.layout.Priority.ALWAYS);
            borderPane.getStyleClass().add("responsive-border-pane");
        }
    }
    
    /**
     * Enhanced service injection with responsive setup
     */
    private void performEnhancedServiceInjection(Object childController) {
        System.out.println("MainLayoutController.performEnhancedServiceInjection: Starting enhanced service injection for " + 
                         (childController != null ? childController.getClass().getSimpleName() : "null"));
        
        if (serviceFactory != null && childController != null) {
            try {
                if (childController instanceof HomeScreenController) {
                    System.out.println("MainLayoutController: Enhanced injection for HomeScreenController");
                    HomeScreenController homeController = (HomeScreenController) childController;
                    
                    homeController.setProductService(serviceFactory.getProductService());
                    homeController.setCartService(serviceFactory.getCartService());
                    homeController.setMainLayoutController(this);
                    
                    // Note: Removed optional method calls that don't exist yet
                    // homeController.setCurrentScreenSize(currentScreenSize);
                    // homeController.setScreenInfo(currentScreenInfo);
                    
                    homeController.completeInitialization();
                    System.out.println("MainLayoutController: HomeScreenController enhanced injection completed");
                    
                } else if (childController instanceof CartScreenController) {
                    System.out.println("MainLayoutController: Enhanced injection for CartScreenController");
                    CartScreenController cartController = (CartScreenController) childController;
                    
                    cartController.setCartService(serviceFactory.getCartService());
                    cartController.setMainLayoutController(this);
                    
                    // Note: Removed optional method calls that don't exist yet
                    // cartController.setCurrentScreenSize(currentScreenSize);
                    
                    System.out.println("MainLayoutController: CartScreenController enhanced injection completed");
                    
                } else if (childController instanceof ProductDetailScreenController) {
                    System.out.println("MainLayoutController: Enhanced injection for ProductDetailScreenController");
                    ProductDetailScreenController detailController = (ProductDetailScreenController) childController;
                    
                    // Inject services immediately to ensure they're available before setProductId is called
                    detailController.setMainLayoutController(this);
                    detailController.setProductService(serviceFactory.getProductService());
                    detailController.setCartService(serviceFactory.getCartService());
                    
                    if (sceneManager != null) {
                        detailController.setSceneManager(sceneManager);
                    }
                    
                    System.out.println("MainLayoutController: ProductDetailScreenController enhanced injection completed - Services ready for product loading");
                }
                // Add more controllers as needed
                
            } catch (Exception e) {
                System.err.println("MainLayoutController.performEnhancedServiceInjection: Error in enhanced injection: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.err.println("MainLayoutController.performEnhancedServiceInjection: ServiceFactory or childController is null");
        }
    }
    
    /**
     * Apply responsive classes to content and main layout
     */
    private void applyResponsiveClassesToContent() {
        // Apply to main container
        applyResponsiveClasses();
        
        // Apply to loaded content if it exists
        if (contentPane.getCenter() instanceof javafx.scene.layout.Region) {
            javafx.scene.layout.Region content = (javafx.scene.layout.Region) contentPane.getCenter();
            ResponsiveLayoutManager.applyResponsiveLayout(content, 
                mainBorderPane.getScene() != null ? mainBorderPane.getScene().getWidth() : 1200);
        }
    }


    @FXML
    void handleLoginAction(ActionEvent event) {
        // loadContent(FXMLPaths.LOGIN_SCREEN); // Use your FXML path constants
        loadContent("/com/aims/presentation/views/login_screen.fxml");
        setHeaderTitle("User Login");
    }

    @FXML
    void handleLogoutAction(ActionEvent event) {
        if (authenticationService != null && currentSessionId != null) {
            authenticationService.logout(currentSessionId);
        }
        setCurrentUser(null, null); // Clear current user and update UI
        // AlertHelper.showInfoAlert("Logout", "You have been successfully logged out.");
        setFooterStatus("Successfully logged out.");
    }

    @FXML
    void handleExitAction(ActionEvent event) {
        Stage stage = (Stage) mainBorderPane.getScene().getWindow();
        // Optional: Show confirmation dialog before exiting
        // boolean confirmed = AlertHelper.showConfirmationDialog("Exit Application", "Are you sure you want to exit AIMS?");
        // if (confirmed) {
        //     stage.close();
        // }
        stage.close();
    }

    @FXML
    public void navigateToHome() { // Made public to be callable from initialize or other controllers
        // loadContent(FXMLPaths.HOME_SCREEN);
        loadContent("/com/aims/presentation/views/home_screen.fxml");
        setHeaderTitle("AIMS Home");
    }

    @FXML
    void navigateToCart(ActionEvent event) {
        // loadContent(FXMLPaths.CART_SCREEN);
        loadContent("/com/aims/presentation/views/cart_screen.fxml");
        setHeaderTitle("Shopping Cart");
    }

    @FXML
    void navigateToUserManagement(ActionEvent event) {
        // loadContent(FXMLPaths.ADMIN_USER_MANAGEMENT_SCREEN);
        loadContent("/com/aims/presentation/views/admin_user_management_screen.fxml");
        setHeaderTitle("User Management");
    }

    @FXML
    void navigateToAdminProductList(ActionEvent event) {
        // loadContent(FXMLPaths.ADMIN_PRODUCT_LIST_SCREEN);
        loadContent("/com/aims/presentation/views/admin_product_list_screen.fxml");
        setHeaderTitle("Product Management (Admin)");
    }

    @FXML
    void navigateToProductManagerDashboard() {
        // loadContent(FXMLPaths.PM_DASHBOARD_SCREEN);
        loadContent("/com/aims/presentation/views/pm_dashboard_screen.fxml");
        setHeaderTitle("Product Manager Dashboard");
    }
    
    @FXML
    void navigateToAdminDashboard() {
        // loadContent(FXMLPaths.ADMIN_DASHBOARD_SCREEN);
        loadContent("/com/aims/presentation/views/admin_dashboard_screen.fxml");
        setHeaderTitle("Administrator Dashboard");
    }


    @FXML
    void navigateToPmProductList(ActionEvent event) {
        // loadContent(FXMLPaths.PM_PRODUCT_LIST_SCREEN); // Or use admin product list screen
        loadContent("/com/aims/presentation/views/admin_product_list_screen.fxml"); // Assuming PM uses the same for now
        setHeaderTitle("Product Management");
    }

    @FXML
    void navigateToPmPendingOrders(ActionEvent event) {
        // loadContent(FXMLPaths.PM_PENDING_ORDERS_LIST_SCREEN);
        loadContent("/com/aims/presentation/views/pm_pending_orders_list_screen.fxml");
        setHeaderTitle("Pending Orders Review");
    }

    public void setHeaderTitle(String title) {
        if (headerTitle != null) {
            headerTitle.setText(title);
        }
    }

    public void setFooterStatus(String status) {
        if (footerLabel != null) {
            footerLabel.setText(status);
        }
    }

    /**
     * Enhanced responsive classes application using ResponsiveLayoutManager
     */
    private void applyResponsiveClasses() {
        if (mainBorderPane != null && mainBorderPane.getScene() != null) {
            double width = mainBorderPane.getScene().getWidth();
            ResponsiveLayoutManager.applyResponsiveLayout(mainBorderPane, width);
            
            // Apply screen-specific optimizations
            applyScreenSpecificOptimizations();
        }
    }
    
    /**
     * Get current screen size for child controllers
     */
    public ResponsiveLayoutManager.ScreenSize getCurrentScreenSize() {
        return currentScreenSize;
    }
    
    /**
     * Get current screen info for child controllers
     */
    public ScreenDetectionService.ScreenInfo getCurrentScreenInfo() {
        return currentScreenInfo;
    }
    
    // =========================================================================
    // ENHANCED RESPONSIVE FULL-SCREEN METHODS - PHASE 1 & 2 IMPLEMENTATION
    // =========================================================================
    
    /**
     * Enforce comprehensive full-screen constraints on loaded content
     */
    private void enforceFullScreenConstraints(Parent content) {
        System.out.println("MainLayoutController.enforceFullScreenConstraints: Applying full-screen constraints");
        
        if (content instanceof javafx.scene.layout.Region) {
            javafx.scene.layout.Region regionContent = (javafx.scene.layout.Region) content;
            
            // Apply full-screen sizing constraints
            regionContent.setPrefWidth(javafx.scene.layout.Region.USE_COMPUTED_SIZE);
            regionContent.setPrefHeight(javafx.scene.layout.Region.USE_COMPUTED_SIZE);
            regionContent.setMaxWidth(Double.MAX_VALUE);
            regionContent.setMaxHeight(Double.MAX_VALUE);
            regionContent.setMinWidth(0);
            regionContent.setMinHeight(0);
            
            // Apply full-screen style classes
            if (!regionContent.getStyleClass().contains("fullscreen-main")) {
                regionContent.getStyleClass().add("fullscreen-main");
            }
            if (!regionContent.getStyleClass().contains("responsive-fullscreen")) {
                regionContent.getStyleClass().add("responsive-fullscreen");
            }
            
            System.out.println("MainLayoutController.enforceFullScreenConstraints: Full-screen constraints applied successfully");
        }
    }
    
    /**
     * Bind loaded content size to parent container for real-time scaling
     */
    private void bindContentToParentSize(Parent content) {
        System.out.println("MainLayoutController.bindContentToParentSize: Setting up real-time size binding");
        
        if (content instanceof javafx.scene.layout.Region && contentPane != null) {
            javafx.scene.layout.Region regionContent = (javafx.scene.layout.Region) content;
            
            // Bind width and height to parent
            regionContent.prefWidthProperty().bind(contentPane.widthProperty());
            regionContent.prefHeightProperty().bind(contentPane.heightProperty());
            
            // Setup growth properties
            javafx.scene.layout.HBox.setHgrow(regionContent, javafx.scene.layout.Priority.ALWAYS);
            javafx.scene.layout.VBox.setVgrow(regionContent, javafx.scene.layout.Priority.ALWAYS);
            
            // Add real-time update listener
            contentPane.widthProperty().addListener((observable, oldValue, newValue) -> {
                updateLoadedContentResponsiveness(newValue.doubleValue(), contentPane.getHeight());
            });
            
            contentPane.heightProperty().addListener((observable, oldValue, newValue) -> {
                updateLoadedContentResponsiveness(contentPane.getWidth(), newValue.doubleValue());
            });
            
            System.out.println("MainLayoutController.bindContentToParentSize: Real-time size binding completed");
        }
    }
    
    /**
     * Apply comprehensive responsive layout with screen-specific optimizations
     */
    private void applyFullScreenResponsiveLayout() {
        System.out.println("MainLayoutController.applyFullScreenResponsiveLayout: Applying comprehensive responsive layout");
        
        if (mainBorderPane != null && mainBorderPane.getScene() != null) {
            double currentWidth = mainBorderPane.getScene().getWidth();
            double currentHeight = mainBorderPane.getScene().getHeight();
            
            // Update screen size detection
            currentScreenSize = ResponsiveLayoutManager.detectScreenSize(currentWidth);
            
            // Apply responsive layout to main container
            ResponsiveLayoutManager.applyResponsiveLayout(mainBorderPane, currentWidth);
            
            // Apply full-screen classes
            if (!mainBorderPane.getStyleClass().contains("responsive-fullscreen-container")) {
                mainBorderPane.getStyleClass().add("responsive-fullscreen-container");
            }
            
            // Apply content pane full-screen classes
            if (contentPane != null && !contentPane.getStyleClass().contains("fullscreen-content-pane")) {
                contentPane.getStyleClass().add("fullscreen-content-pane");
            }
            
            // Apply screen-specific optimizations
            applyScreenSpecificOptimizations();
            
            // Update loaded content if exists
            if (contentPane.getCenter() != null) {
                updateLoadedContentResponsiveness(currentWidth, currentHeight);
            }
            
            System.out.println("MainLayoutController.applyFullScreenResponsiveLayout: Comprehensive layout applied for " +
                             currentScreenSize + " (" + currentWidth + "x" + currentHeight + ")");
        }
    }
    
    /**
     * Update responsive layout in real-time based on window size changes
     */
    private void updateFullScreenResponsiveLayout(double width, double height) {
        System.out.println("MainLayoutController.updateFullScreenResponsiveLayout: Updating layout for " + width + "x" + height);
        
        // Check if significant size change occurred
        boolean significantChange = Math.abs(width - lastKnownWidth) > 50 || Math.abs(height - lastKnownHeight) > 50;
        
        if (significantChange) {
            lastKnownWidth = width;
            lastKnownHeight = height;
            
            // Detect new screen size
            ResponsiveLayoutManager.ScreenSize newScreenSize = ResponsiveLayoutManager.detectScreenSize(width);
            
            // Apply changes if screen size category changed
            if (currentScreenSize != newScreenSize) {
                currentScreenSize = newScreenSize;
                
                // Apply responsive layout
                ResponsiveLayoutManager.applyResponsiveLayout(mainBorderPane, width);
                
                // Update screen-specific optimizations
                applyScreenSpecificOptimizations();
                
                // Update loaded content
                updateLoadedContentResponsiveness(width, height);
                
                System.out.println("MainLayoutController.updateFullScreenResponsiveLayout: Layout updated to " + newScreenSize);
            }
        }
    }
    
    /**
     * Update loaded content responsiveness during window resize
     */
    private void updateLoadedContentResponsiveness(double width, double height) {
        if (contentPane != null && contentPane.getCenter() instanceof javafx.scene.layout.Region) {
            javafx.scene.layout.Region content = (javafx.scene.layout.Region) contentPane.getCenter();
            
            // Apply responsive layout to content
            ResponsiveLayoutManager.applyResponsiveLayout(content, width);
            
            // Ensure content maintains full-screen behavior
            enforceFullScreenConstraints(content);
            
            // Apply current screen size class
            String responsiveClass = ResponsiveLayoutManager.getResponsiveStyleClass(currentScreenSize);
            if (!content.getStyleClass().contains(responsiveClass)) {
                // Remove old responsive classes
                content.getStyleClass().removeIf(styleClass ->
                    styleClass.startsWith("responsive-desktop-") ||
                    styleClass.equals("responsive-mobile") ||
                    styleClass.equals("responsive-tablet"));
                
                // Add new responsive class
                content.getStyleClass().add(responsiveClass);
            }
        }
    }

    // Interface for child controllers to get a reference to this MainLayoutController
    public interface IChildController {
        void setMainLayoutController(MainLayoutController mainLayoutController);
    }
}
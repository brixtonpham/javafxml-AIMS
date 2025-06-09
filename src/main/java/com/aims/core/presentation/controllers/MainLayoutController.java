package com.aims.core.presentation.controllers;

import com.aims.core.application.services.IAuthenticationService; // For logout
import com.aims.core.entities.UserAccount; // To store current user
import com.aims.core.presentation.utils.FXMLSceneManager; // Your scene management utility
import com.aims.core.shared.ServiceFactory;
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

    public MainLayoutController() {
        // Initialize services via DI in a real app
        // For example:
        // this.authenticationService = ServiceFactory.getAuthenticationService();
        // this.sceneManager = FXMLSceneManager.getInstance(this); // Pass this controller to scene manager if needed
    }

    public void initialize() {
        // Set initial state
        updateUserSpecificMenus();
        
        // Load CSS stylesheets for layout fixes
        try {
            // Load existing layout-fix.css
            String cssPath = "/com/aims/presentation/styles/layout-fix.css";
            if (getClass().getResource(cssPath) != null) {
                mainBorderPane.getStylesheets().add(getClass().getResource(cssPath).toExternalForm());
                System.out.println("MainLayoutController: Layout fix CSS loaded successfully");
            } else {
                System.out.println("MainLayoutController: Layout fix CSS not found at: " + cssPath);
            }
            
            // Load new fullscreen-layout.css
            String fullscreenCssPath = "/com/aims/presentation/styles/fullscreen-layout.css";
            if (getClass().getResource(fullscreenCssPath) != null) {
                mainBorderPane.getStylesheets().add(getClass().getResource(fullscreenCssPath).toExternalForm());
                System.out.println("MainLayoutController: Fullscreen layout CSS loaded successfully");
            } else {
                System.out.println("MainLayoutController: Fullscreen layout CSS not found at: " + fullscreenCssPath);
            }
        } catch (Exception e) {
            System.err.println("MainLayoutController: Error loading CSS: " + e.getMessage());
        }
        
        // Don't load home screen here - will be loaded after dependencies are injected
        setHeaderTitle("AIMS Home");
    }
    
    /**
     * Called after all dependencies have been injected to complete initialization
     */
    public void completeInitialization() {
        navigateToHome(); // Load the home screen after dependencies are ready
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
                // Note: The current controller is managed by FXMLSceneManager internally
                // We don't need to track it here since we can't access getCurrentController
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

    public Object loadContent(String fxmlPath) {
        System.out.println("MainLayoutController: Loading content: " + fxmlPath);
        System.out.println("MainLayoutController: SceneManager available: " + (sceneManager != null));
        System.out.println("MainLayoutController: ServiceFactory available: " + (serviceFactory != null));
        
        if (sceneManager != null) {
            // Use FXMLSceneManager for proper dependency injection
            Object controller = sceneManager.loadFXMLIntoPane(contentPane, fxmlPath);
            this.currentController = controller; // Store the loaded controller
            System.out.println("Content loaded successfully: " + fxmlPath +
                             (controller != null ? " with controller: " + controller.getClass().getSimpleName() : ""));
            return controller;
        } else {
            // Fallback to direct FXMLLoader usage if sceneManager is not set up
            System.out.println("Attempting to load content: " + fxmlPath + " (Using direct FXMLLoader - SceneManager not available)");
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                Parent newContent = loader.load();
                Object childController = loader.getController();
                this.currentController = childController; // Store the loaded controller
                
                // Ensure content fills the entire contentPane with comprehensive sizing constraints
                if (newContent instanceof javafx.scene.layout.Region) {
                    javafx.scene.layout.Region regionContent = (javafx.scene.layout.Region) newContent;
                    
                    // Set preferred size to USE_COMPUTED_SIZE for dynamic sizing
                    regionContent.setPrefWidth(javafx.scene.layout.Region.USE_COMPUTED_SIZE);
                    regionContent.setPrefHeight(javafx.scene.layout.Region.USE_COMPUTED_SIZE);
                    
                    // Set maximum size to allow full expansion
                    regionContent.setMaxWidth(Double.MAX_VALUE);
                    regionContent.setMaxHeight(Double.MAX_VALUE);
                    
                    // Set minimum size to ensure proper display
                    regionContent.setMinWidth(800.0);
                    regionContent.setMinHeight(500.0);
                    
                    System.out.println("MainLayoutController.loadContent: Applied fullscreen sizing constraints to: " +
                                     regionContent.getClass().getSimpleName());
                }
                
                // Apply HBox.hgrow and VBox.vgrow properties if content is within layout containers
                if (newContent instanceof javafx.scene.layout.BorderPane) {
                    javafx.scene.layout.BorderPane borderPane = (javafx.scene.layout.BorderPane) newContent;
                    javafx.scene.layout.HBox.setHgrow(borderPane, javafx.scene.layout.Priority.ALWAYS);
                    javafx.scene.layout.VBox.setVgrow(borderPane, javafx.scene.layout.Priority.ALWAYS);
                    System.out.println("MainLayoutController.loadContent: Applied HBox.hgrow=ALWAYS and VBox.vgrow=ALWAYS to BorderPane");
                }
                
                // Pass this MainLayoutController to child controllers if they need it
                if (childController instanceof IChildController) {
                    ((IChildController) childController).setMainLayoutController(this);
                }
                
                // Manual service injection for known controllers
                System.out.println("MainLayoutController.loadContent: Starting fallback service injection for " + childController.getClass().getSimpleName());
                if (serviceFactory != null) {
                    System.out.println("MainLayoutController.loadContent: ServiceFactory is available for injection");
                    
                    if (childController instanceof HomeScreenController) {
                        System.out.println("MainLayoutController.loadContent: Fallback injection for HomeScreenController");
                        HomeScreenController homeController = (HomeScreenController) childController;
                        try {
                            homeController.setProductService(serviceFactory.getProductService());
                            System.out.println("MainLayoutController.loadContent: ProductService injected into HomeScreenController (fallback)");
                            
                            homeController.setCartService(serviceFactory.getCartService());
                            System.out.println("MainLayoutController.loadContent: CartService injected into HomeScreenController (fallback)");
                            
                            homeController.completeInitialization(); // Call after services are injected
                            System.out.println("MainLayoutController.loadContent: HomeScreenController initialization completed (fallback)");
                        } catch (Exception e) {
                            System.err.println("MainLayoutController.loadContent: Error in fallback injection for HomeScreenController: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                    else if (childController instanceof CartScreenController) {
                        System.out.println("MainLayoutController.loadContent: Fallback injection for CartScreenController");
                        CartScreenController cartController = (CartScreenController) childController;
                        try {
                            cartController.setCartService(serviceFactory.getCartService());
                            System.out.println("MainLayoutController.loadContent: CartService injected into CartScreenController (fallback)");
                        } catch (Exception e) {
                            System.err.println("MainLayoutController.loadContent: Error in fallback injection for CartScreenController: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                    else if (childController instanceof ProductDetailScreenController) {
                        System.out.println("MainLayoutController.loadContent: Injecting services into ProductDetailScreenController");
                        ProductDetailScreenController detailController = (ProductDetailScreenController) childController;
                        try {
                            // Get ServiceFactory instance
                            System.out.println("MainLayoutController.loadContent: Getting ServiceFactory instance");
                            
                            // Inject services with enhanced error handling
                            System.out.println("MainLayoutController.loadContent: About to inject MainLayoutController into ProductDetailScreenController");
                            detailController.setMainLayoutController(this);
                            System.out.println("MainLayoutController.loadContent: MainLayoutController injected successfully");
                            
                            System.out.println("MainLayoutController.loadContent: About to inject ProductService into ProductDetailScreenController");
                            detailController.setProductService(serviceFactory.getProductService());
                            System.out.println("MainLayoutController.loadContent: ProductService injected successfully");
                            
                            System.out.println("MainLayoutController.loadContent: About to inject CartService into ProductDetailScreenController");
                            detailController.setCartService(serviceFactory.getCartService());
                            System.out.println("MainLayoutController.loadContent: CartService injected successfully");
                            
                            if (sceneManager != null) {
                                System.out.println("MainLayoutController.loadContent: About to inject SceneManager into ProductDetailScreenController");
                                detailController.setSceneManager(sceneManager);
                                System.out.println("MainLayoutController.loadContent: SceneManager injected successfully");
                            } else {
                                System.out.println("MainLayoutController.loadContent: SceneManager is null, skipping injection");
                            }
                            
                            System.out.println("MainLayoutController.loadContent: All services injected successfully into ProductDetailScreenController");
                        } catch (Exception e) {
                            System.err.println("Error injecting services into ProductDetailScreenController: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                    else {
                        System.out.println("MainLayoutController.loadContent: No fallback injection logic for " + childController.getClass().getSimpleName());
                    }
                    // Add more controllers as needed
                } else {
                    System.err.println("MainLayoutController.loadContent: ServiceFactory is null, cannot perform fallback service injection");
                }

                // Use setCenter and ensure proper alignment with enhanced layout properties
                contentPane.setCenter(newContent);
                javafx.scene.layout.BorderPane.setAlignment(newContent, javafx.geometry.Pos.CENTER);
                
                // Apply BorderPane layout constraints to ensure content fills the center
                javafx.scene.layout.BorderPane.setMargin(newContent, new javafx.geometry.Insets(0));
                
                System.out.println("MainLayoutController.loadContent: Content positioned in center with BorderPane.alignment=CENTER");
                
                return childController;
            } catch (IOException e) {
                e.printStackTrace();
                setFooterStatus("Error loading page: " + fxmlPath.substring(fxmlPath.lastIndexOf('/') + 1));
                return null;
            }
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
    void navigateToHome() { // Made public to be callable from initialize or other controllers
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

    // Interface for child controllers to get a reference to this MainLayoutController
    public interface IChildController {
        void setMainLayoutController(MainLayoutController mainLayoutController);
    }
}
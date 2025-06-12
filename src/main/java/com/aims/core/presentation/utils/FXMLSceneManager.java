package com.aims.core.presentation.utils;

import com.aims.core.presentation.controllers.MainLayoutController; // Để có thể set main controller
import com.aims.core.shared.ServiceFactory;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;

public class FXMLSceneManager {

    private Stage primaryStage;
    private MainLayoutController mainLayoutController; // Nếu bạn muốn các controller con truy cập MainLayout
    private ServiceFactory serviceFactory; // For dependency injection into controllers
    
    // Navigation history support
    private NavigationHistory navigationHistory;
    private NavigationContext currentContext;

    private static FXMLSceneManager instance;

    private FXMLSceneManager() {
        this.navigationHistory = new NavigationHistory();
    }

    public static synchronized FXMLSceneManager getInstance() {
        if (instance == null) {
            instance = new FXMLSceneManager();
        }
        return instance;
    }

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    public void setMainLayoutController(MainLayoutController mainLayoutController) {
        this.mainLayoutController = mainLayoutController;
    }

    public void setServiceFactory(ServiceFactory serviceFactory) {
        this.serviceFactory = serviceFactory;
    }

    public ServiceFactory getServiceFactory() {
        return serviceFactory;
    }

    public MainLayoutController getMainLayoutController() {
        return mainLayoutController;
    }

    public FXMLLoader getLoader(String fxmlPath) {
        return new FXMLLoader(getClass().getResource(fxmlPath));
    }

    /**
     * Loads an FXML file and returns its root Parent node and its controller.
     * Use this if you need to interact with the controller before showing the scene.
     */
    public <T> LoadedFXML<T> loadFXMLWithController(String fxmlPath) throws IOException {
        System.out.println("FXMLSceneManager.loadFXMLWithController: Loading FXML from " + fxmlPath);
        
        FXMLLoader loader = getLoader(fxmlPath);
        Parent root = loader.load();
        T controller = loader.getController();
        
        System.out.println("FXMLSceneManager.loadFXMLWithController: Loaded controller: " + 
            (controller != null ? controller.getClass().getSimpleName() : "null"));
        
        // Inject MainLayoutController for child controllers
        if (controller instanceof MainLayoutController.IChildController && mainLayoutController != null) {
            System.out.println("FXMLSceneManager.loadFXMLWithController: Injecting MainLayoutController into IChildController");
            ((MainLayoutController.IChildController) controller).setMainLayoutController(mainLayoutController);
        }
        
        // Inject services using ServiceFactory
        if (serviceFactory != null) {
            System.out.println("FXMLSceneManager.loadFXMLWithController: ServiceFactory available, calling injectServices()");
            injectServices(controller);
        } else {
            System.out.println("FXMLSceneManager.loadFXMLWithController: ServiceFactory is null, skipping service injection");
        }
        
        System.out.println("FXMLSceneManager.loadFXMLWithController: Completed loading FXML for " + fxmlPath);
        return new LoadedFXML<>(root, controller);
    }
    
    /**
     * Injects services into controllers based on their type.
     * Only injects services if the controller has the appropriate setter methods.
     */
    private void injectServices(Object controller) {
        if (controller == null) {
            System.out.println("FXMLSceneManager.injectServices: Controller is null, cannot inject services");
            return;
        }
        
        if (serviceFactory == null) {
            System.err.println("FXMLSceneManager.injectServices: ServiceFactory is null, cannot inject services");
            System.err.println("FXMLSceneManager.injectServices: This is a critical error that will cause application failures");
            return;
        }
        
        System.out.println("FXMLSceneManager.injectServices: Starting service injection for " + controller.getClass().getSimpleName());
        
        // ENHANCED: Validate injection prerequisites
        boolean injectionPrerequisitesMet = validateInjectionPrerequisites(controller);
        if (!injectionPrerequisitesMet) {
            System.err.println("FXMLSceneManager.injectServices: CRITICAL - Injection prerequisites not met for " + controller.getClass().getSimpleName());
            return;
        }
        
        // Check for controllers that have confirmed setter methods
        if (controller instanceof com.aims.core.presentation.controllers.HomeScreenController) {
            System.out.println("FXMLSceneManager.injectServices: Detected HomeScreenController, injecting services...");
            com.aims.core.presentation.controllers.HomeScreenController homeController = 
                (com.aims.core.presentation.controllers.HomeScreenController) controller;
            
            try {
                // Inject ProductService with null check
                com.aims.core.application.services.IProductService productService = serviceFactory.getProductService();
                if (productService != null) {
                    homeController.setProductService(productService);
                    System.out.println("FXMLSceneManager.injectServices: ProductService injected into HomeScreenController");
                } else {
                    System.err.println("FXMLSceneManager.injectServices: ProductService is null, injection failed");
                }
                
                // Inject CartService with null check
                com.aims.core.application.services.ICartService cartService = serviceFactory.getCartService();
                if (cartService != null) {
                    homeController.setCartService(cartService);
                    System.out.println("FXMLSceneManager.injectServices: CartService injected into HomeScreenController");
                } else {
                    System.err.println("FXMLSceneManager.injectServices: CartService is null, injection failed");
                }
                
                // Complete initialization only if at least ProductService is available
                if (productService != null) {
                    homeController.completeInitialization();
                    System.out.println("FXMLSceneManager.injectServices: HomeScreenController initialization completed");
                } else {
                    System.err.println("FXMLSceneManager.injectServices: Cannot complete HomeScreenController initialization - ProductService is null");
                }
            } catch (Exception e) {
                System.err.println("FXMLSceneManager.injectServices: Error injecting services into HomeScreenController: " + e.getMessage());
                e.printStackTrace();
            }
        }
        else if (controller instanceof com.aims.core.presentation.controllers.CartScreenController) {
            System.out.println("FXMLSceneManager.injectServices: Detected CartScreenController, injecting services...");
            com.aims.core.presentation.controllers.CartScreenController cartController = 
                (com.aims.core.presentation.controllers.CartScreenController) controller;
            
            try {
                cartController.setCartService(serviceFactory.getCartService());
                System.out.println("FXMLSceneManager.injectServices: CartService injected into CartScreenController");
            } catch (Exception e) {
                System.err.println("FXMLSceneManager.injectServices: Error injecting services into CartScreenController: " + e.getMessage());
                e.printStackTrace();
            }
        }
        else if (controller instanceof com.aims.core.presentation.controllers.ProductDetailScreenController) {
            System.out.println("FXMLSceneManager.injectServices: Detected ProductDetailScreenController, injecting services...");
            com.aims.core.presentation.controllers.ProductDetailScreenController detailController = 
                (com.aims.core.presentation.controllers.ProductDetailScreenController) controller;
            
            try {
                detailController.setProductService(serviceFactory.getProductService());
                System.out.println("FXMLSceneManager.injectServices: ProductService injected into ProductDetailScreenController");
                
                detailController.setCartService(serviceFactory.getCartService());
                System.out.println("FXMLSceneManager.injectServices: CartService injected into ProductDetailScreenController");
                
                // Set additional dependencies if available
                if (mainLayoutController != null) {
                    detailController.setMainLayoutController(mainLayoutController);
                    System.out.println("FXMLSceneManager.injectServices: MainLayoutController injected into ProductDetailScreenController");
                }
                
                detailController.setSceneManager(this);
                System.out.println("FXMLSceneManager.injectServices: SceneManager injected into ProductDetailScreenController");
                
            } catch (Exception e) {
                System.err.println("FXMLSceneManager.injectServices: Error injecting services into ProductDetailScreenController: " + e.getMessage());
                e.printStackTrace();
            }
        }
        else if (controller instanceof com.aims.core.presentation.controllers.PaymentProcessingScreenController) {
            System.out.println("FXMLSceneManager.injectServices: Detected PaymentProcessingScreenController, injecting services...");
            com.aims.core.presentation.controllers.PaymentProcessingScreenController paymentController =
                (com.aims.core.presentation.controllers.PaymentProcessingScreenController) controller;
            
            try {
                paymentController.setPaymentService(serviceFactory.getPaymentService());
                System.out.println("FXMLSceneManager.injectServices: PaymentService injected into PaymentProcessingScreenController");
                
                paymentController.setOrderService(serviceFactory.getOrderService());
                System.out.println("FXMLSceneManager.injectServices: OrderService injected into PaymentProcessingScreenController");
                
                // Set additional dependencies if available
                if (mainLayoutController != null) {
                    paymentController.setMainLayoutController(mainLayoutController);
                    System.out.println("FXMLSceneManager.injectServices: MainLayoutController injected into PaymentProcessingScreenController");
                }
                
                paymentController.setSceneManager(this);
                System.out.println("FXMLSceneManager.injectServices: SceneManager injected into PaymentProcessingScreenController");
                
            } catch (Exception e) {
                System.err.println("FXMLSceneManager.injectServices: Error injecting services into PaymentProcessingScreenController: " + e.getMessage());
                e.printStackTrace();
            }
        }
        else if (controller instanceof com.aims.core.presentation.controllers.CustomerOrderDetailController) {
            System.out.println("FXMLSceneManager.injectServices: Detected CustomerOrderDetailController, injecting services...");
            com.aims.core.presentation.controllers.CustomerOrderDetailController orderDetailController =
                (com.aims.core.presentation.controllers.CustomerOrderDetailController) controller;
            
            try {
                orderDetailController.setOrderService(serviceFactory.getOrderService());
                System.out.println("FXMLSceneManager.injectServices: OrderService injected into CustomerOrderDetailController");
                
                // Set additional dependencies if available
                if (mainLayoutController != null) {
                    orderDetailController.setMainLayoutController(mainLayoutController);
                    System.out.println("FXMLSceneManager.injectServices: MainLayoutController injected into CustomerOrderDetailController");
                }
                
                orderDetailController.setSceneManager(this);
                System.out.println("FXMLSceneManager.injectServices: SceneManager injected into CustomerOrderDetailController");
                
            } catch (Exception e) {
                System.err.println("FXMLSceneManager.injectServices: Error injecting services into CustomerOrderDetailController: " + e.getMessage());
                e.printStackTrace();
            }
        }
        else if (controller instanceof com.aims.core.presentation.controllers.OrderReviewController) {
            System.out.println("FXMLSceneManager.injectServices: Detected OrderReviewController, injecting services...");
            com.aims.core.presentation.controllers.OrderReviewController orderReviewController =
                (com.aims.core.presentation.controllers.OrderReviewController) controller;
            
            try {
                orderReviewController.setOrderService(serviceFactory.getOrderService());
                System.out.println("FXMLSceneManager.injectServices: OrderService injected into OrderReviewController");
                
                // Set additional dependencies if available
                if (mainLayoutController != null) {
                    orderReviewController.setMainLayoutController(mainLayoutController);
                    System.out.println("FXMLSceneManager.injectServices: MainLayoutController injected into OrderReviewController");
                }
                
                orderReviewController.setSceneManager(this);
                System.out.println("FXMLSceneManager.injectServices: SceneManager injected into OrderReviewController");
                
            } catch (Exception e) {
                System.err.println("FXMLSceneManager.injectServices: Error injecting services into OrderReviewController: " + e.getMessage());
                e.printStackTrace();
            }
        }
        else if (controller instanceof com.aims.core.presentation.controllers.DeliveryInfoScreenController) {
            System.out.println("FXMLSceneManager.injectServices: Detected DeliveryInfoScreenController, injecting services...");
            com.aims.core.presentation.controllers.DeliveryInfoScreenController deliveryController =
                (com.aims.core.presentation.controllers.DeliveryInfoScreenController) controller;
            
            try {
                // Inject OrderService
                com.aims.core.application.services.IOrderService orderService = serviceFactory.getOrderService();
                if (orderService != null) {
                    deliveryController.setOrderService(orderService);
                    System.out.println("FXMLSceneManager.injectServices: OrderService injected into DeliveryInfoScreenController");
                } else {
                    System.err.println("FXMLSceneManager.injectServices: OrderService is null, injection failed");
                }
                
                // Inject DeliveryService
                com.aims.core.application.services.IDeliveryCalculationService deliveryService = serviceFactory.getDeliveryCalculationService();
                if (deliveryService != null) {
                    deliveryController.setDeliveryService(deliveryService);
                    System.out.println("FXMLSceneManager.injectServices: DeliveryCalculationService injected into DeliveryInfoScreenController");
                } else {
                    System.err.println("FXMLSceneManager.injectServices: DeliveryCalculationService is null, injection failed");
                }
                
                // Set additional dependencies if available
                if (mainLayoutController != null) {
                    deliveryController.setMainLayoutController(mainLayoutController);
                    System.out.println("FXMLSceneManager.injectServices: MainLayoutController injected into DeliveryInfoScreenController");
                } else {
                    System.err.println("FXMLSceneManager.injectServices: MainLayoutController is null - CRITICAL INJECTION FAILURE");
                }
                
            } catch (Exception e) {
                System.err.println("FXMLSceneManager.injectServices: Error injecting services into DeliveryInfoScreenController: " + e.getMessage());
                e.printStackTrace();
            }
        }
        else if (controller instanceof com.aims.core.presentation.controllers.OrderSummaryController) {
            System.out.println("FXMLSceneManager.injectServices: Detected OrderSummaryController, injecting services...");
            com.aims.core.presentation.controllers.OrderSummaryController orderSummaryController =
                (com.aims.core.presentation.controllers.OrderSummaryController) controller;
            
            try {
                // CRITICAL: Set MainLayoutController for navigation
                if (mainLayoutController != null) {
                    orderSummaryController.setMainLayoutController(mainLayoutController);
                    System.out.println("FXMLSceneManager.injectServices: MainLayoutController injected into OrderSummaryController");
                } else {
                    System.err.println("FXMLSceneManager.injectServices: CRITICAL - MainLayoutController is null for OrderSummaryController");
                }
            } catch (Exception e) {
                System.err.println("FXMLSceneManager.injectServices: Error injecting services into OrderSummaryController: " + e.getMessage());
                e.printStackTrace();
            }
        }
        else if (controller instanceof com.aims.core.presentation.controllers.PaymentMethodScreenController) {
            System.out.println("FXMLSceneManager.injectServices: Detected PaymentMethodScreenController, injecting services...");
            com.aims.core.presentation.controllers.PaymentMethodScreenController paymentMethodController =
                (com.aims.core.presentation.controllers.PaymentMethodScreenController) controller;
            
            try {
                // CRITICAL: Enhanced validation before injection
                boolean injectionSuccessful = validatePaymentMethodControllerInjection(paymentMethodController);
                if (!injectionSuccessful) {
                    System.err.println("FXMLSceneManager.injectServices: CRITICAL - PaymentMethodScreenController injection validation failed");
                    return;
                }
                
                // CRITICAL: Set MainLayoutController for navigation
                if (mainLayoutController != null) {
                    paymentMethodController.setMainLayoutController(mainLayoutController);
                    System.out.println("FXMLSceneManager.injectServices: MainLayoutController injected into PaymentMethodScreenController");
                } else {
                    System.err.println("FXMLSceneManager.injectServices: CRITICAL - MainLayoutController is null for PaymentMethodScreenController");
                    throw new IllegalStateException("MainLayoutController is required for PaymentMethodScreenController navigation");
                }
                
                // Inject PaymentService with enhanced error handling
                try {
                    com.aims.core.application.services.IPaymentService paymentService = serviceFactory.getPaymentService();
                    if (paymentService != null) {
                        paymentMethodController.setPaymentService(paymentService);
                        System.out.println("FXMLSceneManager.injectServices: PaymentService injected into PaymentMethodScreenController");
                    } else {
                        System.out.println("FXMLSceneManager.injectServices: PaymentService is null - PaymentMethodScreenController will operate with limited functionality");
                    }
                } catch (Exception pe) {
                    System.err.println("FXMLSceneManager.injectServices: PaymentService injection failed: " + pe.getMessage());
                    // Continue without PaymentService - controller should handle gracefully
                }
                
                // Validate post-injection state
                validatePaymentMethodControllerPostInjection(paymentMethodController);
                
            } catch (Exception e) {
                System.err.println("FXMLSceneManager.injectServices: CRITICAL Error injecting services into PaymentMethodScreenController: " + e.getMessage());
                e.printStackTrace();
                // Attempt fallback recovery
                attemptPaymentMethodControllerFallback(paymentMethodController);
            }
        }
        else {
            System.out.println("FXMLSceneManager.injectServices: No specific injection logic for " + controller.getClass().getSimpleName());
        }
        
        System.out.println("FXMLSceneManager.injectServices: Service injection completed for " + controller.getClass().getSimpleName());
    }
    
    public static class LoadedFXML<T> {
        public final Parent parent;
        public final T controller;
        public LoadedFXML(Parent parent, T controller) {
            this.parent = parent;
            this.controller = controller;
        }
    }


    /**
     * Loads an FXML file into a given Pane (e.g., the contentPane of MainLayoutController).
     * Returns the controller of the loaded FXML.
     */
    public <T> T loadFXMLIntoPane(Pane containerPane, String fxmlPath) {
        try {
            LoadedFXML<T> loaded = loadFXMLWithController(fxmlPath);
            containerPane.getChildren().setAll(loaded.parent);
            return loaded.controller;
        } catch (IOException | RuntimeException e) { // Catch RuntimeException for controller instantiation issues
            e.printStackTrace();
            AlertHelper.showErrorDialog("Navigation Error", "Could not load screen: " + fxmlPath.substring(fxmlPath.lastIndexOf('/') + 1), "An error occurred while trying to display the page.", e);
            System.err.println("Error loading FXML into pane: " + fxmlPath + " - " + e.getMessage());
            return null;
        }
    }

    /**
     * Loads an FXML file into a new modal dialog window.
     * Returns the controller of the loaded FXML.
     */
    public <T> T loadFXMLIntoNewWindow(String fxmlPath, String title, Window owner) {
        try {
            LoadedFXML<T> loaded = loadFXMLWithController(fxmlPath);
            
            Stage dialogStage = new Stage();
            dialogStage.setTitle(title);
            dialogStage.initModality(Modality.WINDOW_MODAL);
            if (owner != null) {
                dialogStage.initOwner(owner);
            } else if (primaryStage != null) {
                 dialogStage.initOwner(primaryStage);
            }
            Scene scene = new Scene(loaded.parent);
            
            // Apply global and theme styles
            try {
                String globalCssPath = "/styles/global.css";
                String themeCssPath = "/styles/theme.css";
                
                if (getClass().getResource(globalCssPath) != null) {
                    scene.getStylesheets().add(getClass().getResource(globalCssPath).toExternalForm());
                }
                
                if (getClass().getResource(themeCssPath) != null) {
                    scene.getStylesheets().add(getClass().getResource(themeCssPath).toExternalForm());
                }
            } catch (Exception e) {
                System.err.println("Warning: Could not load CSS files in dialog: " + e.getMessage());
            }
            
            dialogStage.setScene(scene);
            
            // If the controller needs the stage (e.g., to close itself)
            // if (loaded.controller instanceof SomeDialogControllerInterface) {
            //    ((SomeDialogControllerInterface) loaded.controller).setDialogStage(dialogStage);
            // }
            
            dialogStage.showAndWait(); // Use show() if you don't want to block
            return loaded.controller;
        } catch (IOException | RuntimeException e) { // Catch RuntimeException for controller instantiation issues
            e.printStackTrace();
            AlertHelper.showErrorDialog("Window Load Error", "Could not open window: " + title, "An error occurred while trying to display the window.", e);
            System.err.println("Error loading FXML into new window: " + fxmlPath + " - " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Switches the primary stage's scene to the given FXML file.
     */
    public <T> T switchPrimaryScene(String fxmlPath, String title) {
        if (primaryStage == null) {
            System.err.println("Primary stage not set in FXMLSceneManager.");
            return null;
        }
        try {
            LoadedFXML<T> loaded = loadFXMLWithController(fxmlPath);
            Scene scene = new Scene(loaded.parent);
            
            // Apply global and theme styles
            try {
                String globalCssPath = "/styles/global.css";
                String themeCssPath = "/styles/theme.css";
                
                if (getClass().getResource(globalCssPath) != null) {
                    scene.getStylesheets().add(getClass().getResource(globalCssPath).toExternalForm());
                }
                
                if (getClass().getResource(themeCssPath) != null) {
                    scene.getStylesheets().add(getClass().getResource(themeCssPath).toExternalForm());
                }
            } catch (Exception e) {
                System.err.println("Warning: Could not load CSS files in primary scene: " + e.getMessage());
            }
            
            primaryStage.setTitle(title);
            primaryStage.setScene(scene);
            primaryStage.show();
            return loaded.controller;
        } catch (IOException | RuntimeException e) { // Catch RuntimeException for controller instantiation issues
            e.printStackTrace();
            AlertHelper.showErrorDialog("Scene Switch Error", "Could not switch to scene: " + title, "An error occurred while trying to display the new scene.", e);
            System.err.println("Error switching primary scene: " + fxmlPath + " - " + e.getMessage());
            return null;
        }
    }
    
    // Navigation History Methods
    
    /**
     * Loads content with navigation history support.
     * Creates a navigation context and tracks the navigation in history.
     *
     * @param containerPane The pane to load content into
     * @param fxmlPath The FXML file path to load
     * @param title The title for the navigation context
     * @return The controller of the loaded FXML
     */
    public <T> T loadContentWithHistory(Pane containerPane, String fxmlPath, String title) {
        return loadContentWithHistory(containerPane, fxmlPath, title, null);
    }
    
    /**
     * Loads content with navigation history support using a provided context.
     *
     * @param containerPane The pane to load content into
     * @param fxmlPath The FXML file path to load
     * @param title The title for the navigation context
     * @param context The navigation context to use (or null to create a new one)
     * @return The controller of the loaded FXML
     */
    public <T> T loadContentWithHistory(Pane containerPane, String fxmlPath, String title, NavigationContext context) {
        System.out.println("FXMLSceneManager.loadContentWithHistory: Loading " + fxmlPath + " with title: " + title);
        
        // Store current context in history before navigation (if we have one)
        if (currentContext != null) {
            System.out.println("FXMLSceneManager.loadContentWithHistory: Pushing current context to history: " + currentContext.getScreenPath());
            navigationHistory.pushNavigation(currentContext);
        }
        
        // Create new context or use provided one
        NavigationContext newContext = context != null ? context : new NavigationContext(fxmlPath, title);
        
        // Load content using existing method
        T controller = loadFXMLIntoPane(containerPane, fxmlPath);
        
        if (controller != null) {
            // Update current context
            currentContext = newContext;
            System.out.println("FXMLSceneManager.loadContentWithHistory: Updated current context to: " + fxmlPath);
            
            // Update header if available
            if (mainLayoutController != null) {
                mainLayoutController.setHeaderTitle(title);
            }
        } else {
            System.err.println("FXMLSceneManager.loadContentWithHistory: Failed to load controller for " + fxmlPath + ". Error dialog should have been shown by loadFXMLIntoPane.");
            // Potentially navigate to a safe screen like home if mainLayoutController is available
            if (mainLayoutController != null) {
                // Consider if navigating home here is always the right action or if it could cause loops.
                // For now, rely on the error dialog and the calling method to handle the null controller.
            }
        }
        
        return controller;
    }
    
    /**
     * Navigates back using the navigation history.
     * Restores the previous screen and its context.
     *
     * @return true if navigation was successful, false if no history available or error occurred
     */
    public boolean navigateBack() {
        System.out.println("FXMLSceneManager.navigateBack: Attempting to navigate back");
        
        if (!navigationHistory.hasPrevious()) {
            System.out.println("FXMLSceneManager.navigateBack: No navigation history available");
            return false;
        }
        
        NavigationContext previousContext = navigationHistory.popNavigation();
        if (previousContext == null) {
            System.err.println("FXMLSceneManager.navigateBack: Previous context is null");
            return false;
        }
        
        try {
            System.out.println("FXMLSceneManager.navigateBack: Loading previous screen: " + previousContext.getScreenPath());
            
            // Get the current content pane
            Pane contentPane = getCurrentContentPane();
            if (contentPane == null) {
                System.err.println("FXMLSceneManager.navigateBack: Cannot get current content pane");
                return false;
            }
            
            // Load previous screen
            Object controller = loadFXMLIntoPane(contentPane, previousContext.getScreenPath());
            
            if (controller != null) {
                // Restore context if it's a search screen
                restoreScreenContext(controller, previousContext);
                
                // Update current context
                currentContext = previousContext;
                
                // Update header if available
                if (mainLayoutController != null) {
                    mainLayoutController.setHeaderTitle(previousContext.getScreenTitle());
                }
                
                System.out.println("FXMLSceneManager.navigateBack: Successfully navigated back to " + previousContext.getScreenPath());
                return true;
            } else {
                System.err.println("FXMLSceneManager.navigateBack: Failed to load controller for " + previousContext.getScreenPath() + ". Error dialog should have been shown by loadFXMLIntoPane.");
                // If loading the previous screen fails, it's problematic.
                // We might try to pop again or go home. For now, return false.
                return false;
            }
            
        } catch (Exception e) { // Catch any other unexpected errors during back navigation logic
            System.err.println("FXMLSceneManager.navigateBack: Error during back navigation: " + e.getMessage());
            e.printStackTrace();
            AlertHelper.showErrorDialog("Navigation Error", "Could not navigate back.", "An unexpected error occurred.", e);
            // Attempt to go to home screen as a last resort if mainLayoutController is available
            if (mainLayoutController != null && mainLayoutController.getContentPane() != null) {
                 try {
                    System.out.println("FXMLSceneManager.navigateBack: Attempting to navigate to home screen after error.");
                    mainLayoutController.navigateToHome(); // Assuming MainLayoutController has a navigateToHome method
                 } catch (Exception ex) {
                    System.err.println("FXMLSceneManager.navigateBack: Failed to navigate to home screen after error: " + ex.getMessage());
                 }
            }
            return false;
        }
    }
    
    /**
     * Gets the current content pane from the main layout controller.
     *
     * @return The current content pane, or null if not available
     */
    private Pane getCurrentContentPane() {
        if (mainLayoutController == null) {
            System.err.println("FXMLSceneManager.getCurrentContentPane: MainLayoutController is null");
            return null;
        }
        
        // Try to get the content pane using reflection or a getter method
        // For now, we'll assume there's a way to get it
        try {
            // This would need to be implemented in MainLayoutController
            java.lang.reflect.Method getContentPaneMethod = mainLayoutController.getClass().getMethod("getContentPane");
            return (Pane) getContentPaneMethod.invoke(mainLayoutController);
        } catch (Exception e) {
            System.err.println("FXMLSceneManager.getCurrentContentPane: Cannot access content pane: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Restores the screen context for controllers that support context restoration.
     *
     * @param controller The controller to restore context for
     * @param context The navigation context containing the state to restore
     */
    private void restoreScreenContext(Object controller, NavigationContext context) {
        if (controller == null || context == null) {
            return;
        }
        
        System.out.println("FXMLSceneManager.restoreScreenContext: Attempting to restore context for " + controller.getClass().getSimpleName());
        
        try {
            // Check for HomeScreenController and restore search context
            if (controller instanceof com.aims.core.presentation.controllers.HomeScreenController && context.hasSearchContext()) {
                System.out.println("FXMLSceneManager.restoreScreenContext: Restoring HomeScreenController search context");
                com.aims.core.presentation.controllers.HomeScreenController homeController =
                    (com.aims.core.presentation.controllers.HomeScreenController) controller;
                
                // Use reflection to call restoreSearchContext if it exists
                try {
                    java.lang.reflect.Method restoreMethod = homeController.getClass().getMethod(
                        "restoreSearchContext", String.class, String.class, String.class, int.class);
                    restoreMethod.invoke(homeController,
                        context.getSearchTerm(),
                        context.getCategoryFilter(),
                        context.getSortBy(),
                        context.getCurrentPage());
                    System.out.println("FXMLSceneManager.restoreScreenContext: Successfully restored HomeScreenController context");
                } catch (NoSuchMethodException e) {
                    System.out.println("FXMLSceneManager.restoreScreenContext: HomeScreenController doesn't have restoreSearchContext method yet");
                } catch (Exception e) {
                    System.err.println("FXMLSceneManager.restoreScreenContext: Error restoring HomeScreenController context: " + e.getMessage());
                }
            }
            // Check for ProductSearchResultsController and restore search context
            else if (controller instanceof com.aims.core.presentation.controllers.ProductSearchResultsController && context.hasSearchContext()) {
                System.out.println("FXMLSceneManager.restoreScreenContext: Restoring ProductSearchResultsController search context");
                com.aims.core.presentation.controllers.ProductSearchResultsController searchController =
                    (com.aims.core.presentation.controllers.ProductSearchResultsController) controller;
                
                // Use reflection to call restoreSearchContext if it exists
                try {
                    java.lang.reflect.Method restoreMethod = searchController.getClass().getMethod(
                        "restoreSearchContext", String.class, String.class, String.class, int.class); // Added String.class for sortBy
                    restoreMethod.invoke(searchController,
                        context.getSearchTerm(),
                        context.getCategoryFilter(),
                        context.getSortBy(), // Pass sortBy
                        context.getCurrentPage());
                    System.out.println("FXMLSceneManager.restoreScreenContext: Successfully restored ProductSearchResultsController context");
                } catch (NoSuchMethodException e) {
                    System.out.println("FXMLSceneManager.restoreScreenContext: ProductSearchResultsController restoreSearchContext method signature mismatch or not found: " + e.getMessage());
                } catch (Exception e) {
                    System.err.println("FXMLSceneManager.restoreScreenContext: Error restoring ProductSearchResultsController context: " + e.getMessage());
                }
            }
            // Check for AdminProductListController and restore its context
            else if (controller instanceof com.aims.core.presentation.controllers.AdminProductListController) {
                System.out.println("FXMLSceneManager.restoreScreenContext: Restoring AdminProductListController context");
                com.aims.core.presentation.controllers.AdminProductListController adminProductListController =
                    (com.aims.core.presentation.controllers.AdminProductListController) controller;
                adminProductListController.restoreContext(context); // Call the new restoreContext method
                System.out.println("FXMLSceneManager.restoreScreenContext: Successfully called restoreContext for AdminProductListController");
            }
            else {
                System.out.println("FXMLSceneManager.restoreScreenContext: No specific context restoration for " + controller.getClass().getSimpleName());
            }
        } catch (Exception e) {
            System.err.println("FXMLSceneManager.restoreScreenContext: Unexpected error during context restoration: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Gets the current navigation context.
     *
     * @return The current NavigationContext, or null if none set
     */
    public NavigationContext getCurrentContext() {
        return currentContext;
    }
    
    /**
     * Gets the navigation history manager.
     *
     * @return The NavigationHistory instance
     */
    public NavigationHistory getNavigationHistory() {
        return navigationHistory;
    }
    
    /**
     * Preserves search context in the current navigation context.
     * This is useful for updating the current context with search state.
     *
     * @param searchTerm The search term
     * @param category The category filter
     * @param sort The sort order
     * @param page The current page
     */
    public void preserveSearchContext(String searchTerm, String category, String sort, int page) {
        if (currentContext != null) {
            currentContext.withSearchContext(searchTerm, category, sort, page, 1);
            System.out.println("FXMLSceneManager.preserveSearchContext: Updated current context with search state");
        } else {
            System.out.println("FXMLSceneManager.preserveSearchContext: No current context to update");
        }
    }
    
    /**
     * Clears the navigation history.
     * This can be useful when starting a new user session or after logout.
     */
    public void clearNavigationHistory() {
        navigationHistory.clear();
        currentContext = null;
        System.out.println("FXMLSceneManager.clearNavigationHistory: Navigation history cleared");
    }
    
    /**
     * Gets debug information about the current navigation state.
     *
     * @return Debug information string
     */
    public String getNavigationDebugInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("FXMLSceneManager Navigation Debug Info:\n");
        sb.append("Current Context: ").append(currentContext != null ? currentContext.toString() : "null").append("\n");
        sb.append(navigationHistory.getDebugInfo());
        return sb.toString();
    }
    
    /**
     * ENHANCED: Validates that all prerequisites for service injection are met.
     * This helps prevent injection failures and provides early warning of configuration issues.
     *
     * @param controller The controller to validate prerequisites for
     * @return true if all prerequisites are met, false otherwise
     */
    private boolean validateInjectionPrerequisites(Object controller) {
        if (controller == null) {
            System.err.println("FXMLSceneManager.validateInjectionPrerequisites: Controller is null");
            return false;
        }
        
        if (serviceFactory == null) {
            System.err.println("FXMLSceneManager.validateInjectionPrerequisites: CRITICAL - ServiceFactory is null");
            System.err.println("This indicates a fundamental configuration problem that will cause injection failures");
            return false;
        }
        
        // Validate critical services are available
        try {
            if (serviceFactory.getProductService() == null) {
                System.err.println("FXMLSceneManager.validateInjectionPrerequisites: WARNING - ProductService is null");
            }
            if (serviceFactory.getCartService() == null) {
                System.err.println("FXMLSceneManager.validateInjectionPrerequisites: WARNING - CartService is null");
            }
            if (serviceFactory.getOrderService() == null) {
                System.err.println("FXMLSceneManager.validateInjectionPrerequisites: WARNING - OrderService is null");
            }
            if (serviceFactory.getDeliveryCalculationService() == null) {
                System.err.println("FXMLSceneManager.validateInjectionPrerequisites: WARNING - DeliveryCalculationService is null");
            }
        } catch (Exception e) {
            System.err.println("FXMLSceneManager.validateInjectionPrerequisites: ERROR accessing services: " + e.getMessage());
            return false;
        }
        
        // Special validation for controllers that require MainLayoutController
        if (controller instanceof com.aims.core.presentation.controllers.DeliveryInfoScreenController ||
            controller instanceof com.aims.core.presentation.controllers.ProductDetailScreenController ||
            controller instanceof com.aims.core.presentation.controllers.PaymentProcessingScreenController ||
            controller instanceof com.aims.core.presentation.controllers.CustomerOrderDetailController ||
            controller instanceof com.aims.core.presentation.controllers.OrderReviewController ||
            controller instanceof com.aims.core.presentation.controllers.OrderSummaryController ||
            controller instanceof com.aims.core.presentation.controllers.PaymentMethodScreenController) {
            
            if (mainLayoutController == null) {
                System.err.println("FXMLSceneManager.validateInjectionPrerequisites: CRITICAL - MainLayoutController is null for " + controller.getClass().getSimpleName());
                System.err.println("This will cause navigation failures and must be resolved immediately");
                return false;
            }
        }
        
        System.out.println("FXMLSceneManager.validateInjectionPrerequisites: All prerequisites validated for " + controller.getClass().getSimpleName());
        return true;
    }
    
    /**
     * Enhanced validation specifically for PaymentMethodScreenController injection.
     * Validates that all critical dependencies are available before injection.
     *
     * @param controller The PaymentMethodScreenController to validate
     * @return true if validation passes, false otherwise
     */
    private boolean validatePaymentMethodControllerInjection(com.aims.core.presentation.controllers.PaymentMethodScreenController controller) {
        System.out.println("FXMLSceneManager.validatePaymentMethodControllerInjection: Starting validation");
        
        if (controller == null) {
            System.err.println("FXMLSceneManager.validatePaymentMethodControllerInjection: Controller is null");
            return false;
        }
        
        if (mainLayoutController == null) {
            System.err.println("FXMLSceneManager.validatePaymentMethodControllerInjection: CRITICAL - MainLayoutController is null");
            System.err.println("PaymentMethodScreenController requires MainLayoutController for navigation");
            return false;
        }
        
        if (serviceFactory == null) {
            System.err.println("FXMLSceneManager.validatePaymentMethodControllerInjection: CRITICAL - ServiceFactory is null");
            return false;
        }
        
        System.out.println("FXMLSceneManager.validatePaymentMethodControllerInjection: Validation passed");
        return true;
    }
    
    /**
     * Post-injection validation for PaymentMethodScreenController.
     * Ensures that critical dependencies were successfully injected.
     *
     * @param controller The PaymentMethodScreenController to validate
     */
    private void validatePaymentMethodControllerPostInjection(com.aims.core.presentation.controllers.PaymentMethodScreenController controller) {
        System.out.println("FXMLSceneManager.validatePaymentMethodControllerPostInjection: Starting post-injection validation");
        
        // Use reflection to verify MainLayoutController was set
        try {
            java.lang.reflect.Field mainLayoutField = controller.getClass().getDeclaredField("mainLayoutController");
            mainLayoutField.setAccessible(true);
            Object injectedMainLayout = mainLayoutField.get(controller);
            
            if (injectedMainLayout == null) {
                System.err.println("FXMLSceneManager.validatePaymentMethodControllerPostInjection: CRITICAL - MainLayoutController injection failed");
                throw new IllegalStateException("MainLayoutController injection failed for PaymentMethodScreenController");
            } else {
                System.out.println("FXMLSceneManager.validatePaymentMethodControllerPostInjection: MainLayoutController injection verified");
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            System.err.println("FXMLSceneManager.validatePaymentMethodControllerPostInjection: Could not verify injection: " + e.getMessage());
        }
        
        System.out.println("FXMLSceneManager.validatePaymentMethodControllerPostInjection: Post-injection validation completed");
    }
    
    /**
     * Attempts fallback recovery for PaymentMethodScreenController when injection fails.
     * Provides minimal functionality to prevent complete failure.
     *
     * @param controller The PaymentMethodScreenController to provide fallback for
     */
    private void attemptPaymentMethodControllerFallback(com.aims.core.presentation.controllers.PaymentMethodScreenController controller) {
        System.out.println("FXMLSceneManager.attemptPaymentMethodControllerFallback: Attempting fallback recovery");
        
        try {
            // Attempt to set MainLayoutController if available
            if (mainLayoutController != null && controller != null) {
                controller.setMainLayoutController(mainLayoutController);
                System.out.println("FXMLSceneManager.attemptPaymentMethodControllerFallback: MainLayoutController set via fallback");
            }
            
            // Log fallback state
            System.out.println("FXMLSceneManager.attemptPaymentMethodControllerFallback: Fallback recovery completed");
            System.out.println("PaymentMethodScreenController will operate with limited functionality");
            
        } catch (Exception e) {
            System.err.println("FXMLSceneManager.attemptPaymentMethodControllerFallback: Fallback recovery failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
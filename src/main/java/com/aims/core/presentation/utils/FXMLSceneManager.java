package com.aims.core.presentation.utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import com.aims.core.presentation.controllers.MainLayoutController;
import com.aims.core.shared.ServiceFactory;
import java.io.IOException;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.logging.Level;

public class FXMLSceneManager {
    
    private static final Logger logger = Logger.getLogger(FXMLSceneManager.class.getName());
    
    private static FXMLSceneManager instance;
    private Stage primaryStage;
    private ServiceFactory serviceFactory;
    private MainLayoutController mainLayoutController;
    
    // Navigation management
    private NavigationHistory navigationHistory;
    private NavigationContext currentContext;

    public static class LoadedFXML<T> {
        public final T controller;
        public final Node parent;

        public LoadedFXML(T controller, Node parent) {
            this.controller = controller;
            this.parent = parent;
        }

        public T getController() {
            return controller;
        }

        public Node getRoot() {
            return parent;
        }
    }
    
    // Private constructor for singleton pattern
    private FXMLSceneManager() {
        this.navigationHistory = new NavigationHistory();
    }
    
    public FXMLSceneManager(Stage primaryStage, ServiceFactory serviceFactory) {
        this();
        this.primaryStage = primaryStage;
        this.serviceFactory = serviceFactory;
    }

    public static synchronized FXMLSceneManager getInstance() {
        if (instance == null) {
            instance = new FXMLSceneManager();
        }
        return instance;
    }

    public static void initialize(Stage primaryStage, ServiceFactory serviceFactory) {
        instance = new FXMLSceneManager(primaryStage, serviceFactory);
    }
    
    public void setMainLayoutController(MainLayoutController controller) {
        this.mainLayoutController = controller;
        System.out.println("FXMLSceneManager.setMainLayoutController: MainLayoutController injected successfully");
    }
    
    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
        System.out.println("FXMLSceneManager.setPrimaryStage: Primary stage set successfully");
    }
    
    public void setServiceFactory(ServiceFactory serviceFactory) {
        this.serviceFactory = serviceFactory;
        System.out.println("FXMLSceneManager.setServiceFactory: ServiceFactory set successfully");
    }
    
    public FXMLLoader getLoader(String fxmlPath) {
        return new FXMLLoader(getClass().getResource(fxmlPath));
    }

    public <T> LoadedFXML<T> loadFXMLWithController(String fxmlPath) {
        logger.info("FXMLSceneManager.loadFXMLWithController: Loading FXML for " + fxmlPath);
        try {
            // Validate FXML path exists
            if (getClass().getResource(fxmlPath) == null) {
                logger.severe("FXMLSceneManager.loadFXMLWithController: FXML resource not found: " + fxmlPath);
                throw new IOException("FXML resource not found: " + fxmlPath);
            }
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node root = loader.load();
            T controller = loader.getController();
            
            logger.info("FXMLSceneManager.loadFXMLWithController: FXML loaded successfully - Controller: " +
                (controller != null ? controller.getClass().getSimpleName() : "null"));
            
            if (controller != null) {
                try {
                    // PHASE 1 FIX: Enhanced MainLayoutController injection with registry
                    if (controller instanceof MainLayoutController.IChildController) {
                        boolean injectionSuccess = injectMainLayoutControllerWithFallback(controller);
                        if (!injectionSuccess) {
                            logger.warning("FXMLSceneManager.loadFXMLWithController: MainLayoutController injection failed for " +
                                controller.getClass().getSimpleName() + " - controller will have limited functionality");
                        }
                    }
                    
                    // Inject services if available
                    if (serviceFactory != null) {
                        logger.fine("FXMLSceneManager.loadFXMLWithController: ServiceFactory available, injecting services");
                        injectServices(controller);
                    } else {
                        logger.warning("FXMLSceneManager.loadFXMLWithController: ServiceFactory is null, skipping service injection");
                    }
                } catch (Exception injectionException) {
                    logger.log(Level.WARNING, "FXMLSceneManager.loadFXMLWithController: Error during dependency injection for " +
                        controller.getClass().getSimpleName(), injectionException);
                    // Continue with controller even if injection fails partially
                }
            } else {
                logger.warning("FXMLSceneManager.loadFXMLWithController: Controller is null for " + fxmlPath +
                    " - check FXML fx:controller attribute");
            }
            
            logger.info("FXMLSceneManager.loadFXMLWithController: Successfully completed loading FXML for " + fxmlPath);
            return new LoadedFXML<>(controller, root);
            
        } catch (IOException e) {
            logger.log(Level.SEVERE, "FXMLSceneManager.loadFXMLWithController: IO error loading FXML: " + fxmlPath, e);
            throw new RuntimeException("Failed to load FXML: " + fxmlPath + " - " + e.getMessage(), e);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "FXMLSceneManager.loadFXMLWithController: Unexpected error loading FXML: " + fxmlPath, e);
            throw new RuntimeException("Unexpected error loading FXML: " + fxmlPath + " - " + e.getMessage(), e);
        }
    }

    public Object loadContent(String fxmlPath, MainLayoutController parentController) {
        try {
            LoadedFXML<?> loaded = loadFXMLWithController(fxmlPath);
            if (loaded != null && parentController != null) {
                parentController.setContent(loaded.getRoot());
                
                Object controller = loaded.getController();
                if (controller instanceof MainLayoutController.IChildController) {
                    ((MainLayoutController.IChildController) controller).setMainLayoutController(parentController);
                }
                
                return controller;
            }
            return null;
            
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public FXMLLoader loadScene(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Scene scene = new Scene(loader.load());
            primaryStage.setScene(scene);
            
            Object controller = loader.getController();
            if (controller != null) {
                injectServices(controller);
            }
            
            return loader;
            
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void injectServices(Object controller) {
        if (serviceFactory == null) {
            System.out.println("FXMLSceneManager.injectServices: ServiceFactory is null, cannot inject services");
            return;
        }
        
        System.out.println("FXMLSceneManager.injectServices: Starting service injection for " + controller.getClass().getSimpleName());
        
        // Inject services based on controller type
        if (controller instanceof com.aims.core.presentation.controllers.HomeScreenController) {
            System.out.println("FXMLSceneManager.injectServices: Detected HomeScreenController, injecting services...");
            com.aims.core.presentation.controllers.HomeScreenController homeController =
                (com.aims.core.presentation.controllers.HomeScreenController) controller;
            
            try {
                homeController.setProductService(serviceFactory.getProductService());
                System.out.println("FXMLSceneManager.injectServices: ProductService injected into HomeScreenController");
                
                homeController.setCartService(serviceFactory.getCartService());
                System.out.println("FXMLSceneManager.injectServices: CartService injected into HomeScreenController");
                
                homeController.completeInitialization(); // Complete initialization after services are injected
                System.out.println("FXMLSceneManager.injectServices: HomeScreenController initialization completed");
            } catch (Exception e) {
                System.err.println("FXMLSceneManager.injectServices: Error injecting services into HomeScreenController: " + e.getMessage());
                e.printStackTrace();
            }
        } else if (controller instanceof com.aims.core.presentation.controllers.CartScreenController) {
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
        } else if (controller instanceof com.aims.core.presentation.controllers.ProductDetailScreenController) {
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
        } else if (controller instanceof com.aims.core.presentation.controllers.OrderSummaryController) {
            System.out.println("FXMLSceneManager.injectServices: Detected OrderSummaryController, injecting enhanced services...");
            com.aims.core.presentation.controllers.OrderSummaryController orderSummaryController =
                (com.aims.core.presentation.controllers.OrderSummaryController) controller;
            
            try {
                orderSummaryController.setOrderDataLoaderService(serviceFactory.getOrderDataLoaderService());
                System.out.println("FXMLSceneManager.injectServices: OrderDataLoaderService injected into OrderSummaryController");
                
                orderSummaryController.setCartDataValidationService(serviceFactory.getCartDataValidationService());
                System.out.println("FXMLSceneManager.injectServices: CartDataValidationService injected into OrderSummaryController");
                
                orderSummaryController.setSceneManager(this);
                System.out.println("FXMLSceneManager.injectServices: SceneManager injected into OrderSummaryController");
                
            } catch (Exception e) {
                System.err.println("FXMLSceneManager.injectServices: Error injecting services into OrderSummaryController: " + e.getMessage());
                e.printStackTrace();
            }
        } else if (controller instanceof com.aims.core.presentation.controllers.DeliveryInfoScreenController) {
            System.out.println("FXMLSceneManager.injectServices: Detected DeliveryInfoScreenController, injecting enhanced services...");
            com.aims.core.presentation.controllers.DeliveryInfoScreenController deliveryController =
                (com.aims.core.presentation.controllers.DeliveryInfoScreenController) controller;
            
            try {
                deliveryController.setOrderDataLoaderService(serviceFactory.getOrderDataLoaderService());
                System.out.println("FXMLSceneManager.injectServices: OrderDataLoaderService injected into DeliveryInfoScreenController");
                
                deliveryController.setCartDataValidationService(serviceFactory.getCartDataValidationService());
                System.out.println("FXMLSceneManager.injectServices: CartDataValidationService injected into DeliveryInfoScreenController");
                
                deliveryController.setDeliveryCalculationService(serviceFactory.getDeliveryCalculationService());
                System.out.println("FXMLSceneManager.injectServices: DeliveryCalculationService injected into DeliveryInfoScreenController");
                
            } catch (Exception e) {
                System.err.println("FXMLSceneManager.injectServices: Error injecting services into DeliveryInfoScreenController: " + e.getMessage());
                e.printStackTrace();
            }
        } else if (controller instanceof MainLayoutController) {
            MainLayoutController mainController = (MainLayoutController) controller;
            mainController.setServiceFactory(serviceFactory);
            mainController.setSceneManager(this);
            mainController.setAuthenticationService(serviceFactory.getAuthenticationService());
        }
        
        System.out.println("FXMLSceneManager.injectServices: Service injection completed for " + controller.getClass().getSimpleName());
    }

    public void showDialog(String fxmlPath, Consumer<Object> controllerInitializer) {
        try {
            LoadedFXML<?> loaded = loadFXMLWithController(fxmlPath);
            if (loaded != null) {
                Scene dialogScene = new Scene((Pane) loaded.getRoot());
                Stage dialogStage = new Stage();
                dialogStage.setScene(dialogScene);
                
                if (controllerInitializer != null) {
                    controllerInitializer.accept(loaded.getController());
                }
                
                dialogStage.showAndWait();
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }
    
    /**
     * CRITICAL MISSING METHOD: Load FXML into a specific pane
     * Referenced by multiple controllers for navigation
     */
    public <T> T loadFXMLIntoPane(Pane containerPane, String fxmlPath) {
        System.out.println("=== DIAGNOSTIC: FXMLSceneManager.loadFXMLIntoPane() START ===");
        System.out.println("FXMLSceneManager.loadFXMLIntoPane: Loading " + fxmlPath + " into container: " +
            (containerPane != null ? containerPane.getClass().getSimpleName() + "@" + containerPane.hashCode() : "null"));
        
        // Thread safety validation
        boolean isOnFXThread = javafx.application.Platform.isFxApplicationThread();
        System.out.println("FXMLSceneManager.loadFXMLIntoPane: Running on JavaFX Application Thread: " + isOnFXThread);
        if (!isOnFXThread) {
            System.err.println("FXMLSceneManager.loadFXMLIntoPane: WARNING - Not running on JavaFX Application Thread! This may cause UI update failures.");
        }
        
        // Container validation
        if (containerPane != null) {
            System.out.println("FXMLSceneManager.loadFXMLIntoPane: Container state before loading:");
            System.out.println("  - Container type: " + containerPane.getClass().getSimpleName());
            System.out.println("  - Container children count: " + containerPane.getChildren().size());
            System.out.println("  - Container parent: " + (containerPane.getParent() != null ? containerPane.getParent().getClass().getSimpleName() : "null"));
            System.out.println("  - Container scene: " + (containerPane.getScene() != null ? "Scene@" + containerPane.getScene().hashCode() : "null"));
            System.out.println("  - Container visible: " + containerPane.isVisible());
            System.out.println("  - Container managed: " + containerPane.isManaged());
        } else {
            System.err.println("FXMLSceneManager.loadFXMLIntoPane: FAILURE - containerPane is null!");
            System.out.println("=== DIAGNOSTIC: FXMLSceneManager.loadFXMLIntoPane() END ===");
            return null;
        }
        
        try {
            System.out.println("FXMLSceneManager.loadFXMLIntoPane: Calling loadFXMLWithController()...");
            LoadedFXML<T> loaded = loadFXMLWithController(fxmlPath);
            
            if (loaded != null) {
                System.out.println("FXMLSceneManager.loadFXMLIntoPane: FXML loaded successfully");
                System.out.println("  - Loaded controller: " + (loaded.controller != null ? loaded.controller.getClass().getSimpleName() : "null"));
                System.out.println("  - Loaded parent: " + (loaded.parent != null ? loaded.parent.getClass().getSimpleName() + "@" + loaded.parent.hashCode() : "null"));
                
                if (loaded.parent != null) {
                    System.out.println("  - Loaded parent visible: " + loaded.parent.isVisible());
                    System.out.println("  - Loaded parent managed: " + loaded.parent.isManaged());
                }
                
                // Log the content replacement operation
                System.out.println("FXMLSceneManager.loadFXMLIntoPane: Executing containerPane.getChildren().setAll()...");
                
                // Check if we're dealing with a BorderPane specifically
                if (containerPane instanceof javafx.scene.layout.BorderPane) {
                    System.out.println("FXMLSceneManager.loadFXMLIntoPane: WARNING - Container is BorderPane, setAll() may not work as expected!");
                    System.out.println("FXMLSceneManager.loadFXMLIntoPane: Consider using BorderPane.setCenter() instead");
                    
                    // Log BorderPane specific state
                    javafx.scene.layout.BorderPane borderPane = (javafx.scene.layout.BorderPane) containerPane;
                    System.out.println("  - BorderPane center: " + (borderPane.getCenter() != null ? borderPane.getCenter().getClass().getSimpleName() : "null"));
                    System.out.println("  - BorderPane top: " + (borderPane.getTop() != null ? borderPane.getTop().getClass().getSimpleName() : "null"));
                    System.out.println("  - BorderPane bottom: " + (borderPane.getBottom() != null ? borderPane.getBottom().getClass().getSimpleName() : "null"));
                    System.out.println("  - BorderPane left: " + (borderPane.getLeft() != null ? borderPane.getLeft().getClass().getSimpleName() : "null"));
                    System.out.println("  - BorderPane right: " + (borderPane.getRight() != null ? borderPane.getRight().getClass().getSimpleName() : "null"));
                }
                
                // Perform the content replacement
                containerPane.getChildren().setAll(loaded.parent);
                System.out.println("FXMLSceneManager.loadFXMLIntoPane: containerPane.getChildren().setAll() completed successfully");
                
                // Validate the replacement
                System.out.println("FXMLSceneManager.loadFXMLIntoPane: Container state after loading:");
                System.out.println("  - Container children count: " + containerPane.getChildren().size());
                if (!containerPane.getChildren().isEmpty()) {
                    javafx.scene.Node firstChild = containerPane.getChildren().get(0);
                    boolean replacementSuccessful = (firstChild == loaded.parent);
                    System.out.println("  - First child matches loaded content: " + replacementSuccessful);
                    System.out.println("  - First child: " + (firstChild != null ? firstChild.getClass().getSimpleName() + "@" + firstChild.hashCode() : "null"));
                } else {
                    System.err.println("  - WARNING: Container has no children after setAll()!");
                }
                
                // Force layout update
                if (!isOnFXThread) {
                    System.out.println("FXMLSceneManager.loadFXMLIntoPane: Scheduling layout update on JavaFX Application Thread");
                    javafx.application.Platform.runLater(() -> {
                        containerPane.requestLayout();
                        System.out.println("FXMLSceneManager.loadFXMLIntoPane: Layout update requested on correct thread");
                    });
                } else {
                    containerPane.requestLayout();
                    System.out.println("FXMLSceneManager.loadFXMLIntoPane: Layout update requested");
                }
                
                System.out.println("FXMLSceneManager.loadFXMLIntoPane: SUCCESS - Content loading completed");
                System.out.println("=== DIAGNOSTIC: FXMLSceneManager.loadFXMLIntoPane() END ===");
                return loaded.controller;
            } else {
                System.err.println("FXMLSceneManager.loadFXMLIntoPane: FAILURE - loadFXMLWithController returned null");
                System.out.println("=== DIAGNOSTIC: FXMLSceneManager.loadFXMLIntoPane() END ===");
                return null;
            }
        } catch (Exception e) {
            System.err.println("FXMLSceneManager.loadFXMLIntoPane: ERROR during content loading: " + e.getMessage());
            e.printStackTrace();
            System.err.println("FXMLSceneManager.loadFXMLIntoPane: FAILURE - Content loading failed with exception");
            System.out.println("=== DIAGNOSTIC: FXMLSceneManager.loadFXMLIntoPane() END ===");
            return null;
        }
    }
    
    /**
     * CRITICAL MISSING METHOD: Load FXML into a pane (Node version)
     * Used by some controllers with Node parameter
     */
    public <T> T loadFXMLIntoPane(Node containerNode, String fxmlPath) {
        if (containerNode instanceof Pane) {
            return loadFXMLIntoPane((Pane) containerNode, fxmlPath);
        } else {
            System.err.println("loadFXMLIntoPane: Container node is not a Pane, cannot load content");
            return null;
        }
    }
    
    /**
     * CRITICAL MISSING METHOD: Load scene with context and history support
     */
    public <T> T loadScene(String fxmlPath, String title) {
        if (primaryStage == null) {
            System.err.println("Primary stage not set in FXMLSceneManager.");
            return null;
        }
        
        try {
            LoadedFXML<T> loaded = loadFXMLWithController(fxmlPath);
            Scene scene = new Scene((javafx.scene.Parent) loaded.parent);
            
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
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error loading scene: " + fxmlPath + " - " + e.getMessage());
            return null;
        }
    }
    
    /**
     * CRITICAL MISSING METHOD: Load content with navigation history support
     */
    public Object loadContentWithHistory(Pane contentPane, String fxmlPath, String title) {
        System.out.println("FXMLSceneManager.loadContentWithHistory: Loading " + fxmlPath + " with title: " + title);
        
        // Store current context in navigation history before loading new content
        if (currentContext != null) {
            System.out.println("FXMLSceneManager.loadContentWithHistory: Pushing current context to history: " + currentContext.getScreenPath());
            navigationHistory.pushNavigation(currentContext);
        }
        
        // Create new navigation context
        NavigationContext newContext = new NavigationContext(fxmlPath, title);
        
        // Load the new content
        Object controller = loadFXMLIntoPane(contentPane, fxmlPath);
        
        if (controller != null) {
            // Update current context
            currentContext = newContext;
            System.out.println("FXMLSceneManager.loadContentWithHistory: Updated current context to: " + fxmlPath);
            
            // Update header if available
            if (mainLayoutController != null) {
                mainLayoutController.setHeaderTitle(title);
            }
        } else {
            System.err.println("FXMLSceneManager.loadContentWithHistory: Failed to load controller for " + fxmlPath);
        }
        
        return controller;
    }
    
    /**
     * CRITICAL MISSING METHOD: Load content with custom navigation context
     */
    public Object loadContentWithHistory(Pane contentPane, String fxmlPath, String title, NavigationContext context) {
        System.out.println("FXMLSceneManager.loadContentWithHistory: Loading " + fxmlPath + " with custom context");
        
        // Store current context in navigation history before loading new content
        if (currentContext != null) {
            navigationHistory.pushNavigation(currentContext);
        }
        
        // Load the new content
        Object controller = loadFXMLIntoPane(contentPane, fxmlPath);
        
        if (controller != null) {
            // Use the provided context
            currentContext = context;
            
            // Update header if available
            if (mainLayoutController != null) {
                mainLayoutController.setHeaderTitle(title);
            }
        }
        
        return controller;
    }
    
    /**
     * CRITICAL MISSING METHOD: Navigate back to previous screen
     */
    public boolean navigateBack() {
        if (!navigationHistory.hasPrevious()) {
            System.out.println("FXMLSceneManager.navigateBack: No navigation history available");
            return false;
        }
        
        NavigationContext previousContext = navigationHistory.popNavigation();
        if (previousContext == null) {
            System.err.println("FXMLSceneManager.navigateBack: Previous context is null");
            return false;
        }
        
        System.out.println("FXMLSceneManager.navigateBack: Loading previous screen: " + previousContext.getScreenPath());
        
        // Load the previous screen
        Object controller = null;
        if (mainLayoutController != null) {
            controller = mainLayoutController.loadContent(previousContext.getScreenPath());
        }
        
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
            System.err.println("FXMLSceneManager.navigateBack: Failed to load controller for " + previousContext.getScreenPath());
            return false;
        }
    }
    
    /**
     * CRITICAL MISSING METHOD: Preserve search context for navigation
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
     * Helper method to restore search context when navigating back
     */
    private void restoreScreenContext(Object controller, NavigationContext context) {
        if (context == null || !context.hasSearchContext()) {
            return;
        }
        
        System.out.println("FXMLSceneManager.restoreScreenContext: Attempting to restore context for " + controller.getClass().getSimpleName());
        
        // Restore context for HomeScreenController
        if (controller instanceof com.aims.core.presentation.controllers.HomeScreenController) {
            com.aims.core.presentation.controllers.HomeScreenController homeController =
                (com.aims.core.presentation.controllers.HomeScreenController) controller;
            
            System.out.println("FXMLSceneManager.restoreScreenContext: Restoring HomeScreenController search context");
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
        // Restore context for ProductSearchResultsController
        else if (controller instanceof com.aims.core.presentation.controllers.ProductSearchResultsController) {
            com.aims.core.presentation.controllers.ProductSearchResultsController searchController =
                (com.aims.core.presentation.controllers.ProductSearchResultsController) controller;
            
            System.out.println("FXMLSceneManager.restoreScreenContext: Restoring ProductSearchResultsController search context");
            try {
                java.lang.reflect.Method restoreMethod = searchController.getClass().getMethod(
                    "restoreSearchContext", String.class, String.class, int.class);
                restoreMethod.invoke(searchController,
                    context.getSearchTerm(),
                    context.getCategoryFilter(),
                    context.getCurrentPage());
                System.out.println("FXMLSceneManager.restoreScreenContext: Successfully restored ProductSearchResultsController context");
            } catch (NoSuchMethodException e) {
                System.out.println("FXMLSceneManager.restoreScreenContext: ProductSearchResultsController doesn't have restoreSearchContext method yet");
            } catch (Exception e) {
                System.err.println("FXMLSceneManager.restoreScreenContext: Error restoring ProductSearchResultsController context: " + e.getMessage());
            }
        } else {
            System.out.println("FXMLSceneManager.restoreScreenContext: No specific context restoration for " + controller.getClass().getSimpleName());
        }
    }
    
    /**
     * PHASE 1 FIX: Enhanced MainLayoutController injection with fallback strategies.
     * Uses MainLayoutControllerRegistry as primary source with multiple fallback options.
     *
     * @param controller The controller to inject MainLayoutController into
     * @return true if injection was successful, false otherwise
     */
    private boolean injectMainLayoutControllerWithFallback(Object controller) {
        try {
            logger.fine("FXMLSceneManager.injectMainLayoutControllerWithFallback: Starting injection for " +
                controller.getClass().getSimpleName());
            
            MainLayoutController controllerToInject = null;
            String injectionSource = null;
            
            // Strategy 1: Use MainLayoutControllerRegistry (primary)
            try {
                controllerToInject = MainLayoutControllerRegistry.getInstance(2, java.util.concurrent.TimeUnit.SECONDS);
                if (controllerToInject != null) {
                    injectionSource = "MainLayoutControllerRegistry";
                    logger.fine("FXMLSceneManager.injectMainLayoutControllerWithFallback: Using registry controller");
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "FXMLSceneManager.injectMainLayoutControllerWithFallback: Registry access failed", e);
            }
            
            // Strategy 2: Use local instance (fallback)
            if (controllerToInject == null && mainLayoutController != null) {
                controllerToInject = mainLayoutController;
                injectionSource = "Local instance";
                logger.fine("FXMLSceneManager.injectMainLayoutControllerWithFallback: Using local controller instance");
            }
            
            // Strategy 3: Attempt immediate registry access (emergency)
            if (controllerToInject == null) {
                controllerToInject = MainLayoutControllerRegistry.getInstanceImmediate();
                if (controllerToInject != null) {
                    injectionSource = "Registry immediate";
                    logger.fine("FXMLSceneManager.injectMainLayoutControllerWithFallback: Using immediate registry access");
                }
            }
            
            // Perform injection if controller is available
            if (controllerToInject != null) {
                ((MainLayoutController.IChildController) controller).setMainLayoutController(controllerToInject);
                
                // Validate injection
                if (validateInjection(controller, controllerToInject)) {
                    logger.info("FXMLSceneManager.injectMainLayoutControllerWithFallback: Successfully injected MainLayoutController into " +
                        controller.getClass().getSimpleName() + " using " + injectionSource);
                    return true;
                } else {
                    logger.warning("FXMLSceneManager.injectMainLayoutControllerWithFallback: Injection validation failed for " +
                        controller.getClass().getSimpleName());
                    return false;
                }
            } else {
                // Log comprehensive failure information
                logInjectionFailure(controller);
                return false;
            }
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "FXMLSceneManager.injectMainLayoutControllerWithFallback: Unexpected error during injection", e);
            return false;
        }
    }
    
    /**
     * Validates that MainLayoutController injection was successful.
     *
     * @param controller The controller that received injection
     * @param injectedController The controller that was injected
     * @return true if injection is valid, false otherwise
     */
    private boolean validateInjection(Object controller, MainLayoutController injectedController) {
        try {
            // Basic validation - check if injection actually worked
            if (controller instanceof MainLayoutController.IChildController) {
                // We can't directly validate the injection since there's no getter,
                // but we can validate the injected controller itself
                return injectedController != null &&
                       injectedController.getContentPane() != null &&
                       injectedController.getMainContainer() != null;
            }
            return false;
        } catch (Exception e) {
            logger.log(Level.WARNING, "FXMLSceneManager.validateInjection: Validation error", e);
            return false;
        }
    }
    
    /**
     * Logs comprehensive information about injection failures for debugging.
     *
     * @param controller The controller that failed injection
     */
    private void logInjectionFailure(Object controller) {
        StringBuilder failureInfo = new StringBuilder();
        failureInfo.append("MainLayoutController injection failure for ").append(controller.getClass().getSimpleName()).append(":\n");
        
        // Check registry state
        failureInfo.append("Registry available: ").append(MainLayoutControllerRegistry.isAvailable()).append("\n");
        failureInfo.append("Registry debug info:\n").append(MainLayoutControllerRegistry.getDebugInfo()).append("\n");
        
        // Check local instance
        failureInfo.append("Local instance available: ").append(mainLayoutController != null).append("\n");
        
        // Check if controller implements the interface
        failureInfo.append("Controller implements IChildController: ").append(
            controller instanceof MainLayoutController.IChildController).append("\n");
        
        logger.warning("FXMLSceneManager.logInjectionFailure: " + failureInfo.toString());
    }
}
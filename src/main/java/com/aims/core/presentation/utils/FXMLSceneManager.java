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
            System.out.println("FXMLSceneManager.injectServices: ServiceFactory is null, cannot inject services");
            return;
        }
        
        System.out.println("FXMLSceneManager.injectServices: Starting service injection for " + controller.getClass().getSimpleName());
        
        // Check for controllers that have confirmed setter methods
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
        } catch (IOException e) {
            e.printStackTrace();
            // AlertHelper.showErrorDialog("Navigation Error", "Could not load screen.", fxmlPath + "\n" + e.getMessage());
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
        } catch (IOException e) {
            e.printStackTrace();
            // AlertHelper.showErrorDialog("Window Load Error", "Could not open window.", title + "\n" + e.getMessage());
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
        } catch (IOException e) {
            e.printStackTrace();
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
            System.err.println("FXMLSceneManager.loadContentWithHistory: Failed to load controller for " + fxmlPath);
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
                System.err.println("FXMLSceneManager.navigateBack: Failed to load controller for " + previousContext.getScreenPath());
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("FXMLSceneManager.navigateBack: Error during back navigation: " + e.getMessage());
            e.printStackTrace();
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
}
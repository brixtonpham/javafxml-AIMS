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

    private static FXMLSceneManager instance;

    private FXMLSceneManager() {}

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
            // scene.getStylesheets().add(getClass().getResource("/styles/global.css").toExternalForm()); // Apply global styles
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
            // scene.getStylesheets().add(getClass().getResource("/styles/global.css").toExternalForm());
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
}
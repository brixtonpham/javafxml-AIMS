package com.aims;

import com.aims.core.presentation.controllers.MainLayoutController;
import com.aims.core.presentation.utils.FXMLSceneManager;
import com.aims.core.presentation.utils.MainLayoutControllerRegistry;
import com.aims.core.shared.constants.FXMLPaths;
import com.aims.core.shared.ServiceFactory;


import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Screen;
import javafx.geometry.Rectangle2D;

import java.io.IOException;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.logging.Level;

public class AimsApp extends Application {

    private static final Logger logger = Logger.getLogger(AimsApp.class.getName());
    private static final String APP_TITLE = "AIMS - An Internet Media Store";
    private static final double MIN_WINDOW_WIDTH = 1200;
    private static final double MIN_WINDOW_HEIGHT = 720;

    private FXMLSceneManager sceneManager;
    private MainLayoutController mainLayoutController;
    private ServiceFactory serviceFactory;


    @Override
    public void init() throws Exception {
        super.init();
        // Initialize the ServiceFactory which will handle all dependency injection
        this.serviceFactory = ServiceFactory.getInstance();
        
        // Initialize FXMLSceneManager
        this.sceneManager = FXMLSceneManager.getInstance();
    }


    @Override
    public void start(Stage primaryStage) {
        sceneManager.setPrimaryStage(primaryStage);

        try {
            FXMLLoader loader = sceneManager.getLoader(FXMLPaths.MAIN_LAYOUT);
            Parent root = loader.load();

            this.mainLayoutController = loader.getController();
            if (mainLayoutController == null) {
                logger.severe("CRITICAL: MainLayoutController is null after loading FXML: " + FXMLPaths.MAIN_LAYOUT);
                showErrorDialog("Application Startup Error", "Cannot load main application layout controller.");
                return;
            }
            
            // PHASE 1 FIX: Register MainLayoutController with the registry
            try {
                MainLayoutControllerRegistry.setInstance(mainLayoutController);
                logger.info("MainLayoutController successfully registered with MainLayoutControllerRegistry");
            } catch (Exception e) {
                logger.log(Level.SEVERE, "CRITICAL: Failed to register MainLayoutController with registry", e);
                showErrorDialog("Application Startup Error",
                    "Failed to initialize navigation system. MainLayoutController registration failed: " + e.getMessage());
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

            // Enhanced stage configuration following Vietnamese guide specifications
            primaryStage.setTitle(APP_TITLE);
            primaryStage.setScene(scene);
            
            // Vietnamese guide: Enhanced stage configuration
            primaryStage.setMaximized(true);
            primaryStage.setResizable(true);
            Rectangle2D screenBounds2 = Screen.getPrimary().getVisualBounds();
            primaryStage.setMinWidth(Math.min(800, screenBounds2.getWidth() * 0.6));
            primaryStage.setMinHeight(Math.min(600, screenBounds2.getHeight() * 0.6));
            
            // Responsive window sizing
            primaryStage.setWidth(sceneWidth);
            primaryStage.setHeight(sceneHeight);
            
            // Center the window on screen
            primaryStage.setX((screenBounds.getWidth() - sceneWidth) / 2);
            primaryStage.setY((screenBounds.getHeight() - sceneHeight) / 2);
            
            // Enhanced window management with proper shutdown handling
            primaryStage.setOnCloseRequest(event -> {
                logger.info("AIMS Application close requested");
                try {
                    // Graceful shutdown procedures
                    if (serviceFactory != null) {
                        // Any cleanup needed by services
                        logger.info("ServiceFactory cleanup completed");
                    }
                    System.out.println("AIMS Application is closing...");
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Error during application shutdown", e);
                }
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
            
            // PHASE 1 FIX: Verify MainLayoutController registration after initialization
            if (!MainLayoutControllerRegistry.isAvailable()) {
                logger.warning("MainLayoutController registry validation failed after initialization");
                // Try to re-register
                try {
                    MainLayoutControllerRegistry.setInstance(mainLayoutController);
                    logger.info("MainLayoutController re-registration successful");
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Failed to re-register MainLayoutController", e);
                }
            } else {
                logger.info("MainLayoutController registry validation passed - navigation system ready");
            }

        } catch (IOException e) {
            e.printStackTrace();
            showErrorDialog("Application Load Error", "Failed to load the main application interface: \n" + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            showErrorDialog("Application Startup Error", "An unexpected error occurred during application startup: \n" + e.getMessage());
        }
    }

    private void showErrorDialog(String title, String content) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle("Application Error");
        alert.setHeaderText(title);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
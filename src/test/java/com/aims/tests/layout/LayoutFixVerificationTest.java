package com.aims.tests.layout;

import com.aims.core.presentation.controllers.MainLayoutController;
import com.aims.core.presentation.controllers.HomeScreenController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Manual test class to verify that the layout fix works correctly.
 * This test loads the main layout and home screen to visually verify 
 * that the content fills the entire window properly.
 */
public class LayoutFixVerificationTest extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        System.out.println("LayoutFixVerificationTest: Starting layout verification test...");
        
        // Load main layout
        FXMLLoader mainLoader = new FXMLLoader(getClass().getResource("/com/aims/presentation/views/main_layout.fxml"));
        Parent root = mainLoader.load();
        MainLayoutController mainController = mainLoader.getController();
        
        // Set scene with specific size to test layout
        Scene scene = new Scene(root, 1400, 900);
        
        primaryStage.setTitle("AIMS Layout Fix Verification Test");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1200);
        primaryStage.setMinHeight(800);
        
        // Set up stage size change listener to debug layout
        primaryStage.widthProperty().addListener((obs, oldVal, newVal) -> {
            System.out.println("Stage width changed: " + oldVal + " -> " + newVal);
        });
        
        primaryStage.heightProperty().addListener((obs, oldVal, newVal) -> {
            System.out.println("Stage height changed: " + oldVal + " -> " + newVal);
        });
        
        primaryStage.show();
        
        // Initialize main controller and load home screen after stage is shown
        javafx.application.Platform.runLater(() -> {
            try {
                System.out.println("Loading home screen for layout verification...");
                mainController.completeInitialization();
                
                // Log dimensions after loading
                javafx.application.Platform.runLater(() -> {
                    System.out.println("Final stage size: " + primaryStage.getWidth() + "x" + primaryStage.getHeight());
                    System.out.println("Scene size: " + scene.getWidth() + "x" + scene.getHeight());
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("Error during initialization: " + e.getMessage());
            }
        });
    }

    public static void main(String[] args) {
        System.out.println("Starting AIMS Layout Fix Verification Test");
        System.out.println("This test will open a window to visually verify that:");
        System.out.println("1. Home screen fills the entire content area");
        System.out.println("2. Product grid is centered and responsive");
        System.out.println("3. No content appears in a small corner");
        System.out.println();
        launch(args);
    }
}

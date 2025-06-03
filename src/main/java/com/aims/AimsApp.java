package com.aims;

import com.aims.core.presentation.controllers.MainLayoutController;
import com.aims.core.presentation.utils.FXMLSceneManager;
import com.aims.core.shared.constants.FXMLPaths;
import com.aims.core.shared.ServiceFactory;


import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class AimsApp extends Application {

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
        sceneManager.setPrimaryStage(primaryStage); // Cung cấp primaryStage cho SceneManager

        try {
            FXMLLoader loader = sceneManager.getLoader(FXMLPaths.MAIN_LAYOUT);
            Parent root = loader.load();

            this.mainLayoutController = loader.getController();
            if (mainLayoutController == null) {
                System.err.println("CRITICAL: MainLayoutController is null after loading FXML: " + FXMLPaths.MAIN_LAYOUT);
                showErrorDialog("Application Startup Error", "Cannot load main application layout controller.");
                return;
            }

            // Cung cấp HostServices cho MainLayoutController (để mở trình duyệt)
            // mainLayoutController.setHostServices(getHostServices()); // Cần thêm phương thức này vào MainLayoutController

            // Cung cấp MainLayoutController cho SceneManager để các controller con có thể tham chiếu
            sceneManager.setMainLayoutController(this.mainLayoutController);

            // Inject dependencies into MainLayoutController
            mainLayoutController.setAuthenticationService(serviceFactory.getAuthenticationService());
            mainLayoutController.setSceneManager(sceneManager);
            mainLayoutController.setServiceFactory(serviceFactory);
            
            // Set the ServiceFactory in SceneManager so it can inject dependencies into child controllers
            sceneManager.setServiceFactory(serviceFactory);


            Scene scene = new Scene(root, MIN_WINDOW_WIDTH, MIN_WINDOW_HEIGHT);

            // Load CSS
            try {
                String globalCssPath = "/styles/global.css"; // Giả sử global.css nằm trong src/main/resources/styles/
                String themeCssPath = "/styles/theme.css";   // Giả sử theme.css nằm trong src/main/resources/styles/
                
                if (getClass().getResource(globalCssPath) != null) {
                    scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource(globalCssPath)).toExternalForm());
                } else {
                    System.err.println("Warning: Global CSS not found at " + globalCssPath);
                }
                if (getClass().getResource(themeCssPath) != null) {
                     scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource(themeCssPath)).toExternalForm());
                } else {
                     System.err.println("Warning: Theme CSS not found at " + themeCssPath);
                }

            } catch (NullPointerException e) {
                System.err.println("Warning: Could not load one or more CSS files. Ensure they are in src/main/resources/styles/ and paths are correct.");
                e.printStackTrace();
            }


            primaryStage.setTitle(APP_TITLE);
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(MIN_WINDOW_WIDTH);
            primaryStage.setMinHeight(MIN_WINDOW_HEIGHT);
            primaryStage.setOnCloseRequest(event -> {
                // Xử lý dọn dẹp tài nguyên nếu cần trước khi đóng ứng dụng
                System.out.println("AIMS Application is closing...");
                // Ví dụ: SQLiteConnector.getInstance().closeConnection(); // Nếu bạn có cơ chế đóng kết nối chung
            });
            primaryStage.show();

            // The MainLayoutController's initialize method will load the home screen by default
            // mainLayoutController.navigateToHome(); // This is now typically called in MainLayoutController.initialize()

        } catch (IOException e) {
            e.printStackTrace();
            showErrorDialog("Application Load Error", "Failed to load the main application interface: \n" + e.getMessage());
        } catch (Exception e) {
            // Catch-all for other initialization errors
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
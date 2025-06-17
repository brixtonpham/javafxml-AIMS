package com.aims.core.presentation.controllers;

import javafx.fxml.FXML;
import javafx.scene.layout.BorderPane;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.VBox;
import javafx.event.ActionEvent;
import javafx.application.Platform;

import com.aims.core.application.services.*;
import com.aims.core.entities.UserAccount;
import com.aims.core.presentation.utils.FXMLSceneManager;
import com.aims.core.shared.ServiceFactory;
import com.aims.core.shared.constants.FXMLPaths;
import java.util.Stack;

/**
 * Main layout controller that manages the application's root layout
 * and provides access to common services for child controllers
 */
public class MainLayoutController {

    // Interface for child controllers to implement
    public interface IChildController {
        void setMainLayoutController(MainLayoutController controller);
    }

    @FXML private BorderPane mainBorderPane;
    @FXML private BorderPane contentPane;  // This is the actual content container from FXML
    @FXML private VBox headerBox;
    @FXML private Label headerTitle;
    @FXML private Label userNameLabel;
    @FXML private Node loadingIndicator;
    @FXML private MenuBar menuBar;
    @FXML private Menu adminMenu;
    @FXML private Menu pmMenu;
    @FXML private MenuItem loginMenuItem;
    @FXML private MenuItem logoutMenuItem;
    
    private ServiceFactory serviceFactory;
    private FXMLSceneManager sceneManager;
    private IAuthenticationService authService;
    private UserAccount currentUser;
    private Stack<NavigationContext> navigationHistory;

    private static class NavigationContext {
        private final String fxmlPath;
        private final String screenTitle;

        NavigationContext(String fxmlPath, String screenTitle) {
            this.fxmlPath = fxmlPath;
            this.screenTitle = screenTitle;
        }

        public String getFxmlPath() { return fxmlPath; }
        public String getScreenTitle() { return screenTitle; }
    }

    public MainLayoutController() {
        navigationHistory = new Stack<>();
    }

    @FXML
    public void initialize() {
        if (loadingIndicator != null) {
            loadingIndicator.setVisible(false);
            loadingIndicator.setManaged(false);
        }
    }

    public void completeInitialization() {
        loadContent(FXMLPaths.HOME_SCREEN);
        setHeaderTitle("AIMS Home");
    }

    public void setServiceFactory(ServiceFactory factory) {
        this.serviceFactory = factory;
    }

    public void setSceneManager(FXMLSceneManager manager) {
        this.sceneManager = manager;
    }

    public void setAuthenticationService(IAuthenticationService service) {
        this.authService = service;
    }

    public void setContent(Node content) {
        if (contentPane != null) {
            contentPane.setCenter(content);
        } else {
            System.err.println("MainLayoutController.setContent: contentPane is null!");
        }
    }

    public void setHeaderTitle(String title) {
        if (headerTitle != null) {
            headerTitle.setText(title);
        }
    }

    public void showLoading(boolean show) {
        if (loadingIndicator != null) {
            loadingIndicator.setVisible(show);
            loadingIndicator.setManaged(show);
        }
    }

    public void setCurrentUser(UserAccount user, String displayName) {
        this.currentUser = user;
        if (userNameLabel != null) {
            userNameLabel.setText(displayName != null ? displayName : "Guest");
        }
    }

    public UserAccount getCurrentUser() {
        return currentUser;
    }

    public Node getContentPane() {
        return contentPane;
    }

    public BorderPane getMainContainer() {
        return mainBorderPane;
    }

    public Object loadContent(String fxmlPath) {
        if (sceneManager != null && contentPane != null) {
            // Use FXMLSceneManager for proper dependency injection
            Object controller = sceneManager.loadFXMLIntoPane(contentPane, fxmlPath);
            System.out.println("MainLayoutController.loadContent: Content loaded successfully: " + fxmlPath +
                             (controller != null ? " with controller: " + controller.getClass().getSimpleName() : ""));
            return controller;
        } else {
            System.err.println("MainLayoutController.loadContent: SceneManager or contentPane is null!");
            System.err.println("  SceneManager: " + (sceneManager != null ? "available" : "null"));
            System.err.println("  ContentPane: " + (contentPane != null ? "available" : "null"));
            return null;
        }
    }

    public Object loadContentWithHistory(String fxmlPath, String screenTitle) {
        System.out.println("MainLayoutController.loadContentWithHistory: Loading " + fxmlPath + " with title: " + screenTitle);
        
        if (sceneManager != null && contentPane != null) {
            // Use FXMLSceneManager's history-aware loading
            Object controller = sceneManager.loadContentWithHistory(contentPane, fxmlPath, screenTitle);
            System.out.println("MainLayoutController.loadContentWithHistory: Content loaded successfully: " + fxmlPath +
                             (controller != null ? " with controller: " + controller.getClass().getSimpleName() : ""));
            return controller;
        } else {
            System.err.println("MainLayoutController.loadContentWithHistory: SceneManager or contentPane not available, falling back to regular loadContent");
            navigationHistory.push(new NavigationContext(fxmlPath, screenTitle));
            return loadContent(fxmlPath);
        }
    }

    public boolean navigateBack() {
        if (navigationHistory.size() > 1) {
            navigationHistory.pop(); // Remove current
            NavigationContext previous = navigationHistory.peek();
            loadContent(previous.getFxmlPath());
            setHeaderTitle(previous.getScreenTitle());
            return true;
        }
        return false;
    }

    @FXML
    public void navigateToHome(ActionEvent event) {
        navigationHistory.clear();
        loadContentWithHistory(FXMLPaths.HOME_SCREEN, "AIMS Home");
    }

    // Overloaded method for backward compatibility
    public void navigateToHome() {
        navigationHistory.clear();
        loadContentWithHistory(FXMLPaths.HOME_SCREEN, "AIMS Home");
    }

    @FXML
    public void handleLogoutAction(ActionEvent event) {
        if (authService != null && currentUser != null) {
            authService.logout(currentUser.getUserId());
        }
        currentUser = null;
        setCurrentUser(null, "Guest");
        navigateToHome(event);
    }

    @FXML
    public void handleLoginAction(ActionEvent event) {
        loadContentWithHistory(FXMLPaths.LOGIN_SCREEN, "Login");
    }

    @FXML
    public void handleExitAction(ActionEvent event) {
        Platform.exit();
    }

    @FXML
    public void navigateToCart(ActionEvent event) {
        loadContentWithHistory(FXMLPaths.CART_SCREEN, "Shopping Cart");
    }

    @FXML
    public void navigateToUserManagement(ActionEvent event) {
        loadContentWithHistory(FXMLPaths.ADMIN_USER_MANAGEMENT_SCREEN, "User Management");
    }

    @FXML
    public void navigateToAdminProductList(ActionEvent event) {
        loadContentWithHistory(FXMLPaths.ADMIN_PRODUCT_LIST_SCREEN, "Product Management");
    }

    @FXML
    public void navigateToPmProductList(ActionEvent event) {
        loadContentWithHistory(FXMLPaths.ADMIN_PRODUCT_LIST_SCREEN, "Product Management");
    }

    @FXML
    public void navigateToPmPendingOrders(ActionEvent event) {
        loadContentWithHistory(FXMLPaths.PM_PENDING_ORDERS_LIST_SCREEN, "Pending Orders");
    }

    // Service access methods for convenience
    public IProductService getProductService() {
        return serviceFactory.getProductService();
    }

    public IOrderService getOrderService() {
        return serviceFactory.getOrderService();
    }

    public ICartService getCartService() {
        return serviceFactory.getCartService();
    }

    public IPaymentService getPaymentService() {
        return serviceFactory.getPaymentService();
    }

    public IDeliveryCalculationService getDeliveryService() {
        return serviceFactory.getDeliveryCalculationService();
    }

    public IUserAccountService getUserAccountService() {
        return serviceFactory.getUserAccountService();
    }

    public IAuthenticationService getAuthenticationService() {
        return serviceFactory.getAuthenticationService();
    }
}
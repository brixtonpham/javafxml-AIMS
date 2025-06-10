package com.aims.core.presentation.controllers;

import com.aims.core.application.services.IAuthenticationService;
import com.aims.core.entities.UserAccount;
import com.aims.core.entities.Role; // Để kiểm tra vai trò sau khi đăng nhập
// import com.aims.presentation.utils.AlertHelper;
import com.aims.core.presentation.utils.FXMLSceneManager;
// import com.aims.core.application.dtos.UserSessionDTO; // Nếu AuthenticationService trả về DTO session
import com.aims.core.shared.exceptions.AuthenticationException;
import com.aims.core.shared.exceptions.ResourceNotFoundException;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
// import javafx.stage.Stage; // Nếu cần đóng cửa sổ login

import java.sql.SQLException;
import java.util.Set;

public class LoginScreenController {

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Button loginButton;
    @FXML
    private Button cancelButton;
    @FXML
    private Label errorMessageLabel;

    // @Inject
    private IAuthenticationService authenticationService;
    private MainLayoutController mainLayoutController; // Để cập nhật menu hoặc điều hướng
    private FXMLSceneManager sceneManager;
    // private Stage currentStage; // Để có thể đóng cửa sổ login nếu nó là popup

    public LoginScreenController() {
        // authenticationService = new AuthenticationServiceImpl(...); // DI
    }

    public void setMainLayoutController(MainLayoutController mainLayoutController) {
        this.mainLayoutController = mainLayoutController;
    }
    
    public void setSceneManager(FXMLSceneManager sceneManager) {
        this.sceneManager = sceneManager;
    }
    
    public void setAuthenticationService(IAuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }
    
    // public void setCurrentStage(Stage stage) { this.currentStage = stage; }


    public void initialize() {
        setErrorMessage("", false);
        // loginButton.disableProperty().bind(
        //     usernameField.textProperty().isEmpty().or(passwordField.textProperty().isEmpty())
        // ); // Tự động disable nút login nếu trường trống
    }

    @FXML
    void handleLoginAction(ActionEvent event) {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            setErrorMessage("Username and password cannot be empty.", true);
            return;
        }
        setErrorMessage("", false); // Clear previous errors

        if (authenticationService == null) {
            setErrorMessage("Authentication service is not available.", true);
            return;
        }

        try {
            UserAccount authenticatedUser = authenticationService.login(username, password);
            System.out.println("Login successful for user: " + authenticatedUser.getUsername());

            // Save user information in MainLayoutController
            if (mainLayoutController != null) {
                mainLayoutController.setCurrentUser(authenticatedUser, authenticatedUser.getUserId());
            }

            // Navigate to appropriate dashboard based on role
            Set<Role> roles = authenticatedUser.getRoleAssignments().stream()
                                       .map(assignment -> assignment.getRole())
                                       .collect(java.util.stream.Collectors.toSet());

            boolean isAdmin = roles.stream().anyMatch(role -> "ADMIN".equalsIgnoreCase(role.getRoleId()));
            boolean isPm = roles.stream().anyMatch(role -> "PRODUCT_MANAGER".equalsIgnoreCase(role.getRoleId()));

            if (isAdmin) {
                if (mainLayoutController != null) {
                    mainLayoutController.loadContent("/com/aims/presentation/views/admin_dashboard_screen.fxml");
                    mainLayoutController.setHeaderTitle("Admin Dashboard");
                    System.out.println("Navigated to Admin Dashboard");
                }
            } else if (isPm) {
                if (mainLayoutController != null) {
                    mainLayoutController.loadContent("/com/aims/presentation/views/pm_dashboard_screen.fxml");
                    mainLayoutController.setHeaderTitle("Product Manager Dashboard");
                    System.out.println("Navigated to PM Dashboard");
                }
            } else {
                // User does not have sufficient privileges for admin/PM access
                setErrorMessage("User does not have sufficient privileges for admin access.", true);
                if (authenticationService != null) {
                    authenticationService.logout(authenticatedUser.getUserId());
                }
                return;
            }

            // Clear login form after successful login
            usernameField.clear();
            passwordField.clear();
            setErrorMessage("Login successful!", true);
            errorMessageLabel.setStyle("-fx-text-fill: green;");

        } catch (AuthenticationException e) {
            setErrorMessage("Invalid username or password.", true);
            System.err.println("Authentication failed: " + e.getMessage());
        } catch (SQLException e) {
            setErrorMessage("Database error during login. Please try again later.", true);
            e.printStackTrace();
        } catch (Exception e) {
            setErrorMessage("An unexpected error occurred: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }

    @FXML
    void handleCancelAction(ActionEvent event) {
        System.out.println("Login cancelled.");
        
        // Clear the form fields
        usernameField.clear();
        passwordField.clear();
        setErrorMessage("", false);
        
        // Navigate back to home screen
        if (mainLayoutController != null) {
            mainLayoutController.navigateToHome(); // Navigate to home screen
        }
    }

    private void setErrorMessage(String message, boolean visible) {
        errorMessageLabel.setText(message);
        errorMessageLabel.setVisible(visible);
        errorMessageLabel.setManaged(visible);
        if (visible && !message.toLowerCase().contains("successful")) {
            errorMessageLabel.setStyle("-fx-text-fill: red;"); // Default to red for errors
        }
    }
}
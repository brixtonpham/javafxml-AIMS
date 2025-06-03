package com.aims.core.presentation.controllers;

import com.aims.core.application.services.IAuthenticationService;
import com.aims.core.entities.UserAccount;
import com.aims.core.entities.Role; // Để kiểm tra vai trò sau khi đăng nhập
// import com.aims.presentation.utils.AlertHelper;
// import com.aims.presentation.utils.FXMLSceneManager;
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
    // private MainLayoutController mainLayoutController; // Để cập nhật menu hoặc điều hướng
    // private FXMLSceneManager sceneManager;
    // private Stage currentStage; // Để có thể đóng cửa sổ login nếu nó là popup

    public LoginScreenController() {
        // authenticationService = new AuthenticationServiceImpl(...); // DI
    }

    // public void setMainLayoutController(MainLayoutController mainLayoutController) { this.mainLayoutController = mainLayoutController; }
    // public void setSceneManager(FXMLSceneManager sceneManager) { this.sceneManager = sceneManager; }
    // public void setAuthenticationService(IAuthenticationService authenticationService) { this.authenticationService = authenticationService; }
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

        // if (authenticationService == null) {
        //     AlertHelper.showErrorAlert("Service Error", "Authentication service is not available.");
        //     return;
        // }

        try {
            // UserAccount authenticatedUser = authenticationService.login(username, password);
            // System.out.println("Login successful for user: " + authenticatedUser.getUsername());

            // // TODO: Lưu thông tin người dùng đăng nhập (ví dụ trong MainLayoutController hoặc một session manager)
            // // mainLayoutController.setCurrentUser(authenticatedUser);
            // // mainLayoutController.updateMenuVisibility();

            // // Điều hướng đến dashboard tương ứng với vai trò
            // Set<Role> roles = authenticatedUser.getRoleAssignments().stream()
            //                            .map(UserRoleAssignment::getRole)
            //                            .collect(Collectors.toSet()); // Lấy roles nếu entity UserAccount có getRoleAssignments()

            // // Hoặc lấy roles từ IUserAccountService nếu cần:
            // // Set<Role> roles = userAccountService.getUserRoles(authenticatedUser.getUserId());


            // boolean isAdmin = roles.stream().anyMatch(role -> "ADMIN".equalsIgnoreCase(role.getRoleId()));
            // boolean isPm = roles.stream().anyMatch(role -> "PRODUCT_MANAGER".equalsIgnoreCase(role.getRoleId()));

            // if (isAdmin) {
            //     // mainLayoutController.loadContent(FXMLSceneManager.ADMIN_DASHBOARD_SCREEN);
            //     // mainLayoutController.setHeaderTitle("Admin Dashboard");
            // } else if (isPm) {
            //     // mainLayoutController.loadContent(FXMLSceneManager.PM_DASHBOARD_SCREEN);
            //     // mainLayoutController.setHeaderTitle("Product Manager Dashboard");
            // } else {
            //     // Vai trò không phù hợp để vào hệ thống quản trị
            //     setErrorMessage("User does not have sufficient privileges.", true);
            //     // authenticationService.logout(null); // Clear any partial login state if applicable
            //     return;
            // }

            // if (currentStage != null) { // Nếu màn hình login là một cửa sổ riêng
            //     currentStage.close();
            // }


            // --- Giả lập đăng nhập thành công để test navigation ---
            System.out.println("Simulating successful login for: " + username);
            setErrorMessage("Login Successful (Simulated)!", true);
            errorMessageLabel.setStyle("-fx-text-fill: green;");
            // Giả sử điều hướng về home và mainLayout sẽ cập nhật menu
            // if (mainLayoutController != null) {
            //    mainLayoutController.navigateToHome(null);
            // }


        }
        // catch (SQLException e) {
        //     setErrorMessage("Database error during login. Please try again later.", true);
        //     e.printStackTrace();
        // }
        catch (Exception e) { // Bắt các lỗi không mong muốn khác
            setErrorMessage("An unexpected error occurred: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }

    @FXML
    void handleCancelAction(ActionEvent event) {
        System.out.println("Login cancelled.");
        // if (currentStage != null) { // Nếu màn hình login là một cửa sổ riêng
        //     currentStage.close();
        // } else if (mainLayoutController != null) { // Nếu là một phần của layout chính
        //     mainLayoutController.navigateToHome(null); // Quay về trang chủ
        // }
        // Hoặc đơn giản là xóa các trường
        usernameField.clear();
        passwordField.clear();
        setErrorMessage("", false);
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
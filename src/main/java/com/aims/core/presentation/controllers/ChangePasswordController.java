package com.aims.core.presentation.controllers;

import com.aims.core.application.services.IUserAccountService;
import com.aims.core.entities.UserAccount; // Để lấy thông tin người dùng hiện tại
// import com.aims.presentation.utils.AlertHelper;
// import com.aims.presentation.utils.FXMLSceneManager;
// import javafx.stage.Stage; // Nếu đây là một dialog

import com.aims.core.shared.exceptions.AuthenticationException;
import com.aims.core.shared.exceptions.ResourceNotFoundException;
import com.aims.core.shared.exceptions.ValidationException;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;

import java.sql.SQLException;

public class ChangePasswordController {

    @FXML
    private Label usernameLabel;
    @FXML
    private PasswordField oldPasswordField;
    @FXML
    private PasswordField newPasswordField;
    @FXML
    private PasswordField confirmNewPasswordField;
    @FXML
    private Label errorMessageLabel;
    @FXML
    private Label successMessageLabel;
    @FXML
    private Button changePasswordButton;

    // @Inject
    private IUserAccountService userAccountService;
    // private MainLayoutController mainLayoutController; // Để điều hướng sau khi thành công
    // private FXMLSceneManager sceneManager;
    // private Stage dialogStage; // Nếu đây là dialog

    private String currentUserId; // User ID của người dùng đang đăng nhập
    private String currentUsername; // Username của người dùng đang đăng nhập

    public ChangePasswordController() {
        // userAccountService = new UserAccountServiceImpl(...); // DI
    }

    // public void setUserAccountService(IUserAccountService userAccountService) { this.userAccountService = userAccountService; }
    // public void setMainLayoutController(MainLayoutController mainLayoutController) { this.mainLayoutController = mainLayoutController; }
    // public void setSceneManager(FXMLSceneManager sceneManager) { this.sceneManager = sceneManager; }
    // public void setDialogStage(Stage dialogStage) { this.dialogStage = dialogStage; }

    /**
     * Được gọi để thiết lập thông tin người dùng hiện tại.
     * @param userId ID của người dùng đang đăng nhập.
     * @param username Username của người dùng đang đăng nhập.
     */
    public void setUserContext(String userId, String username) {
        this.currentUserId = userId;
        this.currentUsername = username;
        if (usernameLabel != null) {
            usernameLabel.setText(this.currentUsername != null ? this.currentUsername : "N/A");
        }
    }


    public void initialize() {
        setErrorMessage("", false);
        setSuccessMessage("", false);
        if (currentUsername != null) { // Có thể được gọi sau setUserContext nếu FXML được load trước
            usernameLabel.setText(currentUsername);
        }
    }

    @FXML
    void handleChangePasswordAction(ActionEvent event) {
        setErrorMessage("", false);
        setSuccessMessage("", false);

        String oldPass = oldPasswordField.getText();
        String newPass = newPasswordField.getText();
        String confirmNewPass = confirmNewPasswordField.getText();

        if (oldPass.isEmpty() || newPass.isEmpty() || confirmNewPass.isEmpty()) {
            setErrorMessage("All password fields are required.", true);
            return;
        }
        if (!newPass.equals(confirmNewPass)) {
            setErrorMessage("New password and confirmation do not match.", true);
            return;
        }
        if (newPass.length() < 8) { // Ví dụ: chính sách mật khẩu cơ bản
            setErrorMessage("New password must be at least 8 characters long.", true);
            return;
        }
        if (currentUserId == null) {
            setErrorMessage("User context not set. Cannot change password.", true);
            // AlertHelper.showErrorAlert("Error", "User information is missing.");
            return;
        }
        // if (userAccountService == null) {
        //     AlertHelper.showErrorAlert("Service Error", "User account service is not available.");
        //     return;
        // }

        try {
            // userAccountService.changeOwnPassword(currentUserId, oldPass, newPass);
            // AlertHelper.showInfoAlert("Password Changed", "Your password has been changed successfully.");
            // setSuccessMessage("Password changed successfully!", true);
            // clearFields();

            // // Tự động điều hướng về dashboard hoặc màn hình trước đó sau khi thành công
            // if (mainLayoutController != null && sceneManager != null) {
            //     // Ví dụ, nếu biết người dùng là Admin hay PM, điều hướng về dashboard tương ứng
            //     // Hoặc đơn giản là về một màn hình "Tài khoản của tôi" nếu có
            //     // mainLayoutController.navigateToPreviousScreen(); // Cần một cơ chế lưu màn hình trước đó
            // } else if (dialogStage != null) {
            //     dialogStage.close();
            // }


             // --- SIMULATED SUCCESS ---
            System.out.println("Simulated Password Change for user: " + currentUsername);
            System.out.println("Old: " + oldPass + ", New: " + newPass);
            setSuccessMessage("Password changed successfully! (Simulated)", true);
            clearFields();


        }
        // catch (AuthenticationException e) {
        //     setErrorMessage("Incorrect old password. Please try again.", true);
        // } catch (ValidationException | ResourceNotFoundException | SQLException e) {
        //     setErrorMessage("Error changing password: " + e.getMessage(), true);
        //     e.printStackTrace();
        // }
        catch (Exception e) { // Bắt các lỗi không mong muốn khác
            setErrorMessage("An unexpected error occurred: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }

    private void clearFields() {
        oldPasswordField.clear();
        newPasswordField.clear();
        confirmNewPasswordField.clear();
    }

    @FXML
    void handleCancelAction(ActionEvent event) {
        System.out.println("Change Password cancelled.");
        // if (dialogStage != null) {
        //     dialogStage.close();
        // } else if (mainLayoutController != null && sceneManager != null) {
        //     // Điều hướng về màn hình dashboard trước đó
        //     // Ví dụ: mainLayoutController.navigateToPreviousScreen(); hoặc mainLayoutController.navigateToDashboard();
        // }
        clearFields();
        setErrorMessage("", false);
        setSuccessMessage("", false);
    }

    private void setErrorMessage(String message, boolean visible) {
        errorMessageLabel.setText(message);
        errorMessageLabel.setVisible(visible);
        errorMessageLabel.setManaged(visible);
        if (visible) successMessageLabel.setVisible(false); // Ẩn thông báo thành công nếu có lỗi
    }

    private void setSuccessMessage(String message, boolean visible) {
        successMessageLabel.setText(message);
        successMessageLabel.setVisible(visible);
        successMessageLabel.setManaged(visible);
        if (visible) errorMessageLabel.setVisible(false); // Ẩn thông báo lỗi nếu thành công
    }
}
package com.aims.core.presentation.controllers;

import com.aims.core.application.dtos.UserRegistrationDTO;
import com.aims.core.application.services.IUserAccountService;
import com.aims.core.entities.Role;
import com.aims.core.enums.UserStatus;
// import com.aims.presentation.utils.AlertHelper;
// import javafx.stage.Stage; // Nếu đây là một dialog

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxListCell; // For multi-selection in ListView

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class AddUserFormController {

    @FXML
    private TextField usernameField;
    @FXML
    private TextField emailField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private TextField userIdField; // Optional
    @FXML
    private ComboBox<UserStatus> statusComboBox;
    @FXML
    private ListView<Role> rolesListView; // Hiển thị Role objects, nhưng lấy ID khi submit
    @FXML
    private Label errorMessageLabel;
    @FXML
    private Button createUserButton;

    // @Inject
    private IUserAccountService userAccountService;
    private AdminUserManagementController parentController; // Để gọi refresh sau khi thêm
    private String currentAdminId; // Admin thực hiện hành động này
    // private Stage dialogStage; // Nếu đây là một dialog

    private ObservableList<Role> availableRoles = FXCollections.observableArrayList();

    public AddUserFormController() {
        // userAccountService = new UserAccountServiceImpl(...); // DI
    }

    // public void setUserAccountService(IUserAccountService userAccountService) { this.userAccountService = userAccountService; }
    // public void setParentController(AdminUserManagementController parentController) { this.parentController = parentController; }
    // public void setCurrentAdminId(String adminId) { this.currentAdminId = adminId; }
    // public void setDialogStage(Stage dialogStage) { this.dialogStage = dialogStage; }

    public void initialize() {
        setErrorMessage("", false);
        statusComboBox.setItems(FXCollections.observableArrayList(UserStatus.ACTIVE, UserStatus.PENDING_ACTIVATION, UserStatus.SUSPENDED));
        statusComboBox.setValue(UserStatus.ACTIVE); // Default

        // Cấu hình ListView để cho phép chọn nhiều vai trò
        rolesListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        // Custom cell factory để hiển thị tên vai trò
        rolesListView.setCellFactory(listView -> {
            return new ListCell<Role>() {
                @Override
                protected void updateItem(Role role, boolean empty) {
                    super.updateItem(role, empty);
                    if (empty || role == null) {
                        setText(null);
                    } else {
                        setText(role.getRoleName());
                    }
                }
            };
        });

        loadAvailableRoles();
    }

    private void loadAvailableRoles() {
        // if (userAccountService == null || currentAdminId == null) {
        //     setErrorMessage("Cannot load roles: Service or admin context missing.", true);
        //     return;
        // }
        // try {
        //     List<Role> roles = userAccountService.getAllRoles(currentAdminId);
        //     availableRoles.setAll(roles);
        //     rolesListView.setItems(availableRoles);
        // } catch (SQLException | AuthorizationException e) {
        //     setErrorMessage("Error loading available roles: " + e.getMessage(), true);
        //     e.printStackTrace();
        // }

        // Dummy data for UI testing
        System.out.println("loadAvailableRoles called - Implement with actual service call.");
        // Role r1 = new Role("ADMIN", "Administrator");
        // Role r2 = new Role("PRODUCT_MANAGER", "Product Manager");
        // availableRoles.setAll(r1, r2);
        // rolesListView.setItems(availableRoles);
    }

    @FXML
    void handleCreateUserAction(ActionEvent event) {
        setErrorMessage("", false);
        String username = usernameField.getText();
        String email = emailField.getText();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        String customUserId = userIdField.getText(); // Có thể trống
        UserStatus status = statusComboBox.getValue();
        ObservableList<Role> selectedRolesObjects = rolesListView.getSelectionModel().getSelectedItems();

        // --- Basic Validations ---
        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            setErrorMessage("Username, Email, Password, and Confirm Password are required.", true);
            return;
        }
        if (!password.equals(confirmPassword)) {
            setErrorMessage("Passwords do not match.", true);
            return;
        }
        if (password.length() < 8) { // Example policy
            setErrorMessage("Password must be at least 8 characters long.", true);
            return;
        }
        if (selectedRolesObjects.isEmpty()) {
            setErrorMessage("At least one role must be assigned to the user.", true);
            return;
        }
        if (status == null) {
            setErrorMessage("Please select a user status.", true);
            return;
        }
        // TODO: More sophisticated email validation if needed

        // if (userAccountService == null || currentAdminId == null) {
        //     AlertHelper.showErrorAlert("Service Error", "User account service or admin context not available.");
        //     return;
        // }

        UserRegistrationDTO newUserDTO = new UserRegistrationDTO();
        if (customUserId != null && !customUserId.trim().isEmpty()) {
            newUserDTO.setUserId(customUserId.trim()); // Entity UserAccount sẽ tự sinh ID nếu DTO không có
        }
        newUserDTO.setUsername(username);
        newUserDTO.setEmail(email);
        newUserDTO.setPassword(password); // Service sẽ hash mật khẩu này
        newUserDTO.setStatus(status);
        Set<String> selectedRoleIds = selectedRolesObjects.stream().map(Role::getRoleId).collect(Collectors.toSet());
        newUserDTO.setRoleIds(selectedRoleIds);

        try {
            // UserAccount createdUser = userAccountService.createUser(newUserDTO, currentAdminId); // createUser nên nhận DTO
            // AlertHelper.showInfoAlert("User Created", "User '" + createdUser.getUsername() + "' created successfully.");
            //
            // if (parentController != null) {
            //     parentController.refreshUserList(); // Refresh the list in the parent screen
            // }
            // if (dialogStage != null) {
            //     dialogStage.close();
            // }

            // --- SIMULATED SUCCESS ---
            System.out.println("Simulated User Creation:");
            System.out.println("Username: " + newUserDTO.getUsername());
            System.out.println("Email: " + newUserDTO.getEmail());
            System.out.println("Status: " + newUserDTO.getStatus());
            System.out.println("Roles: " + newUserDTO.getRoleIds());
            setErrorMessage("User '" + newUserDTO.getUsername() + "' created (Simulated).", false);
            // Clear form for next entry if not closing dialog
            // clearForm();


        }
        // catch (ValidationException | SQLException | AuthorizationException e) {
        //     setErrorMessage("Error creating user: " + e.getMessage(), true);
        //     e.printStackTrace();
        // }
        catch (Exception e) { // Bắt các lỗi không mong muốn khác
            setErrorMessage("An unexpected error occurred: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }

    private void clearForm() {
        usernameField.clear();
        emailField.clear();
        passwordField.clear();
        confirmPasswordField.clear();
        userIdField.clear();
        statusComboBox.getSelectionModel().selectFirst();
        rolesListView.getSelectionModel().clearSelection();
        setErrorMessage("", false);
    }


    @FXML
    void handleCancelAction(ActionEvent event) {
        System.out.println("Add User cancelled.");
        // if (dialogStage != null) {
        //     dialogStage.close();
        // } else if (mainLayoutController != null && sceneManager != null) { // Nếu là view trong main layout
        //     mainLayoutController.loadContent(FXMLSceneManager.ADMIN_USER_MANAGEMENT_SCREEN);
        // }
        // Hoặc đơn giản là xóa form
        clearForm();
    }

    private void setErrorMessage(String message, boolean isError) {
        errorMessageLabel.setText(message);
        errorMessageLabel.setVisible(message != null && !message.isEmpty());
        errorMessageLabel.setManaged(message != null && !message.isEmpty());
        errorMessageLabel.setStyle(isError ? "-fx-text-fill: red;" : "-fx-text-fill: green;");
    }
}
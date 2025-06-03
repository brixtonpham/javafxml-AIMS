package com.aims.core.presentation.controllers;

import com.aims.core.application.dtos.UserUpdateDTO; // DTO for updating user
import com.aims.core.application.services.IUserAccountService;
import com.aims.core.entities.UserAccount;
import com.aims.core.entities.Role;
import com.aims.core.enums.UserStatus;
// import com.aims.presentation.utils.AlertHelper;
// import javafx.stage.Stage; // If this is a dialog

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

public class EditUserFormController {

    @FXML
    private Label screenTitleLabel;
    @FXML
    private TextField userIdField;
    @FXML
    private TextField usernameField;
    @FXML
    private TextField emailField;
    @FXML
    private ComboBox<UserStatus> statusComboBox;
    @FXML
    private ListView<Role> rolesListView;
    @FXML
    private Label errorMessageLabel;
    @FXML
    private Button updateUserButton;

    // @Inject
    private IUserAccountService userAccountService;
    private AdminUserManagementController parentController; // To refresh list after update
    private String currentAdminIdPerformingAction; // Admin performing the update
    // private Stage dialogStage;

    private UserAccount userToEdit;
    private ObservableList<Role> allAvailableRoles = FXCollections.observableArrayList();
    private Set<String> initialRoleIds = new HashSet<>();


    public EditUserFormController() {
        // userAccountService = new UserAccountServiceImpl(...); // DI
    }

    // public void setUserAccountService(IUserAccountService userAccountService) { this.userAccountService = userAccountService; }
    // public void setParentController(AdminUserManagementController parentController) { this.parentController = parentController; }
    // public void setCurrentAdminIdPerformingAction(String adminId) { this.currentAdminIdPerformingAction = adminId; }
    // public void setDialogStage(Stage dialogStage) { this.dialogStage = dialogStage; }

    public void initialize() {
        setErrorMessage("", false);
        statusComboBox.setItems(FXCollections.observableArrayList(UserStatus.values())); // All statuses available for edit
        rolesListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
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
        loadAllAvailableRoles();
    }

    private void loadAllAvailableRoles() {
        // if (userAccountService == null || currentAdminIdPerformingAction == null) {
        //     setErrorMessage("Cannot load roles: Service or admin context missing.", true);
        //     return;
        // }
        // try {
        //     List<Role> roles = userAccountService.getAllRoles(currentAdminIdPerformingAction);
        //     allAvailableRoles.setAll(roles);
        //     rolesListView.setItems(allAvailableRoles);
        // } catch (SQLException | AuthorizationException e) {
        //     setErrorMessage("Error loading available roles: " + e.getMessage(), true);
        //     e.printStackTrace();
        // }
        System.out.println("loadAllAvailableRoles called - Implement with actual service call.");
        // Dummy data
        // Role r1 = new Role("ADMIN", "Administrator");
        // Role r2 = new Role("PRODUCT_MANAGER", "Product Manager");
        // allAvailableRoles.setAll(r1, r2);
        // rolesListView.setItems(allAvailableRoles);
    }

    /**
     * Called from AdminUserManagementController to pass the user to be edited.
     */
    public void setUserToEdit(UserAccount user) {
        this.userToEdit = user;
        if (userToEdit != null) {
            screenTitleLabel.setText("Edit User: " + userToEdit.getUsername());
            userIdField.setText(userToEdit.getUserId());
            usernameField.setText(userToEdit.getUsername());
            emailField.setText(userToEdit.getEmail());
            statusComboBox.setValue(userToEdit.getUserStatus());
            loadAndSelectUserRoles();
        } else {
            setErrorMessage("No user selected for editing.", true);
            updateUserButton.setDisable(true);
        }
    }

    private void loadAndSelectUserRoles() {
        if (userToEdit == null || userAccountService == null) return;
        initialRoleIds.clear();
        rolesListView.getSelectionModel().clearSelection();

        // try {
        //     Set<Role> userRoles = userAccountService.getUserRoles(userToEdit.getUserId());
        //     if (userRoles != null) {
        //         for (Role role : userRoles) {
        //             initialRoleIds.add(role.getRoleId());
        //             // Find the role in the allAvailableRoles list and select it
        //             for (Role availableRole : allAvailableRoles) {
        //                 if (availableRole.getRoleId().equals(role.getRoleId())) {
        //                     rolesListView.getSelectionModel().select(availableRole);
        //                     break;
        //                 }
        //             }
        //         }
        //     }
        // } catch (SQLException | ResourceNotFoundException e) {
        //     setErrorMessage("Error loading user roles: " + e.getMessage(), true);
        //     e.printStackTrace();
        // }
        System.out.println("loadAndSelectUserRoles called - Implement with actual service call.");
        // Dummy selection for UI testing
        // if (!allAvailableRoles.isEmpty()) {
        //     rolesListView.getSelectionModel().select(allAvailableRoles.get(0));
        //     initialRoleIds.add(allAvailableRoles.get(0).getRoleId());
        // }
    }


    @FXML
    void handleUpdateUserAction(ActionEvent event) {
        setErrorMessage("", false);
        if (userToEdit == null) {
            setErrorMessage("No user selected to update.", true);
            return;
        }

        String email = emailField.getText();
        UserStatus status = statusComboBox.getValue();
        ObservableList<Role> selectedRoleObjects = rolesListView.getSelectionModel().getSelectedItems();

        if (email.isEmpty() || status == null) {
            setErrorMessage("Email and Status are required.", true);
            return;
        }
        if (selectedRoleObjects.isEmpty()) {
            setErrorMessage("User must have at least one role.", true);
            return;
        }
        // TODO: Email format validation

        // if (userAccountService == null || currentAdminIdPerformingAction == null) {
        //     AlertHelper.showErrorAlert("Service Error", "User account service or admin context not available.");
        //     return;
        // }

        UserUpdateDTO updateDTO = new UserUpdateDTO();
        updateDTO.setUserId(userToEdit.getUserId());
        updateDTO.setEmail(email);
        updateDTO.setStatus(status);

        Set<String> newRoleIds = selectedRoleObjects.stream().map(Role::getRoleId).collect(Collectors.toSet());
        Set<String> rolesToAssign = new HashSet<>(newRoleIds);
        rolesToAssign.removeAll(initialRoleIds); // Roles to be newly assigned

        Set<String> rolesToRemove = new HashSet<>(initialRoleIds);
        rolesToRemove.removeAll(newRoleIds);     // Roles to be removed

        updateDTO.setRoleIdsToAssign(rolesToAssign);
        updateDTO.setRoleIdsToRemove(rolesToRemove);

        try {
            // UserAccount updatedUser = userAccountService.updateUserWithRoles(updateDTO, currentAdminIdPerformingAction);
            // // updateUserWithRoles would handle updating basic info AND role changes transactionally
            //
            // AlertHelper.showInfoAlert("User Updated", "User '" + updatedUser.getUsername() + "' updated successfully.");
            // if (parentController != null) {
            //     parentController.refreshUserList();
            // }
            // if (dialogStage != null) {
            //     dialogStage.close();
            // }

            // --- SIMULATED UPDATE ---
            System.out.println("Simulated User Update for: " + updateDTO.getUserId());
            System.out.println("New Email: " + updateDTO.getEmail());
            System.out.println("New Status: " + updateDTO.getStatus());
            System.out.println("Roles to Add: " + updateDTO.getRoleIdsToAssign());
            System.out.println("Roles to Remove: " + updateDTO.getRoleIdsToRemove());
            setErrorMessage("User '" + userToEdit.getUsername() + "' updated (Simulated).", false);


        }
        // catch (ValidationException | SQLException | ResourceNotFoundException | AuthorizationException e) {
        //     setErrorMessage("Error updating user: " + e.getMessage(), true);
        //     e.printStackTrace();
        // }
        catch (Exception e) {
             setErrorMessage("An unexpected error occurred: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }

    @FXML
    void handleCancelAction(ActionEvent event) {
        System.out.println("Edit User cancelled.");
        // if (dialogStage != null) {
        //     dialogStage.close();
        // } else if (mainLayoutController != null && sceneManager != null) { // If not a dialog
        //     mainLayoutController.loadContent(FXMLSceneManager.ADMIN_USER_MANAGEMENT_SCREEN);
        // }
    }

    private void setErrorMessage(String message, boolean isError) {
        errorMessageLabel.setText(message);
        errorMessageLabel.setVisible(message != null && !message.isEmpty());
        errorMessageLabel.setManaged(message != null && !message.isEmpty());
        errorMessageLabel.setStyle(isError ? "-fx-text-fill: red;" : "-fx-text-fill: green;");
    }
}
package com.aims.core.presentation.controllers;

import com.aims.core.application.services.IUserAccountService;
import com.aims.core.entities.UserAccount;
import com.aims.core.entities.Role;
import com.aims.core.enums.UserStatus;
import com.aims.core.enums.UserRole; // For default roles or role checking
// import com.aims.presentation.utils.AlertHelper;
// import com.aims.presentation.utils.FXMLSceneManager;
// import com.aims.common.utils.SearchResult; // If service returns paginated results

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.util.Callback;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class AdminUserManagementController {

    @FXML
    private TextField searchUserField;
    @FXML
    private ComboBox<String> roleFilterComboBox; // String for Role Names or IDs
    @FXML
    private ComboBox<UserStatus> statusFilterComboBox;
    @FXML
    private TableView<UserAccount> usersTableView;
    @FXML
    private TableColumn<UserAccount, String> userIdColumn;
    @FXML
    private TableColumn<UserAccount, String> usernameColumn;
    @FXML
    private TableColumn<UserAccount, String> emailColumn;
    @FXML
    private TableColumn<UserAccount, String> rolesColumn;
    @FXML
    private TableColumn<UserAccount, UserStatus> statusColumn;
    @FXML
    private TableColumn<UserAccount, Void> actionsUserColumn;

    @FXML
    private HBox userPaginationControls;
    @FXML
    private Button prevUserPageButton;
    @FXML
    private Label currentUserPageLabel;
    @FXML
    private Button nextUserPageButton;

    // @Inject
    private IUserAccountService userAccountService;
    // private MainLayoutController mainLayoutController;
    // private FXMLSceneManager sceneManager;

    private ObservableList<UserAccount> userObservableList = FXCollections.observableArrayList();
    private int currentUsersPage = 1;
    private final int USERS_PAGE_SIZE = 20;
    private int totalUserPages = 1;
    private String currentAdminId; // ID of the logged-in admin

    public AdminUserManagementController() {
        // userAccountService = new UserAccountServiceImpl(...); // DI
    }

    // public void setMainLayoutController(MainLayoutController mainLayoutController) { this.mainLayoutController = mainLayoutController; }
    // public void setSceneManager(FXMLSceneManager sceneManager) { this.sceneManager = sceneManager; }
    // public void setUserAccountService(IUserAccountService userAccountService) { this.userAccountService = userAccountService; }
    // public void setCurrentAdminId(String adminId) { this.currentAdminId = adminId; }


    public void initialize() {
        // sceneManager = FXMLSceneManager.getInstance();
        setupUserTableColumns();
        statusFilterComboBox.setItems(FXCollections.observableArrayList(UserStatus.values()));
        // TODO: Load role names/IDs into roleFilterComboBox from userAccountService.getAllRoles()
        // List<Role> roles = userAccountService.getAllRoles(currentAdminId);
        // roleFilterComboBox.setItems(FXCollections.observableArrayList(roles.stream().map(Role::getRoleName).collect(Collectors.toList())));

        loadUsers();
    }

    private void setupUserTableColumns() {
        // userIdColumn, usernameColumn, emailColumn, statusColumn are set via FXML PropertyValueFactory

        // Custom cell for roles
        rolesColumn.setCellValueFactory(cellData -> {
            UserAccount user = cellData.getValue();
            if (user != null && user.getRoleAssignments() != null && !user.getRoleAssignments().isEmpty()) {
                // In a real app, user.getRoleAssignments() would be populated by the service/DAO
                // For now, let's assume it's a Set<Role> or can be fetched
                try {
                    // Set<Role> roles = userAccountService.getUserRoles(user.getUserId()); // Fetch roles
                    // return new SimpleStringProperty(roles.stream().map(Role::getRoleName).collect(Collectors.joining(", ")));
                    return new SimpleStringProperty("Roles (Fetch Needed)"); // Placeholder
                } catch (Exception e) {
                    return new SimpleStringProperty("Error fetching roles");
                }
            }
            return new SimpleStringProperty("");
        });


        // Custom cell for action buttons
        Callback<TableColumn<UserAccount, Void>, TableCell<UserAccount, Void>> cellFactory = param -> {
            final TableCell<UserAccount, Void> cell = new TableCell<>() {
                private final Button editButton = new Button("Edit");
                private final Button deleteButton = new Button("Delete");
                private final Button blockButton = new Button("Block"); // Will toggle to "Unblock"
                private final Button resetButton = new Button("Reset Pwd");
                private final HBox pane = new HBox(5, editButton, deleteButton, blockButton, resetButton);

                {
                    editButton.getStyleClass().add("button-warning-small");
                    deleteButton.getStyleClass().add("button-danger-small");
                    // blockButton styling will change based on status
                    resetButton.getStyleClass().add("button-info-small");
                    pane.setAlignment(javafx.geometry.Pos.CENTER);


                    editButton.setOnAction(event -> {
                        UserAccount user = getTableView().getItems().get(getIndex());
                        handleEditUserAction(user);
                    });
                    deleteButton.setOnAction(event -> {
                        UserAccount user = getTableView().getItems().get(getIndex());
                        handleDeleteUserAction(user);
                    });
                    blockButton.setOnAction(event -> {
                        UserAccount user = getTableView().getItems().get(getIndex());
                        handleBlockUnblockUserAction(user, blockButton);
                    });
                    resetButton.setOnAction(event -> {
                        UserAccount user = getTableView().getItems().get(getIndex());
                        handleResetPasswordAction(user);
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        UserAccount user = getTableView().getItems().get(getIndex());
                        if (user.getUserStatus() == UserStatus.ACTIVE || user.getUserStatus() == UserStatus.PENDING_ACTIVATION) {
                            blockButton.setText("Block");
                            blockButton.getStyleClass().remove("button-success-small");
                            blockButton.getStyleClass().add("button-warning-small");
                        } else if (user.getUserStatus() == UserStatus.SUSPENDED) {
                            blockButton.setText("Unblock");
                            blockButton.getStyleClass().remove("button-warning-small");
                            blockButton.getStyleClass().add("button-success-small");
                        } else { // DELETED or other
                             blockButton.setText("N/A");
                             blockButton.setDisable(true);
                        }
                        // Prevent admin from blocking/deleting self directly through table action
                        if (currentAdminId != null && currentAdminId.equals(user.getUserId())) {
                            deleteButton.setDisable(true);
                            blockButton.setDisable(true);
                        } else {
                            deleteButton.setDisable(false);
                            if (user.getUserStatus() != UserStatus.DELETED) blockButton.setDisable(false);
                        }
                        setGraphic(pane);
                    }
                }
            };
            return cell;
        };
        actionsUserColumn.setCellFactory(cellFactory);
        usersTableView.setItems(userObservableList);
    }

    private void loadUsers() {
        // if (userAccountService == null) {
        //     AlertHelper.showErrorAlert("Service Error", "User account service is not available.");
        //     return;
        // }
        // String searchTerm = searchUserField.getText();
        // String roleFilter = roleFilterComboBox.getValue(); // This would be roleName or roleId
        // UserStatus statusFilter = statusFilterComboBox.getValue();
        //
        // try {
        //     // TODO: IUserAccountService needs a method like:
        //     // SearchResult<UserAccount> searchUsers(String searchTerm, String roleFilter, UserStatus statusFilter, int page, int size, String adminId);
        //     // For now, using getAll and filtering client-side (not efficient for large data)
        //     List<UserAccount> allUsers = userAccountService.getAllUsers(currentAdminId);
        //
        //     // Client-side filtering (replace with server-side/DAO-level filtering)
        //     List<UserAccount> filteredUsers = allUsers.stream()
        //             .filter(user -> searchTerm == null || searchTerm.isEmpty() ||
        //                              user.getUsername().toLowerCase().contains(searchTerm.toLowerCase()) ||
        //                              user.getEmail().toLowerCase().contains(searchTerm.toLowerCase()))
        //             .filter(user -> statusFilter == null || user.getUserStatus() == statusFilter)
        //             // .filter(user -> roleFilter == null || roleFilter.isEmpty() ||
        //             //                  userAccountService.getUserRoles(user.getUserId()).stream() // Inefficient to call service here per user
        //             //                      .anyMatch(role -> role.getRoleName().equals(roleFilter)))
        //             .collect(Collectors.toList());
        //
        //     // Manual pagination for client-side filtered list
        //     int totalItems = filteredUsers.size();
        //     this.totalUserPages = (int) Math.ceil((double) totalItems / USERS_PAGE_SIZE);
        //     if (this.totalUserPages == 0) this.totalUserPages = 1;
        //     if (this.currentUsersPage > this.totalUserPages) this.currentUsersPage = this.totalUserPages;
        //
        //     int fromIndex = (this.currentUsersPage - 1) * USERS_PAGE_SIZE;
        //     int toIndex = Math.min(fromIndex + USERS_PAGE_SIZE, totalItems);
        //     if (fromIndex > toIndex) fromIndex = Math.max(0, toIndex - USERS_PAGE_SIZE);


        //     userObservableList.setAll( (fromIndex <= toIndex) ? filteredUsers.subList(fromIndex, toIndex) : FXCollections.emptyObservableList());
        //     updateUserPaginationControls(this.currentUsersPage, this.totalUserPages, totalItems);
        //
        //     if(userObservableList.isEmpty() && this.currentUsersPage == 1) {
        //         usersTableView.setPlaceholder(new Label("No users found matching your criteria."));
        //     }
        //
        // } catch (SQLException | AuthorizationException e) {
        //     e.printStackTrace();
        //     AlertHelper.showErrorAlert("Database Error", "Failed to load users: " + e.getMessage());
        // }
        System.out.println("loadUsers called - implement with actual service call, filtering, and pagination.");
         // Dummy data for UI testing
        // UserAccount u1 = new UserAccount("ADM001", "admin", "hash", "admin@aims.com", UserStatus.ACTIVE);
        // UserAccount u2 = new UserAccount("PM001", "prodmanager", "hash", "pm@aims.com", UserStatus.ACTIVE);
        // userObservableList.setAll(u1, u2);
        // updateUserPaginationControls(1,1, userObservableList.size());
    }

    private void updateUserPaginationControls(int current, int total, long totalItems) {
        this.currentUsersPage = current;
        this.totalUserPages = total;
         if (totalItems == 0) {
            currentUserPageLabel.setText("No users");
            userPaginationControls.setVisible(false);
        } else {
            currentUserPageLabel.setText("Page " + this.currentUsersPage + " / " + this.totalUserPages);
            userPaginationControls.setVisible(true);
        }
        prevUserPageButton.setDisable(this.currentUsersPage <= 1);
        nextUserPageButton.setDisable(this.currentUsersPage >= this.totalUserPages);
    }

    @FXML
    void handleSearchUserAction(ActionEvent event) {
        currentUsersPage = 1;
        loadUsers();
    }

    @FXML
    void handleClearFiltersAction(ActionEvent event) {
        searchUserField.clear();
        roleFilterComboBox.setValue(null);
        statusFilterComboBox.setValue(null);
        currentUsersPage = 1;
        loadUsers();
    }

    @FXML
    void handleAddNewUserAction(ActionEvent event) {
        System.out.println("Add New User action triggered");
        // if (sceneManager != null && mainLayoutController != null) {
        //     AdminAddUserFormController addUserCtrl = (AdminAddUserFormController) sceneManager.loadFXMLIntoNewWindow(
        //             FXMLSceneManager.ADMIN_ADD_USER_FORM, "Add New User"
        //     ); // Or load into main content pane
        //     addUserCtrl.setAdminUserManagementController(this); // To refresh list after adding
        //     addUserCtrl.setCurrentAdminId(this.currentAdminId);
        //     // addUserCtrl.setMainLayoutController(mainLayoutController); (if dialog)
        // }
    }

    private void handleEditUserAction(UserAccount user) {
        System.out.println("Edit action for user: " + user.getUsername());
        // if (sceneManager != null && mainLayoutController != null) {
        //     AdminEditUserFormController editUserCtrl = (AdminEditUserFormController) sceneManager.loadFXMLIntoNewWindow(
        //             FXMLSceneManager.ADMIN_EDIT_USER_FORM, "Edit User - " + user.getUsername()
        //     );
        //     editUserCtrl.setUserToEdit(user);
        //     editUserCtrl.setAdminUserManagementController(this);
        //     editUserCtrl.setCurrentAdminId(this.currentAdminId);
        // }
    }

    private void handleDeleteUserAction(UserAccount user) {
        System.out.println("Delete action for user: " + user.getUsername());
        // if (userAccountService == null || currentAdminId == null) {
        //     AlertHelper.showErrorAlert("Error", "Service or admin context unavailable."); return;
        // }
        // if (currentAdminId.equals(user.getUserId())) {
        //     AlertHelper.showErrorAlert("Action Denied", "You cannot delete your own account."); return;
        // }
        // boolean confirmed = AlertHelper.showConfirmationDialog("Delete User",
        //         "Are you sure you want to delete user: " + user.getUsername() + " (ID: " + user.getUserId() + ")?");
        // if (confirmed) {
        //     try {
        //         userAccountService.deleteUser(user.getUserId(), currentAdminId);
        //         AlertHelper.showInfoAlert("Success", "User '" + user.getUsername() + "' deleted successfully.");
        //         loadUsers(); // Refresh list
        //     } catch (SQLException | ValidationException | ResourceNotFoundException | AuthorizationException e) {
        //         AlertHelper.showErrorAlert("Deletion Failed", e.getMessage());
        //     }
        // }
    }

    private void handleBlockUnblockUserAction(UserAccount user, Button blockButtonRef) {
        System.out.println("Block/Unblock action for user: " + user.getUsername());
        // if (userAccountService == null || currentAdminId == null) { /*...*/ return; }
        // if (currentAdminId.equals(user.getUserId())) {
        //     AlertHelper.showErrorAlert("Action Denied", "You cannot block/unblock your own account."); return;
        // }
        //
        // String action = (user.getUserStatus() == UserStatus.ACTIVE) ? "block" : "unblock";
        // boolean confirmed = AlertHelper.showConfirmationDialog(action.substring(0,1).toUpperCase() + action.substring(1) + " User",
        //         "Are you sure you want to " + action + " user: " + user.getUsername() + "?");
        //
        // if (confirmed) {
        //     try {
        //         if (user.getUserStatus() == UserStatus.ACTIVE) {
        //             userAccountService.blockUser(user.getUserId(), currentAdminId);
        //         } else if (user.getUserStatus() == UserStatus.SUSPENDED) {
        //             userAccountService.unblockUser(user.getUserId(), currentAdminId);
        //         }
        //         AlertHelper.showInfoAlert("Success", "User '" + user.getUsername() + "' status updated.");
        //         loadUsers(); // Refresh
        //     } catch (SQLException | ValidationException | ResourceNotFoundException | AuthorizationException e) {
        //         AlertHelper.showErrorAlert("Action Failed", e.getMessage());
        //     }
        // }
    }

    private void handleResetPasswordAction(UserAccount user) {
        System.out.println("Reset password action for user: " + user.getUsername());
        // if (userAccountService == null || currentAdminId == null) { /*...*/ return; }
        //
        // TextInputDialog dialog = new TextInputDialog("NewTemporaryPassword123!");
        // dialog.setTitle("Reset Password");
        // dialog.setHeaderText("Reset password for user: " + user.getUsername());
        // dialog.setContentText("Enter new temporary password:");
        // Optional<String> result = dialog.showAndWait();
        //
        // result.ifPresent(newPassword -> {
        //     if (newPassword.trim().isEmpty()) {
        //         AlertHelper.showWarningAlert("Invalid Input", "Password cannot be empty.");
        //         return;
        //     }
        //     try {
        //         userAccountService.resetPassword(user.getUserId(), newPassword, currentAdminId);
        //         AlertHelper.showInfoAlert("Password Reset", "Password for " + user.getUsername() + " has been reset.\nPlease inform the user of their new password.");
        //     } catch (SQLException | ValidationException | ResourceNotFoundException | AuthorizationException e) {
        //         AlertHelper.showErrorAlert("Reset Failed", e.getMessage());
        //     }
        // });
    }


    @FXML
    void handlePrevUserPageAction(ActionEvent event) {
        if (currentUsersPage > 1) {
            currentUsersPage--;
            loadUsers();
        }
    }

    @FXML
    void handleNextUserPageAction(ActionEvent event) {
        if (currentUsersPage < totalUserPages) {
            currentUsersPage++;
            loadUsers();
        }
    }

    /**
     * Public method to allow child controllers (e.g., Add/Edit User forms) to trigger a refresh.
     */
    public void refreshUserList() {
        loadUsers();
    }
}
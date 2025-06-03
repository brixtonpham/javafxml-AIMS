package com.aims.core.presentation.controllers;

import com.aims.core.entities.UserAccount; // Để hiển thị tên người dùng
// import com.aims.presentation.utils.FXMLSceneManager;
// import com.aims.core.application.services.IAuthenticationService; // Để logout

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
// import javafx.stage.Stage;

public class AdminDashboardController {

    @FXML
    private Label welcomeLabel;
    @FXML
    private Button userManagementButton;
    @FXML
    private Button productManagementButton;
    @FXML
    private Button orderManagementButton; // Có thể dẫn đến một màn hình xem/quản lý tất cả đơn hàng
    @FXML
    private Button changeOwnPasswordButton;


    // private MainLayoutController mainLayoutController;
    // private FXMLSceneManager sceneManager;
    // private IAuthenticationService authService; // Để xử lý logout
    private UserAccount currentAdminUser;

    public AdminDashboardController() {
        // authService = new AuthenticationServiceImpl(...); // DI
    }

    // public void setMainLayoutController(MainLayoutController mainLayoutController) {
    //     this.mainLayoutController = mainLayoutController;
    // }
    //
    // public void setSceneManager(FXMLSceneManager sceneManager) {
    //     this.sceneManager = sceneManager;
    // }
    //
    // public void setAuthService(IAuthenticationService authService) {
    //     this.authService = authService;
    // }

    /**
     * Được gọi sau khi Admin đăng nhập thành công và màn hình này được load.
     * @param adminUser Thông tin của Admin đã đăng nhập.
     */
    public void setCurrentAdminUser(UserAccount adminUser) {
        this.currentAdminUser = adminUser;
        if (currentAdminUser != null) {
            welcomeLabel.setText("Welcome, " + currentAdminUser.getUsername() + " (Administrator)!");
        } else {
            welcomeLabel.setText("Welcome, Administrator!");
        }
    }

    public void initialize() {
        // Có thể thiết lập các hành động mặc định hoặc kiểm tra quyền hạn ở đây
    }

    @FXML
    void handleUserManagementAction(ActionEvent event) {
        System.out.println("Navigate to User Management Screen");
        // if (sceneManager != null && mainLayoutController != null) {
        //     mainLayoutController.loadContent(FXMLSceneManager.ADMIN_USER_MANAGEMENT_SCREEN);
        //     mainLayoutController.setHeaderTitle("User Management");
        // }
    }

    @FXML
    void handleProductManagementAction(ActionEvent event) {
        System.out.println("Navigate to Admin Product Management Screen");
        // if (sceneManager != null && mainLayoutController != null) {
        //     mainLayoutController.loadContent(FXMLSceneManager.ADMIN_PRODUCT_LIST_SCREEN);
        //     mainLayoutController.setHeaderTitle("Product Management (Admin)");
        // }
    }

    @FXML
    void handleOrderManagementAction(ActionEvent event) {
        System.out.println("Navigate to Order Management Screen (Admin View - Future)");
        // Đây có thể là một màn hình mới liệt kê tất cả đơn hàng với các bộ lọc,
        // hoặc có thể dẫn đến màn hình pending orders của Product Manager nếu Admin cũng có quyền đó.
        // if (sceneManager != null && mainLayoutController != null) {
        //     // Ví dụ: mainLayoutController.loadContent(FXMLSceneManager.ADMIN_ALL_ORDERS_SCREEN);
        //     // mainLayoutController.setHeaderTitle("All Orders");
        //     // Hoặc nếu dùng chung với PM:
        //     // mainLayoutController.loadContent(FXMLSceneManager.PM_PENDING_ORDERS_SCREEN);
        //     // mainLayoutController.setHeaderTitle("Pending Orders Review");
        // }
    }

    @FXML
    void handleChangeOwnPasswordAction(ActionEvent event) {
        System.out.println("Navigate to Change Own Password Screen");
        // if (sceneManager != null && mainLayoutController != null && currentAdminUser != null) {
        //     ChangePasswordController cpController = (ChangePasswordController) sceneManager.loadFXMLIntoPane(
        //         mainLayoutController.getContentPane(), FXMLSceneManager.CHANGE_PASSWORD_SCREEN
        //     );
        //     cpController.setUserId(currentAdminUser.getUserId()); // Truyền User ID để biết đổi mật khẩu cho ai
        //     cpController.setMainLayoutController(mainLayoutController);
        //     mainLayoutController.setHeaderTitle("Change My Password");
        // }
    }

    @FXML
    void handleLogoutAction(ActionEvent event) {
        System.out.println("Admin Logout action triggered");
        // if (authService != null && currentAdminUser != null) {
        //     authService.logout(currentAdminUser.getUserId()); // Hoặc session ID nếu có
        // }
        // if (mainLayoutController != null) {
        //     mainLayoutController.handleLogoutAction(null); // Gọi phương thức logout của MainLayout
        // }
    }
}
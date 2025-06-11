package com.aims.core.presentation.controllers;

import com.aims.core.entities.UserAccount; // Để hiển thị tên người dùng
import com.aims.core.presentation.utils.FXMLSceneManager;
import com.aims.core.application.services.IAuthenticationService; // Để logout
import com.aims.core.presentation.controllers.ChangePasswordController;

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


    private MainLayoutController mainLayoutController;
    private FXMLSceneManager sceneManager;
    private IAuthenticationService authService; // Để xử lý logout
    private UserAccount currentAdminUser;

    public AdminDashboardController() {
        // authService = new AuthenticationServiceImpl(...); // DI // TODO: Inject through ServiceFactory or constructor
    }

    public void setMainLayoutController(MainLayoutController mainLayoutController) {
        this.mainLayoutController = mainLayoutController;
    }

    public void setSceneManager(FXMLSceneManager sceneManager) {
        this.sceneManager = sceneManager;
    }

    public void setAuthService(IAuthenticationService authService) {
        this.authService = authService;
    }

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
        if (mainLayoutController != null) {
            mainLayoutController.loadContent("/com/aims/presentation/views/admin_user_management_screen.fxml");
            mainLayoutController.setHeaderTitle("User Management");
        }
    }

    @FXML
    void handleProductManagementAction(ActionEvent event) {
        System.out.println("Navigate to Admin Product Management Screen");
        if (mainLayoutController != null) {
            mainLayoutController.loadContent("/com/aims/presentation/views/admin_product_list_screen.fxml");
            mainLayoutController.setHeaderTitle("Product Management (Admin)");
        }
    }

    @FXML
    void handleOrderManagementAction(ActionEvent event) {
        System.out.println("Navigate to Order Management Screen (Admin View - Future)");
        // Đây có thể là một màn hình mới liệt kê tất cả đơn hàng với các bộ lọc,
        // hoặc có thể dẫn đến màn hình pending orders của Product Manager nếu Admin cũng có quyền đó.
        // if (mainLayoutController != null) {
        //     // Ví dụ: mainLayoutController.loadContent("/com/aims/presentation/views/admin_all_orders_screen.fxml");
        //     // mainLayoutController.setHeaderTitle("All Orders");
        //     // Hoặc nếu dùng chung với PM:
        //     // mainLayoutController.loadContent("/com/aims/presentation/views/pm_pending_orders_list_screen.fxml");
        //     // mainLayoutController.setHeaderTitle("Pending Orders Review");
        // }
    }

    @FXML
    void handleChangeOwnPasswordAction(ActionEvent event) {
        System.out.println("Navigate to Change Own Password Screen");
        if (sceneManager != null && mainLayoutController != null && currentAdminUser != null) {
            ChangePasswordController cpController = (ChangePasswordController) sceneManager.loadFXMLIntoPane(
                mainLayoutController.getContentPane(), "/com/aims/presentation/views/change_password_screen.fxml"
            );
            if (cpController != null) {
                cpController.setUserContext(currentAdminUser.getUserId(), currentAdminUser.getUsername()); // Truyền User ID và Username
                cpController.setMainLayoutController(mainLayoutController); // Ensure child controller knows main layout
                mainLayoutController.setHeaderTitle("Change My Password");
            }
        }
    }

    @FXML
    void handleLogoutAction(ActionEvent event) {
        System.out.println("Admin Logout action triggered");
        // The MainLayoutController's handleLogoutAction should be responsible for
        // calling the authenticationService.logout() with the correct session ID.
        if (mainLayoutController != null) {
            mainLayoutController.handleLogoutAction(null); // Gọi phương thức logout của MainLayout
        } else {
            // Fallback or error handling if MainLayoutController is not available
            System.err.println("MainLayoutController not available for logout action.");
            // Optionally, if authService is available and logout by user ID is a valid scenario:
            // if (authService != null && currentAdminUser != null) {
            //     authService.logout(currentAdminUser.getUserId()); // Or appropriate identifier
            // }
        }
    }
}
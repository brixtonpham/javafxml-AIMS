package com.aims.core.presentation.controllers;

import com.aims.core.entities.UserAccount;
import com.aims.core.presentation.utils.FXMLSceneManager;
import com.aims.core.application.services.IAuthenticationService;
import com.aims.core.presentation.controllers.ChangePasswordController;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class ProductManagerDashboardController {

    @FXML
    private Label welcomeLabel;
    @FXML
    private Button productManagementButton;
    @FXML
    private Button pendingOrdersButton;
    @FXML
    private Button viewAllOrdersButton;
    @FXML
    private Button changeOwnPasswordButton;

    private MainLayoutController mainLayoutController;
    private FXMLSceneManager sceneManager;
    private IAuthenticationService authService; // Để xử lý logout
    private UserAccount currentProductManager;

    public ProductManagerDashboardController() {
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
     * Được gọi sau khi Product Manager đăng nhập thành công và màn hình này được load.
     * @param pmUser Thông tin của Product Manager đã đăng nhập.
     */
    public void setCurrentProductManager(UserAccount pmUser) {
        this.currentProductManager = pmUser;
        if (currentProductManager != null) {
            welcomeLabel.setText("Welcome, " + currentProductManager.getUsername() + " (Product Manager)!");
        } else {
            welcomeLabel.setText("Welcome, Product Manager!");
        }
    }

    public void initialize() {
        // Có thể thiết lập các hành động mặc định hoặc kiểm tra quyền hạn ở đây
    }

    @FXML
    void handleProductManagementAction(ActionEvent event) {
        System.out.println("Navigate to Product Management Screen (PM View)");
        if (mainLayoutController != null) {
            // Assuming PMs use the same product list screen as Admins for now
            mainLayoutController.loadContent("/com/aims/presentation/views/admin_product_list_screen.fxml");
            mainLayoutController.setHeaderTitle("Product Management");
        }
    }

    @FXML
    void handlePendingOrdersAction(ActionEvent event) {
        System.out.println("Navigate to Pending Orders Screen");
        if (mainLayoutController != null) {
            mainLayoutController.loadContent("/com/aims/presentation/views/pm_pending_orders_list_screen.fxml");
            mainLayoutController.setHeaderTitle("Pending Orders Review");
        }
    }

    @FXML
    void handleViewAllOrdersAction(ActionEvent event) {
        System.out.println("Navigate to View All Orders Screen (PM Read-Only View - Future)");
        // Chức năng này có thể cần một màn hình mới hiển thị danh sách tất cả đơn hàng
        // với quyền hạn chỉ đọc cho PM, khác với màn hình pending orders.
        // if (sceneManager != null && mainLayoutController != null) {
        //     // mainLayoutController.loadContent(FXMLSceneManager.PM_ALL_ORDERS_READONLY_SCREEN);
        //     // mainLayoutController.setHeaderTitle("All Orders (View)");
        // }
    }

    @FXML
    void handleChangeOwnPasswordAction(ActionEvent event) {
        System.out.println("Navigate to Change Own Password Screen");
        if (sceneManager != null && mainLayoutController != null && currentProductManager != null) {
            ChangePasswordController cpController = (ChangePasswordController) sceneManager.loadFXMLIntoPane(
                mainLayoutController.getContentPane(), "/com/aims/presentation/views/change_password_screen.fxml"
            );
            if (cpController != null) {
                cpController.setUserContext(currentProductManager.getUserId(), currentProductManager.getUsername());
                cpController.setMainLayoutController(mainLayoutController);
                mainLayoutController.setHeaderTitle("Change My Password");
            }
        }
    }

    @FXML
    void handleLogoutAction(ActionEvent event) {
        System.out.println("Product Manager Logout action triggered");
        // The MainLayoutController's handleLogoutAction should be responsible for
        // calling the authenticationService.logout() with the correct session ID.
        if (mainLayoutController != null) {
            mainLayoutController.handleLogoutAction(null); // Gọi phương thức logout của MainLayout
        } else {
            // Fallback or error handling if MainLayoutController is not available
            System.err.println("MainLayoutController not available for logout action.");
        }
    }
}
package com.aims.core.presentation.controllers; // Hoặc package controller của bạn

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.layout.BorderPane;
// import com.aims.presentation.utils.FXMLSceneManager; // Giả sử bạn có lớp này
// import com.aims.core.application.services.IAuthenticationService;
// import com.aims.core.entities.UserAccount; // Để quản lý trạng thái đăng nhập

import java.io.IOException;

public class MainLayoutController { // Có thể kế thừa từ BaseScreenController nếu bạn có

    @FXML
    private BorderPane mainBorderPane;

    @FXML
    private BorderPane contentPane;

    @FXML
    private MenuBar menuBar;

    @FXML
    private Menu adminMenu;

    @FXML
    private Menu pmMenu;

    @FXML
    private Label headerTitle;

    @FXML
    private Label footerLabel;

    // --- Service Dependencies (cần được inject) ---
    // @Inject // Ví dụ
    // private IAuthenticationService authService;
    // private FXMLSceneManager sceneManager;

    // private UserAccount currentUser; // Để lưu trạng thái người dùng đăng nhập

    public void initialize() {
        // sceneManager = FXMLSceneManager.getInstance(); // Khởi tạo scene manager
        // Cập nhật trạng thái menu dựa trên người dùng đăng nhập (currentUser)
        updateMenuVisibility();
        navigateToHome(null); // Load màn hình home mặc định
    }

    private void loadContent(String fxmlPath) {
        try {
            // Parent newContent = sceneManager.loadFXML(fxmlPath); // Hoặc dùng FXMLLoader trực tiếp
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent newContent = loader.load();
            contentPane.setCenter(newContent);
        } catch (IOException e) {
            e.printStackTrace();
            // Hiển thị lỗi cho người dùng (ví dụ: dùng AlertHelper)
            System.err.println("Error loading FXML: " + fxmlPath);
        }
    }

    private void updateMenuVisibility() {
        // if (currentUser != null) {
        //     boolean isAdmin = currentUser.getRoles().stream().anyMatch(role -> role.getRoleId().equals("ADMIN"));
        //     boolean isPm = currentUser.getRoles().stream().anyMatch(role -> role.getRoleId().equals("PRODUCT_MANAGER"));
        //     adminMenu.setVisible(isAdmin);
        //     pmMenu.setVisible(isPm || isAdmin); // Admin có thể thấy cả menu PM
        // } else {
        //     adminMenu.setVisible(false);
        //     pmMenu.setVisible(false);
        // }
        // Tạm thời để test
        adminMenu.setVisible(true);
        pmMenu.setVisible(true);
    }

    @FXML
    void handleLoginAction(ActionEvent event) {
        System.out.println("Login action triggered");
        // loadContent(FXMLSceneManager.LOGIN_SCREEN); // Sử dụng FXMLPaths của bạn
        loadContent("/com/aims/presentation/views/login_screen.fxml");
    }

    @FXML
    void handleLogoutAction(ActionEvent event) {
        System.out.println("Logout action triggered");
        // currentUser = null;
        // authService.logout(sessionId_or_currentUser.getUserId());
        updateMenuVisibility();
        navigateToHome(null); // Quay về trang chủ
        // Hiển thị thông báo đăng xuất thành công
    }

    @FXML
    void handleExitAction(ActionEvent event) {
        System.out.println("Exit action triggered");
        System.exit(0);
    }

    @FXML
    void navigateToHome(ActionEvent event) {
        System.out.println("Navigate to Home triggered");
        // loadContent(FXMLSceneManager.HOME_SCREEN);
        loadContent("/com/aims/presentation/views/home_screen.fxml");
    }

    @FXML
    void navigateToCart(ActionEvent event) {
        System.out.println("Navigate to Cart triggered");
        // loadContent(FXMLSceneManager.CART_SCREEN);
        loadContent("/com/aims/presentation/views/cart_screen.fxml");
    }

    @FXML
    void navigateToUserManagement(ActionEvent event) {
        System.out.println("Navigate to User Management triggered");
        // loadContent(FXMLSceneManager.ADMIN_USER_MANAGEMENT_SCREEN);
        loadContent("/com/aims/presentation/views/admin_user_management_screen.fxml");
    }

    @FXML
    void navigateToAdminProductList(ActionEvent event) {
        System.out.println("Navigate to Admin Product List triggered");
        // loadContent(FXMLSceneManager.ADMIN_PRODUCT_LIST_SCREEN);
         loadContent("/com/aims/presentation/views/admin_product_list_screen.fxml");
    }

    @FXML
    void navigateToPmProductList(ActionEvent event) {
        System.out.println("Navigate to PM Product List triggered");
        // loadContent(FXMLSceneManager.PM_PRODUCT_LIST_SCREEN); // Hoặc dùng chung admin_product_list
         loadContent("/com/aims/presentation/views/admin_product_list_screen.fxml");
    }

    @FXML
    void navigateToPmPendingOrders(ActionEvent event) {
        System.out.println("Navigate to PM Pending Orders triggered");
        // loadContent(FXMLSceneManager.PM_PENDING_ORDERS_SCREEN);
         loadContent("/com/aims/presentation/views/pm_pending_orders_list_screen.fxml");
    }

    // Phương thức để các controller con có thể thay đổi contentPane
    public BorderPane getContentPane() {
        return contentPane;
    }

    // Phương thức để cập nhật header title (ví dụ khi chuyển màn hình)
    public void setHeaderTitle(String title) {
        if (headerTitle != null) {
            headerTitle.setText(title);
        }
    }
}
package com.aims.test.ui.example;

import com.aims.test.base.BaseUITest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

/**
 * Ví dụ về cách sử dụng BaseUITest để quản lý dữ liệu test
 */
public class ExampleUITest extends BaseUITest {
    
    @Test
    @DisplayName("Test Login với dữ liệu hợp lệ")
    void testValidLogin() {
        // BaseUITest sẽ tự động seed dữ liệu login_test trước khi chạy test này
        System.out.println("Thực hiện test login...");
        
        // TODO: Thêm code test UI cho login
        // - Mở trang login
        // - Nhập username/password từ dữ liệu test
        // - Click login
        // - Verify kết quả
        
        // Dữ liệu có sẵn:
        // - user_admin_001 / admin / hashed_password_admin
        // - user_customer_001 / customer1 / hashed_password_customer1
    }
    
    @Test
    @DisplayName("Test thêm sản phẩm vào giỏ hàng")
    void testAddProductToCart() {
        // BaseUITest sẽ tự động seed dữ liệu cart_test trước khi chạy test này
        System.out.println("Thực hiện test add to cart...");
        
        // TODO: Thêm code test UI cho add to cart
        // - Login với user_customer_001
        // - Tìm sản phẩm book_001 (Clean Code)
        // - Click "Add to Cart"
        // - Verify sản phẩm đã được thêm vào cart
        
        // Dữ liệu có sẵn:
        // - Products: book_001, book_002, cd_001, dvd_001...
        // - Cart session: cart_session_001 đã có 2 sản phẩm
    }
    
    @Test
    @DisplayName("Test tạo đơn hàng")
    void testCreateOrder() {
        // BaseUITest sẽ tự động seed dữ liệu order_test trước khi chạy test này
        System.out.println("Thực hiện test create order...");
        
        // TODO: Thêm code test UI cho create order
        // - Login với user_customer_001
        // - Thêm sản phẩm vào cart
        // - Proceed to checkout
        // - Điền thông tin delivery
        // - Xác nhận đơn hàng
        
        // Dữ liệu có sẵn:
        // - Order: order_001 với trạng thái PENDING_PROCESSING
        // - Delivery info: delivery_001 với địa chỉ test
    }
    
    @Test
    @DisplayName("Test thanh toán")
    void testPayment() {
        // BaseUITest sẽ tự động seed dữ liệu payment_test trước khi chạy test này
        System.out.println("Thực hiện test payment...");
        
        // TODO: Thêm code test UI cho payment
        // - Có đơn hàng order_001 cần thanh toán
        // - Chọn phương thức thanh toán pm_card_001
        // - Thực hiện thanh toán
        // - Verify transaction trans_001
        
        // Dữ liệu có sẵn:
        // - Payment method: pm_card_001 (Credit Card)
        // - Transaction: trans_001 với trạng thái SUCCESS
    }
    
    @Test
    @DisplayName("Test với dữ liệu tùy chỉnh")
    void testWithCustomData() {
        // Có thể seed dữ liệu cụ thể trong test
        seedSpecificData("product_management_test");
        
        System.out.println("Thực hiện test với dữ liệu tùy chỉnh...");
        
        // TODO: Test logic với dữ liệu đã được seed
    }
}

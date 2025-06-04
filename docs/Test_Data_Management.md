# AIMS Test Data Management

## Tổng quan

Hệ thống quản lý dữ liệu test cho AIMS được thiết kế để đảm bảo mỗi UI test có môi trường dữ liệu sạch và nhất quán.

## Cấu trúc

```
src/test/resources/
├── aims_test.db              # Database test
└── test_data/
    ├── seed_data.sql         # Dữ liệu mẫu cơ bản
    ├── clear_test_data.sql   # Script xóa dữ liệu
    └── sample_orders.json    # Dữ liệu JSON (nếu cần)

src/test/java/com/aims/test/
├── utils/
│   └── TestDataManager.java  # Class quản lý dữ liệu test
├── base/
│   └── BaseUITest.java       # Base class cho UI tests
└── ui/
    └── example/
        └── ExampleUITest.java # Ví dụ sử dụng
```

## Cách sử dụng

### 1. Khởi tạo Database Test

```bash
# Chạy script setup
./setup_test_db.sh

# Hoặc thủ công
mvn compile
cp target/classes/aims_database.db src/test/resources/aims_test.db
sqlite3 src/test/resources/aims_test.db < src/test/resources/test_data/clear_test_data.sql
sqlite3 src/test/resources/aims_test.db < src/main/resources/migration/V1__create_tables.sql
sqlite3 src/test/resources/aims_test.db < src/test/resources/test_data/seed_data.sql
```

### 2. Tạo UI Test Class

```java
public class LoginUITest extends BaseUITest {
    
    @Test
    @DisplayName("Test đăng nhập thành công")
    void testSuccessfulLogin() {
        // BaseUITest tự động seed dữ liệu login_test
        // Test code here...
    }
}
```

### 3. Quản lý dữ liệu trong test

```java
// Seed dữ liệu cụ thể
seedSpecificData("cart_test");

// Reset dữ liệu
resetTestData();

// Sử dụng TestDataManager trực tiếp
TestDataManager.seedDataForTestCase("payment_test");
TestDataManager.clearTestData();
```

## Các loại dữ liệu test

### Login Test Data
- Admin user: `user_admin_001` / `admin`
- Customer users: `user_customer_001` / `customer1`
- Blocked user: `user_blocked_001`

### Product Test Data
- Books: Clean Code, Design Patterns, The Pragmatic Programmer
- CDs: Abbey Road, Thriller
- DVDs: The Matrix, Inception

### Cart Test Data
- Cart session với sản phẩm có sẵn
- User customer với cart items

### Order Test Data
- Order mẫu với trạng thái PENDING_PROCESSING
- Delivery info và order items

### Payment Test Data
- Payment methods (Credit Card, Debit Card)
- Successful transaction examples

## Nguyên tắc sử dụng

1. **Isolation**: Mỗi test chạy với dữ liệu độc lập
2. **Cleanup**: Dữ liệu được reset sau mỗi test
3. **Consistency**: Dữ liệu luôn ở trạng thái có thể dự đoán
4. **Flexibility**: Có thể seed dữ liệu cụ thể cho từng test case

## Troubleshooting

### Database không tồn tại
```bash
./setup_test_db.sh
```

### Lỗi foreign key constraint
- Kiểm tra thứ tự insert trong seed_data.sql
- Đảm bảo parent records tồn tại trước child records

### Test data không đúng
```java
// Debug trong test
TestDataManager.clearTestData();
TestDataManager.seedTestData();
```

## Mở rộng

Để thêm dữ liệu cho test case mới:

1. Thêm method trong `TestDataManager.seedDataForTestCase()`
2. Tạo SQL insert statements cho dữ liệu cần thiết  
3. Đảm bảo tuân theo foreign key constraints
4. Test với `./setup_test_db.sh`

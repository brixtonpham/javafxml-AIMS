-- seed_data.sql
-- Dữ liệu mẫu cho môi trường test AIMS
-- Chạy script này để khởi tạo dữ liệu test cơ bản

-- Bật kiểm tra khóa ngoại
PRAGMA foreign_keys = ON;

-- Dữ liệu mẫu cho bảng ROLE
INSERT OR REPLACE INTO ROLE (roleID, roleName) VALUES 
('ADMIN', 'Administrator'),
('PRODUCT_MANAGER', 'Product Manager'),
('CUSTOMER', 'Customer');

-- Dữ liệu mẫu cho bảng USER_ACCOUNT
INSERT OR REPLACE INTO USER_ACCOUNT (userID, username, password_hash, email, user_status) VALUES 
('user_admin_001', 'admin', 'hashed_password_admin', 'admin@aims.com', 'ACTIVE'),
('user_pm_001', 'product_manager', 'hashed_password_pm', 'pm@aims.com', 'ACTIVE'),
('user_customer_001', 'customer1', 'hashed_password_customer1', 'customer1@test.com', 'ACTIVE'),
('user_customer_002', 'customer2', 'hashed_password_customer2', 'customer2@test.com', 'ACTIVE'),
('user_blocked_001', 'blocked_user', 'hashed_password_blocked', 'blocked@test.com', 'BLOCKED');

-- Gán quyền cho user
INSERT OR REPLACE INTO USER_ROLE_ASSIGNMENT (userID, roleID) VALUES 
('user_admin_001', 'ADMIN'),
('user_pm_001', 'PRODUCT_MANAGER'),
('user_customer_001', 'CUSTOMER'),
('user_customer_002', 'CUSTOMER'),
('user_blocked_001', 'CUSTOMER');

-- Dữ liệu mẫu cho bảng PRODUCT
INSERT OR REPLACE INTO PRODUCT (productID, title, category, value_amount, price, quantityInStock, description, imageURL, barcode, dimensions_cm, weight_kg, entryDate, productType) VALUES 
-- Books
('book_001', 'Clean Code', 'Programming', 45.99, 45.99, 10, 'A Handbook of Agile Software Craftsmanship', '/images/clean_code.jpg', '9780132350884', '23.5x19.1x2.5', 0.6, '2024-01-15', 'BOOK'),
('book_002', 'Design Patterns', 'Programming', 54.99, 54.99, 5, 'Elements of Reusable Object-Oriented Software', '/images/design_patterns.jpg', '9780201633610', '24x18.5x3', 0.8, '2024-01-16', 'BOOK'),
('book_003', 'The Pragmatic Programmer', 'Programming', 49.99, 49.99, 8, 'Your Journey to Mastery', '/images/pragmatic_programmer.jpg', '9780135957059', '23x19x2', 0.7, '2024-01-17', 'BOOK'),
-- CDs
('cd_001', 'Abbey Road', 'Music', 19.99, 19.99, 15, 'The Beatles greatest album', '/images/abbey_road.jpg', '094638241928', '12.4x14.2x1', 0.1, '2024-01-18', 'CD'),
('cd_002', 'Thriller', 'Music', 17.99, 17.99, 12, 'Michael Jackson iconic album', '/images/thriller.jpg', '074643811228', '12.4x14.2x1', 0.1, '2024-01-19', 'CD'),
-- DVDs
('dvd_001', 'The Matrix', 'Movies', 14.99, 14.99, 20, 'Sci-fi action movie', '/images/matrix.jpg', '085391163923', '19x13.5x1.5', 0.08, '2024-01-20', 'DVD'),
('dvd_002', 'Inception', 'Movies', 16.99, 16.99, 18, 'Mind-bending thriller', '/images/inception.jpg', '883929154524', '19x13.5x1.5', 0.08, '2024-01-21', 'DVD');

-- Chi tiết sản phẩm BOOK
INSERT OR REPLACE INTO BOOK (productID, authors, coverType, publisher, publicationDate, numPages, language, book_genre) VALUES 
('book_001', 'Robert C. Martin', 'Paperback', 'Prentice Hall', '2008-08-01', 464, 'English', 'Programming'),
('book_002', 'Gang of Four', 'Hardcover', 'Addison-Wesley', '1994-10-31', 395, 'English', 'Programming'),
('book_003', 'David Thomas, Andrew Hunt', 'Paperback', 'Addison-Wesley', '2019-09-13', 352, 'English', 'Programming');

-- Chi tiết sản phẩm CD
INSERT OR REPLACE INTO CD (productID, artists, recordLabel, tracklist, cd_genre, releaseDate) VALUES 
('cd_001', 'The Beatles', 'Apple Records', 'Come Together;Something;Maxwell''s Silver Hammer', 'Rock', '1969-09-26'),
('cd_002', 'Michael Jackson', 'Epic Records', 'Wanna Be Startin'' Somethin'';Billie Jean;Beat It;Thriller', 'Pop', '1982-11-30');

-- Chi tiết sản phẩm DVD
INSERT OR REPLACE INTO DVD (productID, discType, director, runtime_minutes, studio, dvd_language, subtitles, dvd_releaseDate, dvd_genre) VALUES 
('dvd_001', 'DVD', 'Lana Wachowski, Lilly Wachowski', 136, 'Warner Bros', 'English', 'English, Spanish, French', '1999-03-31', 'Action'),
('dvd_002', 'DVD', 'Christopher Nolan', 148, 'Warner Bros', 'English', 'English, Spanish, French', '2010-07-16', 'Thriller');

-- Dữ liệu mẫu cho PAYMENT_METHOD
INSERT OR REPLACE INTO PAYMENT_METHOD (paymentMethodID, methodType, userID, isDefault) VALUES 
('pm_card_001', 'CREDIT_CARD', 'user_customer_001', 1),
('pm_card_002', 'DOMESTIC_DEBIT_CARD', 'user_customer_002', 1);

-- Chi tiết thẻ
INSERT OR REPLACE INTO CARD_DETAILS (paymentMethodID, cardholderName, cardNumber_masked, expiryDate_MMYY, validFromDate_MMYY, issuingBank) VALUES 
('pm_card_001', 'Customer One', '**** **** **** 1234', '12/28', '01/24', 'Test Bank A'),
('pm_card_002', 'Customer Two', '**** **** **** 5678', '06/29', '03/24', 'Test Bank B');

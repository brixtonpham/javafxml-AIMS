-- V2__seed_test_data.sql
-- Script seed dữ liệu test cho AIMS
-- Dùng để khởi tạo dữ liệu mẫu cho các kịch bản test UI

-- Bật kiểm tra khóa ngoại
PRAGMA foreign_keys = ON;

-- 1. ROLES - Các vai trò trong hệ thống
INSERT OR REPLACE INTO ROLE (roleID, roleName) VALUES 
('ADMIN', 'Administrator'),
('PRODUCT_MANAGER', 'Product Manager'),
('CUSTOMER', 'Customer');

-- 2. USER_ACCOUNTS - Tài khoản test với mật khẩu đơn giản để test UI
INSERT OR REPLACE INTO USER_ACCOUNT (userID, username, password_hash, email, user_status) VALUES 
-- Admin account
('user_admin_001', 'admin', 'admin123', 'admin@aims.com', 'ACTIVE'),
-- Product Manager account  
('user_pm_001', 'manager', 'manager123', 'manager@aims.com', 'ACTIVE'),
-- Customer accounts
('user_customer_001', 'customer1', 'customer123', 'customer1@test.com', 'ACTIVE'),
('user_customer_002', 'customer2', 'customer123', 'customer2@test.com', 'ACTIVE'),
('user_customer_003', 'john_doe', 'john123', 'john.doe@test.com', 'ACTIVE'),
-- Blocked user để test quyền truy cập
('user_blocked_001', 'blocked_user', 'blocked123', 'blocked@test.com', 'BLOCKED'),
-- Inactive user
('user_inactive_001', 'inactive_user', 'inactive123', 'inactive@test.com', 'INACTIVE');

-- 3. USER_ROLE_ASSIGNMENTS - Gán quyền cho user
INSERT OR REPLACE INTO USER_ROLE_ASSIGNMENT (userID, roleID) VALUES 
('user_admin_001', 'ADMIN'),
('user_pm_001', 'PRODUCT_MANAGER'),
('user_customer_001', 'CUSTOMER'),
('user_customer_002', 'CUSTOMER'),
('user_customer_003', 'CUSTOMER'),
('user_blocked_001', 'CUSTOMER'),
('user_inactive_001', 'CUSTOMER');

-- 4. PRODUCTS - Comprehensive product catalog for home screen UI testing (40 products)
INSERT OR REPLACE INTO PRODUCT (productID, title, category, value_amount, price, quantityInStock, description, imageURL, barcode, dimensions_cm, weight_kg, entryDate, productType) VALUES 
-- BOOKS (15 products with diverse characteristics)
('book_001', 'Clean Code: A Handbook of Agile Software Craftsmanship', 'Programming', 45.99, 45.99, 25, 'Hướng dẫn viết code sạch và dễ maintain. Cuốn sách kinh điển cho developers.', '/images/books/clean_code.jpg', '9780132350884', '23.5x19.1x2.5', 0.6, '2024-01-15', 'BOOK'),
('book_002', 'Short Title', 'Fiction', 12.50, 12.50, 0, 'A brief story.', NULL, '9780201633610', '18x12x1', 0.3, '2024-01-16', 'BOOK'),
('book_003', 'An Extremely Long and Detailed Title About Advanced Machine Learning Algorithms and Their Applications in Modern Data Science', 'Computer Science', 89.99, 89.99, 15, 'Deep dive into ML algorithms with practical examples and case studies from industry experts.', '/images/books/ml_book.jpg', '9780135957059', '26x20x4', 1.2, '2024-01-17', 'BOOK'),
('book_004', 'Cookbook Essentials', 'Lifestyle', 29.99, 29.99, 50, 'Delicious recipes for everyday cooking with step-by-step instructions.', NULL, '9780262033848', '25x20x2', 0.8, '2024-01-18', 'BOOK'),
('book_005', 'History of Ancient Civilizations: From Mesopotamia to the Roman Empire', 'History', 67.50, 67.50, 8, 'Comprehensive overview of ancient civilizations and their lasting contributions to humanity.', '/images/books/history.jpg', '9780596009205', '24x18x3', 0.9, '2024-01-19', 'BOOK'),
('book_006', 'Poetry Collection', 'Literature', 18.75, 18.75, 0, 'Beautiful collection of contemporary poetry.', '/images/books/poetry.jpg', '9780132350885', '20x15x1.5', 0.4, '2024-01-20', 'BOOK'),
('book_007', 'Business Strategy and Innovation in the Digital Age', 'Business', 54.99, 54.99, 32, 'Modern business strategies for digital transformation and competitive advantage.', NULL, '9780201633611', '23x19x2.5', 0.7, '2024-01-21', 'BOOK'),
('book_008', 'Art Techniques', 'Arts', 35.00, 35.00, 12, 'Modern art movements and practical drawing techniques.', '/images/books/art.jpg', '9780135957060', '28x22x2', 1.0, '2024-01-22', 'BOOK'),
('book_009', 'The Ultimate Guide to Healthy Living and Wellness in the 21st Century', 'Health', 41.25, 41.25, 0, 'Complete guide to healthy lifestyle choices and wellness practices.', NULL, '9780262033849', '23x18x2', 0.6, '2024-01-23', 'BOOK'),
('book_010', 'Travel Guide Europe', 'Travel', 22.99, 22.99, 18, 'Essential travel tips and destinations across Europe.', '/images/books/travel.jpg', '9780596009206', '21x15x2', 0.5, '2024-01-24', 'BOOK'),
('book_011', 'Psychology and Human Behavior: Understanding the Mind', 'Psychology', 58.50, 58.50, 7, 'Insights into human psychology and behavior patterns with real-world applications.', '/images/books/psychology.jpg', '9780132350886', '24x19x3', 0.8, '2024-01-25', 'BOOK'),
('book_012', 'Quick Recipes', 'Lifestyle', 16.99, 16.99, 45, 'Fast and easy recipes for busy people.', NULL, '9780201633612', '20x15x1', 0.4, '2024-01-26', 'BOOK'),
('book_013', 'Environmental Science and Sustainability for Future Generations', 'Science', 72.00, 72.00, 0, 'Environmental challenges and sustainable solutions for the future.', '/images/books/environment.jpg', '9780135957061', '25x20x3.5', 1.1, '2024-01-27', 'BOOK'),
('book_014', 'Philosophy 101', 'Philosophy', 39.99, 39.99, 20, 'Introduction to philosophical thinking and major philosophical questions.', NULL, '9780262033850', '22x17x2', 0.6, '2024-01-28', 'BOOK'),
('book_015', 'Mathematics and Its Applications in Engineering and Computer Science', 'Education', 95.50, 95.50, 5, 'Advanced mathematics for technical fields with practical applications.', '/images/books/math.jpg', '9780596009207', '26x21x4', 1.3, '2024-01-29', 'BOOK'),

-- CDs (15 products with diverse characteristics)
('cd_001', 'Classic Rock Anthology', 'Music', 19.99, 19.99, 30, 'Best classic rock hits from the 70s and 80s featuring legendary bands.', '/images/cds/classic_rock.jpg', 'CD001234567890', '12.4x14.2x1', 0.1, '2024-02-01', 'CD'),
('cd_002', 'Pop Hits', 'Music', 15.50, 15.50, 0, 'Latest pop music hits.', NULL, 'CD001234567891', '12.4x14.2x1', 0.1, '2024-02-02', 'CD'),
('cd_003', 'The Complete Works of Beethoven: Symphonies, Concertos, and Chamber Music Collection', 'Classical', 85.00, 85.00, 12, 'Comprehensive collection of Beethoven''s masterpieces performed by world-class orchestras.', '/images/cds/beethoven.jpg', 'CD001234567892', '12.4x14.2x1', 0.1, '2024-02-03', 'CD'),
('cd_004', 'Jazz Collection', 'Music', 24.99, 24.99, 25, 'Smooth jazz classics from legendary artists of the golden age.', NULL, 'CD001234567893', '12.4x14.2x1', 0.1, '2024-02-04', 'CD'),
('cd_005', 'Electronic Dance Music: The Ultimate Festival Experience', 'Electronic', 28.75, 28.75, 0, 'High-energy EDM tracks perfect for the dance floor.', '/images/cds/edm.jpg', 'CD001234567894', '12.4x14.2x1', 0.1, '2024-02-05', 'CD'),
('cd_006', 'Folk Songs', 'Music', 17.99, 17.99, 18, 'Traditional folk music from around the world.', '/images/cds/folk.jpg', 'CD001234567895', '12.4x14.2x1', 0.1, '2024-02-06', 'CD'),
('cd_007', 'Heavy Metal Legends: The Greatest Hits Collection', 'Metal', 32.50, 32.50, 22, 'Powerful metal anthems from the most iconic bands in metal history.', NULL, 'CD001234567896', '12.4x14.2x1', 0.1, '2024-02-07', 'CD'),
('cd_008', 'Chill Ambient', 'Music', 21.00, 21.00, 0, 'Relaxing ambient music for meditation and focus.', '/images/cds/ambient.jpg', 'CD001234567897', '12.4x14.2x1', 0.1, '2024-02-08', 'CD'),
('cd_009', 'World Music Journey: Sounds from Across the Globe', 'World', 35.99, 35.99, 15, 'Diverse musical traditions from different cultures and continents.', NULL, 'CD001234567898', '12.4x14.2x1', 0.1, '2024-02-09', 'CD'),
('cd_010', 'Indie Rock Revolution', 'Music', 26.50, 26.50, 28, 'Independent rock music from emerging and established artists.', '/images/cds/indie.jpg', 'CD001234567899', '12.4x14.2x1', 0.1, '2024-02-10', 'CD'),
('cd_011', 'Classical Piano: Romantic Era Masterpieces', 'Classical', 42.00, 42.00, 0, 'Beautiful piano compositions from the Romantic period by master composers.', '/images/cds/piano.jpg', 'CD001234567900', '12.4x14.2x1', 0.1, '2024-02-11', 'CD'),
('cd_012', 'Hip Hop Classics', 'Music', 23.99, 23.99, 35, 'Contemporary hip hop and rap music from top artists.', NULL, 'CD001234567901', '12.4x14.2x1', 0.1, '2024-02-12', 'CD'),
('cd_013', 'Country Music: Heartland Stories and Melodies', 'Country', 29.99, 29.99, 16, 'Authentic country music with heartfelt lyrics and beautiful melodies.', '/images/cds/country.jpg', 'CD001234567902', '12.4x14.2x1', 0.1, '2024-02-13', 'CD'),
('cd_014', 'Movie Soundtrack', 'Music', 18.50, 18.50, 0, 'Epic movie soundtrack compilation.', NULL, 'CD001234567903', '12.4x14.2x1', 0.1, '2024-02-14', 'CD'),
('cd_015', 'Blues Masters: The Definitive Collection of American Blues', 'Blues', 38.75, 38.75, 11, 'Essential blues recordings from the greatest blues musicians of all time.', '/images/cds/blues.jpg', 'CD001234567904', '12.4x14.2x1', 0.1, '2024-02-15', 'CD'),

-- DVDs (10 products with diverse characteristics)
('dvd_001', 'Action Movie Collection', 'Action', 24.99, 24.99, 20, 'High-octane action movies with spectacular stunts and thrilling sequences.', '/images/dvds/action.jpg', 'DVD1234567890', '19x13.5x1.5', 0.08, '2024-03-01', 'DVD'),
('dvd_002', 'Comedy Classics', 'Comedy', 19.50, 19.50, 0, 'Hilarious comedy collection for the whole family.', NULL, 'DVD1234567891', '19x13.5x1.5', 0.08, '2024-03-02', 'DVD'),
('dvd_003', 'The Epic Historical Drama: A Sweeping Tale of Love, War, and Redemption', 'Drama', 34.99, 34.99, 15, 'Award-winning historical drama with outstanding performances and breathtaking cinematography.', '/images/dvds/drama.jpg', 'DVD1234567892', '19x13.5x1.5', 0.08, '2024-03-03', 'DVD'),
('dvd_004', 'Sci-Fi Adventure Collection', 'Sci-Fi', 29.99, 29.99, 25, 'Futuristic adventures with cutting-edge special effects and imaginative storytelling.', '/images/dvds/scifi.jpg', 'DVD1234567893', '19x13.5x1.5', 0.08, '2024-03-04', 'DVD'),
('dvd_005', 'Horror Collection: The Most Terrifying Films Ever Made', 'Horror', 39.99, 39.99, 0, 'Spine-chilling horror movies that will keep you on the edge of your seat.', NULL, 'DVD1234567894', '19x13.5x1.5', 0.08, '2024-03-05', 'DVD'),
('dvd_006', 'Documentary Series', 'Documentary', 22.50, 22.50, 18, 'Thought-provoking documentary collection exploring fascinating topics.', '/images/dvds/documentary.jpg', 'DVD1234567895', '19x13.5x1.5', 0.08, '2024-03-06', 'DVD'),
('dvd_007', 'Animated Family Adventure: A Magical Journey for All Ages', 'Animation', 27.99, 27.99, 30, 'Heartwarming animated adventures perfect for family movie night.', NULL, 'DVD1234567896', '19x13.5x1.5', 0.08, '2024-03-07', 'DVD'),
('dvd_008', 'Thriller Collection', 'Thriller', 26.50, 26.50, 0, 'Suspenseful thrillers with unexpected twists and heart-pounding moments.', '/images/dvds/thriller.jpg', 'DVD1234567897', '19x13.5x1.5', 0.08, '2024-03-08', 'DVD'),
('dvd_009', 'Romance Collection: Timeless Love Stories', 'Romance', 31.99, 31.99, 12, 'Classic romantic movies that will warm your heart and inspire your soul.', '/images/dvds/romance.jpg', 'DVD1234567898', '19x13.5x1.5', 0.08, '2024-03-09', 'DVD'),
('dvd_010', 'Musical Spectacular', 'Musical', 33.50, 33.50, 22, 'Spectacular musical performances with unforgettable songs and dances.', NULL, 'DVD1234567899', '19x13.5x1.5', 0.08, '2024-03-10', 'DVD');

-- 5. CHI TIẾT SẢN PHẨM BOOK
INSERT OR REPLACE INTO BOOK (productID, authors, coverType, publisher, publicationDate, numPages, language, book_genre) VALUES 
('book_001', 'Robert C. Martin', 'Paperback', 'Prentice Hall', '2008-08-01', 464, 'English', 'Software Engineering'),
('book_002', 'Anonymous', 'Paperback', 'Fiction Press', '2023-01-01', 150, 'English', 'Fiction'),
('book_003', 'Dr. Sarah Johnson', 'Hardcover', 'Tech Publishers', '2023-12-15', 650, 'English', 'Computer Science'),
('book_004', 'Chef Marie Claire', 'Paperback', 'Culinary Books', '2023-06-20', 280, 'English', 'Lifestyle'),
('book_005', 'Prof. William Harrison', 'Hardcover', 'Academic Press', '2023-11-08', 720, 'English', 'History'),
('book_006', 'Various Poets', 'Paperback', 'Poetry House', '2023-03-14', 200, 'English', 'Literature'),
('book_007', 'Michael Thompson', 'Hardcover', 'Business Leaders', '2023-09-22', 420, 'English', 'Business'),
('book_008', 'Anna Rodriguez', 'Paperback', 'Art World', '2023-07-11', 340, 'English', 'Arts'),
('book_009', 'Dr. Lisa Wong', 'Hardcover', 'Health Publishing', '2023-10-05', 480, 'English', 'Health'),
('book_010', 'Travel Team', 'Paperback', 'Wanderlust Press', '2023-04-18', 320, 'English', 'Travel'),
('book_011', 'Dr. James Miller', 'Hardcover', 'Psychology Today', '2023-08-30', 560, 'English', 'Psychology'),
('book_012', 'Quick Chef', 'Paperback', 'Fast Food Books', '2023-05-25', 180, 'English', 'Lifestyle'),
('book_013', 'Dr. Environmental Team', 'Hardcover', 'Green Publishers', '2023-12-01', 680, 'English', 'Science'),
('book_014', 'Prof. Philosophy', 'Paperback', 'Wisdom Books', '2023-02-14', 380, 'English', 'Philosophy'),
('book_015', 'Math Team', 'Hardcover', 'Academic Excellence', '2023-11-20', 850, 'English', 'Education');

-- 6. CHI TIẾT SẢN PHẨM CD
INSERT OR REPLACE INTO CD (productID, artists, recordLabel, tracklist, cd_genre, releaseDate) VALUES 
('cd_001', 'Various Artists', 'Classic Rock Records', 'Highway to Hell;Smoke on the Water;Stairway to Heaven;Free Bird;Hotel California', 'Rock', '2023-02-01'),
('cd_002', 'Pop Stars', 'Hit Records', 'Dancing Queen;Billie Jean;Like a Prayer;I Want It That Way', 'Pop', '2023-02-02'),
('cd_003', 'Vienna Philharmonic', 'Classical Masters', 'Symphony No. 9;Moonlight Sonata;Für Elise;Ode to Joy', 'Classical', '2023-02-03'),
('cd_004', 'Jazz Legends', 'Blue Note Records', 'Take Five;Round Midnight;All Blues;So What', 'Jazz', '2023-02-04'),
('cd_005', 'EDM Collective', 'Electronic Beats', 'Levels;Titanium;Animals;Clarity', 'Electronic', '2023-02-05'),
('cd_006', 'Folk Ensemble', 'Traditional Music', 'Scarborough Fair;House of the Rising Sun;The Water is Wide', 'Folk', '2023-02-06'),
('cd_007', 'Metal Masters', 'Heavy Records', 'Master of Puppets;Iron Man;Paranoid;Breaking the Law', 'Metal', '2023-02-07'),
('cd_008', 'Ambient Artists', 'Peaceful Sounds', 'Deep Peace;Floating;Meditation;Serenity', 'Ambient', '2023-02-08'),
('cd_009', 'World Music Collective', 'Global Sounds', 'African Drums;Celtic Dance;Indian Raga;Latin Rhythm', 'World', '2023-02-09'),
('cd_010', 'Indie Rock Band', 'Independent Label', 'Alternative Song;Indie Anthem;Rock Revolution;Underground', 'Indie Rock', '2023-02-10'),
('cd_011', 'Piano Masters', 'Classical Excellence', 'Chopin Nocturne;Debussy Clair de Lune;Liszt Hungarian Rhapsody', 'Classical Piano', '2023-02-11'),
('cd_012', 'Hip Hop Artists', 'Urban Beats', 'Rap God;Lose Yourself;Stronger;Gold Digger', 'Hip Hop', '2023-02-12'),
('cd_013', 'Country Stars', 'Nashville Records', 'Country Roads;Sweet Caroline;Friends in Low Places', 'Country', '2023-02-13'),
('cd_014', 'Movie Soundtrack', 'Hollywood Music', 'Theme from Titanic;James Bond Theme;Star Wars Main Title', 'Soundtrack', '2023-02-14'),
('cd_015', 'Blues Masters', 'Chicago Blues', 'Sweet Home Chicago;The Thrill is Gone;Pride and Joy', 'Blues', '2023-02-15');

-- 7. CHI TIẾT SẢN PHẨM DVD
INSERT OR REPLACE INTO DVD (productID, discType, director, runtime_minutes, studio, dvd_language, subtitles, dvd_releaseDate, dvd_genre) VALUES 
('dvd_001', 'DVD', 'Action Director', 136, 'Action Studios', 'English', 'English, Spanish, French', '2023-03-01', 'Action'),
('dvd_002', 'DVD', 'Comedy Master', 120, 'Laugh Productions', 'English', 'English, Spanish', '2023-03-02', 'Comedy'),
('dvd_003', 'DVD', 'Drama Director', 180, 'Epic Films', 'English', 'English, Spanish, French, German', '2023-03-03', 'Historical Drama'),
('dvd_004', 'DVD', 'Sci-Fi Visionary', 145, 'Future Films', 'English', 'English, Spanish, French, Japanese', '2023-03-04', 'Science Fiction'),
('dvd_005', 'DVD', 'Horror Master', 105, 'Scary Movies Inc', 'English', 'English, Spanish', '2023-03-05', 'Horror'),
('dvd_006', 'DVD', 'Documentary Team', 95, 'Educational Films', 'English', 'English, Spanish, French', '2023-03-06', 'Documentary'),
('dvd_007', 'DVD', 'Animation Studio', 110, 'Magic Animation', 'English', 'English, Spanish, French, Portuguese', '2023-03-07', 'Animation'),
('dvd_008', 'DVD', 'Thriller Director', 130, 'Suspense Productions', 'English', 'English, Spanish, French', '2023-03-08', 'Thriller'),
('dvd_009', 'DVD', 'Romance Director', 125, 'Romantic Films', 'English', 'English, Spanish, French, Italian', '2023-03-09', 'Romance'),
('dvd_010', 'DVD', 'Musical Director', 140, 'Broadway Studios', 'English', 'English, Spanish, French', '2023-03-10', 'Musical');

-- 8. PAYMENT_METHODS - Phương thức thanh toán test
INSERT OR REPLACE INTO PAYMENT_METHOD (paymentMethodID, methodType, userID, isDefault) VALUES 
('pm_card_001', 'CREDIT_CARD', 'user_customer_001', 1),
('pm_card_002', 'DOMESTIC_DEBIT_CARD', 'user_customer_002', 1),
('pm_card_003', 'CREDIT_CARD', 'user_customer_003', 1),
('pm_card_004', 'DOMESTIC_DEBIT_CARD', 'user_customer_001', 0);

-- 9. CHI TIẾT THẺ
INSERT OR REPLACE INTO CARD_DETAILS (paymentMethodID, cardholderName, cardNumber_masked, expiryDate_MMYY, validFromDate_MMYY, issuingBank) VALUES 
('pm_card_001', 'Customer One', '**** **** **** 1234', '12/28', '01/24', 'VietcomBank'),
('pm_card_002', 'Customer Two', '**** **** **** 5678', '06/29', '03/24', 'TechcomBank'),
('pm_card_003', 'John Doe', '**** **** **** 9999', '09/27', '05/23', 'BIDV'),
('pm_card_004', 'Customer One Alt', '**** **** **** 4321', '03/30', '07/24', 'VietinBank');

-- 10. CARTS - Giỏ hàng test (một số có sẵn items)
INSERT OR REPLACE INTO CART (cartSessionID, userID, lastUpdated) VALUES 
('cart_empty_001', 'user_customer_001', '2024-06-04 09:00:00'),
('cart_with_items_001', 'user_customer_002', '2024-06-04 10:30:00'),
('cart_with_items_002', 'user_customer_003', '2024-06-04 11:15:00'),
('cart_guest_001', NULL, '2024-06-04 12:00:00');

-- 11. CART_ITEMS - Items trong giỏ hàng
INSERT OR REPLACE INTO CART_ITEM (cartSessionID, productID, quantity) VALUES 
-- Cart của customer_002 có nhiều items
('cart_with_items_001', 'book_001', 2),
('cart_with_items_001', 'cd_001', 1),
('cart_with_items_001', 'dvd_001', 1),
-- Cart của customer_003 có ít items
('cart_with_items_002', 'book_002', 1),
('cart_with_items_002', 'cd_003', 1),
-- Guest cart
('cart_guest_001', 'dvd_002', 1);

-- 12. ORDERS - Đơn hàng test với các trạng thái khác nhau
INSERT OR REPLACE INTO ORDER_ENTITY (orderID, userID, orderDate, order_status, totalProductPriceExclVAT, totalProductPriceInclVAT, calculatedDeliveryFee, totalAmountPaid) VALUES 
-- Đơn hàng đang chờ xử lý
('order_pending_001', 'user_customer_001', '2024-06-04 08:00:00', 'PENDING_PROCESSING', 91.98, 101.18, 5.00, 0.0),
-- Đơn hàng đã được approve
('order_approved_001', 'user_customer_002', '2024-06-03 15:30:00', 'APPROVED', 54.99, 60.49, 10.00, 70.49),
-- Đơn hàng đã giao
('order_delivered_001', 'user_customer_003', '2024-06-02 11:20:00', 'DELIVERED', 129.96, 142.96, 15.00, 157.96),
-- Đơn hàng bị hủy
('order_cancelled_001', 'user_customer_001', '2024-06-01 09:45:00', 'CANCELLED', 39.99, 43.99, 5.00, 0.0);

-- 13. ORDER_ITEMS - Items trong đơn hàng
INSERT OR REPLACE INTO ORDER_ITEM (orderID, productID, quantity, priceAtTimeOfOrder, isEligibleForRushDelivery) VALUES 
-- Order pending
('order_pending_001', 'book_001', 2, 45.99, 1),
-- Order approved  
('order_approved_001', 'book_002', 1, 54.99, 1),
-- Order delivered
('order_delivered_001', 'book_003', 1, 49.99, 1),
('order_delivered_001', 'cd_002', 2, 17.99, 0),
('order_delivered_001', 'dvd_003', 2, 19.99, 0),
-- Order cancelled
('order_cancelled_001', 'book_005', 1, 39.99, 1);

-- 14. DELIVERY_INFO - Thông tin giao hàng
INSERT OR REPLACE INTO DELIVERY_INFO (deliveryInfoID, orderID, recipientName, email, phoneNumber, deliveryProvinceCity, deliveryAddress, deliveryInstructions, deliveryMethodChosen, requestedRushDeliveryTime) VALUES 
('delivery_001', 'order_pending_001', 'Nguyen Van A', 'customer1@test.com', '0123456789', 'Hà Nội', '123 Phố Huế, Quận Hai Bà Trưng, Hà Nội', 'Gọi trước khi giao', 'STANDARD', NULL),
('delivery_002', 'order_approved_001', 'Tran Thi B', 'customer2@test.com', '0987654321', 'TP. Hồ Chí Minh', '456 Nguyễn Huệ, Quận 1, TP.HCM', 'Giao trong giờ hành chính', 'RUSH', '2024-06-03 18:00:00'),
('delivery_003', 'order_delivered_001', 'Le Van C', 'john.doe@test.com', '0912345678', 'Đà Nẵng', '789 Trần Phú, Quận Hải Châu, Đà Nẵng', NULL, 'STANDARD', NULL),
('delivery_004', 'order_cancelled_001', 'Nguyen Van A', 'customer1@test.com', '0123456789', 'Hà Nội', '123 Phố Huế, Quận Hai Bà Trưng, Hà Nội', NULL, 'STANDARD', NULL);

-- 15. INVOICES - Hóa đơn
INSERT OR REPLACE INTO INVOICE (invoiceID, orderID, invoiceDate, invoicedTotalAmount) VALUES 
('invoice_001', 'order_approved_001', '2024-06-03 16:00:00', 70.49),
('invoice_002', 'order_delivered_001', '2024-06-02 12:00:00', 157.96);

-- 16. PAYMENT_TRANSACTIONS - Giao dịch thanh toán
INSERT OR REPLACE INTO PAYMENT_TRANSACTION (transactionID, orderID, paymentMethodID, transactionType, externalTransactionID, transaction_status, transactionDateTime, amount, transactionContent) VALUES 
-- Thanh toán thành công cho order_approved
('trans_success_001', 'order_approved_001', 'pm_card_002', 'PAYMENT', 'VNP_TXN_20240603_001', 'SUCCESS', '2024-06-03 15:45:00', 70.49, 'Payment for order_approved_001 via VNPay'),
-- Thanh toán thành công cho order_delivered
('trans_success_002', 'order_delivered_001', 'pm_card_003', 'PAYMENT', 'VNP_TXN_20240602_001', 'SUCCESS', '2024-06-02 11:35:00', 157.96, 'Payment for order_delivered_001 via VNPay'),
-- Giao dịch thất bại
('trans_failed_001', 'order_pending_001', 'pm_card_001', 'PAYMENT', 'VNP_TXN_20240604_001', 'FAILED', '2024-06-04 08:15:00', 106.18, 'Failed payment for order_pending_001 - Insufficient funds');
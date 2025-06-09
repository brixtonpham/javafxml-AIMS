-- V3__seed_ui_test_data.sql
-- Comprehensive test dataset with 30+ products across all categories
-- Features: Diverse products (Books/CDs/DVDs), varied stock levels, price ranges for testing shipping thresholds

-- Enable foreign key constraints
PRAGMA foreign_keys = ON;

-- Clear existing data (in dependency order)
DELETE FROM PAYMENT_TRANSACTION;
DELETE FROM CARD_DETAILS;
DELETE FROM PAYMENT_METHOD;
DELETE FROM INVOICE;
DELETE FROM DELIVERY_INFO;
DELETE FROM ORDER_ITEM;
DELETE FROM ORDER_ENTITY;
DELETE FROM CART_ITEM;
DELETE FROM CART;
DELETE FROM USER_ROLE_ASSIGNMENT;
DELETE FROM BOOK;
DELETE FROM CD;
DELETE FROM DVD;
DELETE FROM PRODUCT;
DELETE FROM USER_ACCOUNT;
DELETE FROM ROLE;

-- Insert Roles
INSERT INTO ROLE (roleID, roleName) VALUES
    ('ADMIN', 'Administrator'),
    ('PRODUCT_MANAGER', 'Product Manager'),
    ('CUSTOMER', 'Customer');

-- Insert Test Users
INSERT INTO USER_ACCOUNT (userID, username, password_hash, email, user_status) VALUES
    ('TEST_USER_001', 'testuser1', '$2a$10$test.hash.1', 'testuser1@test.com', 'ACTIVE'),
    ('TEST_USER_002', 'testuser2', '$2a$10$test.hash.2', 'testuser2@test.com', 'ACTIVE'),
    ('ADMIN_USER_001', 'admin', '$2a$10$admin.hash', 'admin@aims.com', 'ACTIVE'),
    ('PM_USER_001', 'productmanager', '$2a$10$pm.hash', 'pm@aims.com', 'ACTIVE');

-- Assign roles to users
INSERT INTO USER_ROLE_ASSIGNMENT (userID, roleID) VALUES
    ('ADMIN_USER_001', 'ADMIN'),
    ('PM_USER_001', 'PRODUCT_MANAGER'),
    ('TEST_USER_001', 'CUSTOMER'),
    ('TEST_USER_002', 'CUSTOMER');

-- ========================================
-- BOOKS (12 products)
-- ========================================

-- High Stock Books (5-6 items)
INSERT INTO PRODUCT (productID, title, category, value_amount, price, quantityInStock, description, imageURL, barcode, dimensions_cm, weight_kg, entryDate, productType) VALUES
    ('BOOK_001', 'The Complete Guide to Java Programming', 'BOOK', 45.00, 49.50, 15, 'Comprehensive guide to Java programming for beginners and intermediate developers', '/images/books/java_guide.jpg', '9781234567890', '23x15x3', 0.8, '2024-01-01', 'BOOK'),
    ('BOOK_002', 'JavaScript: The Definitive Guide', 'BOOK', 50.00, 55.00, 12, 'The authoritative guide to JavaScript programming', '/images/books/js_guide.jpg', '9781234567891', '24x16x3.5', 0.9, '2024-01-02', 'BOOK'),
    ('BOOK_003', 'Python Machine Learning Handbook', 'BOOK', 60.00, 66.00, 18, 'Advanced Python techniques for machine learning', '/images/books/python_ml.jpg', '9781234567892', '25x17x4', 1.0, '2024-01-03', 'BOOK'),
    ('BOOK_004', 'Clean Code: A Handbook of Agile Software Craftsmanship', 'BOOK', 40.00, 44.00, 20, 'Essential reading for software developers', '/images/books/clean_code.jpg', '9781234567893', '23x15x2.5', 0.7, '2024-01-04', 'BOOK'),
    ('BOOK_005', 'Design Patterns: Elements of Reusable Object-Oriented Software', 'BOOK', 55.00, 60.50, 10, 'Classic book on software design patterns', '/images/books/design_patterns.jpg', '9781234567894', '24x16x3', 0.8, '2024-01-05', 'BOOK');

-- Medium Stock Books (3-4 items)
INSERT INTO PRODUCT (productID, title, category, value_amount, price, quantityInStock, description, imageURL, barcode, dimensions_cm, weight_kg, entryDate, productType) VALUES
    ('BOOK_006', 'The Art of Computer Programming Volume 1', 'BOOK', 70.00, 77.00, 4, 'Donald Knuth''s masterpiece on algorithms', '/images/books/knuth_vol1.jpg', '9781234567895', '26x18x4', 1.2, '2024-01-06', 'BOOK'),
    ('BOOK_007', 'Introduction to Algorithms', 'BOOK', 65.00, 71.50, 3, 'Comprehensive introduction to algorithms and data structures', '/images/books/intro_algorithms.jpg', '9781234567896', '25x17x3.5', 1.1, '2024-01-07', 'BOOK'),
    ('BOOK_008', 'Database System Concepts', 'BOOK', 75.00, 82.50, 3, 'Fundamental concepts of database systems', '/images/books/db_concepts.jpg', '9781234567897', '24x16x4', 1.0, '2024-01-08', 'BOOK');

-- Low Stock Books (1-2 items)
INSERT INTO PRODUCT (productID, title, category, value_amount, price, quantityInStock, description, imageURL, barcode, dimensions_cm, weight_kg, entryDate, productType) VALUES
    ('BOOK_LOW_STOCK_1', 'Advanced Data Structures', 'BOOK', 80.00, 88.00, 1, 'Advanced techniques in data structure implementation', '/images/books/advanced_ds.jpg', '9781234567898', '25x17x3', 0.9, '2024-01-09', 'BOOK'),
    ('BOOK_LOW_STOCK_2', 'Compiler Design Principles', 'BOOK', 85.00, 93.50, 2, 'Comprehensive guide to compiler construction', '/images/books/compiler_design.jpg', '9781234567899', '24x16x3.5', 1.0, '2024-01-10', 'BOOK');

-- Out of Stock Books
INSERT INTO PRODUCT (productID, title, category, value_amount, price, quantityInStock, description, imageURL, barcode, dimensions_cm, weight_kg, entryDate, productType) VALUES
    ('BOOK_OUT_OF_STOCK', 'Quantum Computing: An Applied Approach', 'BOOK', 90.00, 99.00, 0, 'Introduction to quantum computing principles', '/images/books/quantum_computing.jpg', '9781234567900', '26x18x3', 1.1, '2024-01-11', 'BOOK'),
    ('BOOK_SEARCH_TEST', 'The Ultimate Programming Guide for Beginners', 'BOOK', 35.00, 38.50, 25, 'Perfect guide for programming beginners', '/images/books/ultimate_guide.jpg', '9781234567901', '22x14x2', 0.6, '2024-01-12', 'BOOK');

-- Insert BOOK details
INSERT INTO BOOK (productID, authors, coverType, publisher, publicationDate, numPages, language, book_genre) VALUES
    ('BOOK_001', 'John Smith, Jane Doe', 'Paperback', 'Tech Publications', '2023-03-15', 650, 'English', 'Programming'),
    ('BOOK_002', 'David Flanagan', 'Hardcover', 'O''Reilly Media', '2023-05-20', 1096, 'English', 'Programming'),
    ('BOOK_003', 'Sebastian Raschka', 'Paperback', 'Packt Publishing', '2023-07-10', 540, 'English', 'Technology'),
    ('BOOK_004', 'Robert C. Martin', 'Paperback', 'Prentice Hall', '2023-02-28', 464, 'English', 'Programming'),
    ('BOOK_005', 'Gang of Four', 'Hardcover', 'Addison-Wesley', '2023-04-12', 395, 'English', 'Programming'),
    ('BOOK_006', 'Donald E. Knuth', 'Hardcover', 'Addison-Wesley', '2023-06-05', 672, 'English', 'Computer Science'),
    ('BOOK_007', 'Thomas H. Cormen', 'Hardcover', 'MIT Press', '2023-08-18', 1312, 'English', 'Computer Science'),
    ('BOOK_008', 'Abraham Silberschatz', 'Paperback', 'McGraw-Hill', '2023-09-22', 1376, 'English', 'Database'),
    ('BOOK_LOW_STOCK_1', 'Mark Allen Weiss', 'Paperback', 'Pearson', '2023-10-15', 608, 'English', 'Computer Science'),
    ('BOOK_LOW_STOCK_2', 'Alfred V. Aho', 'Hardcover', 'Addison-Wesley', '2023-11-08', 1040, 'English', 'Computer Science'),
    ('BOOK_OUT_OF_STOCK', 'Hidary Jack D.', 'Hardcover', 'Springer', '2023-12-01', 380, 'English', 'Quantum Physics'),
    ('BOOK_SEARCH_TEST', 'Programming Masters', 'Paperback', 'Beginner Press', '2024-01-15', 320, 'English', 'Programming');

-- ========================================
-- CDs (10 products) 
-- ========================================

-- High Stock CDs
INSERT INTO PRODUCT (productID, title, category, value_amount, price, quantityInStock, description, imageURL, barcode, dimensions_cm, weight_kg, entryDate, productType) VALUES
    ('CD_001', 'The Greatest Hits Collection', 'CD', 15.00, 16.50, 25, 'Best hits from various artists', '/images/cds/greatest_hits.jpg', '1234567890123', '12x12x1', 0.1, '2024-01-01', 'CD'),
    ('CD_002', 'Jazz Legends: The Ultimate Collection', 'CD', 18.00, 19.80, 20, 'Classic jazz performances from legendary artists', '/images/cds/jazz_legends.jpg', '1234567890124', '12x12x1', 0.1, '2024-01-02', 'CD'),
    ('CD_003', 'Rock Classics Vol. 1', 'CD', 16.00, 17.60, 22, 'Essential rock songs from the 70s and 80s', '/images/cds/rock_classics.jpg', '1234567890125', '12x12x1', 0.1, '2024-01-03', 'CD'),
    ('CD_004', 'Classical Masterpieces', 'CD', 20.00, 22.00, 15, 'Beautiful classical compositions', '/images/cds/classical.jpg', '1234567890126', '12x12x1', 0.1, '2024-01-04', 'CD'),
    ('CD_005', 'World Music Journey', 'CD', 17.00, 18.70, 18, 'Music from around the world', '/images/cds/world_music.jpg', '1234567890127', '12x12x1', 0.1, '2024-01-05', 'CD');

-- Medium Stock CDs
INSERT INTO PRODUCT (productID, title, category, value_amount, price, quantityInStock, description, imageURL, barcode, dimensions_cm, weight_kg, entryDate, productType) VALUES
    ('CD_006', 'Blues Brothers: Live in Concert', 'CD', 22.00, 24.20, 4, 'Live performance from the Blues Brothers', '/images/cds/blues_brothers.jpg', '1234567890128', '12x12x1', 0.1, '2024-01-06', 'CD'),
    ('CD_007', 'Electronic Beats Vol. 2', 'CD', 19.00, 20.90, 3, 'Modern electronic music compilation', '/images/cds/electronic_beats.jpg', '1234567890129', '12x12x1', 0.1, '2024-01-07', 'CD');

-- Low Stock CDs
INSERT INTO PRODUCT (productID, title, category, value_amount, price, quantityInStock, description, imageURL, barcode, dimensions_cm, weight_kg, entryDate, productType) VALUES
    ('CD_LOW_STOCK_2', 'Rare Jazz Sessions', 'CD', 25.00, 27.50, 2, 'Rare recordings from underground jazz sessions', '/images/cds/rare_jazz.jpg', '1234567890130', '12x12x1', 0.1, '2024-01-08', 'CD'),
    ('CD_008', 'Acoustic Guitar Melodies', 'CD', 21.00, 23.10, 1, 'Peaceful acoustic guitar compositions', '/images/cds/acoustic_guitar.jpg', '1234567890131', '12x12x1', 0.1, '2024-01-09', 'CD');

-- Out of Stock CD
INSERT INTO PRODUCT (productID, title, category, value_amount, price, quantityInStock, description, imageURL, barcode, dimensions_cm, weight_kg, entryDate, productType) VALUES
    ('CD_OUT_OF_STOCK', 'Limited Edition Symphony', 'CD', 30.00, 33.00, 0, 'Limited edition classical symphony recording', '/images/cds/limited_symphony.jpg', '1234567890132', '12x12x1', 0.1, '2024-01-10', 'CD');

-- Insert CD details
INSERT INTO CD (productID, artists, recordLabel, tracklist, cd_genre, releaseDate) VALUES
    ('CD_001', 'Various Artists', 'Universal Music', 'Track 1: Hit Song 1, Track 2: Hit Song 2, Track 3: Hit Song 3', 'Pop', '2023-01-15'),
    ('CD_002', 'Miles Davis, John Coltrane, Bill Evans', 'Blue Note Records', 'Track 1: So What, Track 2: Kind of Blue, Track 3: All Blues', 'Jazz', '2023-02-20'),
    ('CD_003', 'Led Zeppelin, The Beatles, Queen', 'EMI Records', 'Track 1: Stairway to Heaven, Track 2: Bohemian Rhapsody, Track 3: Hey Jude', 'Rock', '2023-03-10'),
    ('CD_004', 'Vienna Philharmonic', 'Deutsche Grammophon', 'Track 1: Symphony No. 9, Track 2: Moonlight Sonata, Track 3: Für Elise', 'Classical', '2023-04-05'),
    ('CD_005', 'Various World Artists', 'World Music Label', 'Track 1: African Rhythms, Track 2: Indian Ragas, Track 3: Celtic Melodies', 'World', '2023-05-12'),
    ('CD_006', 'Blues Brothers', 'Atlantic Records', 'Track 1: Soul Man, Track 2: Sweet Home Chicago, Track 3: Everybody Needs Somebody', 'Blues', '2023-06-18'),
    ('CD_007', 'Various Electronic Artists', 'Electronic Music Corp', 'Track 1: Digital Dreams, Track 2: Synthetic Waves, Track 3: Binary Beats', 'Electronic', '2023-07-25'),
    ('CD_LOW_STOCK_2', 'Underground Jazz Collective', 'Independent Label', 'Track 1: Midnight Sessions, Track 2: Urban Jazz, Track 3: City Lights', 'Jazz', '2023-08-30'),
    ('CD_008', 'Acoustic Masters', 'Indie Music', 'Track 1: Morning Dew, Track 2: Forest Whispers, Track 3: Ocean Breeze', 'Acoustic', '2023-09-14'),
    ('CD_OUT_OF_STOCK', 'London Symphony Orchestra', 'EMI Classics', 'Track 1: Symphony in D Minor, Track 2: Adagio for Strings, Track 3: Finale', 'Classical', '2023-10-08');

-- ========================================
-- DVDs (10 products)
-- ========================================

-- High Stock DVDs
INSERT INTO PRODUCT (productID, title, category, value_amount, price, quantityInStock, description, imageURL, barcode, dimensions_cm, weight_kg, entryDate, productType) VALUES
    ('DVD_001', 'The Shawshank Redemption', 'DVD', 12.00, 13.20, 30, 'Classic drama about hope and friendship', '/images/dvds/shawshank.jpg', '9876543210123', '19x14x1.5', 0.08, '2024-01-01', 'DVD'),
    ('DVD_002', 'The Godfather Trilogy', 'DVD', 25.00, 27.50, 15, 'Complete trilogy of the classic crime saga', '/images/dvds/godfather.jpg', '9876543210124', '19x14x3', 0.15, '2024-01-02', 'DVD'),
    ('DVD_003', 'Inception', 'DVD', 14.00, 15.40, 28, 'Mind-bending science fiction thriller', '/images/dvds/inception.jpg', '9876543210125', '19x14x1.5', 0.08, '2024-01-03', 'DVD'),
    ('DVD_004', 'Pulp Fiction', 'DVD', 13.00, 14.30, 25, 'Quentin Tarantino''s masterpiece', '/images/dvds/pulp_fiction.jpg', '9876543210126', '19x14x1.5', 0.08, '2024-01-04', 'DVD'),
    ('DVD_005', 'The Lord of the Rings: Fellowship', 'DVD', 16.00, 17.60, 20, 'First part of the epic fantasy trilogy', '/images/dvds/lotr_fellowship.jpg', '9876543210127', '19x14x1.5', 0.08, '2024-01-05', 'DVD');

-- Medium Stock DVDs
INSERT INTO PRODUCT (productID, title, category, value_amount, price, quantityInStock, description, imageURL, barcode, dimensions_cm, weight_kg, entryDate, productType) VALUES
    ('DVD_006', 'Star Wars: A New Hope', 'DVD', 15.00, 16.50, 4, 'The original Star Wars movie', '/images/dvds/star_wars_nh.jpg', '9876543210128', '19x14x1.5', 0.08, '2024-01-06', 'DVD'),
    ('DVD_007', 'Casablanca', 'DVD', 11.00, 12.10, 3, 'Classic romantic drama', '/images/dvds/casablanca.jpg', '9876543210129', '19x14x1.5', 0.08, '2024-01-07', 'DVD');

-- Low Stock DVDs
INSERT INTO PRODUCT (productID, title, category, value_amount, price, quantityInStock, description, imageURL, barcode, dimensions_cm, weight_kg, entryDate, productType) VALUES
    ('DVD_LOW_STOCK_3', 'Citizen Kane', 'DVD', 10.00, 11.00, 3, 'Orson Welles'' masterpiece', '/images/dvds/citizen_kane.jpg', '9876543210130', '19x14x1.5', 0.08, '2024-01-08', 'DVD'),
    ('DVD_008', 'The Matrix', 'DVD', 13.50, 14.85, 1, 'Groundbreaking sci-fi action film', '/images/dvds/matrix.jpg', '9876543210131', '19x14x1.5', 0.08, '2024-01-09', 'DVD');

-- Out of Stock DVD
INSERT INTO PRODUCT (productID, title, category, value_amount, price, quantityInStock, description, imageURL, barcode, dimensions_cm, weight_kg, entryDate, productType) VALUES
    ('DVD_OUT_OF_STOCK', 'Rare Director''s Cut Collection', 'DVD', 35.00, 38.50, 0, 'Limited edition director''s cuts', '/images/dvds/directors_cut.jpg', '9876543210132', '19x14x4', 0.2, '2024-01-10', 'DVD');

-- Insert DVD details
INSERT INTO DVD (productID, discType, director, runtime_minutes, studio, dvd_language, subtitles, dvd_releaseDate, dvd_genre) VALUES
    ('DVD_001', 'Standard', 'Frank Darabont', 142, 'Columbia Pictures', 'English', 'English, Spanish, French', '1994-09-23', 'Drama'),
    ('DVD_002', 'Special Edition', 'Francis Ford Coppola', 537, 'Paramount Pictures', 'English', 'English, Italian, Spanish', '1972-03-24', 'Crime'),
    ('DVD_003', 'Standard', 'Christopher Nolan', 148, 'Warner Bros', 'English', 'English, Spanish, French, German', '2010-07-16', 'Sci-Fi'),
    ('DVD_004', 'Special Edition', 'Quentin Tarantino', 154, 'Miramax Films', 'English', 'English, Spanish, French', '1994-10-14', 'Crime'),
    ('DVD_005', 'Extended Edition', 'Peter Jackson', 228, 'New Line Cinema', 'English', 'English, Spanish, French, German', '2001-12-19', 'Fantasy'),
    ('DVD_006', 'Remastered', 'George Lucas', 121, 'Lucasfilm', 'English', 'English, Spanish, French', '1977-05-25', 'Sci-Fi'),
    ('DVD_007', 'Classic Edition', 'Michael Curtiz', 102, 'Warner Bros', 'English', 'English, Spanish, French', '1942-11-26', 'Romance'),
    ('DVD_LOW_STOCK_3', 'Criterion Collection', 'Orson Welles', 119, 'RKO Pictures', 'English', 'English, Spanish, French', '1941-05-01', 'Drama'),
    ('DVD_008', 'Special Edition', 'The Wachowskis', 136, 'Warner Bros', 'English', 'English, Spanish, French, German', '1999-03-31', 'Sci-Fi'),
    ('DVD_OUT_OF_STOCK', 'Limited Edition', 'Various Directors', 720, 'Independent Studio', 'English', 'English, Spanish, French', '2023-12-01', 'Drama');

-- ========================================
-- Test Cart Sessions and Items
-- ========================================

-- Create test cart sessions for different scenarios
INSERT INTO CART (cartSessionID, userID, lastUpdated) VALUES
    ('empty-cart-session', null, '2024-01-01 10:00:00'),
    ('populated-cart-session', 'TEST_USER_001', '2024-01-01 11:00:00'),
    ('stock-issues-cart', null, '2024-01-01 12:00:00'),
    ('free-shipping-cart', 'TEST_USER_002', '2024-01-01 13:00:00');

-- Populated cart items
INSERT INTO CART_ITEM (cartSessionID, productID, quantity) VALUES
    ('populated-cart-session', 'BOOK_001', 2),
    ('populated-cart-session', 'CD_001', 1),
    ('populated-cart-session', 'DVD_001', 1);

-- Stock issues cart (quantities exceeding available stock)
INSERT INTO CART_ITEM (cartSessionID, productID, quantity) VALUES
    ('stock-issues-cart', 'BOOK_LOW_STOCK_1', 5),  -- Only 1 available
    ('stock-issues-cart', 'CD_LOW_STOCK_2', 10);   -- Only 2 available

-- Free shipping cart (high value items)
INSERT INTO CART_ITEM (cartSessionID, productID, quantity) VALUES
    ('free-shipping-cart', 'BOOK_003', 2),  -- High value book
    ('free-shipping-cart', 'DVD_002', 1);   -- High value DVD

-- ========================================
-- Test Payment Methods
-- ========================================

INSERT INTO PAYMENT_METHOD (paymentMethodID, methodType, userID, isDefault) VALUES
    ('TEST_PAYMENT_001', 'CREDIT_CARD', null, 1),
    ('TEST_PAYMENT_002', 'DOMESTIC_DEBIT_CARD', 'TEST_USER_001', 1);

INSERT INTO CARD_DETAILS (paymentMethodID, cardholderName, cardNumber_masked, expiryDate_MMYY, validFromDate_MMYY, issuingBank) VALUES
    ('TEST_PAYMENT_001', 'Test Cardholder', '**** **** **** 1234', '12/25', '01/23', 'Test Bank'),
    ('TEST_PAYMENT_002', 'Test User One', '**** **** **** 5678', '06/26', '03/24', 'VietinBank');

-- ========================================
-- Test Orders and Transactions
-- ========================================

INSERT INTO ORDER_ENTITY (orderID, userID, orderDate, order_status, totalProductPriceExclVAT, totalProductPriceInclVAT, calculatedDeliveryFee, totalAmountPaid) VALUES
    ('TEST_ORDER_001', 'TEST_USER_001', '2024-01-01 14:00:00', 'PENDING_PROCESSING', 100.00, 110.00, 25.00, 135.00),
    ('TEST_ORDER_002', 'TEST_USER_002', '2024-01-01 15:00:00', 'APPROVED', 150.00, 165.00, 0.00, 165.00);

INSERT INTO ORDER_ITEM (orderID, productID, quantity, priceAtTimeOfOrder, isEligibleForRushDelivery) VALUES
    ('TEST_ORDER_001', 'BOOK_001', 2, 49.50, 1),
    ('TEST_ORDER_001', 'CD_001', 1, 16.50, 1),
    ('TEST_ORDER_002', 'DVD_002', 1, 27.50, 0),
    ('TEST_ORDER_002', 'BOOK_003', 2, 66.00, 1);

INSERT INTO DELIVERY_INFO (deliveryInfoID, orderID, recipientName, email, phoneNumber, deliveryProvinceCity, deliveryAddress, deliveryInstructions, deliveryMethodChosen, requestedRushDeliveryTime) VALUES
    ('DELIVERY_001', 'TEST_ORDER_001', 'Test User One', 'testuser1@test.com', '0123456789', 'Ho Chi Minh City', '123 Test Street, District 1', 'Leave at front door', 'STANDARD', null),
    ('DELIVERY_002', 'TEST_ORDER_002', 'Test User Two', 'testuser2@test.com', '0987654321', 'Hanoi', '456 Rush Street, Hoan Kiem District', 'Rush delivery requested', 'RUSH', '2024-01-02 10:00:00');

INSERT INTO PAYMENT_TRANSACTION (transactionID, orderID, paymentMethodID, transactionType, externalTransactionID, transaction_status, transactionDateTime, amount, transactionContent) VALUES
    ('TXN_TEST_001', 'TEST_ORDER_001', 'TEST_PAYMENT_002', 'PAYMENT', 'VNP_TXN_001', 'SUCCESS', '2024-01-01 14:05:00', 135.00, 'Payment for order TEST_ORDER_001'),
    ('TXN_TEST_002', 'TEST_ORDER_002', 'TEST_PAYMENT_001', 'PAYMENT', 'VNP_TXN_002', 'SUCCESS', '2024-01-01 15:05:00', 165.00, 'Payment for order TEST_ORDER_002');

-- ========================================
-- Additional Test Data for Edge Cases
-- ========================================

-- Products for search testing (already included in products above with searchable titles)

-- High-value products for free shipping threshold testing
INSERT INTO PRODUCT (productID, title, category, value_amount, price, quantityInStock, description, imageURL, barcode, dimensions_cm, weight_kg, entryDate, productType) VALUES
    ('HIGH_VALUE_BOOK', 'Enterprise Software Architecture', 'BOOK', 120.00, 132.00, 5, 'Comprehensive guide to enterprise software design', '/images/books/enterprise_arch.jpg', '9781234567999', '25x17x5', 1.5, '2024-01-20', 'BOOK');

INSERT INTO BOOK (productID, authors, coverType, publisher, publicationDate, numPages, language, book_genre) VALUES
    ('HIGH_VALUE_BOOK', 'Enterprise Experts', 'Hardcover', 'Professional Press', '2024-01-20', 850, 'English', 'Enterprise');

-- Rush delivery eligible products (Books and CDs typically eligible)
-- Already covered in the main product lists above

-- Products with special characters for testing
INSERT INTO PRODUCT (productID, title, category, value_amount, price, quantityInStock, description, imageURL, barcode, dimensions_cm, weight_kg, entryDate, productType) VALUES
    ('SPECIAL_CHAR_BOOK', 'Français & Español: Language Guide', 'BOOK', 30.00, 33.00, 8, 'Language learning guide with special characters', '/images/books/language_guide.jpg', '9781234560000', '22x14x2', 0.7, '2024-01-21', 'BOOK');

INSERT INTO BOOK (productID, authors, coverType, publisher, publicationDate, numPages, language, book_genre) VALUES
    ('SPECIAL_CHAR_BOOK', 'María José & François Dubois', 'Paperback', 'International Press', '2024-01-21', 400, 'Multiple', 'Language');

-- COMMIT the transaction
PRAGMA foreign_keys = ON;

-- Verify data insertion
-- SELECT COUNT(*) as product_count FROM PRODUCT;
-- SELECT COUNT(*) as book_count FROM BOOK; 
-- SELECT COUNT(*) as cd_count FROM CD;
-- SELECT COUNT(*) as dvd_count FROM DVD;
-- SELECT COUNT(*) as cart_count FROM CART;
-- SELECT COUNT(*) as user_count FROM USER_ACCOUNT;
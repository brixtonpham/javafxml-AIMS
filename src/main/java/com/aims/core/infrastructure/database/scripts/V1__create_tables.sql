-- V1__create_tables.sql
-- Script để tạo các bảng cho cơ sở dữ liệu AIMS

-- Bật kiểm tra khóa ngoại (nên được thực thi bởi ứng dụng khi kết nối)
PRAGMA foreign_keys = ON;

-- Bảng PRODUCT (Lớp cha cho các sản phẩm)
CREATE TABLE PRODUCT (
    productID TEXT PRIMARY KEY,
    title TEXT NOT NULL,
    category TEXT,
    value_amount REAL, -- Đã đổi tên từ 'value' để tránh từ khóa SQL
    price REAL NOT NULL,
    quantityInStock INTEGER DEFAULT 0,
    description TEXT,
    imageURL TEXT,
    barcode TEXT UNIQUE,
    dimensions_cm TEXT,
    weight_kg REAL,
    entryDate TEXT, -- Lưu dạng TEXT 'YYYY-MM-DD'
    productType TEXT NOT NULL -- BOOK, CD, DVD, AUDIOBOOK, OTHER
);

-- Bảng BOOK (Kế thừa từ PRODUCT)
CREATE TABLE BOOK (
    productID TEXT PRIMARY KEY, -- Khóa ngoại tới PRODUCT
    authors TEXT,
    coverType TEXT,
    publisher TEXT,
    publicationDate TEXT, -- Lưu dạng TEXT 'YYYY-MM-DD'
    numPages INTEGER,
    language TEXT,
    book_genre TEXT,
    FOREIGN KEY (productID) REFERENCES PRODUCT(productID) ON DELETE CASCADE ON UPDATE CASCADE
);

-- Bảng CD (Kế thừa từ PRODUCT)
CREATE TABLE CD (
    productID TEXT PRIMARY KEY, -- Khóa ngoại tới PRODUCT
    artists TEXT,
    recordLabel TEXT,
    tracklist TEXT,
    cd_genre TEXT,
    releaseDate TEXT, -- Lưu dạng TEXT 'YYYY-MM-DD'
    FOREIGN KEY (productID) REFERENCES PRODUCT(productID) ON DELETE CASCADE ON UPDATE CASCADE
);

-- Bảng DVD (Kế thừa từ PRODUCT)
CREATE TABLE DVD (
    productID TEXT PRIMARY KEY, -- Khóa ngoại tới PRODUCT
    discType TEXT,
    director TEXT,
    runtime_minutes INTEGER,
    studio TEXT,
    dvd_language TEXT,
    subtitles TEXT,
    dvd_releaseDate TEXT, -- Lưu dạng TEXT 'YYYY-MM-DD'
    dvd_genre TEXT,
    FOREIGN KEY (productID) REFERENCES PRODUCT(productID) ON DELETE CASCADE ON UPDATE CASCADE
);

-- Bảng LP (Kế thừa từ PRODUCT)
CREATE TABLE LP (
    productID TEXT PRIMARY KEY, -- Khóa ngoại tới PRODUCT
    artists TEXT,
    recordLabel TEXT,
    tracklist TEXT,
    genre TEXT,
    releaseDate TEXT, -- Lưu dạng TEXT 'YYYY-MM-DD'
    FOREIGN KEY (productID) REFERENCES PRODUCT(productID) ON DELETE CASCADE ON UPDATE CASCADE
);

-- Bảng AUDIOBOOK (Kế thừa từ PRODUCT)
-- CREATE TABLE AUDIOBOOK (
--     productID TEXT PRIMARY KEY, -- Khóa ngoại tới PRODUCT
--     author TEXT,
--     format TEXT,
--     audiobook_language TEXT,
--     accent TEXT,
--     lengthInMinutes INTEGER,
--     FOREIGN KEY (productID) REFERENCES PRODUCT(productID) ON DELETE CASCADE ON UPDATE CASCADE
-- );

-- Bảng USER_ACCOUNT
CREATE TABLE USER_ACCOUNT (
    userID TEXT PRIMARY KEY,
    username TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    email TEXT NOT NULL UNIQUE,
    user_status TEXT NOT NULL -- ACTIVE, BLOCKED, INACTIVE
);

-- Bảng ROLE
CREATE TABLE ROLE (
    roleID TEXT PRIMARY KEY, -- e.g., "ADMIN", "PRODUCT_MANAGER"
    roleName TEXT NOT NULL UNIQUE -- e.g., "Administrator", "Product Manager"
);

-- Bảng USER_ROLE_ASSIGNMENT (Bảng nối User và Role)
CREATE TABLE USER_ROLE_ASSIGNMENT (
    userID TEXT NOT NULL,
    roleID TEXT NOT NULL,
    PRIMARY KEY (userID, roleID),
    FOREIGN KEY (userID) REFERENCES USER_ACCOUNT(userID) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (roleID) REFERENCES ROLE(roleID) ON DELETE CASCADE ON UPDATE CASCADE
);

-- Bảng CART
CREATE TABLE CART (
    cartSessionID TEXT PRIMARY KEY,
    userID TEXT, -- Cho phép NULL cho guest cart
    lastUpdated TEXT NOT NULL, -- Lưu dạng TEXT 'YYYY-MM-DD HH:MM:SS'
    FOREIGN KEY (userID) REFERENCES USER_ACCOUNT(userID) ON DELETE SET NULL ON UPDATE CASCADE
);

-- Bảng CART_ITEM
CREATE TABLE CART_ITEM (
    cartSessionID TEXT NOT NULL,
    productID TEXT NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 1,
    PRIMARY KEY (cartSessionID, productID),
    FOREIGN KEY (cartSessionID) REFERENCES CART(cartSessionID) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (productID) REFERENCES PRODUCT(productID) ON DELETE CASCADE ON UPDATE CASCADE
);

-- Bảng ORDER_ENTITY (Đã đổi tên từ ORDER để tránh từ khóa SQL)
CREATE TABLE ORDER_ENTITY (
    orderID TEXT PRIMARY KEY,
    userID TEXT, -- Cho phép NULL cho guest order
    orderDate TEXT NOT NULL, -- Lưu dạng TEXT 'YYYY-MM-DD HH:MM:SS'
    order_status TEXT NOT NULL, -- PENDING_PROCESSING, APPROVED, etc.
    totalProductPriceExclVAT REAL NOT NULL DEFAULT 0.0,
    totalProductPriceInclVAT REAL NOT NULL DEFAULT 0.0,
    calculatedDeliveryFee REAL NOT NULL DEFAULT 0.0,
    totalAmountPaid REAL NOT NULL DEFAULT 0.0,
    FOREIGN KEY (userID) REFERENCES USER_ACCOUNT(userID) ON DELETE SET NULL ON UPDATE CASCADE
);

-- Bảng ORDER_ITEM
CREATE TABLE ORDER_ITEM (
    orderID TEXT NOT NULL,
    productID TEXT NOT NULL,
    quantity INTEGER NOT NULL,
    priceAtTimeOfOrder REAL NOT NULL,
    isEligibleForRushDelivery INTEGER NOT NULL DEFAULT 0, -- 0 for false, 1 for true
    PRIMARY KEY (orderID, productID),
    FOREIGN KEY (orderID) REFERENCES ORDER_ENTITY(orderID) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (productID) REFERENCES PRODUCT(productID) ON DELETE RESTRICT ON UPDATE CASCADE -- RESTRICT để tránh xóa sản phẩm nếu đã có trong đơn hàng
);

-- Bảng DELIVERY_INFO
CREATE TABLE DELIVERY_INFO (
    deliveryInfoID TEXT PRIMARY KEY,
    orderID TEXT NOT NULL UNIQUE, -- Mỗi đơn hàng chỉ có một thông tin giao hàng
    recipientName TEXT NOT NULL,
    email TEXT,
    phoneNumber TEXT NOT NULL,
    deliveryProvinceCity TEXT NOT NULL,
    deliveryAddress TEXT NOT NULL,
    deliveryInstructions TEXT,
    deliveryMethodChosen TEXT, -- e.g., "STANDARD", "RUSH"
    requestedRushDeliveryTime TEXT, -- Lưu dạng TEXT 'YYYY-MM-DD HH:MM:SS'
    FOREIGN KEY (orderID) REFERENCES ORDER_ENTITY(orderID) ON DELETE CASCADE ON UPDATE CASCADE
);

-- Bảng INVOICE
CREATE TABLE INVOICE (
    invoiceID TEXT PRIMARY KEY,
    orderID TEXT NOT NULL UNIQUE, -- Mỗi đơn hàng chỉ có một hóa đơn chính
    invoiceDate TEXT NOT NULL, -- Lưu dạng TEXT 'YYYY-MM-DD HH:MM:SS'
    invoicedTotalAmount REAL NOT NULL,
    FOREIGN KEY (orderID) REFERENCES ORDER_ENTITY(orderID) ON DELETE CASCADE ON UPDATE CASCADE
);

-- Bảng PAYMENT_METHOD
CREATE TABLE PAYMENT_METHOD (
    paymentMethodID TEXT PRIMARY KEY,
    methodType TEXT NOT NULL, -- CREDIT_CARD, DOMESTIC_DEBIT_CARD, etc.
    userID TEXT, -- Cho phương thức đã lưu của người dùng
    isDefault INTEGER NOT NULL DEFAULT 0, -- 0 for false, 1 for true
    FOREIGN KEY (userID) REFERENCES USER_ACCOUNT(userID) ON DELETE CASCADE ON UPDATE CASCADE
);

-- Bảng CARD_DETAILS
CREATE TABLE CARD_DETAILS (
    paymentMethodID TEXT PRIMARY KEY, -- Cũng là khóa ngoại tới PAYMENT_METHOD
    cardholderName TEXT NOT NULL,
    cardNumber_masked TEXT NOT NULL,
    expiryDate_MMYY TEXT NOT NULL, -- e.g., "12/28"
    validFromDate_MMYY TEXT, -- e.g., "12/23"
    issuingBank TEXT,
    FOREIGN KEY (paymentMethodID) REFERENCES PAYMENT_METHOD(paymentMethodID) ON DELETE CASCADE ON UPDATE CASCADE
);

-- Bảng PAYMENT_TRANSACTION
CREATE TABLE PAYMENT_TRANSACTION (
    transactionID TEXT PRIMARY KEY,
    orderID TEXT NOT NULL,
    paymentMethodID TEXT,
    transactionType TEXT NOT NULL, -- PAYMENT, REFUND
    externalTransactionID TEXT,
    transaction_status TEXT NOT NULL, -- SUCCESS, FAILED, PENDING
    transactionDateTime TEXT NOT NULL, -- Lưu dạng TEXT 'YYYY-MM-DD HH:MM:SS'
    amount REAL NOT NULL,
    transactionContent TEXT,
    FOREIGN KEY (orderID) REFERENCES ORDER_ENTITY(orderID) ON DELETE RESTRICT ON UPDATE CASCADE,
    FOREIGN KEY (paymentMethodID) REFERENCES PAYMENT_METHOD(paymentMethodID) ON DELETE SET NULL ON UPDATE CASCADE
);

-- Bảng PRODUCT_MANAGER_AUDIT_LOG (Theo dõi hoạt động của Product Manager)
CREATE TABLE PRODUCT_MANAGER_AUDIT_LOG (
    auditLogID TEXT PRIMARY KEY,
    managerId TEXT NOT NULL,
    operationType TEXT NOT NULL, -- ADD, UPDATE, DELETE, PRICE_UPDATE
    productId TEXT,
    operationDateTime TEXT NOT NULL, -- Lưu dạng TEXT 'YYYY-MM-DD HH:MM:SS'
    details TEXT,
    FOREIGN KEY (managerId) REFERENCES USER_ACCOUNT(userID) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (productId) REFERENCES PRODUCT(productID) ON DELETE SET NULL ON UPDATE CASCADE
);
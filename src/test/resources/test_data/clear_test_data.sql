-- clear_test_data.sql
-- Script để xóa tất cả dữ liệu test khỏi aims_test.db
-- Giữ lại cấu trúc bảng nhưng xóa hết dữ liệu

-- Tắt kiểm tra khóa ngoại tạm thời để xóa dữ liệu
PRAGMA foreign_keys = OFF;

-- Xóa dữ liệu từ các bảng con trước
DELETE FROM CARD_DETAILS;
DELETE FROM PAYMENT_TRANSACTION;
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

-- Reset auto-increment sequences (nếu có)
DELETE FROM sqlite_sequence;

-- Bật lại kiểm tra khóa ngoại
PRAGMA foreign_keys = ON;

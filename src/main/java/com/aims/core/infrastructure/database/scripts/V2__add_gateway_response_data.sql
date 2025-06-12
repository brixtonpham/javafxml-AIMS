-- V2__add_gateway_response_data.sql
-- Migration script to add gatewayResponseData column to PAYMENT_TRANSACTION table

-- Add gatewayResponseData column to store payment gateway response (e.g., VNPAY payment URL)
ALTER TABLE PAYMENT_TRANSACTION ADD COLUMN gatewayResponseData TEXT;

-- Create index for faster queries on gateway response data
CREATE INDEX idx_payment_transaction_gateway_data ON PAYMENT_TRANSACTION(gatewayResponseData);

-- Update existing records to have NULL gatewayResponseData (this is automatic with ALTER TABLE ADD COLUMN)
-- No additional updates needed as new column defaults to NULL
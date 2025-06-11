-- AIMS Database Audit Table Repair Script
-- Purpose: Create missing PRODUCT_MANAGER_AUDIT_LOG table

PRAGMA foreign_keys = ON;

-- Create the missing audit log table
CREATE TABLE IF NOT EXISTS PRODUCT_MANAGER_AUDIT_LOG (
    auditLogID TEXT PRIMARY KEY,
    managerId TEXT NOT NULL,
    operationType TEXT NOT NULL, -- ADD, UPDATE, DELETE, PRICE_UPDATE
    productId TEXT,
    operationDateTime TEXT NOT NULL, -- Store as TEXT 'YYYY-MM-DD HH:MM:SS'
    details TEXT,
    FOREIGN KEY (managerId) REFERENCES USER_ACCOUNT(userID) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (productId) REFERENCES PRODUCT(productID) ON DELETE SET NULL ON UPDATE CASCADE
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_audit_manager_date ON PRODUCT_MANAGER_AUDIT_LOG(managerId, operationDateTime);
CREATE INDEX IF NOT EXISTS idx_audit_product ON PRODUCT_MANAGER_AUDIT_LOG(productId);
CREATE INDEX IF NOT EXISTS idx_audit_operation_type ON PRODUCT_MANAGER_AUDIT_LOG(operationType);

-- Verify table was created successfully
SELECT 'PRODUCT_MANAGER_AUDIT_LOG table created successfully' as status 
WHERE EXISTS (SELECT name FROM sqlite_master WHERE type='table' AND name='PRODUCT_MANAGER_AUDIT_LOG');
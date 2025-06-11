# AIMS: Administrator Journey Flow - System Administration

## Overview

This document outlines the complete administrator journey within the AIMS application, covering user management, product management, and system administration functions available to users with the Administrator role.

## Administrator Access Architecture

- **Authentication Required**: Administrators must login with valid credentials
- **Role Validation**: [`UserRole.ADMIN`](src/main/java/com/aims/core/enums/UserRole.java) role verification
- **Privileged Access**: Full system administration capabilities
- **Audit Trail**: All administrative actions are logged and tracked

## Administrator Journey Flow

```mermaid
flowchart TD
    %% Application Entry and Authentication
    START([Administrator Access]) --> LOGIN[Login Screen<br/>Username & Password Entry]
    
    LOGIN --> |Valid Admin Credentials| AUTH_SUCCESS[Authentication Success<br/>Role Verification]
    LOGIN --> |Invalid Credentials| AUTH_FAILED[Authentication Failed<br/>Invalid Login Error]
    LOGIN --> |Non-Admin Role| ACCESS_DENIED[Access Denied<br/>Insufficient Privileges]
    
    AUTH_FAILED --> LOGIN
    ACCESS_DENIED --> LOGIN
    
    %% Main Dashboard
    AUTH_SUCCESS --> ADMIN_DASHBOARD[Administrator Dashboard<br/>System Overview<br/>Quick Actions Menu]
    
    %% Primary Administrative Functions
    ADMIN_DASHBOARD --> |User Management| USER_MANAGEMENT[User Management Screen<br/>User List & Operations<br/>Role Assignments]
    ADMIN_DASHBOARD --> |Product Management| PRODUCT_MANAGEMENT[Product Management Screen<br/>Product Catalog Administration<br/>Inventory Oversight]
    ADMIN_DASHBOARD --> |Change Password| CHANGE_PASSWORD[Change Password Screen<br/>Personal Account Security]
    ADMIN_DASHBOARD --> |System Logout| LOGOUT_CONFIRM[Logout Confirmation<br/>End Administrative Session]
    
    %% User Management Operations
    USER_MANAGEMENT --> |Add New User| ADD_USER_FORM[Add User Form<br/>Account Creation<br/>Role Assignment Interface]
    USER_MANAGEMENT --> |Edit Existing User| EDIT_USER_FORM[Edit User Form<br/>Account Modification<br/>Role Management]
    USER_MANAGEMENT --> |Delete User| DELETE_USER_SELECT[Select User for Deletion<br/>User Selection Interface]
    USER_MANAGEMENT --> |View User Details| USER_DETAILS[User Details View<br/>Account Information<br/>Role History]
    USER_MANAGEMENT --> |Reset User Password| PASSWORD_RESET[Password Reset Function<br/>Generate New Password<br/>Email Notification]
    USER_MANAGEMENT --> |Block/Unblock User| USER_STATUS_TOGGLE[User Status Management<br/>Account Enable/Disable]
    USER_MANAGEMENT --> |Back to Dashboard| ADMIN_DASHBOARD
    
    %% Add User Process
    ADD_USER_FORM --> |Enter User Details| USER_DATA_ENTRY[User Data Entry<br/>Username, Email, Contact<br/>Role Selection]
    USER_DATA_ENTRY --> |Assign Roles| ROLE_ASSIGNMENT[Role Assignment Interface<br/>Multiple Role Selection<br/>Admin/Product Manager Options]
    ROLE_ASSIGNMENT --> |Submit User Creation| USER_VALIDATION{User Data<br/>Validation Check}
    
    USER_VALIDATION --> |Valid Data| USER_CREATE_SUCCESS[User Created Successfully<br/>Account Generated<br/>Email Notification Sent]
    USER_VALIDATION --> |Invalid Data| USER_VALIDATION_ERROR[Validation Error Display<br/>Required Fields Missing<br/>Format Violations]
    USER_VALIDATION --> |Duplicate Username/Email| USER_DUPLICATE_ERROR[Duplicate User Error<br/>Username/Email Already Exists]
    
    USER_CREATE_SUCCESS --> USER_MANAGEMENT
    USER_VALIDATION_ERROR --> ADD_USER_FORM
    USER_DUPLICATE_ERROR --> ADD_USER_FORM
    
    %% Edit User Process
    EDIT_USER_FORM --> |Modify User Details| USER_UPDATE_ENTRY[User Update Interface<br/>Editable Fields<br/>Current Role Display]
    USER_UPDATE_ENTRY --> |Update Roles| ROLE_UPDATE[Role Update Interface<br/>Add/Remove Roles<br/>Permission Changes]
    ROLE_UPDATE --> |Submit Changes| USER_UPDATE_VALIDATION{Update Data<br/>Validation}
    
    USER_UPDATE_VALIDATION --> |Valid Updates| USER_UPDATE_SUCCESS[User Updated Successfully<br/>Changes Applied<br/>Notification Sent]
    USER_UPDATE_VALIDATION --> |Invalid Data| USER_UPDATE_ERROR[Update Validation Error<br/>Constraint Violations]
    USER_UPDATE_VALIDATION --> |No Changes| USER_NO_CHANGES[No Changes Detected<br/>Return to Management]
    
    USER_UPDATE_SUCCESS --> USER_MANAGEMENT
    USER_UPDATE_ERROR --> EDIT_USER_FORM
    USER_NO_CHANGES --> USER_MANAGEMENT
    
    %% Delete User Process
    DELETE_USER_SELECT --> |Select User| DELETE_USER_CONFIRM[Delete User Confirmation<br/>Impact Assessment<br/>Data Dependency Check]
    DELETE_USER_CONFIRM --> |Confirm Deletion| USER_DELETE_PROCESS[User Deletion Process<br/>Data Cleanup<br/>Audit Log Entry]
    DELETE_USER_CONFIRM --> |Cancel Deletion| USER_MANAGEMENT
    
    USER_DELETE_PROCESS --> |Deletion Success| USER_DELETE_SUCCESS[User Deleted Successfully<br/>Account Removed<br/>System Cleanup Complete]
    USER_DELETE_PROCESS --> |Deletion Failed| USER_DELETE_ERROR[Deletion Failed<br/>Dependency Conflicts<br/>System Constraints]
    
    USER_DELETE_SUCCESS --> USER_MANAGEMENT
    USER_DELETE_ERROR --> DELETE_USER_CONFIRM
    
    %% Password Reset Process
    PASSWORD_RESET --> |Generate New Password| PASSWORD_GENERATION[Secure Password Generation<br/>Complexity Requirements<br/>Encryption Processing]
    PASSWORD_GENERATION --> |Send Notification| PASSWORD_RESET_NOTIFICATION[Password Reset Notification<br/>Email Delivery<br/>Temporary Password Provision]
    PASSWORD_RESET_NOTIFICATION --> |Reset Success| PASSWORD_RESET_SUCCESS[Password Reset Complete<br/>User Notified<br/>Forced Change Required]
    PASSWORD_RESET_NOTIFICATION --> |Notification Failed| PASSWORD_RESET_FAILED[Notification Delivery Failed<br/>Email Service Error]
    
    PASSWORD_RESET_SUCCESS --> USER_MANAGEMENT
    PASSWORD_RESET_FAILED --> PASSWORD_RESET
    
    %% User Status Management
    USER_STATUS_TOGGLE --> |Block User Account| USER_BLOCK[Block User Account<br/>Disable Access<br/>Session Termination]
    USER_STATUS_TOGGLE --> |Unblock User Account| USER_UNBLOCK[Unblock User Account<br/>Restore Access<br/>Permission Reinstatement]
    
    USER_BLOCK --> |Block Success| USER_BLOCK_SUCCESS[User Blocked Successfully<br/>Access Revoked<br/>Notification Sent]
    USER_UNBLOCK --> |Unblock Success| USER_UNBLOCK_SUCCESS[User Unblocked Successfully<br/>Access Restored<br/>Notification Sent]
    
    USER_BLOCK_SUCCESS --> USER_MANAGEMENT
    USER_UNBLOCK_SUCCESS --> USER_MANAGEMENT
    
    %% Product Management Operations
    PRODUCT_MANAGEMENT --> |Add New Product| ADD_PRODUCT_FORM[Add Product Form<br/>Product Information Entry<br/>Media Type Selection]
    PRODUCT_MANAGEMENT --> |Edit Existing Product| EDIT_PRODUCT_FORM[Edit Product Form<br/>Product Modification<br/>Price Updates]
    PRODUCT_MANAGEMENT --> |Delete Product| DELETE_PRODUCT_SELECT[Select Product for Deletion<br/>Product Selection Interface<br/>Batch Operations Available]
    PRODUCT_MANAGEMENT --> |View Product Details| PRODUCT_DETAILS[Product Details View<br/>Complete Information<br/>Sales History]
    PRODUCT_MANAGEMENT --> |Search Products| PRODUCT_SEARCH[Product Search Interface<br/>Advanced Filtering<br/>Category Selection]
    PRODUCT_MANAGEMENT --> |Back to Dashboard| ADMIN_DASHBOARD
    
    %% Add Product Process
    ADD_PRODUCT_FORM --> |Enter Product Info| PRODUCT_DATA_ENTRY[Product Data Entry<br/>Title, Category, Price<br/>Media-Specific Details]
    PRODUCT_DATA_ENTRY --> |Set Product Type| PRODUCT_TYPE_SELECTION[Product Type Selection<br/>Book/CD/DVD/LP<br/>Type-Specific Fields]
    PRODUCT_TYPE_SELECTION --> |Enter Physical Details| PHYSICAL_PRODUCT_ENTRY[Physical Product Details<br/>Barcode, Dimensions, Weight<br/>Warehouse Information]
    PHYSICAL_PRODUCT_ENTRY --> |Submit Product| PRODUCT_VALIDATION{Product Data<br/>Validation}
    
    PRODUCT_VALIDATION --> |Valid Product Data| PRODUCT_CREATE_SUCCESS[Product Created Successfully<br/>Catalog Updated<br/>Inventory Initialized]
    PRODUCT_VALIDATION --> |Invalid Data| PRODUCT_VALIDATION_ERROR[Product Validation Error<br/>Required Fields Missing<br/>Format Violations]
    PRODUCT_VALIDATION --> |Price Constraint Violation| PRODUCT_PRICE_ERROR[Price Constraint Error<br/>Must be 30%-150% of Value<br/>Business Rule Violation]
    
    PRODUCT_CREATE_SUCCESS --> PRODUCT_MANAGEMENT
    PRODUCT_VALIDATION_ERROR --> ADD_PRODUCT_FORM
    PRODUCT_PRICE_ERROR --> ADD_PRODUCT_FORM
    
    %% Edit Product Process
    EDIT_PRODUCT_FORM --> |Modify Product Info| PRODUCT_UPDATE_ENTRY[Product Update Interface<br/>Editable Fields<br/>Current Values Display]
    PRODUCT_UPDATE_ENTRY --> |Update Price| PRICE_UPDATE_CHECK{Price Update<br/>Validation<br/>Daily Limit Check}
    PRICE_UPDATE_CHECK --> |Valid Price Update| PRODUCT_UPDATE_VALIDATION{Product Update<br/>Validation}
    PRICE_UPDATE_CHECK --> |Price Limit Exceeded| PRICE_LIMIT_ERROR[Price Update Limit Error<br/>Maximum 2 Updates/Day<br/>Business Rule Constraint]
    PRICE_UPDATE_CHECK --> |Price Range Violation| PRICE_RANGE_ERROR[Price Range Error<br/>30%-150% of Product Value<br/>Constraint Violation]
    
    PRODUCT_UPDATE_VALIDATION --> |Valid Updates| PRODUCT_UPDATE_SUCCESS[Product Updated Successfully<br/>Changes Applied<br/>Catalog Refreshed]
    PRODUCT_UPDATE_VALIDATION --> |Invalid Data| PRODUCT_UPDATE_ERROR[Update Validation Error<br/>Data Constraints Violated]
    
    PRODUCT_UPDATE_SUCCESS --> PRODUCT_MANAGEMENT
    PRODUCT_UPDATE_ERROR --> EDIT_PRODUCT_FORM
    PRICE_LIMIT_ERROR --> EDIT_PRODUCT_FORM
    PRICE_RANGE_ERROR --> EDIT_PRODUCT_FORM
    
    %% Delete Product Process
    DELETE_PRODUCT_SELECT --> |Select Products| DELETE_PRODUCT_CONFIRM[Delete Product Confirmation<br/>Impact Assessment<br/>Order Dependency Check<br/>Batch Deletion (Max 10)]
    DELETE_PRODUCT_CONFIRM --> |Confirm Deletion| PRODUCT_DELETE_PROCESS[Product Deletion Process<br/>Inventory Cleanup<br/>Order Impact Assessment]
    DELETE_PRODUCT_CONFIRM --> |Cancel Deletion| PRODUCT_MANAGEMENT
    
    PRODUCT_DELETE_PROCESS --> |Check Daily Limit| DELETE_LIMIT_CHECK{Daily Deletion<br/>Limit Check<br/>Max 30 Products/Day}
    DELETE_LIMIT_CHECK --> |Within Limit| PRODUCT_DELETE_SUCCESS[Products Deleted Successfully<br/>Catalog Updated<br/>Inventory Adjusted]
    DELETE_LIMIT_CHECK --> |Limit Exceeded| DELETE_LIMIT_ERROR[Daily Deletion Limit Exceeded<br/>Maximum 30 Products/Day<br/>Security Constraint]
    
    PRODUCT_DELETE_SUCCESS --> PRODUCT_MANAGEMENT
    DELETE_LIMIT_ERROR --> DELETE_PRODUCT_CONFIRM
    
    %% Password Change Process
    CHANGE_PASSWORD --> |Enter Current Password| CURRENT_PASSWORD_VERIFY[Current Password Verification<br/>Authentication Check]
    CURRENT_PASSWORD_VERIFY --> |Valid Current Password| NEW_PASSWORD_ENTRY[New Password Entry<br/>Complexity Requirements<br/>Confirmation Field]
    CURRENT_PASSWORD_VERIFY --> |Invalid Current Password| CURRENT_PASSWORD_ERROR[Current Password Error<br/>Authentication Failed]
    
    NEW_PASSWORD_ENTRY --> |Submit Password Change| PASSWORD_VALIDATION{Password<br/>Validation<br/>Complexity Check}
    PASSWORD_VALIDATION --> |Valid New Password| PASSWORD_CHANGE_SUCCESS[Password Changed Successfully<br/>Security Updated<br/>Session Refreshed]
    PASSWORD_VALIDATION --> |Invalid Password| PASSWORD_COMPLEXITY_ERROR[Password Complexity Error<br/>Requirements Not Met]
    PASSWORD_VALIDATION --> |Password Match Error| PASSWORD_CONFIRM_ERROR[Password Confirmation Error<br/>Passwords Do Not Match]
    
    PASSWORD_CHANGE_SUCCESS --> ADMIN_DASHBOARD
    CURRENT_PASSWORD_ERROR --> CHANGE_PASSWORD
    PASSWORD_COMPLEXITY_ERROR --> CHANGE_PASSWORD
    PASSWORD_CONFIRM_ERROR --> CHANGE_PASSWORD
    
    %% Logout Process
    LOGOUT_CONFIRM --> |Confirm Logout| LOGOUT_PROCESS[Logout Process<br/>Session Termination<br/>Security Cleanup]
    LOGOUT_CONFIRM --> |Cancel Logout| ADMIN_DASHBOARD
    
    LOGOUT_PROCESS --> |Logout Complete| SESSION_END[Session Ended<br/>Return to Home Screen<br/>Guest Mode Active]
    SESSION_END --> START
    
    %% Error Handling (Global)
    ADMIN_DASHBOARD -.-> SYSTEM_ERROR[System Error Dialog<br/>Database Connection Issues<br/>Service Unavailable]
    USER_MANAGEMENT -.-> SYSTEM_ERROR
    PRODUCT_MANAGEMENT -.-> SYSTEM_ERROR
    SYSTEM_ERROR -.-> |Retry| ADMIN_DASHBOARD
    SYSTEM_ERROR -.-> |Logout| SESSION_END
    
    %% Success Notifications (Global)
    USER_CREATE_SUCCESS -.-> SUCCESS_NOTIFICATION[Success Notification<br/>Operation Completed<br/>Auto-dismiss Message]
    PRODUCT_CREATE_SUCCESS -.-> SUCCESS_NOTIFICATION
    PASSWORD_CHANGE_SUCCESS -.-> SUCCESS_NOTIFICATION
    SUCCESS_NOTIFICATION -.-> ADMIN_DASHBOARD
    
    %% Audit Logging (Background Process)
    USER_CREATE_SUCCESS -.-> AUDIT_LOG[Audit Log Entry<br/>User Creation Recorded<br/>Timestamp & Admin ID]
    PRODUCT_CREATE_SUCCESS -.-> AUDIT_LOG
    USER_DELETE_SUCCESS -.-> AUDIT_LOG
    PRODUCT_DELETE_SUCCESS -.-> AUDIT_LOG
    AUDIT_LOG -.-> DATABASE[Audit Database<br/>Historical Records<br/>Compliance Tracking]
    
    %% Styling
    classDef adminFlow fill:#f3e5f5,stroke:#7b1fa2,stroke-width:3px
    classDef userMgmt fill:#e8eaf6,stroke:#3f51b5,stroke-width:2px
    classDef productMgmt fill:#e0f2f1,stroke:#00695c,stroke-width:2px
    classDef decisionPoint fill:#fff3e0,stroke:#f57c00,stroke-width:2px
    classDef errorState fill:#ffebee,stroke:#d32f2f,stroke-width:2px
    classDef successState fill:#e8f5e8,stroke:#388e3c,stroke-width:2px
    classDef securityFlow fill:#fce4ec,stroke:#ad1457,stroke-width:2px
    classDef auditFlow fill:#f1f8e9,stroke:#689f38,stroke-width:1px,stroke-dasharray: 3 3
    
    %% Apply Styles
    class LOGIN,ADMIN_DASHBOARD,CHANGE_PASSWORD,LOGOUT_CONFIRM adminFlow
    class USER_MANAGEMENT,ADD_USER_FORM,EDIT_USER_FORM,USER_DETAILS,PASSWORD_RESET,USER_STATUS_TOGGLE userMgmt
    class PRODUCT_MANAGEMENT,ADD_PRODUCT_FORM,EDIT_PRODUCT_FORM,PRODUCT_DETAILS,PRODUCT_SEARCH productMgmt
    class AUTH_SUCCESS,USER_VALIDATION,PRODUCT_VALIDATION,DELETE_LIMIT_CHECK,PASSWORD_VALIDATION decisionPoint
    class AUTH_FAILED,ACCESS_DENIED,USER_VALIDATION_ERROR,PRODUCT_VALIDATION_ERROR,SYSTEM_ERROR,DELETE_LIMIT_ERROR errorState
    class USER_CREATE_SUCCESS,PRODUCT_CREATE_SUCCESS,PASSWORD_CHANGE_SUCCESS,SUCCESS_NOTIFICATION successState
    class CURRENT_PASSWORD_VERIFY,PASSWORD_GENERATION,SESSION_END securityFlow
    class AUDIT_LOG,DATABASE auditFlow
```

## Key Administrator Functions

### **User Management Capabilities**
1. **User Account Creation**: Create new user accounts with role assignments
2. **User Information Updates**: Modify existing user details and role permissions
3. **User Account Deletion**: Remove user accounts with dependency checking
4. **Password Management**: Reset user passwords with secure generation
5. **Account Status Control**: Block/unblock user access as needed
6. **Role Administration**: Assign multiple roles to users (Admin, Product Manager)

### **Product Management Operations**
1. **Product Catalog Management**: Add, edit, delete products with full control
2. **Inventory Oversight**: Monitor stock levels and product availability
3. **Price Management**: Update product prices within business rule constraints
4. **Batch Operations**: Delete up to 10 products at once (30 per day limit)
5. **Product Search**: Advanced filtering and search capabilities
6. **Media Type Support**: Handle Books, CDs, DVDs, LP Records with type-specific fields

### **System Administration**
1. **Security Management**: Password policies and account security
2. **Audit Trail**: Comprehensive logging of all administrative actions
3. **Session Management**: Secure login/logout with session control
4. **Error Handling**: Robust error recovery and user feedback
5. **Email Notifications**: Automated notifications for user account changes

## Business Rules for Administrators

### **User Management Constraints**
- **Role Assignments**: Multiple roles can be assigned to single users
- **Email Notifications**: Automatic notifications sent for account changes
- **Password Security**: Complex password requirements enforced
- **Account Dependencies**: User deletion checks for order dependencies
- **Access Control**: Immediate session termination for blocked users

### **Product Management Limits**
- **Daily Limits**: Maximum 30 product deletions per day for security
- **Batch Operations**: Up to 10 products can be deleted simultaneously
- **Price Constraints**: Product prices must be 30%-150% of product value
- **Price Updates**: Maximum 2 price updates per product per day
- **Unlimited Additions**: No limit on new product additions

### **Security and Audit Requirements**
- **Audit Logging**: All administrative actions logged with timestamps
- **Session Security**: Secure session management with proper cleanup
- **Password Policies**: Strong password requirements for all accounts
- **Role Validation**: Continuous role verification throughout session
- **Error Recovery**: Graceful error handling with user feedback

## Administrative Workflows

### **New User Creation Workflow**
1. **Form Entry**: Complete user information with required fields
2. **Role Selection**: Assign appropriate roles (Admin/Product Manager)
3. **Validation**: Comprehensive data validation and duplicate checking
4. **Account Generation**: Secure account creation with password generation
5. **Notification**: Email notification sent to new user
6. **Audit Log**: Creation action logged for compliance

### **Product Management Workflow**
1. **Product Information**: Enter complete product details by type
2. **Physical Attributes**: Set dimensions, weight, barcode, warehouse details
3. **Pricing**: Set initial price within value constraints
4. **Validation**: Business rule validation and constraint checking
5. **Catalog Update**: Product added to searchable catalog
6. **Inventory**: Initial stock quantity set and tracked

### **Security Management Workflow**
1. **Authentication**: Secure login with role verification
2. **Session Management**: Active session monitoring and timeout handling
3. **Password Security**: Regular password updates with complexity requirements
4. **Access Control**: Role-based function access enforcement
5. **Audit Compliance**: Comprehensive action logging and reporting
6. **Secure Logout**: Proper session cleanup and security measures

---

*This administrator journey documentation covers all administrative functions available in the AIMS application, including comprehensive user management, product catalog administration, and system security oversight.*
package com.aims.core.shared.constants;

public final class FXMLPaths {

    private FXMLPaths() {} // Private constructor to prevent instantiation

    private static final String VIEWS_BASE = "/com/aims/presentation/views/";
    private static final String PARTIALS_BASE = VIEWS_BASE + "partials/";
    private static final String DIALOGS_BASE = VIEWS_BASE + "dialogs/";

    // Main Layout
    public static final String MAIN_LAYOUT = VIEWS_BASE + "main_layout.fxml";

    // Core Customer Screens
    public static final String HOME_SCREEN = VIEWS_BASE + "home_screen.fxml";
    public static final String PRODUCT_SEARCH_RESULTS_SCREEN = VIEWS_BASE + "product_search_results_screen.fxml";
    public static final String PRODUCT_DETAIL_SCREEN = VIEWS_BASE + "product_detail_screen.fxml";
    public static final String CART_SCREEN = VIEWS_BASE + "cart_screen.fxml";
    public static final String DELIVERY_INFO_SCREEN = VIEWS_BASE + "delivery_info_screen.fxml";
    public static final String ORDER_SUMMARY_SCREEN = VIEWS_BASE + "order_summary_screen.fxml";
    public static final String PAYMENT_METHOD_SCREEN = VIEWS_BASE + "payment_method_screen.fxml";
    public static final String PAYMENT_PROCESSING_SCREEN = VIEWS_BASE + "payment_processing_screen.fxml";
    public static final String PAYMENT_RESULT_SCREEN = VIEWS_BASE + "payment_result_screen.fxml";
    public static final String CUSTOMER_ORDER_DETAIL_SCREEN = VIEWS_BASE + "customer_order_detail_screen.fxml";

    // Admin & Product Manager Screens
    public static final String LOGIN_SCREEN = VIEWS_BASE + "login_screen.fxml";
    public static final String ADMIN_DASHBOARD_SCREEN = VIEWS_BASE + "admin_dashboard_screen.fxml";
    public static final String PM_DASHBOARD_SCREEN = VIEWS_BASE + "pm_dashboard_screen.fxml";
    public static final String ADMIN_PRODUCT_LIST_SCREEN = VIEWS_BASE + "admin_product_list_screen.fxml"; // PM might reuse this
    public static final String ADMIN_ADD_EDIT_PRODUCT_SCREEN = VIEWS_BASE + "admin_add_edit_product_screen.fxml"; // PM might reuse this
    public static final String PM_PENDING_ORDERS_LIST_SCREEN = VIEWS_BASE + "pm_pending_orders_list_screen.fxml";
    public static final String PM_ORDER_REVIEW_SCREEN = VIEWS_BASE + "pm_order_review_screen.fxml";
    public static final String ADMIN_USER_MANAGEMENT_SCREEN = VIEWS_BASE + "admin_user_management_screen.fxml";
    public static final String ADMIN_ADD_USER_FORM = VIEWS_BASE + "admin_add_user_form.fxml";
    public static final String ADMIN_EDIT_USER_FORM = VIEWS_BASE + "admin_edit_user_form.fxml";
    public static final String CHANGE_PASSWORD_SCREEN = VIEWS_BASE + "change_password_screen.fxml";


    // Partial Components
    public static final String PRODUCT_CARD = PARTIALS_BASE + "product_card.fxml";
    public static final String CART_ITEM_ROW = PARTIALS_BASE + "cart_item_row_card_style.fxml"; // If using card style for cart items
    public static final String ORDER_ITEM_ROW = PARTIALS_BASE + "order_item_row.fxml";

    // Dialogs
    public static final String CONFIRMATION_DIALOG = DIALOGS_BASE + "confirmation_dialog.fxml";
    public static final String ERROR_DIALOG = DIALOGS_BASE + "error_dialog.fxml";
    public static final String INFO_DIALOG = DIALOGS_BASE + "info_dialog.fxml";
    public static final String STOCK_INSUFFICIENT_DIALOG = DIALOGS_BASE + "stock_insufficient_dialog.fxml";
    public static final String RUSH_ORDER_OPTIONS_DIALOG = DIALOGS_BASE + "rush_order_options_dialog.fxml";
}
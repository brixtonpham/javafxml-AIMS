package com.aims.core.infrastructure.config;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Universal Payment Gateway Configuration Manager
 * 
 * This class replaces the VNPay-specific configuration with a flexible
 * system that can support multiple payment gateways simultaneously.
 * 
 * Key features:
 * - Multi-gateway configuration support
 * - Environment-specific settings
 * - Graceful fallback handling
 * - Configuration validation
 */
public class PaymentGatewayConfig {
    
    // Supported gateway types
    public static final String VNPAY = "VNPAY";
    public static final String STRIPE = "STRIPE";
    public static final String PAYPAL = "PAYPAL";
    public static final String MOMO = "MOMO";
    
    // Current active gateway
    private static String activeGateway;
    
    // Gateway configurations
    private static final Map<String, Map<String, String>> gatewayConfigs = new HashMap<>();
    
    // Default configuration file
    private static final String CONFIG_FILE = "payment_gateway_config.properties";
    
    static {
        loadConfiguration();
    }
    
    /**
     * Load configuration from properties file
     */
    private static void loadConfiguration() {
        try (InputStream input = PaymentGatewayConfig.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            Properties prop = new Properties();
            
            if (input == null) {
                System.err.println("WARNING: Unable to find " + CONFIG_FILE + ". Using fallback configuration.");
                loadFallbackConfiguration();
                return;
            }
            
            prop.load(input);
            
            // Load active gateway
            activeGateway = prop.getProperty("payment.gateway.active", VNPAY);
            
            // Load configurations for each gateway
            loadVNPayConfiguration(prop);
            loadStripeConfiguration(prop);
            loadPayPalConfiguration(prop);
            loadMoMoConfiguration(prop);
            
            validateConfiguration();
            
        } catch (Exception ex) {
            System.err.println("CRITICAL: Error loading payment gateway configuration: " + ex.getMessage());
            ex.printStackTrace();
            loadFallbackConfiguration();
        }
    }
    
    /**
     * Load VNPay specific configuration
     */
    private static void loadVNPayConfiguration(Properties prop) {
        Map<String, String> vnpayConfig = new HashMap<>();
        vnpayConfig.put("merchant_code", prop.getProperty("vnpay.merchant.code"));
        vnpayConfig.put("hash_secret", prop.getProperty("vnpay.hash.secret"));
        vnpayConfig.put("payment_url", prop.getProperty("vnpay.payment.url"));
        vnpayConfig.put("api_url", prop.getProperty("vnpay.api.url"));
        vnpayConfig.put("version", prop.getProperty("vnpay.version", "2.1.0"));
        vnpayConfig.put("return_url", prop.getProperty("vnpay.return.url"));
        vnpayConfig.put("ipn_url", prop.getProperty("vnpay.ipn.url"));
        
        gatewayConfigs.put(VNPAY, vnpayConfig);
    }
    
    /**
     * Load Stripe specific configuration
     */
    private static void loadStripeConfiguration(Properties prop) {
        Map<String, String> stripeConfig = new HashMap<>();
        stripeConfig.put("public_key", prop.getProperty("stripe.public.key"));
        stripeConfig.put("secret_key", prop.getProperty("stripe.secret.key"));
        stripeConfig.put("webhook_secret", prop.getProperty("stripe.webhook.secret"));
        stripeConfig.put("api_url", prop.getProperty("stripe.api.url", "https://api.stripe.com"));
        stripeConfig.put("return_url", prop.getProperty("stripe.return.url"));
        
        gatewayConfigs.put(STRIPE, stripeConfig);
    }
    
    /**
     * Load PayPal specific configuration
     */
    private static void loadPayPalConfiguration(Properties prop) {
        Map<String, String> paypalConfig = new HashMap<>();
        paypalConfig.put("client_id", prop.getProperty("paypal.client.id"));
        paypalConfig.put("client_secret", prop.getProperty("paypal.client.secret"));
        paypalConfig.put("environment", prop.getProperty("paypal.environment", "sandbox"));
        paypalConfig.put("return_url", prop.getProperty("paypal.return.url"));
        paypalConfig.put("cancel_url", prop.getProperty("paypal.cancel.url"));
        
        gatewayConfigs.put(PAYPAL, paypalConfig);
    }
    
    /**
     * Load MoMo specific configuration
     */
    private static void loadMoMoConfiguration(Properties prop) {
        Map<String, String> momoConfig = new HashMap<>();
        momoConfig.put("partner_code", prop.getProperty("momo.partner.code"));
        momoConfig.put("access_key", prop.getProperty("momo.access.key"));
        momoConfig.put("secret_key", prop.getProperty("momo.secret.key"));
        momoConfig.put("endpoint", prop.getProperty("momo.endpoint"));
        momoConfig.put("return_url", prop.getProperty("momo.return.url"));
        momoConfig.put("notify_url", prop.getProperty("momo.notify.url"));
        
        gatewayConfigs.put(MOMO, momoConfig);
    }
    
    /**
     * Load fallback configuration for development/testing
     */
    private static void loadFallbackConfiguration() {
        activeGateway = VNPAY;
        
        // VNPay fallback configuration
        Map<String, String> vnpayFallback = new HashMap<>();
        vnpayFallback.put("merchant_code", "YOUR_TMN_CODE_HERE");
        vnpayFallback.put("hash_secret", "YOUR_HASH_SECRET_HERE");
        vnpayFallback.put("payment_url", "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html");
        vnpayFallback.put("api_url", "https://sandbox.vnpayment.vn/merchant_webapi/api/transaction");
        vnpayFallback.put("version", "2.1.0");
        vnpayFallback.put("return_url", "http://localhost:8080/aims_project/payment_return");
        vnpayFallback.put("ipn_url", "http://localhost:8080/aims_project/payment_ipn");
        
        gatewayConfigs.put(VNPAY, vnpayFallback);
        
        System.err.println("WARNING: Using fallback payment gateway configuration. Please configure " + CONFIG_FILE + " for production use.");
    }
    
    /**
     * Validate configuration completeness
     */
    private static void validateConfiguration() {
        Map<String, String> activeConfig = gatewayConfigs.get(activeGateway);
        if (activeConfig == null) {
            System.err.println("CRITICAL: No configuration found for active gateway: " + activeGateway);
            return;
        }
        
        // Validate based on gateway type
        switch (activeGateway) {
            case VNPAY:
                validateVNPayConfig(activeConfig);
                break;
            case STRIPE:
                validateStripeConfig(activeConfig);
                break;
            case PAYPAL:
                validatePayPalConfig(activeConfig);
                break;
            case MOMO:
                validateMoMoConfig(activeConfig);
                break;
        }
    }
    
    private static void validateVNPayConfig(Map<String, String> config) {
        if (isNullOrPlaceholder(config.get("merchant_code")) ||
            isNullOrPlaceholder(config.get("hash_secret")) ||
            config.get("payment_url") == null ||
            config.get("api_url") == null ||
            config.get("return_url") == null) {
            System.err.println("CRITICAL: VNPay configuration is incomplete. Please check " + CONFIG_FILE);
        }
    }
    
    private static void validateStripeConfig(Map<String, String> config) {
        if (isNullOrPlaceholder(config.get("public_key")) ||
            isNullOrPlaceholder(config.get("secret_key"))) {
            System.err.println("CRITICAL: Stripe configuration is incomplete. Please check " + CONFIG_FILE);
        }
    }
    
    private static void validatePayPalConfig(Map<String, String> config) {
        if (isNullOrPlaceholder(config.get("client_id")) ||
            isNullOrPlaceholder(config.get("client_secret"))) {
            System.err.println("CRITICAL: PayPal configuration is incomplete. Please check " + CONFIG_FILE);
        }
    }
    
    private static void validateMoMoConfig(Map<String, String> config) {
        if (isNullOrPlaceholder(config.get("partner_code")) ||
            isNullOrPlaceholder(config.get("access_key")) ||
            isNullOrPlaceholder(config.get("secret_key"))) {
            System.err.println("CRITICAL: MoMo configuration is incomplete. Please check " + CONFIG_FILE);
        }
    }
    
    private static boolean isNullOrPlaceholder(String value) {
        return value == null || value.trim().isEmpty() || 
               value.contains("YOUR_") || value.contains("PLACEHOLDER");
    }
    
    // Public getters
    
    /**
     * Get the currently active payment gateway
     */
    public static String getActiveGateway() {
        return activeGateway;
    }
    
    /**
     * Get configuration for a specific gateway
     */
    public static Map<String, String> getGatewayConfig(String gatewayType) {
        return gatewayConfigs.get(gatewayType);
    }
    
    /**
     * Get configuration for the active gateway
     */
    public static Map<String, String> getActiveGatewayConfig() {
        return gatewayConfigs.get(activeGateway);
    }
    
    /**
     * Get a specific configuration value for the active gateway
     */
    public static String getConfigValue(String key) {
        Map<String, String> config = getActiveGatewayConfig();
        return config != null ? config.get(key) : null;
    }
    
    /**
     * Get a specific configuration value for a specific gateway
     */
    public static String getConfigValue(String gatewayType, String key) {
        Map<String, String> config = getGatewayConfig(gatewayType);
        return config != null ? config.get(key) : null;
    }
    
    /**
     * Check if a gateway is properly configured
     */
    public static boolean isGatewayConfigured(String gatewayType) {
        Map<String, String> config = getGatewayConfig(gatewayType);
        if (config == null) return false;
        
        switch (gatewayType) {
            case VNPAY:
                return !isNullOrPlaceholder(config.get("merchant_code")) &&
                       !isNullOrPlaceholder(config.get("hash_secret"));
            case STRIPE:
                return !isNullOrPlaceholder(config.get("public_key")) &&
                       !isNullOrPlaceholder(config.get("secret_key"));
            case PAYPAL:
                return !isNullOrPlaceholder(config.get("client_id")) &&
                       !isNullOrPlaceholder(config.get("client_secret"));
            case MOMO:
                return !isNullOrPlaceholder(config.get("partner_code")) &&
                       !isNullOrPlaceholder(config.get("access_key")) &&
                       !isNullOrPlaceholder(config.get("secret_key"));
            default:
                return false;
        }
    }
    
    /**
     * Set active gateway (for testing or runtime switching)
     */
    public static void setActiveGateway(String gatewayType) {
        if (gatewayConfigs.containsKey(gatewayType)) {
            activeGateway = gatewayType;
            System.out.println("Payment gateway switched to: " + gatewayType);
        } else {
            System.err.println("WARNING: Attempted to switch to unconfigured gateway: " + gatewayType);
        }
    }
    
    /**
     * Get list of available gateways
     */
    public static String[] getAvailableGateways() {
        return gatewayConfigs.keySet().toArray(new String[0]);
    }
    
    /**
     * Reload configuration from file
     */
    public static void reloadConfiguration() {
        gatewayConfigs.clear();
        loadConfiguration();
    }
}
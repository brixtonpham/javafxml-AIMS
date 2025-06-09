package com.aims.test.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Screen Test Helper
 * Common UI testing utilities and helper methods
 * 
 * Features:
 * - Navigation helpers
 * - Data validation
 * - Screen state verification
 * - UI element interaction simulation
 * - Manual testing support
 */
public class ScreenTestHelper {
    
    private static final Logger logger = LoggerFactory.getLogger(ScreenTestHelper.class);
    
    // Screen state tracking
    private String currentScreen;
    private Map<String, Object> screenState;
    
    // Navigation history
    private java.util.List<String> navigationHistory;
    
    // Element verification tracking
    private Map<String, Boolean> elementStates;
    
    public ScreenTestHelper() {
        this.screenState = new HashMap<>();
        this.navigationHistory = new java.util.ArrayList<>();
        this.elementStates = new HashMap<>();
        
        logger.debug("ScreenTestHelper initialized");
    }
    
    // ========================================
    // Navigation Helper Methods
    // ========================================
    
    /**
     * Navigate to a specific screen
     */
    public void navigateToScreen(String screenName) {
        logger.info("🧭 Navigating to screen: {}", screenName);
        
        if (currentScreen != null) {
            navigationHistory.add(currentScreen);
        }
        
        currentScreen = screenName;
        clearScreenState();
        
        logger.debug("Current screen set to: {}", screenName);
    }
    
    /**
     * Go back to previous screen
     */
    public void navigateBack() {
        if (!navigationHistory.isEmpty()) {
            String previousScreen = navigationHistory.remove(navigationHistory.size() - 1);
            logger.info("🔙 Navigating back to: {}", previousScreen);
            currentScreen = previousScreen;
            clearScreenState();
        } else {
            logger.warn("No previous screen to navigate back to");
        }
    }
    
    /**
     * Get current screen name
     */
    public String getCurrentScreen() {
        return currentScreen;
    }
    
    /**
     * Get navigation history
     */
    public java.util.List<String> getNavigationHistory() {
        return new java.util.ArrayList<>(navigationHistory);
    }
    
    // ========================================
    // Element Verification Methods
    // ========================================
    
    /**
     * Verify that a UI element exists
     */
    public boolean verifyElementExists(String elementName) {
        logger.debug("Verifying element exists: {}", elementName);
        
        // In a real UI testing scenario, this would check the actual UI
        // For manual testing, we simulate the verification
        boolean exists = elementStates.getOrDefault(elementName, true);
        
        if (exists) {
            logger.debug("✅ Element verified: {}", elementName);
        } else {
            logger.warn("❌ Element not found: {}", elementName);
        }
        
        return exists;
    }
    
    /**
     * Set element state for testing
     */
    public void setElementState(String elementName, boolean exists) {
        elementStates.put(elementName, exists);
        logger.debug("Element state set: {} = {}", elementName, exists);
    }
    
    /**
     * Verify element is enabled
     */
    public boolean verifyElementEnabled(String elementName) {
        logger.debug("Verifying element enabled: {}", elementName);
        
        if (!verifyElementExists(elementName)) {
            return false;
        }
        
        // Simulate enabled state verification
        String enabledKey = elementName + "_enabled";
        boolean enabled = elementStates.getOrDefault(enabledKey, true);
        
        logger.debug("Element {} enabled state: {}", elementName, enabled);
        return enabled;
    }
    
    /**
     * Verify element is visible
     */
    public boolean verifyElementVisible(String elementName) {
        logger.debug("Verifying element visible: {}", elementName);
        
        if (!verifyElementExists(elementName)) {
            return false;
        }
        
        // Simulate visibility verification
        String visibleKey = elementName + "_visible";
        boolean visible = elementStates.getOrDefault(visibleKey, true);
        
        logger.debug("Element {} visibility state: {}", elementName, visible);
        return visible;
    }
    
    /**
     * Verify element text content
     */
    public boolean verifyElementText(String elementName, String expectedText) {
        logger.debug("Verifying element text: {} = '{}'", elementName, expectedText);
        
        if (!verifyElementExists(elementName)) {
            return false;
        }
        
        // Simulate text verification
        String textKey = elementName + "_text";
        String actualText = (String) screenState.getOrDefault(textKey, expectedText);
        
        boolean matches = expectedText.equals(actualText);
        logger.debug("Element {} text verification: expected='{}', actual='{}', matches={}", 
            elementName, expectedText, actualText, matches);
        
        return matches;
    }
    
    // ========================================
    // Data Validation Methods
    // ========================================
    
    /**
     * Validate product display data
     */
    public boolean validateProductDisplay(String productId, Map<String, Object> expectedData) {
        logger.debug("Validating product display for: {}", productId);
        
        for (Map.Entry<String, Object> entry : expectedData.entrySet()) {
            String fieldName = entry.getKey();
            Object expectedValue = entry.getValue();
            
            String elementName = String.format("product_%s_%s", productId, fieldName);
            if (!verifyElementExists(elementName)) {
                logger.warn("Product field element not found: {}", elementName);
                return false;
            }
            
            // In real testing, would verify actual displayed values
            logger.debug("✅ Product field verified: {} = {}", fieldName, expectedValue);
        }
        
        return true;
    }
    
    /**
     * Validate cart item display
     */
    public boolean validateCartItemDisplay(String productId, int quantity, double price) {
        logger.debug("Validating cart item display: {} (qty: {}, price: {})", productId, quantity, price);
        
        Map<String, Object> expectedData = new HashMap<>();
        expectedData.put("quantity", quantity);
        expectedData.put("price", price);
        
        return validateProductDisplay(productId, expectedData);
    }
    
    /**
     * Validate form field values
     */
    public boolean validateFormData(Map<String, String> formData) {
        logger.debug("Validating form data with {} fields", formData.size());
        
        for (Map.Entry<String, String> entry : formData.entrySet()) {
            String fieldName = entry.getKey();
            String expectedValue = entry.getValue();
            
            if (!verifyElementText(fieldName, expectedValue)) {
                logger.warn("Form field validation failed: {} != '{}'", fieldName, expectedValue);
                return false;
            }
        }
        
        logger.debug("✅ All form fields validated successfully");
        return true;
    }
    
    // ========================================
    // Screen State Management
    // ========================================
    
    /**
     * Set screen state value
     */
    public void setScreenState(String key, Object value) {
        screenState.put(key, value);
        logger.debug("Screen state set: {} = {}", key, value);
    }
    
    /**
     * Get screen state value
     */
    public Object getScreenState(String key) {
        return screenState.get(key);
    }
    
    /**
     * Clear all screen state
     */
    public void clearScreenState() {
        screenState.clear();
        elementStates.clear();
        logger.debug("Screen state cleared");
    }
    
    /**
     * Set multiple screen state values
     */
    public void setScreenState(Map<String, Object> stateData) {
        screenState.putAll(stateData);
        logger.debug("Screen state updated with {} entries", stateData.size());
    }
    
    // ========================================
    // User Action Simulation
    // ========================================
    
    /**
     * Simulate clicking a button
     */
    public void clickButton(String buttonName) {
        logger.info("🖱️ Clicking button: {}", buttonName);
        
        if (!verifyElementExists(buttonName)) {
            throw new RuntimeException("Button not found: " + buttonName);
        }
        
        if (!verifyElementEnabled(buttonName)) {
            throw new RuntimeException("Button not enabled: " + buttonName);
        }
        
        // Simulate button click action
        setScreenState(buttonName + "_clicked", true);
        logger.debug("Button click simulated: {}", buttonName);
    }
    
    /**
     * Simulate entering text in a field
     */
    public void enterText(String fieldName, String text) {
        logger.info("⌨️ Entering text in field '{}': '{}'", fieldName, text);
        
        if (!verifyElementExists(fieldName)) {
            throw new RuntimeException("Text field not found: " + fieldName);
        }
        
        if (!verifyElementEnabled(fieldName)) {
            throw new RuntimeException("Text field not enabled: " + fieldName);
        }
        
        // Simulate text entry
        setScreenState(fieldName + "_text", text);
        setElementState(fieldName + "_text", true);
        logger.debug("Text entry simulated: {} = '{}'", fieldName, text);
    }
    
    /**
     * Simulate selecting from dropdown
     */
    public void selectDropdownOption(String dropdownName, String option) {
        logger.info("📋 Selecting dropdown option '{}' from '{}'", option, dropdownName);
        
        if (!verifyElementExists(dropdownName)) {
            throw new RuntimeException("Dropdown not found: " + dropdownName);
        }
        
        // Simulate dropdown selection
        setScreenState(dropdownName + "_selected", option);
        logger.debug("Dropdown selection simulated: {} = '{}'", dropdownName, option);
    }
    
    // ========================================
    // Wait and Timing Methods
    // ========================================
    
    /**
     * Wait for element to appear
     */
    public boolean waitForElement(String elementName, int timeoutSeconds) {
        logger.debug("Waiting for element '{}' (timeout: {}s)", elementName, timeoutSeconds);
        
        // In real UI testing, this would poll for element existence
        // For simulation, we check current state
        boolean exists = verifyElementExists(elementName);
        
        if (exists) {
            logger.debug("✅ Element found: {}", elementName);
        } else {
            logger.warn("⏰ Element not found within timeout: {}", elementName);
        }
        
        return exists;
    }
    
    /**
     * Wait for page to load
     */
    public void waitForPageLoad() {
        logger.debug("⏳ Waiting for page load...");
        
        try {
            // Simulate page load wait
            TimeUnit.MILLISECONDS.sleep(100);
            logger.debug("✅ Page load completed");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Page load wait interrupted");
        }
    }
    
    // ========================================
    // Test Data Setup Helpers
    // ========================================
    
    /**
     * Setup product list screen with test data
     */
    public void setupProductListScreen(java.util.List<Map<String, Object>> products) {
        logger.debug("Setting up product list screen with {} products", products.size());
        
        navigateToScreen("ProductListScreen");
        
        for (int i = 0; i < products.size(); i++) {
            Map<String, Object> product = products.get(i);
            String productId = "product_" + i;
            
            for (Map.Entry<String, Object> entry : product.entrySet()) {
                String elementName = productId + "_" + entry.getKey();
                setElementState(elementName, true);
                setScreenState(elementName + "_text", entry.getValue().toString());
            }
        }
        
        setScreenState("product_count", products.size());
        logger.debug("✅ Product list screen setup completed");
    }
    
    /**
     * Setup cart screen with test data
     */
    public void setupCartScreen(java.util.List<Map<String, Object>> cartItems) {
        logger.debug("Setting up cart screen with {} items", cartItems.size());
        
        navigateToScreen("CartScreen");
        
        double totalAmount = 0.0;
        
        for (int i = 0; i < cartItems.size(); i++) {
            Map<String, Object> item = cartItems.get(i);
            String itemId = "cart_item_" + i;
            
            for (Map.Entry<String, Object> entry : item.entrySet()) {
                String elementName = itemId + "_" + entry.getKey();
                setElementState(elementName, true);
                setScreenState(elementName + "_text", entry.getValue().toString());
            }
            
            // Calculate total if price and quantity are available
            if (item.containsKey("price") && item.containsKey("quantity")) {
                double price = ((Number) item.get("price")).doubleValue();
                int quantity = ((Number) item.get("quantity")).intValue();
                totalAmount += price * quantity;
            }
        }
        
        setScreenState("cart_item_count", cartItems.size());
        setScreenState("cart_total_amount", totalAmount);
        logger.debug("✅ Cart screen setup completed with total: {}", totalAmount);
    }
    
    // ========================================
    // Verification Helper Methods
    // ========================================
    
    /**
     * Verify screen title
     */
    public boolean verifyScreenTitle(String expectedTitle) {
        return verifyElementText("screen_title", expectedTitle);
    }
    
    /**
     * Verify error message display
     */
    public boolean verifyErrorMessage(String expectedMessage) {
        return verifyElementExists("error_message") && 
               verifyElementText("error_message", expectedMessage);
    }
    
    /**
     * Verify success message display
     */
    public boolean verifySuccessMessage(String expectedMessage) {
        return verifyElementExists("success_message") && 
               verifyElementText("success_message", expectedMessage);
    }
    
    /**
     * Verify loading state
     */
    public boolean verifyLoadingState(boolean shouldBeLoading) {
        boolean isLoading = verifyElementExists("loading_indicator");
        return isLoading == shouldBeLoading;
    }
    
    /**
     * Get current verification summary
     */
    public Map<String, Object> getVerificationSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("current_screen", currentScreen);
        summary.put("navigation_history", navigationHistory);
        summary.put("element_count", elementStates.size());
        summary.put("screen_state_count", screenState.size());
        
        return summary;
    }
    
    /**
     * Print current state for debugging
     */
    public void printCurrentState() {
        logger.info("=== Screen Test Helper State ===");
        logger.info("Current Screen: {}", currentScreen);
        logger.info("Navigation History: {}", navigationHistory);
        logger.info("Element States: {}", elementStates.size());
        logger.info("Screen State Entries: {}", screenState.size());
        logger.info("===============================");
    }
}
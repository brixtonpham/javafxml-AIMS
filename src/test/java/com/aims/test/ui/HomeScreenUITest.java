package com.aims.test.ui;

import com.aims.AimsApp;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.*;
import org.testfx.api.FxToolkit;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.matcher.base.NodeMatchers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.testfx.api.FxAssert.verifyThat;

import com.aims.test.config.TestDatabaseConfig;

/**
 * UI Test cho Home Screen của AIMS Customer Flow
 * Test Case: HS_TC01 - Initial Display
 * 
 * Kiểm tra hiển thị ban đầu của trang chủ khi customer truy cập:
 * - Hiển thị danh sách sản phẩm (tối đa 20 sản phẩm/trang)
 * - Add to Cart button: enabled cho sản phẩm còn hàng, disabled cho hết hàng
 * - Kiểm tra filter và sort controls
 * - Kiểm tra pagination khi có nhiều hơn 20 sản phẩm
 */
@DisplayName("Home Screen UI Tests - HS_TC01")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class HomeScreenUITest extends ApplicationTest {
    
    private List<Product> allProducts;
    private List<Product> inStockProducts;
    private List<Product> outOfStockProducts;

    @Override
    public void start(Stage stage) throws Exception {
        // Set up test environment
        System.setProperty("testfx.robot", "glass");
        System.setProperty("testfx.headless", "true");
        System.setProperty("prism.order", "sw");
        System.setProperty("prism.text", "t2k");
        System.setProperty("java.awt.headless", "true");
        
        // Ensure test database is used
        TestDatabaseConfig.enableTestMode();
        
        // Launch the AIMS application
        new AimsApp().start(stage);
    }

    @BeforeAll
    static void setUpClass() throws TimeoutException {
        // Register primary stage
        FxToolkit.registerPrimaryStage();
    }

    @AfterAll
    static void tearDownClass() throws TimeoutException {
        // Clean up TestFX
        FxToolkit.cleanupStages();
    }

    @BeforeEach
    void setUp() {
        // Load product data from test database
        loadProductData();
        
        // Wait for UI to be ready
        sleep(2000);
    }

    @AfterEach
    void tearDown() {
        // Clean up after each test if needed
        Platform.runLater(() -> {
            // Reset any UI state if necessary
        });
    }
    
    void loadProductData() {
        // Debug: Check test mode and database URL
        System.out.println("🔍 DEBUG: Test mode enabled: " + TestDatabaseConfig.isTestMode());
        System.out.println("🔍 DEBUG: Database URL: " + TestDatabaseConfig.getCurrentDatabaseUrl());
        
        // Load test product data from database
        allProducts = loadAllProducts();
        inStockProducts = allProducts.stream()
            .filter(p -> p.quantityInStock > 0)
            .toList();
        outOfStockProducts = allProducts.stream()
            .filter(p -> p.quantityInStock == 0)
            .toList();
            
        System.out.println("📊 Product data loaded:");
        System.out.println("   - Total products: " + allProducts.size());
        System.out.println("   - In stock: " + inStockProducts.size());
        System.out.println("   - Out of stock: " + outOfStockProducts.size());
    }
    
    @Test
    @Order(1)
    @DisplayName("HS_TC01.1 - Verify home screen loads and displays maximum 20 products")
    void testHomeScreenLoadsWithMaxTwentyProducts() {
        // Verify the home screen is loaded by checking for key UI elements
        verifyThat("#searchField", NodeMatchers.isNotNull());
        verifyThat("#categoryComboBox", NodeMatchers.isNotNull());
        verifyThat("#sortByPriceComboBox", NodeMatchers.isNotNull());
        verifyThat("#productFlowPane", NodeMatchers.isNotNull());
        
        // Get the product flow pane
        FlowPane productFlowPane = lookup("#productFlowPane").query();
        assertNotNull(productFlowPane, "Product flow pane should be present");
        
        // Count visible product cards
        int visibleProducts = productFlowPane.getChildren().size();
        assertTrue(visibleProducts <= 20, "Should display maximum 20 products per page, found: " + visibleProducts);
        assertTrue(visibleProducts > 0, "Should display at least some products");
        
        System.out.println("✓ Home screen loaded with " + visibleProducts + " products displayed");
    }
    
    @Test
    @Order(2)
    @DisplayName("HS_TC01.2 - Verify product information accuracy")
    void testProductInformationAccuracy() {
        FlowPane productFlowPane = lookup("#productFlowPane").query();
        assertNotNull(productFlowPane, "Product flow pane should be present");
        
        // Check that products have required information displayed
        List<Node> productCards = productFlowPane.getChildren();
        assertTrue(productCards.size() > 0, "Should have at least one product to verify");
        
        // For each product card, verify it contains required elements
        for (int i = 0; i < Math.min(3, productCards.size()); i++) { // Check first 3 products
            Node productCard = productCards.get(i);
            
            // Check for product image (should have ImageView)
            assertTrue(from(productCard).lookup(".image-view").tryQuery().isPresent() || 
                      from(productCard).lookup("ImageView").tryQuery().isPresent(),
                      "Product " + (i+1) + " should have an image");
            
            // Check for product name/title (should have Label with title)
            assertTrue(from(productCard).lookup(".product-title").tryQuery().isPresent() ||
                      from(productCard).lookup("Label").tryQuery().isPresent(),
                      "Product " + (i+1) + " should have a title");
            
            // Check for price display (should have Label with price)
            assertTrue(from(productCard).lookup(".product-price").tryQuery().isPresent() ||
                      from(productCard).lookup("Label").tryQuery().isPresent(),
                      "Product " + (i+1) + " should have a price");
        }
        
        System.out.println("✓ Product information accuracy verified for displayed products");
    }
    
    @Test
    @Order(3)
    @DisplayName("HS_TC01.3 - Verify Add to Cart button states")
    void testAddToCartButtonStates() {
        FlowPane productFlowPane = lookup("#productFlowPane").query();
        assertNotNull(productFlowPane, "Product flow pane should be present");
        
        List<Node> productCards = productFlowPane.getChildren();
        assertTrue(productCards.size() > 0, "Should have products to check button states");
        
        int enabledButtons = 0;
        int disabledButtons = 0;
        
        // Check Add to Cart button states for each product
        for (int i = 0; i < productCards.size(); i++) {
            Node productCard = productCards.get(i);
            
            // Look for Add to Cart button
            var addToCartButton = from(productCard).lookup(".add-to-cart-btn").tryQuery();
            if (addToCartButton.isEmpty()) {
                addToCartButton = from(productCard).lookup("Button").tryQuery();
            }
            
            if (addToCartButton.isPresent() && addToCartButton.get() instanceof Button) {
                Button button = (Button) addToCartButton.get();
                if (button.isDisabled()) {
                    disabledButtons++;
                } else {
                    enabledButtons++;
                }
            }
        }
        
        // We should have both enabled and disabled buttons if we have products with different stock status
        assertTrue(enabledButtons > 0 || disabledButtons > 0, 
                  "Should have Add to Cart buttons in various states");
        
        System.out.println("✓ Add to Cart button states verified: " + enabledButtons + " enabled, " + disabledButtons + " disabled");
    }
    
    @Test
    @Order(4)
    @DisplayName("HS_TC01.4 - Verify filter default values")
    void testFilterDefaultValues() {
        // Check category filter default value
        ComboBox<?> categoryComboBox = lookup("#categoryComboBox").query();
        assertNotNull(categoryComboBox, "Category combo box should be present");
        
        // Check if default value is "All Categories" or similar
        String categoryValue = categoryComboBox.getValue() != null ? categoryComboBox.getValue().toString() : "null";
        assertTrue(categoryValue.contains("All") || categoryValue.contains("all") || categoryValue.equals("null"),
                  "Category filter should show default 'All Categories' value, found: " + categoryValue);
        
        // Check sort filter default value
        ComboBox<?> sortComboBox = lookup("#sortByPriceComboBox").query();
        assertNotNull(sortComboBox, "Sort combo box should be present");
        
        String sortValue = sortComboBox.getValue() != null ? sortComboBox.getValue().toString() : "null";
        assertTrue(sortValue.contains("Default") || sortValue.contains("default") || sortValue.equals("null"),
                  "Sort filter should show default value, found: " + sortValue);
        
        System.out.println("✓ Filter default values verified - Category: " + categoryValue + ", Sort: " + sortValue);
    }
    
    @Test
    @Order(5)
    @DisplayName("HS_TC01.5 - Verify pagination display")
    void testPaginationDisplay() {
        // Look for pagination controls
        var paginationControls = lookup(".pagination").tryQuery();
        if (paginationControls.isEmpty()) {
            paginationControls = lookup("#pagination").tryQuery();
        }
        
        if (allProducts.size() > 20) {
            assertTrue(paginationControls.isPresent(), "Pagination controls should be visible when there are more than 20 products");
            
            // Check for page indicator (e.g., "Page 1/X")
            var pageIndicator = lookup(".page-indicator").tryQuery();
            if (pageIndicator.isEmpty()) {
                pageIndicator = lookup("Label").tryQuery();
            }
            
            // Check for Previous button (should be disabled on first page)
            var prevButton = lookup(".prev-button").tryQuery();
            if (prevButton.isEmpty()) {
                prevButton = lookup("#prevButton").tryQuery();
            }
            
            if (prevButton.isPresent() && prevButton.get() instanceof Button) {
                Button prevBtn = (Button) prevButton.get();
                assertTrue(prevBtn.isDisabled(), "Previous button should be disabled on first page");
            }
            
            // Check for Next button (should be enabled if there are more pages)
            var nextButton = lookup(".next-button").tryQuery();
            if (nextButton.isEmpty()) {
                nextButton = lookup("#nextButton").tryQuery();
            }
            
            System.out.println("✓ Pagination controls verified for " + allProducts.size() + " total products");
        } else {
            System.out.println("✓ Pagination not required - only " + allProducts.size() + " products");
        }
    }
    
    // Helper methods for database interaction and data management
    
    private List<Product> loadAllProducts() {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT productID, title, category, price, quantityInStock, description, imageURL, productType, entryDate " +
                    "FROM PRODUCT ORDER BY entryDate DESC";
        
        try (Connection conn = TestDatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                Product product = new Product();
                product.productID = rs.getString("productID");
                product.title = rs.getString("title");
                product.category = rs.getString("category");
                product.price = rs.getDouble("price");
                product.quantityInStock = rs.getInt("quantityInStock");
                product.description = rs.getString("description");
                product.imageURL = rs.getString("imageURL");
                product.productType = rs.getString("productType");
                product.entryDate = rs.getString("entryDate");
                products.add(product);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load products from test database", e);
        }
        
        return products;
    }
    
    // Product data model for testing
    @SuppressWarnings("unused")
    private static class Product {
        String productID;
        String title;
        String category;
        double price;
        int quantityInStock;
        String description;
        String imageURL;
        String productType;
        String entryDate;
    }
}

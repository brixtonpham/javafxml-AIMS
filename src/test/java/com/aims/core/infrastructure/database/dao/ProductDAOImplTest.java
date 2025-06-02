package com.aims.core.infrastructure.database.dao;

import com.aims.core.entities.Product;
import com.aims.core.entities.Book;
import com.aims.core.enums.ProductType;
import com.aims.core.infrastructure.database.SQLiteConnector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class ProductDAOImplTest {

    private static final String TEST_DB_URL_PREFIX = "jdbc:sqlite:";
    private String testDbUrl;
    private IProductDAO productDAO;
    private Connection connection;

    @TempDir
    Path tempDir; // JUnit 5 temporary directory

    @BeforeEach
    void setUp() throws SQLException, IOException {
        // Create a temporary database file for each test
        Path dbFile = tempDir.resolve("aims_test.db");
        testDbUrl = TEST_DB_URL_PREFIX + dbFile.toAbsolutePath().toString();

        // Initialize SQLiteConnector with the test database URL
        // This might require a modification in SQLiteConnector or a test-specific setup
        // For now, we'll manage the connection directly for testing purposes.
        System.setProperty("TEST_DB_URL", testDbUrl); // Pass it as a system property

        connection = DriverManager.getConnection(testDbUrl);
        SQLiteConnector.getInstance().setConnection(connection); // Assuming a setter for testing or a test-specific instance

        // Run schema creation script
        Path schemaScriptPath = Path.of("src/main/java/com/aims/core/infrastructure/database/scripts/V1__create_tables.sql");
        String schemaSql = Files.readString(schemaScriptPath);
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("PRAGMA foreign_keys = ON;"); // Ensure FKs are on for the test DB session
            stmt.executeUpdate(schemaSql); // This will fail if tables exist, so ensure clean DB or handle
        }

        productDAO = new ProductDAOImpl(); // Uses the connection from SQLiteConnector
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            // Clean up database after each test (optional, could also drop tables or use @DirtiesContext if Spring)
            try (Statement stmt = connection.createStatement()) {
                // Order matters due to foreign keys if not using CASCADE delete on tables
                stmt.executeUpdate("DELETE FROM BOOK;");
                stmt.executeUpdate("DELETE FROM CD;");
                stmt.executeUpdate("DELETE FROM DVD;");
                stmt.executeUpdate("DELETE FROM PRODUCT;");
                // Add other tables if necessary
            }
            connection.close();
        }
        System.clearProperty("TEST_DB_URL");
        SQLiteConnector.getInstance().setConnection(null); // Reset connection in SQLiteConnector
    }

    @Test
    void testAddAndGetProduct_Book() throws SQLException {
        Book book = new Book();
        book.setProductId("B001");
        book.setTitle("Test Book Title");
        book.setCategory("Fiction");
        book.setValueAmount(25.0f);
        book.setPrice(29.99f);
        book.setQuantityInStock(100);
        book.setDescription("A great book for testing.");
        book.setImageUrl("/images/test_book.png");
        book.setBarcode("1234567890123");
        book.setDimensionsCm("20x15x3");
        book.setWeightKg(0.5f);
        book.setEntryDate(LocalDate.now());
        book.setProductType(ProductType.BOOK);
        // Book specific
        book.setAuthors("Author One, Author Two");
        book.setCoverType("Hardcover");
        book.setPublisher("Test Publisher");
        book.setPublicationDate(LocalDate.of(2023, 1, 15));
        book.setNumPages(300);
        book.setLanguage("English");
        book.setBookGenre("Science Fiction");

        productDAO.addBookDetails(book);

        Product retrievedProduct = productDAO.getById("B001");
        assertNotNull(retrievedProduct, "Product should not be null");
        assertTrue(retrievedProduct instanceof Book, "Product should be an instance of Book");
        assertEquals(ProductType.BOOK, retrievedProduct.getProductType(), "Product type should be BOOK");

        Book retrievedBook = (Book) retrievedProduct;
        assertEquals("Test Book Title", retrievedBook.getTitle());
        assertEquals("Author One, Author Two", retrievedBook.getAuthors());
        assertEquals(100, retrievedBook.getQuantityInStock());
        assertEquals(LocalDate.of(2023, 1, 15), retrievedBook.getPublicationDate());
    }
    
    @Test
    void testUpdateProduct_Book() throws SQLException {
        // First, add a book
        Book book = new Book();
        book.setProductId("B002");
        book.setTitle("Original Book Title");
        book.setCategory("Non-Fiction");
        book.setValueAmount(30.0f);
        book.setPrice(35.00f);
        book.setQuantityInStock(50);
        book.setProductType(ProductType.BOOK);
        book.setAuthors("Original Author");
        book.setCoverType("Paperback");
        book.setPublisher("Original Publisher");
        book.setPublicationDate(LocalDate.of(2022, 5, 10));
        book.setNumPages(250);
        book.setLanguage("English");
        book.setBookGenre("History");

        productDAO.addBookDetails(book);

        // Now update it
        Book bookToUpdate = (Book) productDAO.getById("B002"); // Fetch the just added book
        assertNotNull(bookToUpdate);
        
        bookToUpdate.setTitle("Updated Book Title");
        bookToUpdate.setQuantityInStock(45);
        bookToUpdate.setAuthors("Updated Author");
        bookToUpdate.setPrice(32.50f);

        productDAO.updateBookDetails(bookToUpdate);

        Product retrievedProduct = productDAO.getById("B002");
        assertNotNull(retrievedProduct);
        assertTrue(retrievedProduct instanceof Book);
        Book updatedBook = (Book) retrievedProduct;

        assertEquals("Updated Book Title", updatedBook.getTitle());
        assertEquals(45, updatedBook.getQuantityInStock());
        assertEquals("Updated Author", updatedBook.getAuthors());
        assertEquals(32.50f, updatedBook.getPrice());
    }

    @Test
    void testDeleteProduct() throws SQLException {
        Book book = new Book();
        book.setProductId("B003");
        book.setTitle("To Be Deleted");
        book.setCategory("Mystery");
        book.setValueAmount(15.0f);
        book.setPrice(19.99f);
        book.setQuantityInStock(20);
        book.setProductType(ProductType.BOOK);
        book.setAuthors("Anonymous");
        book.setCoverType("Ebook");
        book.setPublisher("SelfPub");
        book.setPublicationDate(LocalDate.of(2024, 1, 1));
        book.setNumPages(180);
        book.setLanguage("English");
        book.setBookGenre("Thriller");

        productDAO.addBookDetails(book);
        assertNotNull(productDAO.getById("B003"), "Product should exist before deletion");

        productDAO.delete("B003");
        assertNull(productDAO.getById("B003"), "Product should not exist after deletion");
    }

    @Test
    void testGetAllProducts_Simplified() throws SQLException {
        // Add a couple of products of different types (simplified for getAll)
        Product product1 = new Product("P001", "Generic Product 1", "Electronics", 100f, 120f, 10, null, null, null, null, 0, null, ProductType.OTHER);
        Book book1 = new Book();
        book1.setProductId("B004");
        book1.setTitle("Another Book");
        book1.setCategory("Education");
        book1.setValueAmount(50f);
        book1.setPrice(60f);
        book1.setQuantityInStock(30);
        book1.setProductType(ProductType.BOOK);
        book1.setAuthors("Educator");
        // ... set other required book fields ...

        productDAO.addBaseProduct(product1); // Using addBaseProduct for simplicity in this test case
        productDAO.addBookDetails(book1);    // Adds base and book details

        java.util.List<Product> products = productDAO.getAll();
        assertNotNull(products);
        // Note: The current getAll() in DAOImpl only fetches base product info.
        // This test will reflect that. If getAll() is enhanced to fetch full subtype details,
        // this test would need to be adjusted.
        assertTrue(products.size() >= 2, "Should retrieve at least two products");

        boolean foundP001 = false;
        boolean foundB004 = false;
        for (Product p : products) {
            if ("P001".equals(p.getProductId())) foundP001 = true;
            if ("B004".equals(p.getProductId())) foundB004 = true;
        }
        assertTrue(foundP001, "Generic Product P001 not found in getAll()");
        assertTrue(foundB004, "Book B004 not found in getAll()");
    }
    
    // Add more tests for CD, DVD, findByTitle, findByCategory, updateStock etc.
    // Remember to handle the specific fields for CD and DVD entities.
}

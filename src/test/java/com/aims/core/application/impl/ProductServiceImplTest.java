package com.aims.core.application.impl;

import com.aims.core.entities.Book;
import com.aims.core.entities.CD;
import com.aims.core.entities.DVD;
import com.aims.core.entities.Product;
import com.aims.core.infrastructure.database.dao.IProductDAO;
import com.aims.core.shared.exceptions.ResourceNotFoundException;
import com.aims.core.shared.exceptions.ValidationException;
import com.aims.core.shared.utils.SearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ProductServiceImplTest {

    @Mock
    private IProductDAO productDAO;

    @InjectMocks
    private ProductServiceImpl productService;

    private Book sampleBook;
    private CD sampleCD;
    private DVD sampleDVD;
    private String managerId = "manager-" + UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        sampleBook = new Book();
        sampleBook.setProductId("BOOK-" + UUID.randomUUID().toString());
        sampleBook.setTitle("Test Book");
        sampleBook.setPrice(100.0f); // Price before VAT
        sampleBook.setValueAmount(90.0f); // Value to check price constraints
        sampleBook.setQuantityInStock(10);
        sampleBook.setEntryDate(LocalDate.now());
        sampleBook.setAuthors("Test Author"); // Corrected: setAuthors instead of setAuthor
        sampleBook.setCoverType("Hardcover");
        sampleBook.setPublisher("Test Publisher");
        sampleBook.setPublicationDate(LocalDate.of(2023, 1, 1));
        sampleBook.setNumPages(300); // Corrected: setNumPages instead of setPages
        sampleBook.setLanguage("English");

        sampleCD = new CD();
        sampleCD.setProductId("CD-" + UUID.randomUUID().toString());
        sampleCD.setTitle("Test CD");
        sampleCD.setPrice(50.0f);
        sampleCD.setValueAmount(40.0f);
        sampleCD.setQuantityInStock(5);
        sampleCD.setEntryDate(LocalDate.now());
        sampleCD.setArtists("Test Artist"); // Corrected: setArtists instead of setArtist
        sampleCD.setRecordLabel("Test Label");
        sampleCD.setCdGenre("Pop"); // Corrected: setCdGenre instead of setMusicType
        sampleCD.setReleaseDate(LocalDate.of(2022, 5, 5)); // Corrected: setReleaseDate

        sampleDVD = new DVD();
        sampleDVD.setProductId("DVD-" + UUID.randomUUID().toString());
        sampleDVD.setTitle("Test DVD");
        sampleDVD.setPrice(75.0f);
        sampleDVD.setValueAmount(60.0f);
        sampleDVD.setQuantityInStock(8);
        sampleDVD.setEntryDate(LocalDate.now());
        sampleDVD.setDiscType("Blu-ray");
        sampleDVD.setDirector("Test Director");
        sampleDVD.setRuntimeMinutes(120); // Corrected: setRuntimeMinutes instead of setRuntime
        sampleDVD.setStudio("Test Studio");
        sampleDVD.setDvdLanguage("English"); // Corrected: setDvdLanguage instead of setLanguage
        sampleDVD.setSubtitles("English, Spanish");
        sampleDVD.setDvdReleaseDate(LocalDate.of(2021, 10, 10)); // Corrected: setDvdReleaseDate instead of setReleaseDate
    }

    // --- Add Product Tests ---
    @Test
    void addBook_success() throws SQLException, ValidationException {
        doNothing().when(productDAO).addBookDetails(any(Book.class));
        when(productDAO.getById(sampleBook.getProductId())).thenReturn(sampleBook);

        Book addedBook = productService.addBook(sampleBook);

        assertNotNull(addedBook);
        assertEquals(sampleBook.getProductId(), addedBook.getProductId());
        verify(productDAO, times(1)).addBookDetails(sampleBook);
    }

    @Test
    void addCD_success() throws SQLException, ValidationException {
        doNothing().when(productDAO).addCDDetails(any(CD.class));
        when(productDAO.getById(sampleCD.getProductId())).thenReturn(sampleCD);

        CD addedCD = productService.addCD(sampleCD);

        assertNotNull(addedCD);
        assertEquals(sampleCD.getProductId(), addedCD.getProductId());
        verify(productDAO, times(1)).addCDDetails(sampleCD);
    }

    @Test
    void addDVD_success() throws SQLException, ValidationException {
        doNothing().when(productDAO).addDVDDetails(any(DVD.class));
        when(productDAO.getById(sampleDVD.getProductId())).thenReturn(sampleDVD);

        DVD addedDVD = productService.addDVD(sampleDVD);

        assertNotNull(addedDVD);
        assertEquals(sampleDVD.getProductId(), addedDVD.getProductId());
        verify(productDAO, times(1)).addDVDDetails(sampleDVD);
    }

    @Test
    void addBook_priceOutOfRange_throwsValidationException() {
        sampleBook.setPrice(10.0f); // Value is 90, min price is 0.3*90 = 27
        assertThrows(ValidationException.class, () -> productService.addBook(sampleBook));
    }

    // --- Update Product Tests ---
    @Test
    void updateBook_success() throws SQLException, ValidationException, ResourceNotFoundException {
        when(productDAO.getById(sampleBook.getProductId())).thenReturn(sampleBook);
        doNothing().when(productDAO).updateBookDetails(any(Book.class));

        sampleBook.setTitle("Updated Test Book");
        Book updatedBook = productService.updateBook(sampleBook);

        assertNotNull(updatedBook);
        assertEquals("Updated Test Book", updatedBook.getTitle());
        verify(productDAO, times(1)).updateBookDetails(sampleBook);
    }

    @Test
    void updateBook_notFound_throwsResourceNotFoundException() throws SQLException {
        when(productDAO.getById(anyString())).thenReturn(null);
        assertThrows(ResourceNotFoundException.class, () -> productService.updateBook(sampleBook));
    }

    // --- Get Product Tests ---
    @Test
    void getProductById_success() throws SQLException {
        when(productDAO.getById(sampleBook.getProductId())).thenReturn(sampleBook);
        Product foundProduct = productService.getProductById(sampleBook.getProductId());
        assertNotNull(foundProduct);
        assertEquals(sampleBook.getProductId(), foundProduct.getProductId());
    }

    // --- Delete Product Tests ---
    @Test
    void deleteProduct_success() throws SQLException, ValidationException, ResourceNotFoundException {
        when(productDAO.getById(sampleBook.getProductId())).thenReturn(sampleBook);
        doNothing().when(productDAO).delete(sampleBook.getProductId());

        productService.deleteProduct(sampleBook.getProductId(), managerId);
        verify(productDAO, times(1)).delete(sampleBook.getProductId());
    }

    @Test
    void deleteProduct_inOrder_throwsValidationException() throws SQLException {
        when(productDAO.getById(sampleBook.getProductId())).thenReturn(sampleBook);
        doThrow(new SQLException("constraint failed")).when(productDAO).delete(sampleBook.getProductId());

        assertThrows(ValidationException.class, () -> productService.deleteProduct(sampleBook.getProductId(), managerId));
    }

    @Test
    void deleteProducts_batchSuccess() throws SQLException, ValidationException {
        List<String> productIds = Arrays.asList(sampleBook.getProductId(), sampleCD.getProductId());
        when(productDAO.getById(sampleBook.getProductId())).thenReturn(sampleBook);
        when(productDAO.getById(sampleCD.getProductId())).thenReturn(sampleCD);
        doNothing().when(productDAO).delete(anyString());

        productService.deleteProducts(productIds, managerId);

        verify(productDAO, times(1)).delete(sampleBook.getProductId());
        verify(productDAO, times(1)).delete(sampleCD.getProductId());
    }

    @Test
    void deleteProducts_tooMany_throwsValidationException() {
        List<String> productIds = new ArrayList<>();
        for (int i = 0; i < 11; i++) productIds.add("ID-" + i);
        assertThrows(ValidationException.class, () -> productService.deleteProducts(productIds, managerId));
    }


    // --- Price and Stock Update Tests ---
    @Test
    void updateProductPrice_success() throws SQLException, ValidationException, ResourceNotFoundException {
        when(productDAO.getById(sampleBook.getProductId())).thenReturn(sampleBook);
        doNothing().when(productDAO).updateBaseProduct(any(Product.class));

        float newPrice = 80.0f; // Within 30%-150% of value 90 (27 to 135)
        Product updatedProduct = productService.updateProductPrice(sampleBook.getProductId(), newPrice, managerId);

        assertNotNull(updatedProduct);
        assertEquals(newPrice, updatedProduct.getPrice());
        verify(productDAO, times(1)).updateBaseProduct(any(Product.class));
    }

    @Test
    void updateProductPrice_invalidPrice_throwsValidationException() throws SQLException {
        when(productDAO.getById(sampleBook.getProductId())).thenReturn(sampleBook);
        float newPriceTooLow = 10.0f;
        assertThrows(ValidationException.class, () -> productService.updateProductPrice(sampleBook.getProductId(), newPriceTooLow, managerId));
    }

    @Test
    void updateProductStock_success() throws SQLException, ValidationException, ResourceNotFoundException {
        when(productDAO.getById(sampleBook.getProductId())).thenReturn(sampleBook);
        // Mock the behavior of productDAO.updateStock to reflect the stock change on the sampleBook object
        // This is important because the service method might return the same object instance or a new one
        // depending on its implementation, but we want to ensure our assertion uses the *updated* state.
        doAnswer(invocation -> {
            String productId = invocation.getArgument(0);
            int newQuantity = invocation.getArgument(1);
            if (sampleBook.getProductId().equals(productId)) {
                sampleBook.setQuantityInStock(newQuantity);
            }
            return null;
        }).when(productDAO).updateStock(anyString(), anyInt());

        int initialStock = sampleBook.getQuantityInStock(); // e.g. 10
        int quantityChange = 5;
        Product updatedProduct = productService.updateProductStock(sampleBook.getProductId(), quantityChange);

        assertNotNull(updatedProduct);
        // The expected stock is the initial stock plus the change.
        assertEquals(initialStock + quantityChange, updatedProduct.getQuantityInStock());
        // Verify that the DAO was called to update the stock to the new correct value.
        verify(productDAO, times(1)).updateStock(sampleBook.getProductId(), initialStock + quantityChange);
    }

    @Test
    void updateProductStock_negativeResult_throwsValidationException() throws SQLException {
        when(productDAO.getById(sampleBook.getProductId())).thenReturn(sampleBook);
        int quantityChange = -20; // Current stock is 10
        assertThrows(ValidationException.class, () -> productService.updateProductStock(sampleBook.getProductId(), quantityChange));
    }

    // --- Customer Facing Methods (VAT applied) ---
    @Test
    void getProductsForDisplay_success_appliesVAT() throws SQLException {
        List<Product> products = Arrays.asList(sampleBook, sampleCD);
        when(productDAO.getAll()).thenReturn(products);

        SearchResult<Product> result = productService.getProductsForDisplay(1, 10);

        assertNotNull(result);
        assertEquals(2, result.totalResults());
        assertEquals(2, result.results().size());
        // Check VAT application (10%)
        assertEquals(100.0f * 1.1f, result.results().get(0).getPrice(), 0.01f);
        assertEquals(50.0f * 1.1f, result.results().get(1).getPrice(), 0.01f);
    }

    @Test
    void searchProducts_byCategoryAndTerm_appliesVATAndSorts() throws SQLException {
        // Make titles different for filtering
        sampleBook.setTitle("Alpha Book Search");
        sampleCD.setTitle("Beta CD Search");
        sampleDVD.setTitle("Gamma DVD NoMatch"); // Will be filtered out by category

        // Simulate DAO returning products of a specific category
        List<Product> booksInCategory = Arrays.asList(sampleBook, new Book()); // Add another book to test filtering
        ((Book)booksInCategory.get(1)).setTitle("Another Alpha Book");
        ((Book)booksInCategory.get(1)).setPrice(120f); // Different price for sorting
        ((Book)booksInCategory.get(1)).setValueAmount(100f);


        when(productDAO.findByCategory("Book")).thenReturn(booksInCategory);

        // Search for "Alpha" in "Book" category, sort by price ASC
        SearchResult<Product> result = productService.searchProducts("Alpha", "Book", 1, 10, "ASC");

        assertNotNull(result);
        assertEquals(2, result.totalResults()); // Both "Alpha Book Search" and "Another Alpha Book"
        assertEquals(2, result.results().size());

        // Check VAT and Sorting (sampleBook price 100, other book 120)
        // Prices with VAT: 110, 132
        assertEquals(100.0f * 1.1f, result.results().get(0).getPrice(), 0.01f); // sampleBook first due to ASC sort
        assertEquals(120.0f * 1.1f, result.results().get(1).getPrice(), 0.01f);
        assertTrue(result.results().get(0).getTitle().contains("Alpha"));
        assertTrue(result.results().get(1).getTitle().contains("Alpha"));
    }

    @Test
    void getProductDetailsForCustomer_success_appliesVAT() throws SQLException, ResourceNotFoundException {
        when(productDAO.getById(sampleDVD.getProductId())).thenReturn(sampleDVD);

        Product product = productService.getProductDetailsForCustomer(sampleDVD.getProductId());

        assertNotNull(product);
        assertEquals(sampleDVD.getProductId(), product.getProductId());
        assertEquals(75.0f * 1.1f, product.getPrice(), 0.01f); // Check VAT
    }
}

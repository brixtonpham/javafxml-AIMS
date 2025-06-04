package com.aims.core.application.services;

import com.aims.core.entities.Product;
import com.aims.core.entities.Book;
import com.aims.core.entities.CD;
import com.aims.core.entities.DVD;
// import com.aims.core.dtos.ProductDTO; // Consider DTOs for creation/updates
// import com.aims.core.dtos.BookDTO;
// import com.aims.core.dtos.CDDTO;
// import com.aims.core.dtos.DVDDTO;
import com.aims.core.shared.exceptions.ValidationException; // Assuming you have these custom exceptions
import com.aims.core.shared.exceptions.ResourceNotFoundException; // Assuming you have these custom exceptions
import com.aims.core.shared.utils.SearchResult; // Assuming a SearchResult utility class

import java.sql.SQLException;
import java.util.List;

/**
 * Service interface for managing products.
 * This includes CRUD operations, business rule enforcement,
 * and product retrieval for customers.
 */
public interface IProductService {

    // --- Product Management (for Product Managers) ---

    /**
     * Adds a new Book product to the system.
     * Validates data according to business rules (e.g., price constraints).
     * VAT is handled according to system policy (e.g., stored pre-VAT, displayed post-VAT).
     *
     * @param book The Book object to add. (Consider using a BookDTO for creation)
     * @return The created Book, potentially with updated information like an ID.
     * @throws SQLException If a database error occurs.
     * @throws ValidationException If business rules are violated (e.g., invalid price, daily limits).
     */
    Book addBook(Book book) throws SQLException, ValidationException;

    /**
     * Adds a new CD product to the system.
     *
     * @param cd The CD object to add.
     * @return The created CD.
     * @throws SQLException If a database error occurs.
     * @throws ValidationException If business rules are violated.
     */
    CD addCD(CD cd) throws SQLException, ValidationException;

    /**
     * Adds a new DVD product to the system.
     *
     * @param dvd The DVD object to add.
     * @return The created DVD.
     * @throws SQLException If a database error occurs.
     * @throws ValidationException If business rules are violated.
     */
    DVD addDVD(DVD dvd) throws SQLException, ValidationException;

    /**
     * Updates an existing Book product.
     * Validates price constraints and update frequency as per business rules.
     *
     * @param book The Book object with updated details.
     * @return The updated Book.
     * @throws SQLException If a database error occurs.
     * @throws ValidationException If business rules are violated.
     * @throws ResourceNotFoundException If the product to update is not found.
     */
    Book updateBook(Book book) throws SQLException, ValidationException, ResourceNotFoundException;

    /**
     * Updates an existing CD product.
     *
     * @param cd The CD object with updated details.
     * @return The updated CD.
     * @throws SQLException If a database error occurs.
     * @throws ValidationException If business rules are violated.
     * @throws ResourceNotFoundException If the product to update is not found.
     */
    CD updateCD(CD cd) throws SQLException, ValidationException, ResourceNotFoundException;

    /**
     * Updates an existing DVD product.
     *
     * @param dvd The DVD object with updated details.
     * @return The updated DVD.
     * @throws SQLException If a database error occurs.
     * @throws ValidationException If business rules are violated.
     * @throws ResourceNotFoundException If the product to update is not found.
     */
    DVD updateDVD(DVD dvd) throws SQLException, ValidationException, ResourceNotFoundException;

    /**
     * Retrieves a product by its ID. Returns the specific subtype (Book, CD, DVD).
     *
     * @param productId The ID of the product.
     * @return The Product object (Book, CD, or DVD) or null if not found.
     * @throws SQLException If a database error occurs.
     */
    Product getProductById(String productId) throws SQLException;

    /**
     * Deletes a product by its ID.
     * Handles constraints (e.g., product in existing orders cannot be deleted as per DB schema).
     * Checks daily deletion limits for product managers.
     *
     * @param productId The ID of the product to delete.
     * @param managerId The ID of the product manager performing the action (for tracking limits).
     * @throws SQLException If a database error occurs.
     * @throws ValidationException if deletion violates business rules (e.g., product in use, daily limit exceeded).
     * @throws ResourceNotFoundException If the product to delete is not found.
     */
    void deleteProduct(String productId, String managerId) throws SQLException, ValidationException, ResourceNotFoundException;

    /**
     * Deletes multiple products by their IDs (up to 10 at once).
     * Checks daily deletion/update limits for product managers.
     *
     * @param productIds List of product IDs to delete.
     * @param managerId The ID of the product manager performing the action.
     * @throws SQLException If a database error occurs.
     * @throws ValidationException if trying to delete more than 10 at once, daily limit exceeded, or any product is in use.
     */
    void deleteProducts(List<String> productIds, String managerId) throws SQLException, ValidationException;

    /**
     * Updates the price of a product.
     * Checks price constraints (30%-150% of value) and daily update limit (twice a day per product).
     *
     * @param productId The ID of the product.
     * @param newPrice The new price (excluding VAT).
     * @param managerId The ID of the product manager performing the action.
     * @return The updated Product.
     * @throws SQLException If a database error occurs.
     * @throws ValidationException If business rules regarding price or update frequency are violated.
     * @throws ResourceNotFoundException If the product is not found.
     */
    Product updateProductPrice(String productId, float newPrice, String managerId) throws SQLException, ValidationException, ResourceNotFoundException;

    /**
     * Updates the stock quantity of a product. Can be positive or negative (for sales or returns).
     *
     * @param productId The ID of the product.
     * @param quantityChange The change in quantity. Positive for adding stock, negative for reducing.
     * @return The updated Product.
     * @throws SQLException If a database error occurs.
     * @throws ValidationException if new stock would be negative (unless specifically allowed for backorders).
     * @throws ResourceNotFoundException If the product is not found.
     */
    Product updateProductStock(String productId, int quantityChange) throws SQLException, ValidationException, ResourceNotFoundException;


    // --- Product Viewing/Searching (for Customers) ---

    /**
     * Gets a list of products for display, potentially random, paginated.
     * Prices returned should be inclusive of 10% VAT for customer display.
     *
     * @param pageNumber The page number for pagination (1-indexed).
     * @param pageSize The number of products per page (e.g., 20).
     * @return A SearchResult containing a list of products (with VAT-inclusive prices) and pagination info.
     * @throws SQLException If a database error occurs.
     */
    SearchResult<Product> getProductsForDisplay(int pageNumber, int pageSize) throws SQLException;

    /**
     * Searches products based on various attributes (title, category, etc.).
     * Results are paginated. Prices returned should be inclusive of 10% VAT.
     *
     * @param searchTerm The term to search for (can be null or empty).
     * @param category Filter by category (can be null or empty).
     * @param pageNumber The page number for pagination (1-indexed).
     * @param pageSize The number of products per page.
     * @param sortByPrice "ASC" for ascending, "DESC" for descending, or null for default sorting.
     * @return A SearchResult of matching products (with VAT-inclusive prices).
     * @throws SQLException If a database error occurs.
     */
    SearchResult<Product> searchProducts(String searchTerm, String category, int pageNumber, int pageSize, String sortByPrice) throws SQLException;

    /**
     * Retrieves detailed information for a single product, including subtype details.
     * The price returned should be inclusive of 10% VAT for customer display.
     *
     * @param productId The ID of the product.
     * @return The Product with details (e.g., Book with authors, CD with artists), with VAT-inclusive price.
     * @throws SQLException If a database error occurs.
     * @throws ResourceNotFoundException If the product is not found.
     */
    Product getProductDetailsForCustomer(String productId) throws SQLException, ResourceNotFoundException;

    /**
     * Advanced search for products with comprehensive filtering and sorting options.
     * Prices returned should be inclusive of 10% VAT for customer display.
     *
     * @param keyword The search keyword (searches across title, description, category, and subtype-specific fields)
     * @param category The category filter (null or empty for all categories)
     * @param sortBy The field to sort by (title, price, category, entryDate, quantity)
     * @param sortOrder The sort order (ASC or DESC)
     * @param pageNumber The page number (1-based)
     * @param pageSize The number of items per page
     * @return A SearchResult containing matching products with VAT-inclusive prices and pagination info
     * @throws SQLException If a database error occurs.
     */
    SearchResult<Product> advancedSearchProducts(String keyword, String category, String sortBy, String sortOrder, int pageNumber, int pageSize) throws SQLException;

    /**
     * Gets all available product categories.
     * @return A list of all categories
     * @throws SQLException If a database error occurs.
     */
    List<String> getAllCategories() throws SQLException;

    // --- Helper or Internal methods (if any, might not be in interface) ---
    // float calculatePriceWithVAT(float priceExclVAT);
}
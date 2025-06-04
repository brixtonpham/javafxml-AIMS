package com.aims.core.infrastructure.database.dao;

import com.aims.core.entities.Product;
import com.aims.core.entities.Book;
import com.aims.core.entities.CD;
import com.aims.core.entities.DVD;

import java.sql.SQLException;
import java.util.List;

public interface IProductDAO {

    /**
     * Retrieves a Product (including its specific subtype data) from the database by its ID.
     *
     * @param productId The ID of the product to retrieve.
     * @return The Product object if found, otherwise null.
     * @throws SQLException If a database access error occurs.
     */
    Product getById(String productId) throws SQLException;

    /**
     * Retrieves all Products from the database.
     * Note: For simplicity, this might initially return only base Product details.
     * A more advanced version would fetch subtype details as well.
     *
     * @return A list of all Product objects.
     * @throws SQLException If a database access error occurs.
     */
    List<Product> getAll() throws SQLException;

    /**
     * Adds a new base Product to the database.
     * This method should be called first before adding subtype details.
     *
     * @param product The Product object to add.
     * @throws SQLException If a database access error occurs.
     */
    void addBaseProduct(Product product) throws SQLException;

    /**
     * Adds specific Book details to the database for an existing Product.
     *
     * @param book The Book object containing subtype details.
     * @throws SQLException If a database access error occurs.
     */
    void addBookDetails(Book book) throws SQLException;

    /**
     * Adds specific CD details to the database for an existing Product.
     *
     * @param cd The CD object containing subtype details.
     * @throws SQLException If a database access error occurs.
     */
    void addCDDetails(CD cd) throws SQLException;

    /**
     * Adds specific DVD details to the database for an existing Product.
     *
     * @param dvd The DVD object containing subtype details.
     * @throws SQLException If a database access error occurs.
     */
    void addDVDDetails(DVD dvd) throws SQLException;

    /**
     * Updates an existing Product's base information in the database.
     *
     * @param product The Product object with updated information.
     * @throws SQLException If a database access error occurs.
     */
    void updateBaseProduct(Product product) throws SQLException;

    /**
     * Updates specific Book details in the database.
     *
     * @param book The Book object with updated subtype information.
     * @throws SQLException If a database access error occurs.
     */
    void updateBookDetails(Book book) throws SQLException;

    /**
     * Updates specific CD details in the database.
     *
     * @param cd The CD object with updated subtype information.
     * @throws SQLException If a database access error occurs.
     */
    void updateCDDetails(CD cd) throws SQLException;

    /**
     * Updates specific DVD details in the database.
     *
     * @param dvd The DVD object with updated subtype information.
     * @throws SQLException If a database access error occurs.
     */
    void updateDVDDetails(DVD dvd) throws SQLException;


    /**
     * Deletes a Product (and its associated subtype data due to CASCADE ON DELETE) from the database by its ID.
     *
     * @param productId The ID of the product to delete.
     * @throws SQLException If a database access error occurs.
     */
    void delete(String productId) throws SQLException;

    /**
     * Finds products by their title.
     *
     * @param title The title (or part of it) to search for.
     * @return A list of matching Product objects.
     * @throws SQLException If a database access error occurs.
     */
    List<Product> findByTitle(String title) throws SQLException;

    /**
     * Finds products by their category.
     *
     * @param category The category to search for.
     * @return A list of matching Product objects.
     * @throws SQLException If a database access error occurs.
     */
    List<Product> findByCategory(String category) throws SQLException;

    /**
     * Updates the stock quantity of a product.
     * @param productId The ID of the product.
     * @param newQuantity The new stock quantity.
     * @throws SQLException If a database access error occurs.
     */
    void updateStock(String productId, int newQuantity) throws SQLException;

    /**
     * Advanced search for products with filtering, sorting, and pagination.
     * @param keyword The search keyword (searches across title, description, category, and subtype-specific fields)
     * @param category The category filter (null or empty for all categories)
     * @param sortBy The field to sort by (title, price, category, entryDate, quantity)
     * @param sortOrder The sort order (ASC or DESC)
     * @param page The page number (1-based)
     * @param pageSize The number of items per page
     * @return A list of matching Product objects with full subtype details
     * @throws SQLException If a database access error occurs.
     */
    List<Product> searchProducts(String keyword, String category, String sortBy, String sortOrder, int page, int pageSize) throws SQLException;

    /**
     * Gets the total count of products matching the search criteria.
     * @param keyword The search keyword
     * @param category The category filter
     * @return The total count of matching products
     * @throws SQLException If a database access error occurs.
     */
    int getSearchResultsCount(String keyword, String category) throws SQLException;

    /**
     * Gets all distinct categories from the products.
     * @return A list of all available categories
     * @throws SQLException If a database access error occurs.
     */
    List<String> getAllCategories() throws SQLException;

}
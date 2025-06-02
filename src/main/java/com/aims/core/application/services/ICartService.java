package com.aims.core.application.services;

import com.aims.core.entities.Cart;
import com.aims.core.shared.exceptions.ValidationException;
import com.aims.core.shared.exceptions.ResourceNotFoundException;
import com.aims.core.shared.exceptions.InventoryException;

import java.sql.SQLException;
// Potentially DTOs for returning cart information to decouple from entities if complex
// import com.aims.core.dtos.CartDTO;
// import com.aims.core.dtos.CartItemDTO;

/**
 * Service interface for managing shopping carts.
 * This includes adding, removing, and updating items in the cart,
 * retrieving cart details, and ensuring inventory availability.
 */
public interface ICartService {

    /**
     * Retrieves the current cart for a given session ID.
     * If no cart exists for the session, a new one might be created or null returned,
     * depending on application policy.
     * This should include cart items and potentially warnings about stock levels.
     * The total price displayed should be exclusive of VAT. [cite: 325]
     *
     * @param cartSessionId The unique session identifier for the cart.
     * @return The Cart object, which includes items and calculated totals (excluding VAT).
     * @throws SQLException If a database error occurs.
     */
    Cart getCart(String cartSessionId) throws SQLException;

    /**
     * Adds a product to the specified cart or updates its quantity if it already exists.
     * Checks product availability before adding. [cite: 326]
     *
     * @param cartSessionId The session ID of the cart.
     * @param productId The ID of the product to add.
     * @param quantity The quantity of the product to add.
     * @return The updated Cart.
     * @throws SQLException If a database error occurs.
     * @throws ResourceNotFoundException If the product is not found.
     * @throws ValidationException If the quantity is invalid or product stock is insufficient. [cite: 326]
     * @throws InventoryException If there is an inventory-related issue (e.g., product not available in the requested quantity).
     */
    Cart addItemToCart(String cartSessionId, String productId, int quantity) throws SQLException, ResourceNotFoundException, ValidationException, InventoryException;

    /**
     * Removes a product completely from the specified cart. [cite: 327]
     *
     * @param cartSessionId The session ID of the cart.
     * @param productId The ID of the product to remove.
     * @return The updated Cart.
     * @throws SQLException If a database error occurs.
     * @throws ResourceNotFoundException If the cart or product in cart is not found.
     */
    Cart removeItemFromCart(String cartSessionId, String productId) throws SQLException, ResourceNotFoundException;

    /**
     * Updates the quantity of an existing product in the cart. [cite: 327]
     * If quantity is 0, the item might be removed based on business rules.
     * Checks product availability for the new quantity. [cite: 326]
     *
     * @param cartSessionId The session ID of the cart.
     * @param productId The ID of the product to update.
     * @param quantity The new quantity for the product. Must be non-negative.
     * @return The updated Cart.
     * @throws SQLException If a database error occurs.
     * @throws ResourceNotFoundException If the cart or product in cart is not found.
     * @throws ValidationException If the quantity is invalid or product stock is insufficient for the change. [cite: 326]
     * @throws InventoryException If there is an inventory-related issue (e.g., product not available in the requested quantity).
     */
    Cart updateItemQuantity(String cartSessionId, String productId, int quantity) throws SQLException, ResourceNotFoundException, ValidationException, InventoryException;

    /**
     * Clears all items from the specified cart. [cite: 328]
     * This is typically called after a successful order placement.
     *
     * @param cartSessionId The session ID of the cart to clear.
     * @return The emptied Cart.
     * @throws SQLException If a database error occurs.
     * @throws ResourceNotFoundException If the cart is not found.
     */
    Cart clearCart(String cartSessionId) throws SQLException, ResourceNotFoundException;

    /**
     * Associates an existing guest cart with a user account, typically after login.
     *
     * @param cartSessionId The session ID of the guest cart.
     * @param userId The ID of the user to associate the cart with.
     * @return The updated Cart, now associated with the user.
     * @throws SQLException If a database error occurs.
     * @throws ResourceNotFoundException If the cart or user is not found.
     * @throws ValidationException If the cart is already associated with another user or other conflicts.
     */
    Cart associateCartWithUser(String cartSessionId, String userId) throws SQLException, ResourceNotFoundException, ValidationException;

    /**
     * Creates a new cart session, potentially for a guest or a new user session.
     *
     * @param userId The ID of the user if known (can be null for a guest).
     * @return The newly created Cart.
     * @throws SQLException If a database error occurs.
     */
    Cart createNewCart(String userId) throws SQLException;

    /**
     * Calculates the total price of products in the cart, excluding VAT. [cite: 325]
     * Also checks and provides information about inventory shortages for items in the cart. [cite: 326]
     *
     * @param cart The cart for which to calculate details.
     * @return A data structure (e.g., a CartDTO or the Cart entity itself enhanced with this info)
     * containing the total price (excl. VAT) and list of stock insufficiencies.
     * @throws SQLException If database error occurs when fetching product prices or stock.
     */
    // This logic might be embedded within getCart or be a separate utility if Cart is a rich domain object.
    // For an interface, we'd expect the returned Cart from getCart to already have this.
    // However, explicitly defining it might be useful for re-calculation without full cart retrieval.
    // Cart recalculateCartDetails(Cart cart) throws SQLException;
}
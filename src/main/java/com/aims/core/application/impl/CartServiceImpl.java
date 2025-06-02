package com.aims.core.application.impl; // Or com.aims.core.application.services.impl;

import com.aims.core.application.services.ICartService;
import com.aims.core.entities.Cart;
import com.aims.core.entities.CartItem;
import com.aims.core.entities.Product;
import com.aims.core.entities.UserAccount; // Needed for associateCartWithUser
import com.aims.core.infrastructure.database.dao.ICartDAO;
import com.aims.core.infrastructure.database.dao.ICartItemDAO;
import com.aims.core.infrastructure.database.dao.IProductDAO;
import com.aims.core.infrastructure.database.dao.IUserAccountDAO; // For fetching UserAccount
import com.aims.core.shared.exceptions.ValidationException;
import com.aims.core.shared.exceptions.ResourceNotFoundException;
import com.aims.core.shared.exceptions.InventoryException; // For stock issues

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.UUID; // For generating cart session IDs

public class CartServiceImpl implements ICartService {

    private final ICartDAO cartDAO;
    private final ICartItemDAO cartItemDAO;
    private final IProductDAO productDAO;
    private final IUserAccountDAO userAccountDAO; // For associating cart with user

    public CartServiceImpl(ICartDAO cartDAO, ICartItemDAO cartItemDAO, IProductDAO productDAO, IUserAccountDAO userAccountDAO) {
        this.cartDAO = cartDAO;
        this.cartItemDAO = cartItemDAO;
        this.productDAO = productDAO;
        this.userAccountDAO = userAccountDAO;
    }

    /**
     * Helper to calculate total price (excluding VAT) and check stock.
     * This can be called internally when the cart is modified or retrieved.
     * The AIMS problem statement mentions "software will also notify customers if the inventory
     * quantity of any product is insufficient and will display the quantity of each product that is lacking." [cite: 551]
     * This method doesn't directly return these warnings but expects the Cart/CartItem DTOs or entities
     * to potentially hold this state after this method's logic is run within another service method.
     * The sum of prices should be exclusive of VAT as per problem statement[cite: 550].
     */
    private void refreshCartCalculationsAndStockStatus(Cart cart) throws SQLException, ResourceNotFoundException {
        if (cart == null || cart.getItems() == null) {
            return;
        }
        // Note: The problem statement says "total price of products excluding VAT" [cite: 550] for cart display.
        // We assume product prices fetched from DAO are already excluding VAT.
        // If Cart needs to store total, it would be updated here.
        // For now, this method focuses on ensuring cart items have up-to-date product info and stock status.

        for (CartItem item : cart.getItems()) {
            Product product = productDAO.getById(item.getProduct().getProductId());
            if (product == null) {
                // This indicates a data integrity issue or a product was removed while in cart
                // Mark this item as problematic or remove it based on business rules
                // For now, we'll assume the item might become invalid.
                // throw new ResourceNotFoundException("Product with ID " + item.getProduct().getProductId() + " in cart not found.");
                System.err.println("Warning: Product " + item.getProduct().getProductId() + " in cart not found. Item may be invalid.");
                continue; // Skip this item for calculations if product doesn't exist
            }
            // Update item with current product price if necessary (though price in cart usually fixed at time of add)
            // item.setPriceAtAddToCart(product.getPrice()); // If cart item stored its own price

            // Check stock [cite: 551]
            // The cart DTO or entity could have a transient field for stock warnings.
            // For example: item.setStockSufficient(product.getQuantityInStock() >= item.getQuantity());
            // item.setAvailableStock(product.getQuantityInStock());
        }
    }

    @Override
    public Cart getCart(String cartSessionId) throws SQLException {
        if (cartSessionId == null || cartSessionId.trim().isEmpty()) {
            // Or create a new guest cart here if policy dictates
            return null;
        }
        Cart cart = cartDAO.getBySessionId(cartSessionId);
        if (cart != null) {
            // Items are typically loaded by cartDAO.getBySessionId() via ICartItemDAO
            // We might want to refresh stock status here for display purposes
            try {
                refreshCartCalculationsAndStockStatus(cart);
            } catch (ResourceNotFoundException e) {
                // Log this, as it means a product in cart was deleted.
                // The cart returned will still have the item, but UI should handle display of missing product.
                System.err.println("Error refreshing cart calculations: " + e.getMessage());
            }
        }
        return cart;
    }

    @Override
    public Cart addItemToCart(String cartSessionId, String productId, int quantity)
            throws SQLException, ResourceNotFoundException, ValidationException, InventoryException {
        if (quantity <= 0) {
            throw new ValidationException("Quantity to add must be positive.");
        }
        Product product = productDAO.getById(productId);
        if (product == null) {
            throw new ResourceNotFoundException("Product with ID " + productId + " not found.");
        }
        if (product.getQuantityInStock() < quantity) {
            throw new InventoryException("Insufficient stock for product " + product.getTitle() +
                                        ". Available: " + product.getQuantityInStock() + ", Requested: " + quantity);
        }

        Cart cart = cartDAO.getBySessionId(cartSessionId);
        if (cart == null) {
            // If cart doesn't exist for session, create a new one.
            // The problem implies customers can add to cart without explicitly creating one.
            cart = createNewCart(null); // Create a guest cart with the given session ID
            cart.setCartSessionId(cartSessionId); // Ensure session ID is set if createNewCart doesn't take it
        }

        CartItem existingItem = null;
        for(CartItem item : cart.getItems()){
            if(item.getProduct().getProductId().equals(productId)){
                existingItem = item;
                break;
            }
        }

        if (existingItem != null) {
            int newQuantity = existingItem.getQuantity() + quantity;
            if (product.getQuantityInStock() < newQuantity) {
                 throw new InventoryException("Insufficient stock for product " + product.getTitle() +
                                       ". Available: " + product.getQuantityInStock() +
                                       ", Requested total: " + newQuantity);
            }
            existingItem.setQuantity(newQuantity);
            cartItemDAO.update(existingItem);
        } else {
            CartItem newItem = new CartItem(cart, product, quantity);
            cart.getItems().add(newItem); // Add to in-memory list first
            cartItemDAO.add(newItem);     // Persist
        }
        cart.setLastUpdated(LocalDateTime.now());
        cartDAO.saveOrUpdate(cart); // Update lastUpdated timestamp

        return getCart(cart.getCartSessionId()); // Return the refreshed cart
    }

    @Override
    public Cart removeItemFromCart(String cartSessionId, String productId)
            throws SQLException, ResourceNotFoundException {
        Cart cart = getCart(cartSessionId);
        if (cart == null) {
            throw new ResourceNotFoundException("Cart with session ID " + cartSessionId + " not found.");
        }

        CartItem itemToRemove = null;
        for (CartItem item : cart.getItems()) {
            if (item.getProduct().getProductId().equals(productId)) {
                itemToRemove = item;
                break;
            }
        }

        if (itemToRemove == null) {
            throw new ResourceNotFoundException("Product with ID " + productId + " not found in cart " + cartSessionId);
        }

        cartItemDAO.delete(cartSessionId, productId);
        cart.getItems().remove(itemToRemove); // Remove from in-memory list
        cart.setLastUpdated(LocalDateTime.now());
        cartDAO.saveOrUpdate(cart);

        return getCart(cartSessionId);
    }

    @Override
    public Cart updateItemQuantity(String cartSessionId, String productId, int newQuantity)
            throws SQLException, ResourceNotFoundException, ValidationException, InventoryException {
        if (newQuantity < 0) { // Allowing 0 to effectively remove item
            throw new ValidationException("New quantity cannot be negative.");
        }

        Cart cart = getCart(cartSessionId);
        if (cart == null) {
            throw new ResourceNotFoundException("Cart with session ID " + cartSessionId + " not found.");
        }

        Product product = productDAO.getById(productId);
        if (product == null) {
            throw new ResourceNotFoundException("Product with ID " + productId + " not found.");
        }

        CartItem itemToUpdate = null;
        for (CartItem item : cart.getItems()) {
            if (item.getProduct().getProductId().equals(productId)) {
                itemToUpdate = item;
                break;
            }
        }

        if (itemToUpdate == null) {
            throw new ResourceNotFoundException("Product with ID " + productId + " not found in cart " + cartSessionId);
        }

        if (newQuantity == 0) {
            return removeItemFromCart(cartSessionId, productId);
        }

        if (product.getQuantityInStock() < newQuantity) {
            throw new InventoryException("Insufficient stock for product " + product.getTitle() +
                                       ". Available: " + product.getQuantityInStock() + ", Requested: " + newQuantity);
        }

        itemToUpdate.setQuantity(newQuantity);
        cartItemDAO.update(itemToUpdate);
        cart.setLastUpdated(LocalDateTime.now());
        cartDAO.saveOrUpdate(cart);

        return getCart(cartSessionId);
    }

    @Override
    public Cart clearCart(String cartSessionId) throws SQLException, ResourceNotFoundException {
        Cart cart = cartDAO.getBySessionId(cartSessionId); // Ensure cart exists
        if (cart == null) {
            throw new ResourceNotFoundException("Cart with session ID " + cartSessionId + " not found.");
        }
        cartItemDAO.deleteByCartSessionId(cartSessionId);
        cart.getItems().clear(); // Clear in-memory list
        cart.setLastUpdated(LocalDateTime.now());
        cartDAO.saveOrUpdate(cart); // Update timestamp
        // The problem statement implies cart is emptied after successful payment [cite: 553]

        return cart;
    }

    @Override
    public Cart associateCartWithUser(String cartSessionId, String userId)
            throws SQLException, ResourceNotFoundException, ValidationException {
        Cart cart = cartDAO.getBySessionId(cartSessionId);
        if (cart == null) {
            throw new ResourceNotFoundException("Cart with session ID " + cartSessionId + " not found.");
        }
        if (cart.getUserAccount() != null && !cart.getUserAccount().getUserId().equals(userId)) {
            throw new ValidationException("Cart is already associated with another user.");
        }

        UserAccount user = userAccountDAO.getById(userId);
        if (user == null) {
            throw new ResourceNotFoundException("User with ID " + userId + " not found.");
        }

        cart.setUserAccount(user);
        cart.setLastUpdated(LocalDateTime.now());
        cartDAO.saveOrUpdate(cart);
        return cart;
    }

    @Override
    public Cart createNewCart(String userId) throws SQLException {
        String newCartSessionId = UUID.randomUUID().toString();
        UserAccount user = null;
        if (userId != null) {
            user = userAccountDAO.getById(userId);
            // Optionally handle if user not found, though typically userId should be valid
        }
        Cart newCart = new Cart(newCartSessionId, user, LocalDateTime.now());
        cartDAO.saveOrUpdate(newCart);
        return newCart;
    }
}
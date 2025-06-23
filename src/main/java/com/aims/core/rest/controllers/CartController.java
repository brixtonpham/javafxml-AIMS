package com.aims.core.rest.controllers;

import com.aims.core.application.services.ICartService;
import com.aims.core.entities.Cart;
import com.aims.core.shared.ServiceFactory;
import com.aims.core.shared.exceptions.ResourceNotFoundException;
import com.aims.core.shared.exceptions.ValidationException;
import com.aims.core.shared.exceptions.InventoryException;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.sql.SQLException;
import java.util.Map;
import java.util.HashMap;

/**
 * REST controller for cart management operations
 */
@RestController
@RequestMapping("/api/cart")
@CrossOrigin(origins = "http://localhost:3000")
public class CartController extends BaseController {

    private final ICartService cartService;

    public CartController() {
        this.cartService = ServiceFactory.getCartService();
    }

    /**
     * Get cart by session ID
     */
    @GetMapping("/{sessionId}")
    public ResponseEntity<ApiResponse<Cart>> getCart(@PathVariable String sessionId) {
        try {
            Cart cart = cartService.getCart(sessionId);
            if (cart == null) {
                // Create new cart if none exists
                cart = cartService.createNewCart(null);
            }
            return success(cart, "Cart retrieved successfully");
        } catch (SQLException e) {
            return error("Database error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            return error("An error occurred while retrieving cart: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Add item to cart
     */
    @PostMapping("/{sessionId}/items")
    public ResponseEntity<ApiResponse<Cart>> addItemToCart(
            @PathVariable String sessionId,
            @RequestBody AddItemRequest request) {
        try {
            Cart cart = cartService.addItemToCart(sessionId, request.getProductId(), request.getQuantity());
            return success(cart, "Item added to cart successfully");
        } catch (ResourceNotFoundException e) {
            return error("Product not found: " + e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (ValidationException e) {
            Map<String, String> errors = new HashMap<>();
            errors.put("validation", e.getMessage());
            return validationError("Invalid request", errors);
        } catch (InventoryException e) {
            return error("Inventory error: " + e.getMessage(), HttpStatus.CONFLICT);
        } catch (SQLException e) {
            return error("Database error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            return error("An error occurred while adding item to cart: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Update item quantity in cart
     */
    @PutMapping("/{sessionId}/items/{productId}")
    public ResponseEntity<ApiResponse<Cart>> updateItemQuantity(
            @PathVariable String sessionId,
            @PathVariable String productId,
            @RequestBody UpdateQuantityRequest request) {
        try {
            Cart cart = cartService.updateItemQuantity(sessionId, productId, request.getQuantity());
            return success(cart, "Item quantity updated successfully");
        } catch (ResourceNotFoundException e) {
            return error("Cart or item not found: " + e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (ValidationException e) {
            Map<String, String> errors = new HashMap<>();
            errors.put("validation", e.getMessage());
            return validationError("Invalid request", errors);
        } catch (InventoryException e) {
            return error("Inventory error: " + e.getMessage(), HttpStatus.CONFLICT);
        } catch (SQLException e) {
            return error("Database error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            return error("An error occurred while updating item quantity: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Remove item from cart
     */
    @DeleteMapping("/{sessionId}/items/{productId}")
    public ResponseEntity<ApiResponse<Cart>> removeItemFromCart(
            @PathVariable String sessionId,
            @PathVariable String productId) {
        try {
            Cart cart = cartService.removeItemFromCart(sessionId, productId);
            return success(cart, "Item removed from cart successfully");
        } catch (ResourceNotFoundException e) {
            return error("Cart or item not found: " + e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (SQLException e) {
            return error("Database error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            return error("An error occurred while removing item from cart: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Clear all items from cart
     */
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<ApiResponse<Cart>> clearCart(@PathVariable String sessionId) {
        try {
            Cart cart = cartService.clearCart(sessionId);
            return success(cart, "Cart cleared successfully");
        } catch (ResourceNotFoundException e) {
            return error("Cart not found: " + e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (SQLException e) {
            return error("Database error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            return error("An error occurred while clearing cart: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Associate cart with user (after login)
     */
    @PostMapping("/{sessionId}/associate")
    public ResponseEntity<ApiResponse<Cart>> associateCartWithUser(
            @PathVariable String sessionId,
            @RequestBody AssociateUserRequest request) {
        try {
            Cart cart = cartService.associateCartWithUser(sessionId, request.getUserId());
            return success(cart, "Cart associated with user successfully");
        } catch (ResourceNotFoundException e) {
            return error("Cart or user not found: " + e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (ValidationException e) {
            Map<String, String> errors = new HashMap<>();
            errors.put("validation", e.getMessage());
            return validationError("Invalid request", errors);
        } catch (SQLException e) {
            return error("Database error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            return error("An error occurred while associating cart with user: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Create new cart
     */
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<Cart>> createNewCart(@RequestBody(required = false) CreateCartRequest request) {
        try {
            String userId = request != null ? request.getUserId() : null;
            Cart cart = cartService.createNewCart(userId);
            return success(cart, "New cart created successfully");
        } catch (SQLException e) {
            return error("Database error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            return error("An error occurred while creating cart: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Request DTOs
    public static class AddItemRequest {
        private String productId;
        private int quantity;

        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }

        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
    }

    public static class UpdateQuantityRequest {
        private int quantity;

        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
    }

    public static class AssociateUserRequest {
        private String userId;

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
    }

    public static class CreateCartRequest {
        private String userId;

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
    }
}
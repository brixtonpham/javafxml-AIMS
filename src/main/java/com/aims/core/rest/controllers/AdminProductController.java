package com.aims.core.rest.controllers;

import com.aims.core.application.services.IProductService;
import com.aims.core.entities.Product;
import com.aims.core.entities.Book;
import com.aims.core.entities.CD;
import com.aims.core.entities.DVD;
import com.aims.core.entities.LP;
import com.aims.core.shared.ServiceFactory;
import com.aims.core.shared.exceptions.ResourceNotFoundException;
import com.aims.core.shared.exceptions.ValidationException;
import com.aims.core.shared.exceptions.InventoryException;
import com.aims.core.shared.utils.SearchResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * REST controller for administrative product management operations
 * Used by administrators and product managers
 */
@RestController
@RequestMapping("/api/admin/products")
@CrossOrigin(origins = "http://localhost:3000")
public class AdminProductController extends BaseController {

    private final IProductService productService;

    public AdminProductController() {
        this.productService = ServiceFactory.getProductService();
    }

    /**
     * Add a new Book product
     */
    @PostMapping("/books")
    public ResponseEntity<ApiResponse<Book>> addBook(@RequestBody Book book) {
        try {
            Book createdBook = productService.addBook(book);
            return success(createdBook, "Book added successfully");
        } catch (ValidationException e) {
            Map<String, String> errors = new HashMap<>();
            errors.put("validation", e.getMessage());
            return validationError("Book creation failed", errors);
        } catch (SQLException e) {
            return error("Database error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            return error("An error occurred while adding book: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Add a new CD product
     */
    @PostMapping("/cds")
    public ResponseEntity<ApiResponse<CD>> addCD(@RequestBody CD cd) {
        try {
            CD createdCD = productService.addCD(cd);
            return success(createdCD, "CD added successfully");
        } catch (ValidationException e) {
            Map<String, String> errors = new HashMap<>();
            errors.put("validation", e.getMessage());
            return validationError("CD creation failed", errors);
        } catch (SQLException e) {
            return error("Database error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            return error("An error occurred while adding CD: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Add a new DVD product
     */
    @PostMapping("/dvds")
    public ResponseEntity<ApiResponse<DVD>> addDVD(@RequestBody DVD dvd) {
        try {
            DVD createdDVD = productService.addDVD(dvd);
            return success(createdDVD, "DVD added successfully");
        } catch (ValidationException e) {
            Map<String, String> errors = new HashMap<>();
            errors.put("validation", e.getMessage());
            return validationError("DVD creation failed", errors);
        } catch (SQLException e) {
            return error("Database error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            return error("An error occurred while adding DVD: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Add a new LP product
     */
    @PostMapping("/lps")
    public ResponseEntity<ApiResponse<LP>> addLP(@RequestBody LP lp) {
        try {
            LP createdLP = productService.addLP(lp);
            return success(createdLP, "LP added successfully");
        } catch (ValidationException e) {
            Map<String, String> errors = new HashMap<>();
            errors.put("validation", e.getMessage());
            return validationError("LP creation failed", errors);
        } catch (SQLException e) {
            return error("Database error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            return error("An error occurred while adding LP: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Update an existing Book product
     */
    @PutMapping("/books/{productId}")
    public ResponseEntity<ApiResponse<Book>> updateBook(
            @PathVariable String productId,
            @RequestBody Book book) {
        try {
            book.setProductId(productId);
            Book updatedBook = productService.updateBook(book);
            return success(updatedBook, "Book updated successfully");
        } catch (ResourceNotFoundException e) {
            return error("Book not found: " + e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (ValidationException e) {
            Map<String, String> errors = new HashMap<>();
            errors.put("validation", e.getMessage());
            return validationError("Book update failed", errors);
        } catch (SQLException e) {
            return error("Database error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            return error("An error occurred while updating book: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Update an existing CD product
     */
    @PutMapping("/cds/{productId}")
    public ResponseEntity<ApiResponse<CD>> updateCD(
            @PathVariable String productId,
            @RequestBody CD cd) {
        try {
            cd.setProductId(productId);
            CD updatedCD = productService.updateCD(cd);
            return success(updatedCD, "CD updated successfully");
        } catch (ResourceNotFoundException e) {
            return error("CD not found: " + e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (ValidationException e) {
            Map<String, String> errors = new HashMap<>();
            errors.put("validation", e.getMessage());
            return validationError("CD update failed", errors);
        } catch (SQLException e) {
            return error("Database error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            return error("An error occurred while updating CD: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Update an existing DVD product
     */
    @PutMapping("/dvds/{productId}")
    public ResponseEntity<ApiResponse<DVD>> updateDVD(
            @PathVariable String productId,
            @RequestBody DVD dvd) {
        try {
            dvd.setProductId(productId);
            DVD updatedDVD = productService.updateDVD(dvd);
            return success(updatedDVD, "DVD updated successfully");
        } catch (ResourceNotFoundException e) {
            return error("DVD not found: " + e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (ValidationException e) {
            Map<String, String> errors = new HashMap<>();
            errors.put("validation", e.getMessage());
            return validationError("DVD update failed", errors);
        } catch (SQLException e) {
            return error("Database error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            return error("An error occurred while updating DVD: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Update an existing LP product
     */
    @PutMapping("/lps/{productId}")
    public ResponseEntity<ApiResponse<LP>> updateLP(
            @PathVariable String productId,
            @RequestBody LP lp) {
        try {
            lp.setProductId(productId);
            LP updatedLP = productService.updateLP(lp);
            return success(updatedLP, "LP updated successfully");
        } catch (ResourceNotFoundException e) {
            return error("LP not found: " + e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (ValidationException e) {
            Map<String, String> errors = new HashMap<>();
            errors.put("validation", e.getMessage());
            return validationError("LP update failed", errors);
        } catch (SQLException e) {
            return error("Database error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            return error("An error occurred while updating LP: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get product by ID for admin/manager view
     */
    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<Product>> getProductById(@PathVariable String productId) {
        try {
            Product product = productService.getProductById(productId);
            if (product == null) {
                return error("Product not found", HttpStatus.NOT_FOUND);
            }
            return success(product, "Product retrieved successfully");
        } catch (SQLException e) {
            return error("Database error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            return error("An error occurred while retrieving product: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Delete a single product
     */
    @DeleteMapping("/{productId}")
    public ResponseEntity<ApiResponse<String>> deleteProduct(
            @PathVariable String productId,
            @RequestParam String managerId) {
        try {
            productService.deleteProduct(productId, managerId);
            return success("Product deleted", "Product deleted successfully");
        } catch (ResourceNotFoundException e) {
            return error("Product not found: " + e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (ValidationException e) {
            Map<String, String> errors = new HashMap<>();
            errors.put("validation", e.getMessage());
            return validationError("Product deletion failed", errors);
        } catch (SQLException e) {
            return error("Database error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            return error("An error occurred while deleting product: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Delete multiple products (up to 10)
     */
    @PostMapping("/bulk-delete")
    public ResponseEntity<ApiResponse<String>> deleteProducts(@RequestBody BulkDeleteRequest request) {
        try {
            productService.deleteProducts(request.getProductIds(), request.getManagerId());
            return success("Products deleted", "Products deleted successfully");
        } catch (ValidationException e) {
            Map<String, String> errors = new HashMap<>();
            errors.put("validation", e.getMessage());
            return validationError("Bulk deletion failed", errors);
        } catch (SQLException e) {
            return error("Database error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            return error("An error occurred while deleting products: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Update product price
     */
    @PutMapping("/{productId}/price")
    public ResponseEntity<ApiResponse<Product>> updateProductPrice(
            @PathVariable String productId,
            @RequestBody UpdatePriceRequest request) {
        try {
            Product product = productService.updateProductPrice(productId, request.getNewPrice(), request.getManagerId());
            return success(product, "Product price updated successfully");
        } catch (ResourceNotFoundException e) {
            return error("Product not found: " + e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (ValidationException e) {
            Map<String, String> errors = new HashMap<>();
            errors.put("validation", e.getMessage());
            return validationError("Price update failed", errors);
        } catch (SQLException e) {
            return error("Database error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            return error("An error occurred while updating price: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Update product stock
     */
    @PutMapping("/{productId}/stock")
    public ResponseEntity<ApiResponse<Product>> updateProductStock(
            @PathVariable String productId,
            @RequestBody UpdateStockRequest request) {
        try {
            Product product = productService.updateProductStock(productId, request.getQuantityChange());
            return success(product, "Product stock updated successfully");
        } catch (ResourceNotFoundException e) {
            return error("Product not found: " + e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (ValidationException e) {
            Map<String, String> errors = new HashMap<>();
            errors.put("validation", e.getMessage());
            return validationError("Stock update failed", errors);
        } catch (InventoryException e) {
            return error("Inventory error: " + e.getMessage(), HttpStatus.CONFLICT);
        } catch (SQLException e) {
            return error("Database error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            return error("An error occurred while updating stock: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get products for admin display with pagination
     */
    @GetMapping("/list")
    public ResponseEntity<PaginatedApiResponse<Product>> getProductsForAdmin(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {
        try {
            SearchResult<Product> result = productService.getProductsForDisplay(page, limit);
            return paginatedSuccess(result.getItems(), page, limit, result.getTotalItems());
        } catch (SQLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Search products for admin with advanced options
     */
    @GetMapping("/search")
    public ResponseEntity<PaginatedApiResponse<Product>> searchProductsForAdmin(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "title") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortOrder,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {
        try {
            SearchResult<Product> result = productService.advancedSearchProducts(
                keyword, category, sortBy, sortOrder, page, limit);
            return paginatedSuccess(result.getItems(), page, limit, result.getTotalItems());
        } catch (SQLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Search products by type for admin
     */
    @GetMapping("/search-by-type")
    public ResponseEntity<PaginatedApiResponse<Product>> searchByProductTypeForAdmin(
            @RequestParam(required = false) String productType,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "title") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortOrder,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {
        try {
            SearchResult<Product> result = productService.searchByProductType(
                productType, keyword, sortBy, sortOrder, page, limit);
            return paginatedSuccess(result.getItems(), page, limit, result.getTotalItems());
        } catch (SQLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get all product categories
     */
    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<String>>> getAllCategories() {
        try {
            List<String> categories = productService.getAllCategories();
            return success(categories, "Categories retrieved successfully");
        } catch (SQLException e) {
            return error("Database error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            return error("An error occurred while retrieving categories: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get all product types
     */
    @GetMapping("/types")
    public ResponseEntity<ApiResponse<List<String>>> getAllProductTypes() {
        try {
            List<String> types = productService.getAllProductTypes();
            return success(types, "Product types retrieved successfully");
        } catch (SQLException e) {
            return error("Database error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            return error("An error occurred while retrieving product types: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Request DTOs
    public static class BulkDeleteRequest {
        private List<String> productIds;
        private String managerId;

        public List<String> getProductIds() { return productIds; }
        public void setProductIds(List<String> productIds) { this.productIds = productIds; }

        public String getManagerId() { return managerId; }
        public void setManagerId(String managerId) { this.managerId = managerId; }
    }

    public static class UpdatePriceRequest {
        private float newPrice;
        private String managerId;

        public float getNewPrice() { return newPrice; }
        public void setNewPrice(float newPrice) { this.newPrice = newPrice; }

        public String getManagerId() { return managerId; }
        public void setManagerId(String managerId) { this.managerId = managerId; }
    }

    public static class UpdateStockRequest {
        private int quantityChange;

        public int getQuantityChange() { return quantityChange; }
        public void setQuantityChange(int quantityChange) { this.quantityChange = quantityChange; }
    }
}
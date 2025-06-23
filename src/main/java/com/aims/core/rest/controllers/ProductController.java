package com.aims.core.rest.controllers;

import com.aims.core.application.services.IProductService;
import com.aims.core.entities.Product;
import com.aims.core.shared.exceptions.ResourceNotFoundException;
import com.aims.core.shared.exceptions.ValidationException;
import com.aims.core.shared.utils.SearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.List;

/**
 * REST Controller for product endpoints
 */
@RestController
@RequestMapping("/api/products")

public class ProductController extends BaseController {
    
    @Autowired
    private IProductService productService;
    
    /**
     * Get products with pagination and filtering
     */
    @GetMapping
    public ResponseEntity<PaginatedApiResponse<Product>> getProducts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String productType,
            @RequestParam(defaultValue = "entryDate") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortOrder) {
        
        try {
            SearchResult<Product> result = productService.searchProducts(
                keyword, category, productType, sortBy, sortOrder, page, pageSize);
            
            return paginatedSuccess(result.results(), page, pageSize, (int) result.totalResults());
            
        } catch (SQLException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get product by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Product>> getProductById(@PathVariable String id) {
        try {
            Product product = productService.getProductById(id);
            if (product == null) {
                return error("Product not found", org.springframework.http.HttpStatus.NOT_FOUND);
            }
            return success(product, "Product retrieved successfully");
            
        } catch (SQLException e) {
            return error("Database error occurred", org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Search products
     */
    @GetMapping("/search")
    public ResponseEntity<PaginatedApiResponse<Product>> searchProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String productType,
            @RequestParam(defaultValue = "entryDate") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortOrder,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        
        try {
            SearchResult<Product> result = productService.searchProducts(
                keyword, category, productType, sortBy, sortOrder, page, pageSize);
            
            return paginatedSuccess(result.results(), page, pageSize, (int) result.totalResults());
            
        } catch (SQLException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Advanced search products
     */
    @PostMapping("/advanced-search")
    public ResponseEntity<PaginatedApiResponse<Product>> advancedSearch(
            @RequestBody AdvancedSearchRequest request) {
        
        try {
            SearchResult<Product> result = productService.searchProducts(
                request.getKeyword(), 
                request.getCategory(), 
                request.getProductType(),
                request.getSortBy() != null ? request.getSortBy() : "entryDate",
                request.getSortOrder() != null ? request.getSortOrder() : "DESC",
                request.getPage() != null ? request.getPage() : 1,
                request.getPageSize() != null ? request.getPageSize() : 20
            );
            
            return paginatedSuccess(result.results(),
                request.getPage() != null ? request.getPage() : 1,
                request.getPageSize() != null ? request.getPageSize() : 20,
                (int) result.totalResults());
            
        } catch (SQLException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Search products by type
     */
    @GetMapping("/by-type")
    public ResponseEntity<PaginatedApiResponse<Product>> searchByProductType(
            @RequestParam String productType,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "entryDate") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortOrder,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        
        try {
            SearchResult<Product> result = productService.searchProducts(
                keyword, category, productType, sortBy, sortOrder, page, pageSize);
            
            return paginatedSuccess(result.results(), page, pageSize, (int) result.totalResults());
            
        } catch (SQLException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get product categories
     */
    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<String>>> getCategories() {
        try {
            List<String> categories = productService.getAllCategories();
            return success(categories, "Categories retrieved successfully");
            
        } catch (SQLException e) {
            return error("Database error occurred", org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Get product types
     */
    @GetMapping("/types")
    public ResponseEntity<ApiResponse<List<String>>> getProductTypes() {
        try {
            List<String> types = productService.getAllProductTypes();
            return success(types, "Product types retrieved successfully");
            
        } catch (SQLException e) {
            return error("Database error occurred", org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    // Request DTOs
    public static class AdvancedSearchRequest {
        private String keyword;
        private String category;
        private String productType;
        private String sortBy;
        private String sortOrder;
        private Integer page;
        private Integer pageSize;
        
        // Getters and setters
        public String getKeyword() { return keyword; }
        public void setKeyword(String keyword) { this.keyword = keyword; }
        
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        
        public String getProductType() { return productType; }
        public void setProductType(String productType) { this.productType = productType; }
        
        public String getSortBy() { return sortBy; }
        public void setSortBy(String sortBy) { this.sortBy = sortBy; }
        
        public String getSortOrder() { return sortOrder; }
        public void setSortOrder(String sortOrder) { this.sortOrder = sortOrder; }
        
        public Integer getPage() { return page; }
        public void setPage(Integer page) { this.page = page; }
        
        public Integer getPageSize() { return pageSize; }
        public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
    }
}
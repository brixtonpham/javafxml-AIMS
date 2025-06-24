package com.aims.core.rest.controllers;

import com.aims.core.application.services.IProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.List;

/**
 * REST Controller for categories endpoints
 */
@RestController
@RequestMapping("/api/categories")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"}, allowCredentials = "true")
public class CategoryController extends BaseController {
    
    @Autowired
    private IProductService productService;
    
    /**
     * Get all product categories
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<String>>> getAllCategories() {
        try {
            List<String> categories = productService.getAllCategories();
            return success(categories, "Categories retrieved successfully");
            
        } catch (SQLException e) {
            return error("Database error occurred", org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}

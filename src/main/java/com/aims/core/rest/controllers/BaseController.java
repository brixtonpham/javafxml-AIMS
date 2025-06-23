package com.aims.core.rest.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import java.util.HashMap;
import java.util.Map;

/**
 * Base controller providing common functionality for all REST controllers
 */
public abstract class BaseController {
    
    /**
     * Create a success response with data
     */
    protected <T> ResponseEntity<ApiResponse<T>> success(T data) {
        return success(data, "Operation completed successfully");
    }
    
    /**
     * Create a success response with data and message
     */
    protected <T> ResponseEntity<ApiResponse<T>> success(T data, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(true);
        response.setData(data);
        response.setMessage(message);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Create an error response
     */
    protected <T> ResponseEntity<ApiResponse<T>> error(String message, HttpStatus status) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(false);
        response.setMessage(message);
        return ResponseEntity.status(status).body(response);
    }
    
    /**
     * Create an error response with validation errors
     */
    protected <T> ResponseEntity<ApiResponse<T>> validationError(String message, Map<String, String> errors) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(false);
        response.setMessage(message);
        response.setErrors(errors);
        return ResponseEntity.badRequest().body(response);
    }
    
    /**
     * Create a paginated response
     */
    protected <T> ResponseEntity<PaginatedApiResponse<T>> paginatedSuccess(
            java.util.List<T> items, 
            int page, 
            int limit, 
            long total) {
        
        PaginatedApiResponse<T> response = new PaginatedApiResponse<>();
        response.setSuccess(true);
        response.setMessage("Data retrieved successfully");
        response.setItems(items);
        
        PaginationInfo pagination = new PaginationInfo();
        pagination.setPage(page);
        pagination.setLimit(limit);
        pagination.setTotal(total);
        pagination.setPages((int) Math.ceil((double) total / limit));
        pagination.setHasNext(page * limit < total);
        pagination.setHasPrev(page > 1);
        
        response.setPagination(pagination);
        
        return ResponseEntity.ok(response);
    }
    
    // Inner classes for API responses
    public static class ApiResponse<T> {
        private boolean success;
        private T data;
        private String message;
        private Map<String, String> errors;
        
        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public T getData() { return data; }
        public void setData(T data) { this.data = data; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public Map<String, String> getErrors() { return errors; }
        public void setErrors(Map<String, String> errors) { this.errors = errors; }
    }
    
    public static class PaginatedApiResponse<T> {
        private boolean success;
        private java.util.List<T> items;
        private PaginationInfo pagination;
        private String message;
        
        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public java.util.List<T> getItems() { return items; }
        public void setItems(java.util.List<T> items) { this.items = items; }
        
        public PaginationInfo getPagination() { return pagination; }
        public void setPagination(PaginationInfo pagination) { this.pagination = pagination; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
    
    public static class PaginationInfo {
        private int page;
        private int limit;
        private long total;
        private int pages;
        private boolean hasNext;
        private boolean hasPrev;
        
        // Getters and setters
        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }
        
        public int getLimit() { return limit; }
        public void setLimit(int limit) { this.limit = limit; }
        
        public long getTotal() { return total; }
        public void setTotal(long total) { this.total = total; }
        
        public int getPages() { return pages; }
        public void setPages(int pages) { this.pages = pages; }
        
        public boolean isHasNext() { return hasNext; }
        public void setHasNext(boolean hasNext) { this.hasNext = hasNext; }
        
        public boolean isHasPrev() { return hasPrev; }
        public void setHasPrev(boolean hasPrev) { this.hasPrev = hasPrev; }
    }
}
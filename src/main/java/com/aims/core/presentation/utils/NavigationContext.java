package com.aims.core.presentation.utils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * NavigationContext stores navigation state and search context information
 * to enable smart navigation that preserves user context when navigating
 * between screens in the AIMS application.
 */
public class NavigationContext {
    private final String screenPath;
    private final String screenTitle;
    private final Map<String, Object> contextData;
    private final LocalDateTime timestamp;
    
    // Search-specific context
    private String searchTerm;
    private String categoryFilter;
    private String sortBy;
    private int currentPage = 1;
    private int totalPages = 1;
    
    /**
     * Creates a new NavigationContext for the specified screen.
     * 
     * @param screenPath The FXML path of the screen
     * @param screenTitle The display title of the screen
     */
    public NavigationContext(String screenPath, String screenTitle) {
        this.screenPath = screenPath;
        this.screenTitle = screenTitle;
        this.contextData = new HashMap<>();
        this.timestamp = LocalDateTime.now();
    }
    
    /**
     * Builder pattern for adding search context to this navigation entry.
     * 
     * @param searchTerm The search term used
     * @param category The category filter applied
     * @param sort The sort order applied
     * @param page The current page number
     * @param totalPages The total number of pages
     * @return This NavigationContext instance for method chaining
     */
    public NavigationContext withSearchContext(String searchTerm, String category, String sort, int page, int totalPages) {
        this.searchTerm = searchTerm;
        this.categoryFilter = category;
        this.sortBy = sort;
        this.currentPage = Math.max(1, page);
        this.totalPages = Math.max(1, totalPages);
        return this;
    }
    
    /**
     * Adds custom context data to this navigation entry.
     * 
     * @param key The key for the context data
     * @param value The value to store
     * @return This NavigationContext instance for method chaining
     */
    public NavigationContext withContextData(String key, Object value) {
        this.contextData.put(key, value);
        return this;
    }
    
    /**
     * Gets custom context data by key.
     * 
     * @param key The key to look up
     * @return The stored value, or null if not found
     */
    public Object getContextData(String key) {
        return contextData.get(key);
    }
    
    /**
     * Gets custom context data by key with type casting.
     * 
     * @param key The key to look up
     * @param type The expected type of the value
     * @return The stored value cast to the specified type, or null if not found or wrong type
     */
    @SuppressWarnings("unchecked")
    public <T> T getContextData(String key, Class<T> type) {
        Object value = contextData.get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }
    
    /**
     * Checks if this navigation context has search context data.
     * 
     * @return true if search context is available
     */
    public boolean hasSearchContext() {
        return searchTerm != null || categoryFilter != null || sortBy != null;
    }
    
    // Getters
    
    public String getScreenPath() {
        return screenPath;
    }
    
    public String getScreenTitle() {
        return screenTitle;
    }
    
    public Map<String, Object> getContextData() {
        return new HashMap<>(contextData); // Return defensive copy
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public String getSearchTerm() {
        return searchTerm;
    }
    
    public String getCategoryFilter() {
        return categoryFilter;
    }
    
    public String getSortBy() {
        return sortBy;
    }
    
    public int getCurrentPage() {
        return currentPage;
    }
    
    public int getTotalPages() {
        return totalPages;
    }
    
    // Setters for updating context
    
    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
    }
    
    public void setCategoryFilter(String categoryFilter) {
        this.categoryFilter = categoryFilter;
    }
    
    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }
    
    public void setCurrentPage(int currentPage) {
        this.currentPage = Math.max(1, currentPage);
    }
    
    public void setTotalPages(int totalPages) {
        this.totalPages = Math.max(1, totalPages);
    }
    
    @Override
    public String toString() {
        return "NavigationContext{" +
                "screenPath='" + screenPath + '\'' +
                ", screenTitle='" + screenTitle + '\'' +
                ", searchTerm='" + searchTerm + '\'' +
                ", categoryFilter='" + categoryFilter + '\'' +
                ", sortBy='" + sortBy + '\'' +
                ", currentPage=" + currentPage +
                ", totalPages=" + totalPages +
                ", timestamp=" + timestamp +
                '}';
    }
}
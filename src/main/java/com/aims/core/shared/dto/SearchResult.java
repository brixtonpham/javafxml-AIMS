package com.aims.core.shared.dto;

import java.util.List;

/**
 * Generic class for paginated search results
 */
public class SearchResult<T> {
    private List<T> items;
    private int totalItems;
    private int pageNumber;
    private int pageSize;
    private int totalPages;

    public SearchResult(List<T> items, int totalItems, int pageNumber, int pageSize) {
        this.items = items;
        this.totalItems = totalItems;
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        this.totalPages = (int) Math.ceil((double) totalItems / pageSize);
    }

    public List<T> getItems() {
        return items;
    }

    public void setItems(List<T> items) {
        this.items = items;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(int totalItems) {
        this.totalItems = totalItems;
        this.totalPages = (int) Math.ceil((double) totalItems / pageSize);
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
        this.totalPages = (int) Math.ceil((double) totalItems / pageSize);
    }

    public int getTotalPages() {
        return totalPages;
    }

    public boolean hasNext() {
        return pageNumber < totalPages - 1;
    }

    public boolean hasPrevious() {
        return pageNumber > 0;
    }
}
package com.aims.core.shared.utils;

import java.util.List;

/**
 * A generic class to hold paginated search results.
 * @param <T> The type of the items in the results list.
 */
public record SearchResult<T>(
    List<T> results,
    int currentPage,
    int totalPages,
    long totalResults // Total number of items matching the query, not just on this page
) {
    // Constructor, getters are implicitly defined by the record.
    // You can add convenience methods if needed, e.g.,
    // public boolean hasNextPage() { return currentPage < totalPages; }
    // public boolean hasPreviousPage() { return currentPage > 1; }
}
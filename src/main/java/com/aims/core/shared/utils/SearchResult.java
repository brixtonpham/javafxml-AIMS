package com.aims.core.shared.utils;

import java.util.List;

public class SearchResult<T> {
    private List<T> results;
    private int totalCount;

    public SearchResult(List<T> results, int totalCount) {
        this.results = results;
        this.totalCount = totalCount;
    }

    public List<T> getResults() {
        return results;
    }

    public int getTotalCount() {
        return totalCount;
    }
}

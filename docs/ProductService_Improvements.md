# ProductService Improvements Required

## 1. Enhanced Search Method

```java
@Override
public SearchResult<Product> searchProducts(String searchTerm, String category, int pageNumber, int pageSize, String sortByPrice) throws SQLException {
    // Use database-level pagination instead of application-level
    SearchResult<Product> result;
    
    if ((searchTerm != null && !searchTerm.trim().isEmpty()) || (category != null && !category.trim().isEmpty())) {
        // Use enhanced DAO method with database pagination
        result = productDAO.searchProductsPaginated(searchTerm, category, pageNumber, pageSize, sortByPrice);
    } else {
        // Get all products with pagination
        result = productDAO.getAllProductsPaginated(pageNumber, pageSize, sortByPrice);
    }
    
    // Apply VAT to all products in results
    List<Product> productsWithVAT = addVAT(new ArrayList<>(result.results()));
    
    return new SearchResult<>(productsWithVAT, result.currentPage(), result.totalPages(), result.totalResults());
}
```

## 2. Add getAllCategories() Method

```java
@Override
public List<String> getAllCategories() throws SQLException {
    return productDAO.getAllCategories();
}
```

## 3. Advanced Search Features

```java
@Override
public SearchResult<Product> advancedSearch(SearchCriteria criteria) throws SQLException {
    // SearchCriteria would include:
    // - searchTerm, category, priceRange, productType, etc.
    
    List<Product> results = productDAO.advancedSearch(criteria);
    List<Product> productsWithVAT = addVAT(new ArrayList<>(results));
    
    // Apply additional filtering/sorting if needed
    if (criteria.getSortBy() != null) {
        productsWithVAT = applySorting(productsWithVAT, criteria.getSortBy());
    }
    
    // Manual pagination if not done at DAO level
    return paginateResults(productsWithVAT, criteria.getPageNumber(), criteria.getPageSize());
}
```

## 4. Performance Optimizations

```java
@Override
public SearchResult<Product> getProductsForDisplay(int pageNumber, int pageSize) throws SQLException {
    // Use database pagination instead of loading all products
    SearchResult<Product> result = productDAO.getAllProductsPaginated(pageNumber, pageSize, null);
    
    // Apply VAT to results
    List<Product> productsWithVAT = addVAT(new ArrayList<>(result.results()));
    
    return new SearchResult<>(productsWithVAT, result.currentPage(), result.totalPages(), result.totalResults());
}
```

## 5. Caching Strategy (Optional)

```java
@Service
public class ProductServiceImpl implements IProductService {
    
    @Cacheable("categories")
    @Override
    public List<String> getAllCategories() throws SQLException {
        return productDAO.getAllCategories();
    }
    
    @Cacheable(value = "products", key = "#pageNumber + '_' + #pageSize")
    @Override
    public SearchResult<Product> getProductsForDisplay(int pageNumber, int pageSize) throws SQLException {
        // Implementation with caching
    }
}
```

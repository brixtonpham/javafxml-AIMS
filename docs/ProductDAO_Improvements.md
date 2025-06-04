# ProductDAO Improvements Required

## 1. Enhance findByTitle() Method

```java
@Override
public List<Product> findByTitle(String title) throws SQLException {
    List<Product> products = new ArrayList<>();
    String sql = "SELECT * FROM PRODUCT WHERE title LIKE ?";
    
    try (Connection conn = getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, "%" + title + "%");
        
        try (ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                Product baseProduct = mapBaseProduct(rs);
                // Fetch full subtype details using existing getById logic
                Product fullProduct = getById(baseProduct.getProductId());
                if (fullProduct != null) {
                    products.add(fullProduct);
                }
            }
        }
    } catch (SQLException e) {
        SQLiteConnector.printSQLException(e);
        throw e;
    }
    return products;
}
```

## 2. Add Database-Level Pagination

```java
@Override
public SearchResult<Product> findByTitlePaginated(String title, int pageNumber, int pageSize) throws SQLException {
    List<Product> products = new ArrayList<>();
    int offset = (pageNumber - 1) * pageSize;
    
    // Count total results
    String countSql = "SELECT COUNT(*) FROM PRODUCT WHERE title LIKE ?";
    int totalResults = 0;
    
    try (Connection conn = getConnection();
         PreparedStatement countStmt = conn.prepareStatement(countSql)) {
        countStmt.setString(1, "%" + title + "%");
        try (ResultSet countRs = countStmt.executeQuery()) {
            if (countRs.next()) {
                totalResults = countRs.getInt(1);
            }
        }
    }
    
    // Get paginated results
    String sql = "SELECT * FROM PRODUCT WHERE title LIKE ? LIMIT ? OFFSET ?";
    try (Connection conn = getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, "%" + title + "%");
        pstmt.setInt(2, pageSize);
        pstmt.setInt(3, offset);
        
        try (ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                Product baseProduct = mapBaseProduct(rs);
                Product fullProduct = getById(baseProduct.getProductId());
                if (fullProduct != null) {
                    products.add(fullProduct);
                }
            }
        }
    }
    
    int totalPages = (int) Math.ceil((double) totalResults / pageSize);
    return new SearchResult<>(products, pageNumber, totalPages, totalResults);
}
```

## 3. Add Multi-Field Search

```java
@Override
public List<Product> searchMultiField(String searchTerm, String category, String sortBy) throws SQLException {
    StringBuilder sqlBuilder = new StringBuilder("SELECT DISTINCT p.* FROM PRODUCT p ");
    
    // Join with subtype tables for comprehensive search
    sqlBuilder.append("LEFT JOIN BOOK b ON p.productID = b.productID ");
    sqlBuilder.append("LEFT JOIN CD c ON p.productID = c.productID ");
    sqlBuilder.append("LEFT JOIN DVD d ON p.productID = d.productID ");
    sqlBuilder.append("WHERE 1=1 ");
    
    List<Object> parameters = new ArrayList<>();
    
    if (searchTerm != null && !searchTerm.trim().isEmpty()) {
        sqlBuilder.append("AND (p.title LIKE ? OR p.description LIKE ? OR b.authors LIKE ? OR c.artists LIKE ? OR d.director LIKE ?) ");
        String searchPattern = "%" + searchTerm + "%";
        for (int i = 0; i < 5; i++) {
            parameters.add(searchPattern);
        }
    }
    
    if (category != null && !category.trim().isEmpty()) {
        sqlBuilder.append("AND p.category = ? ");
        parameters.add(category);
    }
    
    // Add sorting
    if ("ASC".equalsIgnoreCase(sortBy)) {
        sqlBuilder.append("ORDER BY p.price ASC");
    } else if ("DESC".equalsIgnoreCase(sortBy)) {
        sqlBuilder.append("ORDER BY p.price DESC");
    }
    
    // Implementation continues...
}
```

## 4. Add getAllCategories() Method

```java
@Override
public List<String> getAllCategories() throws SQLException {
    List<String> categories = new ArrayList<>();
    String sql = "SELECT DISTINCT category FROM PRODUCT WHERE category IS NOT NULL ORDER BY category";
    
    try (Connection conn = getConnection();
         Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(sql)) {
        while (rs.next()) {
            categories.add(rs.getString("category"));
        }
    } catch (SQLException e) {
        SQLiteConnector.printSQLException(e);
        throw e;
    }
    
    return categories;
}
```

package com.aims.core.infrastructure.database.dao;

import com.aims.core.entities.Product;
import com.aims.core.entities.Book;
import com.aims.core.entities.CD;
import com.aims.core.entities.DVD;
import com.aims.core.entities.LP;
import com.aims.core.enums.ProductType;
import com.aims.core.infrastructure.database.SQLiteConnector;
import com.aims.core.infrastructure.database.utils.DatabaseSchemaValidator;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ProductDAOImpl implements IProductDAO {

    private Connection getConnection() throws SQLException {
        // Ensure foreign key enforcement is on for each connection if not globally set
        Connection conn = SQLiteConnector.getInstance().getConnection();
        return conn;
    }

    // Helper method to map ResultSet to base Product
    private Product mapBaseProduct(ResultSet rs) throws SQLException {
        Product product = new Product();
        product.setProductId(rs.getString("productID"));
        product.setTitle(rs.getString("title"));
        product.setCategory(rs.getString("category"));
        product.setValueAmount(rs.getFloat("value_amount"));
        product.setPrice(rs.getFloat("price"));
        product.setQuantityInStock(rs.getInt("quantityInStock"));
        product.setDescription(rs.getString("description"));
        product.setImageUrl(rs.getString("imageURL"));
        product.setBarcode(rs.getString("barcode"));
        product.setDimensionsCm(rs.getString("dimensions_cm"));
        product.setWeightKg(rs.getFloat("weight_kg"));
        String entryDateStr = rs.getString("entryDate");
        if (entryDateStr != null) {
            product.setEntryDate(LocalDate.parse(entryDateStr));
        }
        // Ensure productType is always set, even if it's from the base product table directly
        String productTypeStr = rs.getString("productType");
        if (productTypeStr != null) {
            product.setProductType(ProductType.valueOf(productTypeStr));
        }
        // Handle version field for optimistic locking
        try {
            Long version = rs.getLong("version");
            if (!rs.wasNull()) {
                product.setVersion(version);
            } else {
                product.setVersion(0L);
            }
        } catch (SQLException e) {
            // Version column might not exist in older database schemas
            product.setVersion(0L);
        }
        return product;
    }

    // Helper method to copy base properties from a source Product to a target Product
    // This is useful when creating specific subtypes (Book, CD, DVD) after fetching base product info
    private void copyBaseProductProperties(Product source, Product target) {
        target.setProductId(source.getProductId());
        target.setTitle(source.getTitle());
        target.setCategory(source.getCategory());
        target.setValueAmount(source.getValueAmount());
        target.setPrice(source.getPrice());
        target.setQuantityInStock(source.getQuantityInStock());
        target.setDescription(source.getDescription());
        target.setImageUrl(source.getImageUrl());
        target.setBarcode(source.getBarcode());
        target.setDimensionsCm(source.getDimensionsCm());
        target.setWeightKg(source.getWeightKg());
        target.setEntryDate(source.getEntryDate());
        target.setProductType(source.getProductType()); // Ensure product type is also copied
        target.setVersion(source.getVersion()); // Copy version for optimistic locking
    }

    @Override
    public Product getById(String productId) throws SQLException {
        String sqlProduct = "SELECT * FROM PRODUCT WHERE productID = ?";
        Product baseProduct = null;
        Connection conn = getConnection(); // Get connection
        PreparedStatement psProduct = null;
        ResultSet rsProduct = null;

        try {
            psProduct = conn.prepareStatement(sqlProduct);
            psProduct.setString(1, productId);
            rsProduct = psProduct.executeQuery();

            if (rsProduct.next()) {
                baseProduct = mapBaseProduct(rsProduct);
                ProductType type = baseProduct.getProductType();

                // Fetch subtype details based on ProductType
                // These inner try-with-resources for subtype fetching are okay as they will use the same 'conn'
                // and will close their own PreparedStatements and ResultSets.
                switch (type) {
                    case BOOK:
                        String sqlBook = "SELECT * FROM BOOK WHERE productID = ?";
                        try (PreparedStatement psBook = conn.prepareStatement(sqlBook)) {
                            psBook.setString(1, productId);
                            try (ResultSet rsBook = psBook.executeQuery()) {
                                if (rsBook.next()) {
                                    Book book = new Book();
                                    copyBaseProductProperties(baseProduct, book);
                                    book.setAuthors(rsBook.getString("authors"));
                                    book.setCoverType(rsBook.getString("coverType"));
                                    book.setPublisher(rsBook.getString("publisher"));
                                    String pubDateStr = rsBook.getString("publicationDate");
                                    if (pubDateStr != null) book.setPublicationDate(LocalDate.parse(pubDateStr));
                                    book.setNumPages(rsBook.getInt("numPages"));
                                    book.setLanguage(rsBook.getString("language"));
                                    book.setBookGenre(rsBook.getString("book_genre"));
                                    return book;
                                }
                            }
                        }
                        break;
                    case CD:
                        String sqlCD = "SELECT * FROM CD WHERE productID = ?";
                        try (PreparedStatement psCD = conn.prepareStatement(sqlCD)) {
                            psCD.setString(1, productId);
                            try (ResultSet rsCD = psCD.executeQuery()) {
                                if (rsCD.next()) {
                                    CD cd = new CD();
                                    copyBaseProductProperties(baseProduct, cd);
                                    cd.setArtists(rsCD.getString("artists"));
                                    cd.setRecordLabel(rsCD.getString("recordLabel"));
                                    cd.setTracklist(rsCD.getString("tracklist"));
                                    cd.setCdGenre(rsCD.getString("cd_genre"));
                                    String relDateStr = rsCD.getString("releaseDate");
                                    if (relDateStr != null) cd.setReleaseDate(LocalDate.parse(relDateStr));
                                    return cd;
                                }
                            }
                        }
                        break;
                    case DVD:
                        String sqlDVD = "SELECT * FROM DVD WHERE productID = ?";
                        try (PreparedStatement psDVD = conn.prepareStatement(sqlDVD)) {
                            psDVD.setString(1, productId);
                            try (ResultSet rsDVD = psDVD.executeQuery()) {
                                if (rsDVD.next()) {
                                    DVD dvd = new DVD();
                                    copyBaseProductProperties(baseProduct, dvd);
                                    dvd.setDiscType(rsDVD.getString("discType"));
                                    dvd.setDirector(rsDVD.getString("director"));
                                    dvd.setRuntimeMinutes(rsDVD.getInt("runtime_minutes"));
                                    dvd.setStudio(rsDVD.getString("studio"));
                                    dvd.setDvdLanguage(rsDVD.getString("dvd_language"));
                                    dvd.setSubtitles(rsDVD.getString("subtitles"));
                                    String relDateStr = rsDVD.getString("dvd_releaseDate");
                                    if (relDateStr != null) dvd.setDvdReleaseDate(LocalDate.parse(relDateStr));
                                    dvd.setDvdGenre(rsDVD.getString("dvd_genre"));
                                    return dvd;
                                }
                            }
                        }
                        break;
                    case LP:
                        String sqlLP = "SELECT * FROM LP WHERE productID = ?";
                        try (PreparedStatement psLP = conn.prepareStatement(sqlLP)) {
                            psLP.setString(1, productId);
                            try (ResultSet rsLP = psLP.executeQuery()) {
                                if (rsLP.next()) {
                                    LP lp = new LP();
                                    copyBaseProductProperties(baseProduct, lp);
                                    lp.setArtists(rsLP.getString("artists"));
                                    lp.setRecordLabel(rsLP.getString("recordLabel"));
                                    lp.setTracklist(rsLP.getString("tracklist"));
                                    lp.setGenre(rsLP.getString("genre"));
                                    String relDateStr = rsLP.getString("releaseDate");
                                    if (relDateStr != null) lp.setReleaseDate(LocalDate.parse(relDateStr));
                                    return lp;
                                }
                            }
                        }
                        break;
                    default:
                        return baseProduct;
                }
                return baseProduct;
            }
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        } finally {
            // Close ResultSet and PreparedStatement used for the base product, but not the Connection
            if (rsProduct != null) {
                try {
                    rsProduct.close();
                } catch (SQLException e) {
                    SQLiteConnector.printSQLException(e);
                }
            }
            if (psProduct != null) {
                try {
                    psProduct.close();
                } catch (SQLException e) {
                    SQLiteConnector.printSQLException(e);
                }
            }
            // Connection is NOT closed here
        }
        return null;
    }


    @Override
    public List<Product> getAll() throws SQLException {
        List<Product> products = new ArrayList<>();
        // This is a simplified getAll that fetches only base product info.
        // A comprehensive getAll would require joining or multiple queries to fetch subtype details.
        String sql = "SELECT * FROM PRODUCT";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                products.add(mapBaseProduct(rs));
            }
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
        return products;
    }

    @Override
    public void addBaseProduct(Product product) throws SQLException {
        String sql = "INSERT INTO PRODUCT (productID, title, category, value_amount, price, quantityInStock, description, imageURL, barcode, dimensions_cm, weight_kg, entryDate, productType) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, product.getProductId());
            pstmt.setString(2, product.getTitle());
            pstmt.setString(3, product.getCategory());
            pstmt.setFloat(4, product.getValueAmount());
            pstmt.setFloat(5, product.getPrice());
            pstmt.setInt(6, product.getQuantityInStock());
            pstmt.setString(7, product.getDescription());
            pstmt.setString(8, product.getImageUrl());
            pstmt.setString(9, product.getBarcode());
            pstmt.setString(10, product.getDimensionsCm());
            pstmt.setFloat(11, product.getWeightKg());
            pstmt.setString(12, product.getEntryDate() != null ? product.getEntryDate().toString() : null);
            pstmt.setString(13, product.getProductType().name());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
    }

    @Override
    public void addBookDetails(Book book) throws SQLException {
        addBaseProduct(book); // First, add/ensure base product exists
        String sql = "INSERT INTO BOOK (productID, authors, coverType, publisher, publicationDate, numPages, language, book_genre) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, book.getProductId());
            pstmt.setString(2, book.getAuthors());
            pstmt.setString(3, book.getCoverType());
            pstmt.setString(4, book.getPublisher());
            pstmt.setString(5, book.getPublicationDate() != null ? book.getPublicationDate().toString() : null);
            pstmt.setInt(6, book.getNumPages());
            pstmt.setString(7, book.getLanguage());
            pstmt.setString(8, book.getBookGenre());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            // Consider transaction rollback if addBaseProduct succeeded but this failed
            throw e;
        }
    }

    @Override
    public void addCDDetails(CD cd) throws SQLException {
        addBaseProduct(cd);
        String sql = "INSERT INTO CD (productID, artists, recordLabel, tracklist, cd_genre, releaseDate) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, cd.getProductId());
            pstmt.setString(2, cd.getArtists());
            pstmt.setString(3, cd.getRecordLabel());
            pstmt.setString(4, cd.getTracklist());
            pstmt.setString(5, cd.getCdGenre());
            pstmt.setString(6, cd.getReleaseDate() != null ? cd.getReleaseDate().toString() : null);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
    }

    @Override
    public void addDVDDetails(DVD dvd) throws SQLException {
        addBaseProduct(dvd);
        String sql = "INSERT INTO DVD (productID, discType, director, runtime_minutes, studio, dvd_language, subtitles, dvd_releaseDate, dvd_genre) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, dvd.getProductId());
            pstmt.setString(2, dvd.getDiscType());
            pstmt.setString(3, dvd.getDirector());
            pstmt.setInt(4, dvd.getRuntimeMinutes());
            pstmt.setString(5, dvd.getStudio());
            pstmt.setString(6, dvd.getDvdLanguage());
            pstmt.setString(7, dvd.getSubtitles());
            pstmt.setString(8, dvd.getDvdReleaseDate() != null ? dvd.getDvdReleaseDate().toString() : null);
            pstmt.setString(9, dvd.getDvdGenre());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
    }

    @Override
    public void addLPDetails(LP lp) throws SQLException {
        addBaseProduct(lp);
        String sql = "INSERT INTO LP (productID, artists, recordLabel, tracklist, genre, releaseDate) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, lp.getProductId());
            pstmt.setString(2, lp.getArtists());
            pstmt.setString(3, lp.getRecordLabel());
            pstmt.setString(4, lp.getTracklist());
            pstmt.setString(5, lp.getGenre());
            pstmt.setString(6, lp.getReleaseDate() != null ? lp.getReleaseDate().toString() : null);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
    }

    @Override
    public void updateBaseProduct(Product product) throws SQLException {
        String sql = "UPDATE PRODUCT SET title = ?, category = ?, value_amount = ?, price = ?, quantityInStock = ?, description = ?, imageURL = ?, barcode = ?, dimensions_cm = ?, weight_kg = ?, entryDate = ?, productType = ? WHERE productID = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, product.getTitle());
            pstmt.setString(2, product.getCategory());
            pstmt.setFloat(3, product.getValueAmount());
            pstmt.setFloat(4, product.getPrice());
            pstmt.setInt(5, product.getQuantityInStock());
            pstmt.setString(6, product.getDescription());
            pstmt.setString(7, product.getImageUrl());
            pstmt.setString(8, product.getBarcode());
            pstmt.setString(9, product.getDimensionsCm());
            pstmt.setFloat(10, product.getWeightKg());
            pstmt.setString(11, product.getEntryDate() != null ? product.getEntryDate().toString() : null);
            pstmt.setString(12, product.getProductType().name());
            pstmt.setString(13, product.getProductId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
    }

    @Override
    public void updateBookDetails(Book book) throws SQLException {
        updateBaseProduct(book); // Update base product fields first
        String sql = "UPDATE BOOK SET authors = ?, coverType = ?, publisher = ?, publicationDate = ?, numPages = ?, language = ?, book_genre = ? WHERE productID = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, book.getAuthors());
            pstmt.setString(2, book.getCoverType());
            pstmt.setString(3, book.getPublisher());
            pstmt.setString(4, book.getPublicationDate() != null ? book.getPublicationDate().toString() : null);
            pstmt.setInt(5, book.getNumPages());
            pstmt.setString(6, book.getLanguage());
            pstmt.setString(7, book.getBookGenre());
            pstmt.setString(8, book.getProductId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
    }
    
    @Override
    public void updateCDDetails(CD cd) throws SQLException {
        updateBaseProduct(cd);
        String sql = "UPDATE CD SET artists = ?, recordLabel = ?, tracklist = ?, cd_genre = ?, releaseDate = ? WHERE productID = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, cd.getArtists());
            pstmt.setString(2, cd.getRecordLabel());
            pstmt.setString(3, cd.getTracklist());
            pstmt.setString(4, cd.getCdGenre());
            pstmt.setString(5, cd.getReleaseDate() != null ? cd.getReleaseDate().toString() : null);
            pstmt.setString(6, cd.getProductId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
    }

    @Override
    public void updateDVDDetails(DVD dvd) throws SQLException {
        updateBaseProduct(dvd);
        String sql = "UPDATE DVD SET discType = ?, director = ?, runtime_minutes = ?, studio = ?, dvd_language = ?, subtitles = ?, dvd_releaseDate = ?, dvd_genre = ? WHERE productID = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, dvd.getDiscType());
            pstmt.setString(2, dvd.getDirector());
            pstmt.setInt(3, dvd.getRuntimeMinutes());
            pstmt.setString(4, dvd.getStudio());
            pstmt.setString(5, dvd.getDvdLanguage());
            pstmt.setString(6, dvd.getSubtitles());
            pstmt.setString(7, dvd.getDvdReleaseDate() != null ? dvd.getDvdReleaseDate().toString() : null);
            pstmt.setString(8, dvd.getDvdGenre());
            pstmt.setString(9, dvd.getProductId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
    }

    @Override
    public void updateLPDetails(LP lp) throws SQLException {
        updateBaseProduct(lp);
        String sql = "UPDATE LP SET artists = ?, recordLabel = ?, tracklist = ?, genre = ?, releaseDate = ? WHERE productID = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, lp.getArtists());
            pstmt.setString(2, lp.getRecordLabel());
            pstmt.setString(3, lp.getTracklist());
            pstmt.setString(4, lp.getGenre());
            pstmt.setString(5, lp.getReleaseDate() != null ? lp.getReleaseDate().toString() : null);
            pstmt.setString(6, lp.getProductId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
    }

    @Override
    public void delete(String productId) throws SQLException {
        // Với ON DELETE CASCADE đã được thiết lập trong DB script cho các bảng con,
        // chỉ cần xóa từ bảng PRODUCT là đủ.
        String sql = "DELETE FROM PRODUCT WHERE productID = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, productId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
    }

    @Override
    public List<Product> findByTitle(String title) throws SQLException {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT * FROM PRODUCT WHERE title LIKE ?"; // Sử dụng LIKE cho tìm kiếm một phần
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "%" + title + "%");
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                // Cần logic để lấy đúng subtype tương tự như getById() nếu muốn đầy đủ thông tin
                products.add(mapBaseProduct(rs));
            }
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
        return products;
    }

    @Override
    public List<Product> findByCategory(String category) throws SQLException {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT * FROM PRODUCT WHERE category = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, category);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                // Cần logic để lấy đúng subtype
                products.add(mapBaseProduct(rs));
            }
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
        return products;
    }
    
    @Override
    public void updateStock(String productId, int newQuantity) throws SQLException {
        String sql = "UPDATE PRODUCT SET quantityInStock = ? WHERE productID = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, newQuantity);
            pstmt.setString(2, productId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
    }

    @Override
    public void updateStockWithVersion(String productId, int newQuantity, Long expectedVersion) throws SQLException {
        String sql = "UPDATE PRODUCT SET quantityInStock = ?, version = version + 1 WHERE productID = ? AND version = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, newQuantity);
            pstmt.setString(2, productId);
            pstmt.setLong(3, expectedVersion != null ? expectedVersion : 0L);
            
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected == 0) {
                // No rows were updated, indicating version mismatch (optimistic lock conflict)
                throw new SQLException("Optimistic lock conflict: Product version has changed. Expected version: " + expectedVersion);
            }
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
    }

    @Override
    public List<Product> searchProducts(String keyword, String category, String sortBy, String sortOrder, int page, int pageSize) throws SQLException {
        List<Product> products = new ArrayList<>();
        
        Connection conn = getConnection();
        
        // Check which tables exist before building query
        boolean bookExists = DatabaseSchemaValidator.checkTableExists(conn, "BOOK");
        boolean cdExists = DatabaseSchemaValidator.checkTableExists(conn, "CD");
        boolean dvdExists = DatabaseSchemaValidator.checkTableExists(conn, "DVD");
        boolean lpExists = DatabaseSchemaValidator.checkTableExists(conn, "LP");
        
        // Build base query with conditional joins based on table existence
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT DISTINCT p.* FROM PRODUCT p ");
        
        if (bookExists) sql.append("LEFT JOIN BOOK b ON p.productID = b.productID ");
        if (cdExists) sql.append("LEFT JOIN CD c ON p.productID = c.productID ");
        if (dvdExists) sql.append("LEFT JOIN DVD d ON p.productID = d.productID ");
        if (lpExists) sql.append("LEFT JOIN LP l ON p.productID = l.productID ");
        
        sql.append("WHERE p.quantityInStock > 0 ");
        
        List<String> conditions = new ArrayList<>();
        List<Object> parameters = new ArrayList<>();
        
        // Enhanced keyword search with conditional fields
        if (keyword != null && !keyword.trim().isEmpty()) {
            String keywordPattern = "%" + keyword.trim() + "%";
            StringBuilder keywordCondition = new StringBuilder();
            keywordCondition.append("(p.title LIKE ? OR p.description LIKE ? OR p.category LIKE ?");
            
            // Add 3 basic parameters
            parameters.add(keywordPattern);
            parameters.add(keywordPattern);
            parameters.add(keywordPattern);
            
            // Add conditional search fields
            if (bookExists) {
                keywordCondition.append(" OR b.authors LIKE ? OR b.publisher LIKE ?");
                parameters.add(keywordPattern);
                parameters.add(keywordPattern);
            }
            if (cdExists) {
                keywordCondition.append(" OR c.artists LIKE ? OR c.recordLabel LIKE ?");
                parameters.add(keywordPattern);
                parameters.add(keywordPattern);
            }
            if (dvdExists) {
                keywordCondition.append(" OR d.director LIKE ? OR d.studio LIKE ?");
                parameters.add(keywordPattern);
                parameters.add(keywordPattern);
            }
            if (lpExists) {
                keywordCondition.append(" OR l.artists LIKE ? OR l.recordLabel LIKE ?");
                parameters.add(keywordPattern);
                parameters.add(keywordPattern);
            }
            
            keywordCondition.append(")");
            conditions.add(keywordCondition.toString());
        }
        
        // Category filter
        if (category != null && !category.trim().isEmpty() && !"All".equalsIgnoreCase(category)) {
            conditions.add("p.category = ?");
            parameters.add(category);
        }
        
        // Apply conditions
        for (String condition : conditions) {
            sql.append(" AND ").append(condition);
        }
        
        // Add sorting with validation
        if (sortBy != null && !sortBy.trim().isEmpty()) {
            String validatedSortBy = validateSortColumn(sortBy);
            sql.append(" ORDER BY ").append(validatedSortBy);
            if (sortOrder != null && sortOrder.equalsIgnoreCase("DESC")) {
                sql.append(" DESC");
            } else {
                sql.append(" ASC");
            }
        } else {
            sql.append(" ORDER BY p.title ASC"); // Default sorting
        }
        
        // Add pagination
        sql.append(" LIMIT ? OFFSET ?");
        parameters.add(pageSize);
        parameters.add((page - 1) * pageSize);
        
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            ps = conn.prepareStatement(sql.toString());
            
            // Set parameters
            for (int i = 0; i < parameters.size(); i++) {
                ps.setObject(i + 1, parameters.get(i));
            }
            
            rs = ps.executeQuery();
            while (rs.next()) {
                // Get full product with subtype details
                Product fullProduct = getById(rs.getString("productID"));
                if (fullProduct != null) {
                    products.add(fullProduct);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error in searchProducts: " + e.getMessage());
            // Log the SQL that failed for debugging
            System.err.println("Failed SQL: " + sql.toString());
            throw e;
        } finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
        }
        
        return products;
    }
    
    // Helper method to validate sort columns and prevent SQL injection
    private String validateSortColumn(String sortBy) {
        switch (sortBy.toLowerCase()) {
            case "title":
                return "p.title";
            case "price":
                return "p.price";
            case "category":
                return "p.category";
            case "entrydate":
                return "p.entryDate";
            case "quantity":
                return "p.quantityInStock";
            default:
                return "p.title"; // Default safe sorting
        }
    }

    @Override
    public int getSearchResultsCount(String keyword, String category) throws SQLException {
        Connection conn = getConnection();
        
        // Check which tables exist before building query
        boolean bookExists = DatabaseSchemaValidator.checkTableExists(conn, "BOOK");
        boolean cdExists = DatabaseSchemaValidator.checkTableExists(conn, "CD");
        boolean dvdExists = DatabaseSchemaValidator.checkTableExists(conn, "DVD");
        boolean lpExists = DatabaseSchemaValidator.checkTableExists(conn, "LP");
        
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(DISTINCT p.productID) FROM PRODUCT p ");
        
        if (bookExists) sql.append("LEFT JOIN BOOK b ON p.productID = b.productID ");
        if (cdExists) sql.append("LEFT JOIN CD c ON p.productID = c.productID ");
        if (dvdExists) sql.append("LEFT JOIN DVD d ON p.productID = d.productID ");
        if (lpExists) sql.append("LEFT JOIN LP l ON p.productID = l.productID ");
        
        sql.append("WHERE p.quantityInStock > 0 ");
        
        List<String> conditions = new ArrayList<>();
        List<Object> parameters = new ArrayList<>();
        
        // Enhanced keyword search with conditional fields
        if (keyword != null && !keyword.trim().isEmpty()) {
            String keywordPattern = "%" + keyword.trim() + "%";
            StringBuilder keywordCondition = new StringBuilder();
            keywordCondition.append("(p.title LIKE ? OR p.description LIKE ? OR p.category LIKE ?");
            
            // Add 3 basic parameters
            parameters.add(keywordPattern);
            parameters.add(keywordPattern);
            parameters.add(keywordPattern);
            
            // Add conditional search fields
            if (bookExists) {
                keywordCondition.append(" OR b.authors LIKE ? OR b.publisher LIKE ?");
                parameters.add(keywordPattern);
                parameters.add(keywordPattern);
            }
            if (cdExists) {
                keywordCondition.append(" OR c.artists LIKE ? OR c.recordLabel LIKE ?");
                parameters.add(keywordPattern);
                parameters.add(keywordPattern);
            }
            if (dvdExists) {
                keywordCondition.append(" OR d.director LIKE ? OR d.studio LIKE ?");
                parameters.add(keywordPattern);
                parameters.add(keywordPattern);
            }
            if (lpExists) {
                keywordCondition.append(" OR l.artists LIKE ? OR l.recordLabel LIKE ?");
                parameters.add(keywordPattern);
                parameters.add(keywordPattern);
            }
            
            keywordCondition.append(")");
            conditions.add(keywordCondition.toString());
        }
        
        // Category filter
        if (category != null && !category.trim().isEmpty() && !"All".equalsIgnoreCase(category)) {
            conditions.add("p.category = ?");
            parameters.add(category);
        }
        
        // Apply conditions
        for (String condition : conditions) {
            sql.append(" AND ").append(condition);
        }
        
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            ps = conn.prepareStatement(sql.toString());
            
            // Set parameters
            for (int i = 0; i < parameters.size(); i++) {
                ps.setObject(i + 1, parameters.get(i));
            }
            
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error in getSearchResultsCount: " + e.getMessage());
            System.err.println("Failed SQL: " + sql.toString());
            throw e;
        } finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
        }
        
        return 0;
    }

    @Override
    public List<String> getAllCategories() throws SQLException {
        List<String> categories = new ArrayList<>();
        String sql = "SELECT DISTINCT category FROM PRODUCT WHERE category IS NOT NULL AND category != '' ORDER BY category";
        
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
}
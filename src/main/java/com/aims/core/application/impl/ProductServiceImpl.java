package com.aims.core.application.impl; // Or com.aims.core.application.services.impl;

import com.aims.core.application.services.IProductService;
import com.aims.core.application.services.IProductManagerAuditService;
import com.aims.core.application.services.IStockValidationService;
import com.aims.core.entities.Product;
import com.aims.core.entities.Book;
import com.aims.core.entities.CD;
import com.aims.core.entities.DVD;
import com.aims.core.entities.LP;
import com.aims.core.enums.ProductType;
import com.aims.core.infrastructure.database.dao.IProductDAO;
import com.aims.core.shared.exceptions.ValidationException; // Assuming you have these custom exceptions
import com.aims.core.shared.exceptions.ResourceNotFoundException; // Assuming you have these custom exceptions
import com.aims.core.shared.exceptions.InventoryException;
import com.aims.core.shared.utils.SearchResult; // Assuming a SearchResult utility class
import com.aims.core.utils.ProductTypeDisplayMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class ProductServiceImpl implements IProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductServiceImpl.class);
    
    private final IProductDAO productDAO;
    private final IProductManagerAuditService auditService;
    private final IStockValidationService stockValidationService;

    private static final float VAT_RATE = 0.10f; // 10% VAT
    private static final float MIN_PRICE_PERCENTAGE_OF_VALUE = 0.30f;
    private static final float MAX_PRICE_PERCENTAGE_OF_VALUE = 1.50f;
    private static final int MAX_DELETIONS_AT_ONCE = 10;
    
    // Retry configuration for optimistic locking
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final int BASE_RETRY_DELAY_MS = 100;
    private static final int MAX_RETRY_DELAY_MS = 1000;
    
    // Daily limits for product manager operations would be constants too
    // private static final int MAX_UPDATES_DELETES_PER_DAY = 30;
    // private static final int MAX_PRICE_UPDATES_PER_DAY_PER_PRODUCT = 2;


    public ProductServiceImpl(IProductDAO productDAO, IProductManagerAuditService auditService, IStockValidationService stockValidationService) {
        this.productDAO = productDAO;
        this.auditService = auditService;
        this.stockValidationService = stockValidationService;
    }

    private void validateProductPrice(Product product) throws ValidationException {
        if (product.getPrice() < product.getValueAmount() * MIN_PRICE_PERCENTAGE_OF_VALUE ||
            product.getPrice() > product.getValueAmount() * MAX_PRICE_PERCENTAGE_OF_VALUE) {
            throw new ValidationException(
                String.format("Product price must be between %.0f%% and %.0f%% of its value. Value: %.2f, Price: %.2f",
                    MIN_PRICE_PERCENTAGE_OF_VALUE * 100,
                    MAX_PRICE_PERCENTAGE_OF_VALUE * 100,
                    product.getValueAmount(),
                    product.getPrice()
                )
            );
        }
    }

    private Product addVAT(Product product) {
        if (product == null) return null;
        // Create a new instance or clone to avoid modifying the original entity from DAO if it's cached or reused
        // For simplicity, we'll modify a new object if it were a DTO, or assume direct modification for entities if that's the pattern
        // This is a conceptual step; how VAT is applied for display might differ (e.g., in a DTO or view layer)
        // Here, we'll assume the service returns products with price *inclusive* of VAT for customer-facing methods.
        product.setPrice(product.getPrice() * (1 + VAT_RATE));
        return product;
    }

    private List<Product> addVAT(List<Product> products) {
        if (products == null) return new ArrayList<>();
        return products.stream().map(this::addVAT).collect(Collectors.toList());
    }

    /**
     * Gets the current manager ID from the session or context.
     * In a real application, this would get the ID from the authenticated session.
     * For now, we'll use a placeholder that can be overridden.
     */
    private String getCurrentManagerId() {
        // TODO: In a real application, get this from authentication context/session
        // For now, return a default manager ID - this should be injected from the controller
        return "DEFAULT_MANAGER_001";
    }

    // --- Product Management (for Product Managers) ---

    @Override
    public Book addBook(Book book) throws SQLException, ValidationException {
        if (book == null) throw new ValidationException("Book data cannot be null.");
        validateProductPrice(book);
        // ADD operations are unlimited per problem statement
        book.setEntryDate(LocalDate.now()); // Set entry date
        productDAO.addBookDetails(book); // This DAO method should handle base product and book details
        
        // Log the operation
        try {
            auditService.logOperation("SYSTEM", "ADD", book.getProductId(), "Added new Book: " + book.getTitle());
        } catch (SQLException e) {
            // Log error but don't fail the operation
            System.err.println("Failed to log ADD operation: " + e.getMessage());
        }
        
        return productDAO.getById(book.getProductId()) instanceof Book ? (Book) productDAO.getById(book.getProductId()) : null; // Re-fetch to confirm
    }

    @Override
    public CD addCD(CD cd) throws SQLException, ValidationException {
        if (cd == null) throw new ValidationException("CD data cannot be null.");
        validateProductPrice(cd);
        cd.setEntryDate(LocalDate.now());
        productDAO.addCDDetails(cd);
        
        // Log the operation
        try {
            auditService.logOperation("SYSTEM", "ADD", cd.getProductId(), "Added new CD: " + cd.getTitle());
        } catch (SQLException e) {
            System.err.println("Failed to log ADD operation: " + e.getMessage());
        }
        
        return productDAO.getById(cd.getProductId()) instanceof CD ? (CD) productDAO.getById(cd.getProductId()) : null;
    }

    @Override
    public DVD addDVD(DVD dvd) throws SQLException, ValidationException {
        if (dvd == null) throw new ValidationException("DVD data cannot be null.");
        validateProductPrice(dvd);
        dvd.setEntryDate(LocalDate.now());
        productDAO.addDVDDetails(dvd);
        
        // Log the operation
        try {
            auditService.logOperation("SYSTEM", "ADD", dvd.getProductId(), "Added new DVD: " + dvd.getTitle());
        } catch (SQLException e) {
            System.err.println("Failed to log ADD operation: " + e.getMessage());
        }
        
        return productDAO.getById(dvd.getProductId()) instanceof DVD ? (DVD) productDAO.getById(dvd.getProductId()) : null;
    }

    @Override
    public LP addLP(LP lp) throws SQLException, ValidationException {
        if (lp == null) throw new ValidationException("LP data cannot be null.");
        validateProductPrice(lp);
        lp.setEntryDate(LocalDate.now());
        productDAO.addLPDetails(lp);
        
        // Log the operation
        try {
            auditService.logOperation("SYSTEM", "ADD", lp.getProductId(), "Added new LP: " + lp.getTitle());
        } catch (SQLException e) {
            System.err.println("Failed to log ADD operation: " + e.getMessage());
        }
        
        return productDAO.getById(lp.getProductId()) instanceof LP ? (LP) productDAO.getById(lp.getProductId()) : null;
    }

    @Override
    public Book updateBook(Book book) throws SQLException, ValidationException, ResourceNotFoundException {
        if (book == null || book.getProductId() == null) throw new ValidationException("Book data and ID cannot be null.");
        Product existingProduct = productDAO.getById(book.getProductId());
        if (existingProduct == null) throw new ResourceNotFoundException("Book with ID " + book.getProductId() + " not found for update.");
        if (!(existingProduct instanceof Book)) throw new ValidationException("Product ID " + book.getProductId() + " is not a Book.");

        validateProductPrice(book);
        // Check Product Manager daily UPDATE limits
        String managerId = getCurrentManagerId();
        auditService.checkDailyOperationLimit(managerId, 1);
        productDAO.updateBookDetails(book);
        
        // Log the operation
        auditService.logOperation(managerId, "UPDATE", book.getProductId(), "Updated Book: " + book.getTitle());
        
        return (Book) productDAO.getById(book.getProductId());
    }

    @Override
    public CD updateCD(CD cd) throws SQLException, ValidationException, ResourceNotFoundException {
        if (cd == null || cd.getProductId() == null) throw new ValidationException("CD data and ID cannot be null.");
        Product existingProduct = productDAO.getById(cd.getProductId());
        if (existingProduct == null) throw new ResourceNotFoundException("CD with ID " + cd.getProductId() + " not found for update.");
        if (!(existingProduct instanceof CD)) throw new ValidationException("Product ID " + cd.getProductId() + " is not a CD.");

        validateProductPrice(cd);
        // Check Product Manager daily UPDATE limits
        String managerId = getCurrentManagerId();
        auditService.checkDailyOperationLimit(managerId, 1);
        productDAO.updateCDDetails(cd);
        
        // Log the operation
        auditService.logOperation(managerId, "UPDATE", cd.getProductId(), "Updated CD: " + cd.getTitle());
        
        return (CD) productDAO.getById(cd.getProductId());
    }

    @Override
    public DVD updateDVD(DVD dvd) throws SQLException, ValidationException, ResourceNotFoundException {
        if (dvd == null || dvd.getProductId() == null) throw new ValidationException("DVD data and ID cannot be null.");
        Product existingProduct = productDAO.getById(dvd.getProductId());
        if (existingProduct == null) throw new ResourceNotFoundException("DVD with ID " + dvd.getProductId() + " not found for update.");
        if (!(existingProduct instanceof DVD)) throw new ValidationException("Product ID " + dvd.getProductId() + " is not a DVD.");

        validateProductPrice(dvd);
        // Check Product Manager daily UPDATE limits
        String managerId = getCurrentManagerId();
        auditService.checkDailyOperationLimit(managerId, 1);
        productDAO.updateDVDDetails(dvd);
        
        // Log the operation
        auditService.logOperation(managerId, "UPDATE", dvd.getProductId(), "Updated DVD: " + dvd.getTitle());
        
        return (DVD) productDAO.getById(dvd.getProductId());
    }

    @Override
    public LP updateLP(LP lp) throws SQLException, ValidationException, ResourceNotFoundException {
        if (lp == null || lp.getProductId() == null) throw new ValidationException("LP data and ID cannot be null.");
        Product existingProduct = productDAO.getById(lp.getProductId());
        if (existingProduct == null) throw new ResourceNotFoundException("LP with ID " + lp.getProductId() + " not found for update.");
        if (!(existingProduct instanceof LP)) throw new ValidationException("Product ID " + lp.getProductId() + " is not an LP.");

        validateProductPrice(lp);
        // Check Product Manager daily UPDATE limits
        String managerId = getCurrentManagerId();
        auditService.checkDailyOperationLimit(managerId, 1);
        productDAO.updateLPDetails(lp);
        
        // Log the operation
        auditService.logOperation(managerId, "UPDATE", lp.getProductId(), "Updated LP: " + lp.getTitle());
        
        return (LP) productDAO.getById(lp.getProductId());
    }

    @Override
    public Product getProductById(String productId) throws SQLException {
        if (productId == null || productId.trim().isEmpty()) return null;
        return productDAO.getById(productId);
    }

    @Override
    public void deleteProduct(String productId, String managerId) throws SQLException, ValidationException, ResourceNotFoundException {
        if (productId == null) throw new ValidationException("Product ID cannot be null for deletion.");
        Product existingProduct = productDAO.getById(productId);
        if (existingProduct == null) throw new ResourceNotFoundException("Product with ID " + productId + " not found for deletion.");

        // Check Product Manager daily DELETE limits
        auditService.checkDailyOperationLimit(managerId, 1);

        // DB constraint ON DELETE RESTRICT for productID in ORDER_ITEM will prevent deletion if product is in an order.
        // This will manifest as an SQLException from the DAO, which should be caught and potentially re-thrown as ValidationException.
        try {
            productDAO.delete(productId);
            // Log the successful deletion
            auditService.logOperation(managerId, "DELETE", productId, "Deleted product: " + existingProduct.getTitle());
        } catch (SQLException e) {
            // Check for specific constraint violation error code if possible
            // For now, re-throw a generic validation message. A more specific check on e.getErrorCode() or message is better.
            if (e.getMessage().toLowerCase().contains("constraint failed") || e.getMessage().toLowerCase().contains("foreign key")) {
                 throw new ValidationException("Product " + productId + " cannot be deleted as it is part of existing orders.", e);
            }
            throw e;
        }
    }

    @Override
    public void deleteProducts(List<String> productIds, String managerId) throws SQLException, ValidationException {
        if (productIds == null || productIds.isEmpty()) throw new ValidationException("Product ID list cannot be empty.");
        if (productIds.size() > MAX_DELETIONS_AT_ONCE) {
            throw new ValidationException("Cannot delete more than " + MAX_DELETIONS_AT_ONCE + " products at once."); // [cite: 521]
        }

        // Check Product Manager daily DELETE limits for the total count
        auditService.checkDailyOperationLimit(managerId, productIds.size());

        List<String> errors = new ArrayList<>();
        for (String productId : productIds) {
            try {
                deleteProduct(productId, managerId); // Leverages the single delete logic (which includes its own checks, though manager limit check might be duplicated or better handled once for the batch)
            } catch (ResourceNotFoundException e) {
                errors.add("Product ID " + productId + " not found: " + e.getMessage());
            } catch (ValidationException e) {
                errors.add("Validation failed for product ID " + productId + ": " + e.getMessage());
            }
            // SQLException will propagate up and stop the loop
        }
        if (!errors.isEmpty()) {
            throw new ValidationException("Errors occurred during batch deletion: " + String.join("; ", errors));
        }
    }

    @Override
    public Product updateProductPrice(String productId, float newPriceExclVAT, String managerId) throws SQLException, ValidationException, ResourceNotFoundException {
        Product product = productDAO.getById(productId);
        if (product == null) {
            throw new ResourceNotFoundException("Product with ID " + productId + " not found.");
        }

        // Check Product Manager daily PRICE UPDATE limits for this specific product
        auditService.checkPriceUpdateLimit(managerId, productId);

        // Validate new price against product value
        if (newPriceExclVAT < product.getValueAmount() * MIN_PRICE_PERCENTAGE_OF_VALUE ||
            newPriceExclVAT > product.getValueAmount() * MAX_PRICE_PERCENTAGE_OF_VALUE) {
            throw new ValidationException(
                String.format("New price must be between %.0f%% and %.0f%% of product value (%.2f). Attempted price: %.2f",
                    MIN_PRICE_PERCENTAGE_OF_VALUE * 100,
                    MAX_PRICE_PERCENTAGE_OF_VALUE * 100,
                    product.getValueAmount(),
                    newPriceExclVAT
                )
            ); // [cite: 537]
        }

        float oldPrice = product.getPrice();
        product.setPrice(newPriceExclVAT);
        productDAO.updateBaseProduct(product); // Assuming price is on base product table
        
        // Log the price update
        auditService.logOperation(managerId, "PRICE_UPDATE", productId,
            String.format("Price updated from %.2f to %.2f for %s", oldPrice, newPriceExclVAT, product.getTitle()));
        
        return productDAO.getById(productId);
    }

    @Override
    public Product updateProductStock(String productId, int quantityChange) throws SQLException, ValidationException, ResourceNotFoundException, InventoryException {
        return updateProductStockWithRetry(productId, quantityChange, MAX_RETRY_ATTEMPTS);
    }
    
    /**
     * Updates product stock with optimistic locking and retry mechanism
     * @param productId Product to update
     * @param quantityChange Change in quantity (positive for adding, negative for subtracting)
     * @param attemptsLeft Number of retry attempts remaining
     * @return Updated product
     * @throws SQLException Database error
     * @throws ValidationException Invalid operation
     * @throws ResourceNotFoundException Product not found
     * @throws InventoryException Stock operation failed after retries
     */
    private Product updateProductStockWithRetry(String productId, int quantityChange, int attemptsLeft)
            throws SQLException, ValidationException, ResourceNotFoundException, InventoryException {
        
        if (attemptsLeft <= 0) {
            throw new InventoryException("Failed to update stock after maximum retry attempts due to concurrent access. Please try again.");
        }
        
        try {
            Product product = productDAO.getById(productId);
            if (product == null) {
                throw new ResourceNotFoundException("Product with ID " + productId + " not found.");
            }

            int newQuantity = product.getQuantityInStock() + quantityChange;
            if (newQuantity < 0) {
                throw new ValidationException("Stock quantity cannot be negative. Current stock: " +
                                           product.getQuantityInStock() + ", Change: " + quantityChange);
            }
            
            // Enhanced validation using StockValidationService for negative quantity changes (stock reduction)
            if (quantityChange < 0) {
                int requestedReduction = Math.abs(quantityChange);
                try {
                    IStockValidationService.StockValidationResult validationResult =
                        stockValidationService.validateProductStock(productId, requestedReduction);
                    
                    if (!validationResult.isValid()) {
                        throw new InventoryException(String.format(
                            "Stock validation failed for product %s: %s. Available: %d, Requested reduction: %d",
                            product.getTitle(), validationResult.getMessage(),
                            validationResult.getAvailableStock(), requestedReduction));
                    }
                    
                    logger.debug("Stock validation passed for product {} stock reduction of {} units",
                               productId, requestedReduction);
                } catch (ResourceNotFoundException e) {
                    // Already handled by the product check above, but log for clarity
                    logger.warn("Product {} not found during stock validation", productId);
                    throw e;
                }
            }
            
            // Check for critically low stock levels after the update
            try {
                if (stockValidationService.isStockCriticallyLow(productId, 10)) {
                    logger.warn("Product {} will have critically low stock after update: {} units",
                               productId, newQuantity);
                }
            } catch (ResourceNotFoundException e) {
                // Log warning but don't fail the operation
                logger.warn("Could not check critical stock level for product {}: {}", productId, e.getMessage());
            }
            
            // Use optimistic locking for stock updates
            productDAO.updateStockWithVersion(productId, newQuantity, product.getVersion());
            product.setQuantityInStock(newQuantity);
            product.setVersion(product.getVersion() + 1); // Increment version
            
            logger.info("Successfully updated stock for product {} by {} units. New stock: {}",
                       productId, quantityChange, newQuantity);
            
            return product;
            
        } catch (SQLException e) {
            // Check if this is an optimistic lock exception (version mismatch)
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("optimistic lock conflict")) {
                // Handle optimistic lock exception with exponential backoff retry
                logger.warn("Optimistic lock conflict updating stock for product {}. Attempts left: {}. Retrying...",
                           productId, attemptsLeft - 1);
                
                // Exponential backoff with jitter
                int delay = Math.min(BASE_RETRY_DELAY_MS * (MAX_RETRY_ATTEMPTS - attemptsLeft + 1), MAX_RETRY_DELAY_MS);
                int jitter = ThreadLocalRandom.current().nextInt(0, delay / 2);
                
                try {
                    Thread.sleep(delay + jitter);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new SQLException("Stock update interrupted", ie);
                }
                
                return updateProductStockWithRetry(productId, quantityChange, attemptsLeft - 1);
            } else {
                // Re-throw other SQL exceptions
                throw e;
            }
        }
    }


    // --- Product Viewing/Searching (for Customers) ---

    @Override
    public SearchResult<Product> getProductsForDisplay(int pageNumber, int pageSize) throws SQLException {
        // Basic implementation: get all and then manually paginate.
        // A more performant DAO would support LIMIT and OFFSET.
        // For now, this illustrates the concept.
        // This method should also apply VAT to prices.
        List<Product> allProducts = productDAO.getAll(); // This currently fetches base products only from ProductDAOImpl
        // TODO: Enhance ProductDAOImpl.getAll() or this service to fetch full subtype details for display
        // or implement proper pagination in DAO.

        List<Product> productsWithVAT = addVAT(new ArrayList<>(allProducts)); // Create new list to avoid modifying original

        int totalResults = productsWithVAT.size();
        int fromIndex = (pageNumber - 1) * pageSize;
        if (fromIndex >= totalResults) {
            return new SearchResult<>(new ArrayList<>(), pageNumber, 0, 0);
        }

        int toIndex = Math.min(fromIndex + pageSize, totalResults);
        List<Product> pageResults = productsWithVAT.subList(fromIndex, toIndex);
        
        int totalPages = (int) Math.ceil((double) totalResults / pageSize);
        if (totalPages == 0 && totalResults > 0) totalPages = 1;

        return new SearchResult<>(pageResults, pageNumber, totalPages, totalResults);
    }

    public SearchResult<Product> searchProducts(String searchTerm, String category, int pageNumber, int pageSize, String sortByPrice) throws SQLException {
        // This is a simplified search. Real search might involve full-text search or more complex queries.
        // The DAO's findByTitle/findByCategory are basic. Sorting and pagination would ideally be done at DB level.
        List<Product> results;
        if (category != null && !category.trim().isEmpty()) {
            results = productDAO.findByCategory(category);
            if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                String lowerSearchTerm = searchTerm.toLowerCase();
                results = results.stream()
                                 .filter(p -> p.getTitle().toLowerCase().contains(lowerSearchTerm))
                                 .collect(Collectors.toList());
            }
        } else if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            results = productDAO.findByTitle(searchTerm);
        } else {
            results = productDAO.getAll(); // Fallback to all if no criteria
        }
        
        // TODO: Enhance to fetch full subtype details if results only contain base Product info.

        List<Product> productsWithVAT = addVAT(new ArrayList<>(results));

        // Sorting
        if ("ASC".equalsIgnoreCase(sortByPrice)) {
            productsWithVAT.sort((p1, p2) -> Float.compare(p1.getPrice(), p2.getPrice()));
        } else if ("DESC".equalsIgnoreCase(sortByPrice)) {
            productsWithVAT.sort((p1, p2) -> Float.compare(p2.getPrice(), p1.getPrice()));
        }

        // Pagination (manual, for example)
        int totalResults = productsWithVAT.size();
        int fromIndex = (pageNumber - 1) * pageSize;
        if (fromIndex >= totalResults) {
            return new SearchResult<>(new ArrayList<>(), pageNumber, 0, 0); // Return empty result if fromIndex is out of bounds
        }
        int toIndex = Math.min(fromIndex + pageSize, totalResults);
        List<Product> pageResults = productsWithVAT.subList(fromIndex, toIndex);
        
        int totalPages = (int) Math.ceil((double) totalResults / pageSize);
        if (totalPages == 0 && totalResults > 0) totalPages = 1;

        return new SearchResult<>(pageResults, pageNumber, totalPages, totalResults);
    }

    @Override
    public Product getProductDetailsForCustomer(String productId) throws SQLException, ResourceNotFoundException {
        Product product = productDAO.getById(productId);
        if (product == null) {
            throw new ResourceNotFoundException("Product with ID " + productId + " not found.");
        }
        // Apply VAT for customer display
        return addVAT(product); // Modifies the price of the fetched product or a copy
    }

    @Override
    public SearchResult<Product> searchProducts(String keyword, String category, String productType, String sortBy, String sortOrder, int pageNumber, int pageSize) throws SQLException {
        // Enhanced search implementation that supports both category and product type filtering
        ProductType enumType = ProductTypeDisplayMapper.fromDisplayName(productType);
        
        List<Product> products;
        int totalResults;
        
        if (enumType != null) {
            // Use ProductType-based search
            products = productDAO.searchProductsByType(keyword, enumType, sortBy, sortOrder, pageNumber, pageSize);
            totalResults = productDAO.getSearchResultsCountByType(keyword, enumType);
        } else if (category != null && !category.trim().isEmpty()) {
            // Use category-based search
            products = productDAO.searchProducts(keyword, category, sortBy, sortOrder, pageNumber, pageSize);
            totalResults = productDAO.getSearchResultsCount(keyword, category);
        } else {
            // General search without specific filtering
            products = productDAO.searchProducts(keyword, null, sortBy, sortOrder, pageNumber, pageSize);
            totalResults = productDAO.getSearchResultsCount(keyword, null);
        }
        
        // Apply VAT to all products for customer display
        List<Product> productsWithVAT = products.stream()
                .map(this::addVAT)
                .collect(Collectors.toList());
        
        // Calculate pagination
        int totalPages = (int) Math.ceil((double) totalResults / pageSize);
        if (totalPages == 0 && totalResults > 0) totalPages = 1;
        
        return new SearchResult<>(productsWithVAT, pageNumber, totalPages, totalResults);
    }

    @Override
    public SearchResult<Product> advancedSearchProducts(String keyword, String category, String sortBy, String sortOrder, int pageNumber, int pageSize) throws SQLException {
        // Check if category is a product type display name first
        ProductType productType = ProductTypeDisplayMapper.fromDisplayName(category);
        
        List<Product> products;
        int totalResults;
        
        if (productType != null) {
            // Use ProductType-based search for the new filtering system
            products = productDAO.searchProductsByType(keyword, productType, sortBy, sortOrder, pageNumber, pageSize);
            totalResults = productDAO.getSearchResultsCountByType(keyword, productType);
        } else {
            // Fall back to existing category-based search for backward compatibility
            products = productDAO.searchProducts(keyword, category, sortBy, sortOrder, pageNumber, pageSize);
            totalResults = productDAO.getSearchResultsCount(keyword, category);
        }
        
        // Apply VAT to all products for customer display
        List<Product> productsWithVAT = products.stream()
                .map(this::addVAT)
                .collect(Collectors.toList());
        
        // Calculate pagination
        int totalPages = (int) Math.ceil((double) totalResults / pageSize);
        if (totalPages == 0 && totalResults > 0) totalPages = 1;
        
        return new SearchResult<>(productsWithVAT, pageNumber, totalPages, totalResults);
    }

    @Override
    public List<String> getAllCategories() throws SQLException {
        return productDAO.getAllCategories();
    }

    @Override
    public List<String> getAllProductTypes() throws SQLException {
        return ProductTypeDisplayMapper.getAllDisplayNames();
    }

    @Override
    public SearchResult<Product> searchByProductType(String productType, String keyword, String sortBy, String sortOrder, int pageNumber, int pageSize) throws SQLException {
        // Check if productType is a valid display name and convert to enum
        ProductType enumType = ProductTypeDisplayMapper.fromDisplayName(productType);
        
        List<Product> products;
        int totalResults;
        
        if (enumType != null) {
            // Search by product type using DAO method
            products = productDAO.searchProductsByType(keyword, enumType, sortBy, sortOrder, pageNumber, pageSize);
            totalResults = productDAO.getSearchResultsCountByType(keyword, enumType);
        } else {
            // Fallback to regular search if not a valid product type
            products = productDAO.searchProducts(keyword, productType, sortBy, sortOrder, pageNumber, pageSize);
            totalResults = productDAO.getSearchResultsCount(keyword, productType);
        }
        
        // Apply VAT to all products for customer display
        List<Product> productsWithVAT = products.stream()
                .map(this::addVAT)
                .collect(Collectors.toList());
        
        // Calculate pagination
        int totalPages = (int) Math.ceil((double) totalResults / pageSize);
        if (totalPages == 0 && totalResults > 0) totalPages = 1;
        
        return new SearchResult<>(productsWithVAT, pageNumber, totalPages, totalResults);
    }

    // --- Enhanced Inventory Management Methods using StockValidationService ---
    
    /**
     * Gets comprehensive stock information for a product using StockValidationService.
     * Provides real-time stock data including reserved quantities.
     *
     * @param productId Product ID to get stock information for
     * @return StockInfo with detailed stock information
     * @throws SQLException Database error
     * @throws ResourceNotFoundException Product not found
     */
    public IStockValidationService.StockInfo getProductStockInfo(String productId)
            throws SQLException, ResourceNotFoundException {
        return stockValidationService.getStockInfo(productId);
    }
    
    /**
     * Checks if a product has sufficient stock for a given quantity.
     * Uses StockValidationService for enhanced validation including reservations.
     *
     * @param productId Product to check
     * @param requestedQuantity Quantity to validate
     * @return true if sufficient stock is available
     * @throws SQLException Database error
     * @throws ResourceNotFoundException Product not found
     */
    public boolean hasRawStock(String productId, int requestedQuantity)
            throws SQLException, ResourceNotFoundException {
        IStockValidationService.StockValidationResult result =
            stockValidationService.validateProductStock(productId, requestedQuantity);
        return result.isValid();
    }
    
    /**
     * Gets the available stock for a product considering reservations.
     *
     * @param productId Product ID to check
     * @return Available stock quantity (actual stock minus reserved)
     * @throws SQLException Database error
     * @throws ResourceNotFoundException Product not found
     */
    public int getAvailableStock(String productId) throws SQLException, ResourceNotFoundException {
        IStockValidationService.StockInfo stockInfo = stockValidationService.getStockInfo(productId);
        return stockInfo.getAvailableStock();
    }
    
    /**
     * Checks if a product's stock is critically low using configurable threshold.
     *
     * @param productId Product to check
     * @param threshold Critical stock threshold (use default if <= 0)
     * @return true if stock is critically low
     * @throws SQLException Database error
     * @throws ResourceNotFoundException Product not found
     */
    public boolean isStockCriticallyLow(String productId, int threshold)
            throws SQLException, ResourceNotFoundException {
        return stockValidationService.isStockCriticallyLow(productId, threshold);
    }
    
    /**
     * Validates stock for multiple products efficiently.
     * Used for bulk operations like cart validation.
     *
     * @param productStockMap Map of product ID to requested quantity
     * @return BulkStockValidationResult with validation outcomes
     * @throws SQLException Database error
     */
    public IStockValidationService.BulkStockValidationResult validateBulkProductStock(
            java.util.Map<String, Integer> productStockMap) throws SQLException {
        
        if (productStockMap == null || productStockMap.isEmpty()) {
            return new IStockValidationService.BulkStockValidationResult(
                true, new java.util.ArrayList<>(), new java.util.ArrayList<>(),
                0, 0, "No products to validate"
            );
        }
        
        // Convert map to CartItem list for validation
        java.util.List<com.aims.core.entities.CartItem> cartItems = new java.util.ArrayList<>();
        for (java.util.Map.Entry<String, Integer> entry : productStockMap.entrySet()) {
            try {
                Product product = productDAO.getById(entry.getKey());
                if (product != null) {
                    com.aims.core.entities.CartItem cartItem = new com.aims.core.entities.CartItem();
                    cartItem.setProduct(product);
                    cartItem.setQuantity(entry.getValue());
                    cartItems.add(cartItem);
                }
            } catch (SQLException e) {
                logger.warn("Error loading product {} for bulk validation: {}", entry.getKey(), e.getMessage());
            }
        }
        
        return stockValidationService.validateBulkStock(cartItems);
    }

    /**
     * Enhanced version of advancedSearchProducts that supports both category and product type filtering.
     * This provides backward compatibility while adding product type support.
     */
    public SearchResult<Product> enhancedAdvancedSearchProducts(String keyword, String categoryOrType, String sortBy, String sortOrder, int pageNumber, int pageSize) throws SQLException {
        // Check if categoryOrType is a product type display name
        ProductType productType = ProductTypeDisplayMapper.fromDisplayName(categoryOrType);
        
        if (productType != null) {
            // Use product type filtering
            return searchByProductType(categoryOrType, keyword, sortBy, sortOrder, pageNumber, pageSize);
        } else {
            // Fall back to existing category-based search
            return advancedSearchProducts(keyword, categoryOrType, sortBy, sortOrder, pageNumber, pageSize);
        }
    }
}
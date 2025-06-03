package com.aims.core.application.impl; // Or com.aims.core.application.services.impl;

import com.aims.core.application.services.IProductService;
import com.aims.core.entities.Product;
import com.aims.core.entities.Book;
import com.aims.core.entities.CD;
import com.aims.core.entities.DVD;
import com.aims.core.infrastructure.database.dao.IProductDAO;
import com.aims.core.shared.exceptions.ValidationException; // Assuming you have these custom exceptions
import com.aims.core.shared.exceptions.ResourceNotFoundException; // Assuming you have these custom exceptions
import com.aims.core.shared.utils.SearchResult; // Assuming a SearchResult utility class

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ProductServiceImpl implements IProductService {

    private final IProductDAO productDAO;
    // private final IUserActivityLogService userActivityLogService; // For manager daily limits

    private static final float VAT_RATE = 0.10f; // 10% VAT
    private static final float MIN_PRICE_PERCENTAGE_OF_VALUE = 0.30f;
    private static final float MAX_PRICE_PERCENTAGE_OF_VALUE = 1.50f;
    private static final int MAX_DELETIONS_AT_ONCE = 10;
    // Daily limits for product manager operations would be constants too
    // private static final int MAX_UPDATES_DELETES_PER_DAY = 30;
    // private static final int MAX_PRICE_UPDATES_PER_DAY_PER_PRODUCT = 2;


    public ProductServiceImpl(IProductDAO productDAO /*, IUserActivityLogService userActivityLogService */) {
        this.productDAO = productDAO;
        // this.userActivityLogService = userActivityLogService;
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


    // --- Product Management (for Product Managers) ---

    @Override
    public Book addBook(Book book) throws SQLException, ValidationException {
        if (book == null) throw new ValidationException("Book data cannot be null.");
        validateProductPrice(book);
        // TODO: Check Product Manager daily ADD limits if applicable (problem statement says unlimited adds [cite: 522])
        // userActivityLogService.checkAddLimit(managerId); // Example
        book.setEntryDate(LocalDate.now()); // Set entry date
        productDAO.addBookDetails(book); // This DAO method should handle base product and book details
        return productDAO.getById(book.getProductId()) instanceof Book ? (Book) productDAO.getById(book.getProductId()) : null; // Re-fetch to confirm
    }

    @Override
    public CD addCD(CD cd) throws SQLException, ValidationException {
        if (cd == null) throw new ValidationException("CD data cannot be null.");
        validateProductPrice(cd);
        cd.setEntryDate(LocalDate.now());
        productDAO.addCDDetails(cd);
        return productDAO.getById(cd.getProductId()) instanceof CD ? (CD) productDAO.getById(cd.getProductId()) : null;
    }

    @Override
    public DVD addDVD(DVD dvd) throws SQLException, ValidationException {
        if (dvd == null) throw new ValidationException("DVD data cannot be null.");
        validateProductPrice(dvd);
        dvd.setEntryDate(LocalDate.now());
        productDAO.addDVDDetails(dvd);
        return productDAO.getById(dvd.getProductId()) instanceof DVD ? (DVD) productDAO.getById(dvd.getProductId()) : null;
    }

    @Override
    public Book updateBook(Book book) throws SQLException, ValidationException, ResourceNotFoundException {
        if (book == null || book.getProductId() == null) throw new ValidationException("Book data and ID cannot be null.");
        Product existingProduct = productDAO.getById(book.getProductId());
        if (existingProduct == null) throw new ResourceNotFoundException("Book with ID " + book.getProductId() + " not found for update.");
        if (!(existingProduct instanceof Book)) throw new ValidationException("Product ID " + book.getProductId() + " is not a Book.");

        validateProductPrice(book);
        // TODO: Check Product Manager daily UPDATE limits for product type/ID
        // userActivityLogService.checkUpdateDeleteLimit(managerId, 1);
        productDAO.updateBookDetails(book);
        return (Book) productDAO.getById(book.getProductId());
    }

    @Override
    public CD updateCD(CD cd) throws SQLException, ValidationException, ResourceNotFoundException {
        if (cd == null || cd.getProductId() == null) throw new ValidationException("CD data and ID cannot be null.");
        Product existingProduct = productDAO.getById(cd.getProductId());
        if (existingProduct == null) throw new ResourceNotFoundException("CD with ID " + cd.getProductId() + " not found for update.");
        if (!(existingProduct instanceof CD)) throw new ValidationException("Product ID " + cd.getProductId() + " is not a CD.");

        validateProductPrice(cd);
        // TODO: Check Product Manager daily UPDATE limits
        productDAO.updateCDDetails(cd);
        return (CD) productDAO.getById(cd.getProductId());
    }

    @Override
    public DVD updateDVD(DVD dvd) throws SQLException, ValidationException, ResourceNotFoundException {
        if (dvd == null || dvd.getProductId() == null) throw new ValidationException("DVD data and ID cannot be null.");
        Product existingProduct = productDAO.getById(dvd.getProductId());
        if (existingProduct == null) throw new ResourceNotFoundException("DVD with ID " + dvd.getProductId() + " not found for update.");
        if (!(existingProduct instanceof DVD)) throw new ValidationException("Product ID " + dvd.getProductId() + " is not a DVD.");

        validateProductPrice(dvd);
        // TODO: Check Product Manager daily UPDATE limits
        productDAO.updateDVDDetails(dvd);
        return (DVD) productDAO.getById(dvd.getProductId());
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

        // TODO: Check Product Manager daily DELETE limits
        // userActivityLogService.checkUpdateDeleteLimit(managerId, 1);

        // DB constraint ON DELETE RESTRICT for productID in ORDER_ITEM will prevent deletion if product is in an order.
        // This will manifest as an SQLException from the DAO, which should be caught and potentially re-thrown as ValidationException.
        try {
            productDAO.delete(productId);
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

        // TODO: Check Product Manager daily DELETE limits for the total count
        // userActivityLogService.checkUpdateDeleteLimit(managerId, productIds.size());

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

        // TODO: Check Product Manager daily PRICE UPDATE limits for this specific product
        // userActivityLogService.checkPriceUpdateLimit(managerId, productId); [cite: 536]

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

        product.setPrice(newPriceExclVAT);
        productDAO.updateBaseProduct(product); // Assuming price is on base product table
        return productDAO.getById(productId);
    }

    @Override
    public Product updateProductStock(String productId, int quantityChange) throws SQLException, ValidationException, ResourceNotFoundException {
        Product product = productDAO.getById(productId);
        if (product == null) {
            throw new ResourceNotFoundException("Product with ID " + productId + " not found.");
        }

        int newQuantity = product.getQuantityInStock() + quantityChange;
        if (newQuantity < 0) {
            throw new ValidationException("Stock quantity cannot be negative. Current stock: " + product.getQuantityInStock() + ", Change: " + quantityChange);
        }
        productDAO.updateStock(productId, newQuantity);
        product.setQuantityInStock(newQuantity); // Update the in-memory object as well
        return product;
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

    @Override
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
}
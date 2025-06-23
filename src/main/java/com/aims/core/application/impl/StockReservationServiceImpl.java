package com.aims.core.application.impl;

import com.aims.core.application.services.IStockReservationService;
import com.aims.core.entities.Product;
import com.aims.core.infrastructure.database.dao.IProductDAO;
import com.aims.core.shared.exceptions.InventoryException;
import com.aims.core.shared.exceptions.ResourceNotFoundException;
import com.aims.core.shared.exceptions.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Implementation of stock reservation service for preventing overselling during checkout.
 * Uses in-memory reservations with cleanup mechanisms for expired reservations.
 */
@Service
public class StockReservationServiceImpl implements IStockReservationService {
    
    private static final Logger logger = LoggerFactory.getLogger(StockReservationServiceImpl.class);
    private static final int DEFAULT_RESERVATION_TIMEOUT_MINUTES = 15;
    
    private final IProductDAO productDAO;
    private final Map<String, StockReservation> activeReservations = new ConcurrentHashMap<>();
    private final Map<String, ReentrantLock> productLocks = new ConcurrentHashMap<>();
    
    public StockReservationServiceImpl(IProductDAO productDAO) {
        this.productDAO = productDAO;
        
        // Start cleanup thread for expired reservations
        startCleanupThread();
    }
    
    /**
     * Internal class to represent a stock reservation
     */
    private static class StockReservation {
        private final String productId;
        private final int quantity;
        private final LocalDateTime expiresAt;
        private final String reservationId;
        
        public StockReservation(String productId, int quantity, String reservationId, int timeoutMinutes) {
            this.productId = productId;
            this.quantity = quantity;
            this.reservationId = reservationId;
            this.expiresAt = LocalDateTime.now().plusMinutes(timeoutMinutes);
        }
        
        public boolean isExpired() {
            return LocalDateTime.now().isAfter(expiresAt);
        }
        
        // Getters
        public String getProductId() { return productId; }
        public int getQuantity() { return quantity; }
        public LocalDateTime getExpiresAt() { return expiresAt; }
        public String getReservationId() { return reservationId; }
    }
    
    @Override
    public boolean reserveStock(String productId, int quantity, String reservationId, int timeoutMinutes) 
            throws SQLException, ResourceNotFoundException, ValidationException {
        
        if (productId == null || productId.trim().isEmpty()) {
            throw new ValidationException("Product ID cannot be null or empty");
        }
        if (quantity <= 0) {
            throw new ValidationException("Quantity must be positive");
        }
        if (reservationId == null || reservationId.trim().isEmpty()) {
            throw new ValidationException("Reservation ID cannot be null or empty");
        }
        if (timeoutMinutes <= 0) {
            timeoutMinutes = DEFAULT_RESERVATION_TIMEOUT_MINUTES;
        }
        
        logger.info("Attempting to reserve {} units of product {} for reservation {}", 
                   quantity, productId, reservationId);
        
        // Get or create product-specific lock
        ReentrantLock productLock = productLocks.computeIfAbsent(productId, k -> new ReentrantLock());
        
        productLock.lock();
        try {
            // Check if product exists
            Product product = productDAO.getById(productId);
            if (product == null) {
                throw new ResourceNotFoundException("Product with ID " + productId + " not found");
            }
            
            // Calculate currently available stock
            int availableStock = calculateAvailableStock(productId, product.getQuantityInStock());
            
            if (availableStock < quantity) {
                logger.warn("Insufficient stock for reservation. Available: {}, Requested: {}", 
                           availableStock, quantity);
                return false;
            }
            
            // Create reservation
            StockReservation reservation = new StockReservation(productId, quantity, reservationId, timeoutMinutes);
            activeReservations.put(reservationId, reservation);
            
            logger.info("Successfully reserved {} units of product {} for reservation {} (expires at {})", 
                       quantity, productId, reservationId, reservation.getExpiresAt());
            
            return true;
            
        } finally {
            productLock.unlock();
        }
    }
    
    @Override
    public void confirmReservation(String reservationId) 
            throws SQLException, InventoryException {
        
        if (reservationId == null || reservationId.trim().isEmpty()) {
            throw new InventoryException("Reservation ID cannot be null or empty");
        }
        
        StockReservation reservation = activeReservations.get(reservationId);
        if (reservation == null) {
            throw new InventoryException("Reservation " + reservationId + " not found");
        }
        
        if (reservation.isExpired()) {
            activeReservations.remove(reservationId);
            throw new InventoryException("Reservation " + reservationId + " has expired");
        }
        
        String productId = reservation.getProductId();
        ReentrantLock productLock = productLocks.computeIfAbsent(productId, k -> new ReentrantLock());
        
        productLock.lock();
        try {
            Product product = productDAO.getById(productId);
            if (product == null) {
                throw new InventoryException("Product " + productId + " not found for reservation confirmation");
            }
            
            // Actually decrease the stock
            int newStock = product.getQuantityInStock() - reservation.getQuantity();
            if (newStock < 0) {
                throw new InventoryException("Cannot confirm reservation - insufficient actual stock");
            }
            
            productDAO.updateStock(productId, newStock);
            activeReservations.remove(reservationId);
            
            logger.info("Confirmed reservation {} - decreased stock for product {} by {} units", 
                       reservationId, productId, reservation.getQuantity());
            
        } finally {
            productLock.unlock();
        }
    }
    
    @Override
    public void releaseReservation(String reservationId) throws SQLException {
        if (reservationId == null || reservationId.trim().isEmpty()) {
            return;
        }
        
        StockReservation reservation = activeReservations.remove(reservationId);
        if (reservation != null) {
            logger.info("Released reservation {} for {} units of product {}", 
                       reservationId, reservation.getQuantity(), reservation.getProductId());
        }
    }
    
    @Override
    public boolean isStockAvailable(String productId, int quantity) 
            throws SQLException, ResourceNotFoundException {
        
        Product product = productDAO.getById(productId);
        if (product == null) {
            throw new ResourceNotFoundException("Product with ID " + productId + " not found");
        }
        
        int availableStock = calculateAvailableStock(productId, product.getQuantityInStock());
        return availableStock >= quantity;
    }
    
    @Override
    public int getAvailableStock(String productId) 
            throws SQLException, ResourceNotFoundException {
        
        Product product = productDAO.getById(productId);
        if (product == null) {
            throw new ResourceNotFoundException("Product with ID " + productId + " not found");
        }
        
        return calculateAvailableStock(productId, product.getQuantityInStock());
    }
    
    @Override
    public int cleanupExpiredReservations() throws SQLException {
        int cleanedUp = 0;
        
        for (Map.Entry<String, StockReservation> entry : activeReservations.entrySet()) {
            if (entry.getValue().isExpired()) {
                activeReservations.remove(entry.getKey());
                cleanedUp++;
                logger.debug("Cleaned up expired reservation: {}", entry.getKey());
            }
        }
        
        if (cleanedUp > 0) {
            logger.info("Cleaned up {} expired stock reservations", cleanedUp);
        }
        
        return cleanedUp;
    }
    
    @Override
    public Map<String, String> getActiveReservations() throws SQLException {
        Map<String, String> reservationDetails = new ConcurrentHashMap<>();
        
        for (Map.Entry<String, StockReservation> entry : activeReservations.entrySet()) {
            StockReservation reservation = entry.getValue();
            String details = String.format("Product: %s, Quantity: %d, Expires: %s", 
                                          reservation.getProductId(), 
                                          reservation.getQuantity(), 
                                          reservation.getExpiresAt());
            reservationDetails.put(entry.getKey(), details);
        }
        
        return reservationDetails;
    }
    
    /**
     * Calculates available stock by subtracting active reservations from actual stock
     */
    private int calculateAvailableStock(String productId, int actualStock) {
        int reservedQuantity = activeReservations.values().stream()
                .filter(r -> r.getProductId().equals(productId) && !r.isExpired())
                .mapToInt(StockReservation::getQuantity)
                .sum();
        
        return Math.max(0, actualStock - reservedQuantity);
    }
    
    /**
     * Starts a background thread to clean up expired reservations
     */
    private void startCleanupThread() {
        Thread cleanupThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(60000); // Check every minute
                    cleanupExpiredReservations();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.info("Stock reservation cleanup thread interrupted");
                    break;
                } catch (Exception e) {
                    logger.error("Error during stock reservation cleanup", e);
                }
            }
        });
        
        cleanupThread.setDaemon(true);
        cleanupThread.setName("StockReservationCleanup");
        cleanupThread.start();
        
        logger.info("Started stock reservation cleanup thread");
    }
}
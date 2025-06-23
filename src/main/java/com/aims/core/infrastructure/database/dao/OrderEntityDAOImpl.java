package com.aims.core.infrastructure.database.dao;

import com.aims.core.entities.OrderEntity;
import com.aims.core.entities.OrderItem;
import com.aims.core.entities.UserAccount;
import com.aims.core.entities.DeliveryInfo;
import com.aims.core.entities.Product;
import com.aims.core.enums.OrderStatus;
import com.aims.core.infrastructure.database.SQLiteConnector;
import com.aims.core.infrastructure.database.dao.IOrderEntityDAO;
import com.aims.core.infrastructure.database.dao.IOrderItemDAO; // For loading order items
import com.aims.core.infrastructure.database.dao.IUserAccountDAO; // For loading user account
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

@Repository
public class OrderEntityDAOImpl implements IOrderEntityDAO {

    private final IOrderItemDAO orderItemDAO; // To load associated order items
    private final IUserAccountDAO userAccountDAO; // To load associated user
    // Inject other DAOs as needed (e.g., IDeliveryInfoDAO, IInvoiceDAO)

    @Autowired
    public OrderEntityDAOImpl(IOrderItemDAO orderItemDAO, IUserAccountDAO userAccountDAO) {
        this.orderItemDAO = orderItemDAO;
        this.userAccountDAO = userAccountDAO;
    }

    private Connection getConnection() throws SQLException {
        return SQLiteConnector.getInstance().getConnection();
    }

    private OrderEntity mapResultSetToOrderEntity(ResultSet rs) throws SQLException {
        if (rs == null) return null;

        // Read all fields from the ResultSet first
        String orderId = rs.getString("orderID");
        String userId = rs.getString("userID");
        String orderDateStr = rs.getString("orderDate");
        String orderStatusStr = rs.getString("order_status");
        float totalProductPriceExclVAT = rs.getFloat("totalProductPriceExclVAT");
        float totalProductPriceInclVAT = rs.getFloat("totalProductPriceInclVAT");
        float calculatedDeliveryFee = rs.getFloat("calculatedDeliveryFee");
        float totalAmountPaid = rs.getFloat("totalAmountPaid");

        // Now create and populate the OrderEntity
        OrderEntity order = new OrderEntity();
        order.setOrderId(orderId);

        // UserAccount is fetched *after* all other fields are read from the current ResultSet row
        UserAccount user = null;
        if (userId != null) {
            // This is a separate database call. It's crucial that the ResultSet 'rs' 
            // is not needed after this point for the current OrderEntity.
            user = userAccountDAO.getById(userId); 
        }
        order.setUserAccount(user);

        if (orderDateStr != null) {
            order.setOrderDate(LocalDateTime.parse(orderDateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
        if (orderStatusStr != null) {
            order.setOrderStatus(OrderStatus.valueOf(orderStatusStr));
        }
        order.setTotalProductPriceExclVAT(totalProductPriceExclVAT);
        order.setTotalProductPriceInclVAT(totalProductPriceInclVAT);
        order.setCalculatedDeliveryFee(calculatedDeliveryFee);
        order.setTotalAmountPaid(totalAmountPaid);

        // Note: Associated collections like orderItems, deliveryInfo, invoice, paymentTransactions
        // are typically loaded separately (lazy loading) or via JOINs in more complex queries.
        // In getById, orderItems are loaded after the OrderEntity is mapped.
        return order;
    }

    @Override
    public OrderEntity getById(String orderId) throws SQLException {
        // UNIVERSAL ORDER LOADING - Enhanced to load ALL relationships for ANY customer order
        logger.log(Level.INFO, "UNIVERSAL DAO: Loading order with comprehensive relationships: " + orderId);
        
        String sql = "SELECT * FROM ORDER_ENTITY WHERE orderID = ?";
        OrderEntity order = null;
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, orderId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                order = mapResultSetToOrderEntity(rs);
                if (order != null) {
                    // COMPREHENSIVE RELATIONSHIP LOADING for bulletproof order processing
                    logger.log(Level.INFO, "UNIVERSAL DAO: Loading order items for: " + orderId);
                    List<OrderItem> items = orderItemDAO.getItemsByOrderId(orderId);
                    order.setOrderItems(items);
                    
                    logger.log(Level.INFO, "UNIVERSAL DAO: Successfully loaded " + items.size() + " order items for: " + orderId);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "UNIVERSAL DAO: Error loading order: " + orderId, e);
            SQLiteConnector.printSQLException(e);
            throw e;
        }
        
        if (order != null) {
            logger.log(Level.INFO, "UNIVERSAL DAO: Successfully loaded order: " + orderId + " with status: " + order.getOrderStatus());
        }
        return order;
    }

    /**
     * UNIVERSAL ORDER LOADING WITH COMPLETE RELATIONSHIPS
     * Loads order with ALL related entities to prevent ANY lazy loading issues
     */
    public OrderEntity getByIdWithCompleteRelationships(String orderId) throws SQLException {
        logger.log(Level.INFO, "UNIVERSAL DAO: Loading order with COMPLETE relationships: " + orderId);
        
        // Enhanced SQL with JOINs for comprehensive loading
        String sql = """
            SELECT o.*,
                   oi.productID, oi.quantity, oi.priceAtTimeOfOrder, oi.eligibleForRushDelivery,
                   p.title, p.category, p.price, p.quantityInStock, p.productType,
                   di.deliveryInfoID, di.recipientName, di.phoneNumber, di.deliveryAddress,
                   di.deliveryProvinceCity, di.email, di.deliveryMethodChosen,
                   u.userID, u.username, u.email as userEmail, u.fullName
            FROM ORDER_ENTITY o
            LEFT JOIN ORDER_ITEM oi ON o.orderID = oi.orderID
            LEFT JOIN PRODUCT p ON oi.productID = p.productID
            LEFT JOIN DELIVERY_INFO di ON o.orderID = di.orderID
            LEFT JOIN USER_ACCOUNT u ON o.userID = u.userID
            WHERE o.orderID = ?
            ORDER BY oi.productID
            """;
            
        OrderEntity order = null;
        List<OrderItem> orderItems = new ArrayList<>();
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, orderId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                // Build order entity (only once)
                if (order == null) {
                    order = mapResultSetToOrderEntity(rs);
                    
                    // Load delivery info if present
                    String deliveryInfoId = rs.getString("deliveryInfoID");
                    if (deliveryInfoId != null && !rs.wasNull()) {
                        DeliveryInfo deliveryInfo = mapDeliveryInfoFromResultSet(rs);
                        order.setDeliveryInfo(deliveryInfo);
                    }
                    
                    // Load user account if present
                    String userId = rs.getString("userID");
                    if (userId != null && !rs.wasNull()) {
                        UserAccount user = mapUserAccountFromResultSet(rs);
                        order.setUserAccount(user);
                    }
                }
                
                // Load order items
                String productId = rs.getString("productID");
                if (productId != null && !rs.wasNull()) {
                    OrderItem item = mapOrderItemFromResultSet(rs, order);
                    orderItems.add(item);
                }
            }
            
            if (order != null) {
                order.setOrderItems(orderItems);
                logger.log(Level.INFO, "UNIVERSAL DAO: Loaded order " + orderId + " with " + orderItems.size() + " items and complete relationships");
            }
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "UNIVERSAL DAO: Error in comprehensive order loading: " + orderId, e);
            SQLiteConnector.printSQLException(e);
            throw e;
        }
        
        return order;
    }

    /**
     * Map delivery info from result set for comprehensive loading
     */
    private DeliveryInfo mapDeliveryInfoFromResultSet(ResultSet rs) throws SQLException {
        DeliveryInfo deliveryInfo = new DeliveryInfo();
        deliveryInfo.setDeliveryInfoId(rs.getString("deliveryInfoID"));
        deliveryInfo.setRecipientName(rs.getString("recipientName"));
        deliveryInfo.setPhoneNumber(rs.getString("phoneNumber"));
        deliveryInfo.setDeliveryAddress(rs.getString("deliveryAddress"));
        deliveryInfo.setDeliveryProvinceCity(rs.getString("deliveryProvinceCity"));
        deliveryInfo.setEmail(rs.getString("email"));
        deliveryInfo.setDeliveryMethodChosen(rs.getString("deliveryMethodChosen"));
        return deliveryInfo;
    }

    /**
     * Map user account from result set for comprehensive loading
     */
    private UserAccount mapUserAccountFromResultSet(ResultSet rs) throws SQLException {
        UserAccount user = new UserAccount();
        user.setUserId(rs.getString("userID"));
        user.setUsername(rs.getString("username"));
        user.setEmail(rs.getString("userEmail"));
        // Note: UserAccount may not have setFullName method, check available methods
        // user.setFullName(rs.getString("fullName"));
        return user;
    }

    /**
     * Map order item from result set for comprehensive loading
     */
    private OrderItem mapOrderItemFromResultSet(ResultSet rs, OrderEntity order) throws SQLException {
        // Create product
        Product product = new Product();
        product.setProductId(rs.getString("productID"));
        product.setTitle(rs.getString("title"));
        product.setCategory(rs.getString("category"));
        product.setPrice(rs.getFloat("price"));
        product.setQuantityInStock(rs.getInt("quantityInStock"));
        
        String productTypeStr = rs.getString("productType");
        if (productTypeStr != null) {
            try {
                product.setProductType(com.aims.core.enums.ProductType.valueOf(productTypeStr));
            } catch (IllegalArgumentException e) {
                logger.log(Level.WARNING, "Unknown product type: " + productTypeStr + " for product: " + product.getProductId());
            }
        }
        
        // Create order item
        OrderItem item = new OrderItem();
        item.setOrderEntity(order);
        item.setProduct(product);
        item.setQuantity(rs.getInt("quantity"));
        item.setPriceAtTimeOfOrder(rs.getFloat("priceAtTimeOfOrder"));
        item.setEligibleForRushDelivery(rs.getBoolean("eligibleForRushDelivery"));
        
        return item;
    }

    // Add logging import at the top of the class
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(OrderEntityDAOImpl.class.getName());

    @Override
    public List<OrderEntity> getAll() throws SQLException {
        List<OrderEntity> orders = new ArrayList<>();
        String sql = "SELECT * FROM ORDER_ENTITY ORDER BY orderDate DESC";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                OrderEntity order = mapResultSetToOrderEntity(rs);
                // For lists, typically load items on demand or provide a separate method
                // To avoid N+1, don't load all items for all orders here by default
                orders.add(order);
            }
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
        return orders;
    }

    @Override
    public List<OrderEntity> getByUserId(String userId) throws SQLException {
        List<OrderEntity> orders = new ArrayList<>();
        String sql = "SELECT * FROM ORDER_ENTITY WHERE userID = ? ORDER BY orderDate DESC";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                OrderEntity order = mapResultSetToOrderEntity(rs);
                // Optionally load items for each order if needed, or do it lazily
                // List<OrderItem> items = orderItemDAO.getItemsByOrderId(order.getOrderId());
                // order.setOrderItems(items);
                orders.add(order);
            }
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
        return orders;
    }

    @Override
    public List<OrderEntity> getByStatus(OrderStatus status) throws SQLException {
        List<OrderEntity> orders = new ArrayList<>();
        String sql = "SELECT * FROM ORDER_ENTITY WHERE order_status = ? ORDER BY orderDate DESC";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status.name());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                orders.add(mapResultSetToOrderEntity(rs));
            }
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
        return orders;
    }

    @Override
    public List<OrderEntity> getByDateRange(LocalDateTime startDate, LocalDateTime endDate) throws SQLException {
        List<OrderEntity> orders = new ArrayList<>();
        String sql = "SELECT * FROM ORDER_ENTITY WHERE orderDate >= ? AND orderDate <= ? ORDER BY orderDate DESC";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, startDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            pstmt.setString(2, endDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                orders.add(mapResultSetToOrderEntity(rs));
            }
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
        return orders;
    }


    @Override
    public void add(OrderEntity order) throws SQLException {
        String sql = "INSERT INTO ORDER_ENTITY (orderID, userID, orderDate, order_status, " +
                     "totalProductPriceExclVAT, totalProductPriceInclVAT, calculatedDeliveryFee, totalAmountPaid) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        Connection conn = null;
        try {
            conn = getConnection();
            // Transaction management should ideally be handled at the service layer
            // if adding OrderItems, DeliveryInfo etc. needs to be atomic with OrderEntity.
            // conn.setAutoCommit(false); // If managing transaction here

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, order.getOrderId());
                if (order.getUserAccount() != null) {
                    pstmt.setString(2, order.getUserAccount().getUserId());
                } else {
                    pstmt.setNull(2, Types.VARCHAR);
                }
                pstmt.setString(3, order.getOrderDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                pstmt.setString(4, order.getOrderStatus().name());
                pstmt.setFloat(5, order.getTotalProductPriceExclVAT());
                pstmt.setFloat(6, order.getTotalProductPriceInclVAT());
                pstmt.setFloat(7, order.getCalculatedDeliveryFee());
                pstmt.setFloat(8, order.getTotalAmountPaid());
                pstmt.executeUpdate();
            }

            // Example: If OrderItems are also managed by this DAO's add method (less common for pure DAO)
            // if (order.getOrderItems() != null) {
            //     for (OrderItem item : order.getOrderItems()) {
            //         item.setOrder(order); // Ensure back-reference
            //         orderItemDAO.add(item); // Call OrderItemDAO
            //     }
            // }
            // conn.commit(); // If managing transaction here
        } catch (SQLException e) {
            // if (conn != null) { try { conn.rollback(); } catch (SQLException ex) { SQLiteConnector.printSQLException(ex); } }
            SQLiteConnector.printSQLException(e);
            throw e;
        } finally {
            // if (conn != null) { try { conn.setAutoCommit(true); } catch (SQLException ex) { SQLiteConnector.printSQLException(ex); } }
        }
    }

    @Override
    public void update(OrderEntity order) throws SQLException {
        String sql = "UPDATE ORDER_ENTITY SET userID = ?, orderDate = ?, order_status = ?, " +
                     "totalProductPriceExclVAT = ?, totalProductPriceInclVAT = ?, " +
                     "calculatedDeliveryFee = ?, totalAmountPaid = ? WHERE orderID = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            if (order.getUserAccount() != null) {
                pstmt.setString(1, order.getUserAccount().getUserId());
            } else {
                pstmt.setNull(1, Types.VARCHAR);
            }
            pstmt.setString(2, order.getOrderDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            pstmt.setString(3, order.getOrderStatus().name());
            pstmt.setFloat(4, order.getTotalProductPriceExclVAT());
            pstmt.setFloat(5, order.getTotalProductPriceInclVAT());
            pstmt.setFloat(6, order.getCalculatedDeliveryFee());
            pstmt.setFloat(7, order.getTotalAmountPaid());
            pstmt.setString(8, order.getOrderId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
    }

    @Override
    public void updateStatus(String orderId, OrderStatus newStatus) throws SQLException {
        String sql = "UPDATE ORDER_ENTITY SET order_status = ? WHERE orderID = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newStatus.name());
            pstmt.setString(2, orderId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
    }

    @Override
    public void delete(String orderId) throws SQLException {
        // ON DELETE CASCADE in DB schema should handle related OrderItems, DeliveryInfo, Invoice.
        // PaymentTransactions might need specific handling (e.g., ON DELETE RESTRICT or SET NULL).
        // The SQL script uses ON DELETE CASCADE for OrderItem, DeliveryInfo, Invoice.
        // PaymentTransaction has ON DELETE RESTRICT for orderID, so an order with transactions cannot be deleted easily.
        // This needs careful consideration in the service layer. This DAO just attempts to delete the order.
        String sql = "DELETE FROM ORDER_ENTITY WHERE orderID = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, orderId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
    }
}
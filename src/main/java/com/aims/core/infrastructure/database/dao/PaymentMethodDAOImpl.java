package com.aims.core.infrastructure.database.dao;

import com.aims.core.entities.PaymentMethod;
import com.aims.core.entities.CardDetails;
import com.aims.core.entities.UserAccount;
import com.aims.core.enums.PaymentMethodType;
import com.aims.core.infrastructure.database.SQLiteConnector;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PaymentMethodDAOImpl implements IPaymentMethodDAO {

    private final IUserAccountDAO userAccountDAO;
    private final ICardDetailsDAO cardDetailsDAO;

    public PaymentMethodDAOImpl(IUserAccountDAO userAccountDAO, ICardDetailsDAO cardDetailsDAO) {
        this.userAccountDAO = userAccountDAO;
        this.cardDetailsDAO = cardDetailsDAO;
    }

    private Connection getConnection() throws SQLException {
        Connection conn = SQLiteConnector.getInstance().getConnection();
        if (conn == null || conn.isClosed()) {
            throw new SQLException("Database connection is closed or unavailable.");
        }
        return conn;
    }

    private PaymentMethod mapResultSetToPaymentMethod(ResultSet rs) throws SQLException {
        if (rs == null) return null;

        PaymentMethod pm = new PaymentMethod();
        pm.setPaymentMethodId(rs.getString("paymentMethodID"));
        pm.setMethodType(PaymentMethodType.valueOf(rs.getString("methodType")));

        String userId = rs.getString("userID");
        if (userId != null) {
            UserAccount user = userAccountDAO.getById(userId);
            pm.setUserAccount(user);
        }
        pm.setDefault(rs.getInt("isDefault") == 1);

        if (pm.getMethodType() == PaymentMethodType.CREDIT_CARD || pm.getMethodType() == PaymentMethodType.DOMESTIC_DEBIT_CARD) {
            CardDetails details = cardDetailsDAO.getByPaymentMethodId(pm.getPaymentMethodId());
            pm.setCardDetails(details);
        }
        return pm;
    }

    @Override
    public void add(PaymentMethod paymentMethod) throws SQLException {
        String sql = "INSERT INTO PAYMENT_METHOD (paymentMethodID, methodType, userID, isDefault) " +
                     "VALUES (?, ?, ?, ?)";
        Connection conn = null;
        boolean originalAutoCommit = true;
        try {
            conn = getConnection();
            originalAutoCommit = conn.getAutoCommit();

            if (paymentMethod.isDefault() && paymentMethod.getUserAccount() != null) {
                if (originalAutoCommit) {
                    conn.setAutoCommit(false);
                }
                clearDefaultForUserInternal(conn, paymentMethod.getUserAccount().getUserId());
            }

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, paymentMethod.getPaymentMethodId());
                pstmt.setString(2, paymentMethod.getMethodType().name());
                if (paymentMethod.getUserAccount() != null) {
                    pstmt.setString(3, paymentMethod.getUserAccount().getUserId());
                } else {
                    pstmt.setNull(3, Types.VARCHAR);
                }
                pstmt.setInt(4, paymentMethod.isDefault() ? 1 : 0);
                pstmt.executeUpdate();
            }

            if (paymentMethod.getCardDetails() != null &&
                (paymentMethod.getMethodType() == PaymentMethodType.CREDIT_CARD ||
                 paymentMethod.getMethodType() == PaymentMethodType.DOMESTIC_DEBIT_CARD)) {
                cardDetailsDAO.add(paymentMethod.getCardDetails());
            }

            if (!originalAutoCommit) {
                conn.commit();
            }

        } catch (SQLException e) {
            if (conn != null && !originalAutoCommit) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    SQLiteConnector.printSQLException(ex);
                }
            }
            SQLiteConnector.printSQLException(e);
            throw e;
        } finally {
            if (conn != null && !originalAutoCommit) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException ex) {
                    SQLiteConnector.printSQLException(ex);
                }
            }
        }
    }

    @Override
    public void update(PaymentMethod paymentMethod) throws SQLException {
        String sql = "UPDATE PAYMENT_METHOD SET methodType = ?, userID = ?, isDefault = ? " +
                     "WHERE paymentMethodID = ?";
        Connection conn = null;
        boolean originalAutoCommit = true;
        try {
            conn = getConnection();
            originalAutoCommit = conn.getAutoCommit();

            if (paymentMethod.isDefault() && paymentMethod.getUserAccount() != null) {
                if (originalAutoCommit) {
                    conn.setAutoCommit(false);
                }
                clearDefaultForUserInternal(conn, paymentMethod.getUserAccount().getUserId(), paymentMethod.getPaymentMethodId());
            }

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, paymentMethod.getMethodType().name());
                if (paymentMethod.getUserAccount() != null) {
                    pstmt.setString(2, paymentMethod.getUserAccount().getUserId());
                } else {
                    pstmt.setNull(2, Types.VARCHAR);
                }
                pstmt.setInt(3, paymentMethod.isDefault() ? 1 : 0);
                pstmt.setString(4, paymentMethod.getPaymentMethodId());
                pstmt.executeUpdate();
            }

            if (paymentMethod.getCardDetails() != null) {
                cardDetailsDAO.update(paymentMethod.getCardDetails());
            }
            if (!originalAutoCommit) {
                conn.commit();
            }
        } catch (SQLException e) {
            if (conn != null && !originalAutoCommit) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    SQLiteConnector.printSQLException(ex);
                }
            }
            SQLiteConnector.printSQLException(e);
            throw e;
        } finally {
             if (conn != null && !originalAutoCommit) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException ex) {
                    SQLiteConnector.printSQLException(ex);
                }
            }
        }
    }

    @Override
    public PaymentMethod getById(String paymentMethodId) throws SQLException {
        String sql = "SELECT * FROM PAYMENT_METHOD WHERE paymentMethodID = ?";
        Connection conn = getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, paymentMethodId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToPaymentMethod(rs);
                }
            }
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
        return null;
    }

    @Override
    public List<PaymentMethod> getByUserId(String userId) throws SQLException {
        List<PaymentMethod> methods = new ArrayList<>();
        String sql = "SELECT * FROM PAYMENT_METHOD WHERE userID = ?";
        Connection conn = getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    methods.add(mapResultSetToPaymentMethod(rs));
                }
            }
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
        return methods;
    }

    @Override
    public List<PaymentMethod> getAll() throws SQLException {
        List<PaymentMethod> methods = new ArrayList<>();
        String sql = "SELECT * FROM PAYMENT_METHOD";
        Connection conn = getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                methods.add(mapResultSetToPaymentMethod(rs));
            }
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
        return methods;
    }

    @Override
    public void delete(String paymentMethodId) throws SQLException {
        CardDetails cardDetails = null;
        PaymentMethod pm = null;
        Connection conn = getConnection(); // Get connection once for multiple operations if needed

        // Fetch to check type - use the existing connection
        String sqlSelect = "SELECT methodType FROM PAYMENT_METHOD WHERE paymentMethodID = ?";
        try (PreparedStatement pstmtSelect = conn.prepareStatement(sqlSelect)) {
            pstmtSelect.setString(1, paymentMethodId);
            try (ResultSet rs = pstmtSelect.executeQuery()) {
                if (rs.next()) {
                    PaymentMethodType type = PaymentMethodType.valueOf(rs.getString("methodType"));
                    if (type == PaymentMethodType.CREDIT_CARD || type == PaymentMethodType.DOMESTIC_DEBIT_CARD) {
                        // cardDetailsDAO.getByPaymentMethodId should use its own connection logic or be passed one
                        cardDetails = cardDetailsDAO.getByPaymentMethodId(paymentMethodId);
                    }
                }
            }
        }
        // If cardDetailsDAO.getByPaymentMethodId itself uses SQLiteConnector.getInstance().getConnection(), it's fine.

        if (cardDetails != null) {
            cardDetailsDAO.delete(cardDetails.getPaymentMethodId());
        }

        String sqlDelete = "DELETE FROM PAYMENT_METHOD WHERE paymentMethodID = ?";
        try (PreparedStatement pstmtDelete = conn.prepareStatement(sqlDelete)) {
            pstmtDelete.setString(1, paymentMethodId);
            pstmtDelete.executeUpdate();
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
    }

    @Override
    public PaymentMethod getDefaultByUserId(String userId) throws SQLException {
        String sql = "SELECT * FROM PAYMENT_METHOD WHERE userID = ? AND isDefault = 1";
        Connection conn = getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToPaymentMethod(rs);
                }
            }
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
        return null;
    }

    @Override
    public void clearDefaultForUser(String userId) throws SQLException {
        Connection conn = null;
        boolean originalAutoCommit = true;
        try {
            conn = getConnection();
            originalAutoCommit = conn.getAutoCommit();
            if (originalAutoCommit) {
                conn.setAutoCommit(false);
            }
            clearDefaultForUserInternal(conn, userId);
            if (originalAutoCommit) {
                conn.commit();
            }
        } catch (SQLException e) {
            if (conn != null && originalAutoCommit) {
                try { conn.rollback(); } catch (SQLException ex) { SQLiteConnector.printSQLException(ex); }
            }
            SQLiteConnector.printSQLException(e);
            throw e;
        } finally {
            if (conn != null && originalAutoCommit) {
                try { conn.setAutoCommit(true); } catch (SQLException ex) { SQLiteConnector.printSQLException(ex); }
            }
        }
    }

    private void clearDefaultForUserInternal(Connection conn, String userId) throws SQLException {
        String sqlClear = "UPDATE PAYMENT_METHOD SET isDefault = 0 WHERE userID = ? AND isDefault = 1";
        try (PreparedStatement pstmtClear = conn.prepareStatement(sqlClear)) {
            pstmtClear.setString(1, userId);
            pstmtClear.executeUpdate();
        }
    }

    private void clearDefaultForUserInternal(Connection conn, String userId, String excludePaymentMethodId) throws SQLException {
        String sqlClear = "UPDATE PAYMENT_METHOD SET isDefault = 0 WHERE userID = ? AND isDefault = 1 AND paymentMethodID != ?";
        try (PreparedStatement pstmtClear = conn.prepareStatement(sqlClear)) {
            pstmtClear.setString(1, userId);
            pstmtClear.setString(2, excludePaymentMethodId);
            pstmtClear.executeUpdate();
        }
    }

    @Override
    public void setDefault(String userId, String paymentMethodId) throws SQLException {
        Connection conn = null;
        boolean originalAutoCommit = true;
        try {
            conn = getConnection();
            originalAutoCommit = conn.getAutoCommit();
            if (originalAutoCommit) {
                conn.setAutoCommit(false);
            }

            clearDefaultForUserInternal(conn, userId);

            String sqlSetDefault = "UPDATE PAYMENT_METHOD SET isDefault = 1 WHERE userID = ? AND paymentMethodID = ?";
            try (PreparedStatement pstmtSetDefault = conn.prepareStatement(sqlSetDefault)) {
                pstmtSetDefault.setString(1, userId);
                pstmtSetDefault.setString(2, paymentMethodId);
                int rowsAffected = pstmtSetDefault.executeUpdate();
                if (rowsAffected == 0) {
                }
            }

            if (originalAutoCommit) {
                conn.commit();
            }

        } catch (SQLException e) {
            if (conn != null && originalAutoCommit) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    SQLiteConnector.printSQLException(ex);
                }
            }
            SQLiteConnector.printSQLException(e);
            throw e;
        } finally {
            if (conn != null && originalAutoCommit) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException ex) {
                    SQLiteConnector.printSQLException(ex);
                }
            }
        }
    }

    @Override
    public List<PaymentMethod> getByMethodType(PaymentMethodType methodType) throws SQLException {
        List<PaymentMethod> methods = new ArrayList<>();
        String sql = "SELECT * FROM PAYMENT_METHOD WHERE methodType = ?";
        Connection conn = getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, methodType.name());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    methods.add(mapResultSetToPaymentMethod(rs));
                }
            }
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
        return methods;
    }
}
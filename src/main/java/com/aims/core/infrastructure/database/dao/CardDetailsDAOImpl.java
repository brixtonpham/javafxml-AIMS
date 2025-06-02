package com.aims.core.infrastructure.database.dao;

import com.aims.core.entities.CardDetails;
import com.aims.core.entities.PaymentMethod; // For setting the PaymentMethod reference
import com.aims.core.infrastructure.database.SQLiteConnector;
// import com.aims.core.infrastructure.database.dao.ICardDetailsDAO; // Unused import
// Assuming IPaymentMethodDAO exists if we need to fully reconstruct PaymentMethod,
// but usually not needed when just mapping CardDetails.
// import com.aims.infrastructure.database.dao.IPaymentMethodDAO;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types; // For setNull

public class CardDetailsDAOImpl implements ICardDetailsDAO {

    // Optional: Inject IPaymentMethodDAO if you need to fetch the full PaymentMethod object
    // private final IPaymentMethodDAO paymentMethodDAO;
    // public CardDetailsDAOImpl(IPaymentMethodDAO paymentMethodDAO) {
    //     this.paymentMethodDAO = paymentMethodDAO;
    // }

    public CardDetailsDAOImpl() {
        // Default constructor
    }

    private Connection getConnection() throws SQLException {
        return SQLiteConnector.getInstance().getConnection();
    }

    private CardDetails mapResultSetToCardDetails(ResultSet rs) throws SQLException {
        if (rs == null) return null;

        CardDetails details = new CardDetails();

        // To fully set the PaymentMethod object, you might need IPaymentMethodDAO.
        // For simplicity here, we'll create a placeholder PaymentMethod with just the ID,
        // assuming the service layer or the entity itself handles lazy loading or reconstruction if needed.
        PaymentMethod pm = new PaymentMethod();
        pm.setPaymentMethodId(rs.getString("paymentMethodID"));
        details.setPaymentMethod(pm); // Set the PaymentMethod object reference

        details.setCardholderName(rs.getString("cardholderName"));
        details.setCardNumberMasked(rs.getString("cardNumber_masked"));
        details.setExpiryDateMMYY(rs.getString("expiryDate_MMYY"));
        details.setValidFromDateMMYY(rs.getString("validFromDate_MMYY"));
        details.setIssuingBank(rs.getString("issuingBank")); // Corrected typo from setIsSuingBank to setIssuingBank

        return details;
    }

    @Override
    public void add(CardDetails cardDetails) throws SQLException {
        // Assumes paymentMethodID in cardDetails is already set (linking to an existing PaymentMethod)
        // The CardDetails PK is the PaymentMethod's PK.
        String sql = "INSERT INTO CARD_DETAILS (paymentMethodID, cardholderName, cardNumber_masked, " +
                     "expiryDate_MMYY, validFromDate_MMYY, issuingBank) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, cardDetails.getPaymentMethodId()); // Or cardDetails.getPaymentMethod().getPaymentMethodId()
            pstmt.setString(2, cardDetails.getCardholderName());
            pstmt.setString(3, cardDetails.getCardNumberMasked());
            pstmt.setString(4, cardDetails.getExpiryDateMMYY());
            if (cardDetails.getValidFromDateMMYY() != null) {
                pstmt.setString(5, cardDetails.getValidFromDateMMYY());
            } else {
                pstmt.setNull(5, Types.VARCHAR);
            }
            pstmt.setString(6, cardDetails.getIssuingBank());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            // Check for specific SQLite error code for UNIQUE constraint violation (PK violation)
            // Error code 19: SQLITE_CONSTRAINT
            if (e.getErrorCode() == 19 && e.getMessage().toLowerCase().contains("card_details.paymentmethodid")) {
                 throw new SQLException("CardDetails for paymentMethodID '" + cardDetails.getPaymentMethodId() + "' already exists.", e.getSQLState(), e.getErrorCode(), e);
            }
            SQLiteConnector.printSQLException(e);
            throw e;
        }
    }

    @Override
    public void update(CardDetails cardDetails) throws SQLException {
        String sql = "UPDATE CARD_DETAILS SET cardholderName = ?, cardNumber_masked = ?, " +
                     "expiryDate_MMYY = ?, validFromDate_MMYY = ?, issuingBank = ? " +
                     "WHERE paymentMethodID = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, cardDetails.getCardholderName());
            pstmt.setString(2, cardDetails.getCardNumberMasked());
            pstmt.setString(3, cardDetails.getExpiryDateMMYY());
            if (cardDetails.getValidFromDateMMYY() != null) {
                pstmt.setString(4, cardDetails.getValidFromDateMMYY());
            } else {
                pstmt.setNull(4, Types.VARCHAR);
            }
            pstmt.setString(5, cardDetails.getIssuingBank());
            pstmt.setString(6, cardDetails.getPaymentMethodId()); // Or cardDetails.getPaymentMethod().getPaymentMethodId()

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Updating card details failed, no record found for paymentMethodID: " + cardDetails.getPaymentMethodId());
            }
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
    }

    @Override
    public CardDetails getByPaymentMethodId(String paymentMethodId) throws SQLException {
        String sql = "SELECT * FROM CARD_DETAILS WHERE paymentMethodID = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, paymentMethodId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToCardDetails(rs);
            }
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
        return null;
    }

    @Override
    public void delete(String paymentMethodId) throws SQLException {
        String sql = "DELETE FROM CARD_DETAILS WHERE paymentMethodID = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, paymentMethodId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
    }
}
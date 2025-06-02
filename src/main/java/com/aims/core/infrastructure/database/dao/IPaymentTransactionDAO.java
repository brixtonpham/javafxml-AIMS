package com.aims.core.infrastructure.database.dao;

import com.aims.core.entities.PaymentTransaction;
import com.aims.core.enums.TransactionType; // Assuming TransactionType enum exists

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public interface IPaymentTransactionDAO {

    /**
     * Adds a new PaymentTransaction to the database.
     *
     * @param transaction The PaymentTransaction object to add.
     * @throws SQLException If a database access error occurs.
     */
    void add(PaymentTransaction transaction) throws SQLException;

    /**
     * Retrieves a PaymentTransaction from the database by its ID.
     *
     * @param transactionId The ID of the transaction to retrieve.
     * @return The PaymentTransaction object if found, otherwise null.
     * @throws SQLException If a database access error occurs.
     */
    PaymentTransaction getById(String transactionId) throws SQLException;

    /**
     * Retrieves all PaymentTransactions associated with a specific order ID.
     *
     * @param orderId The ID of the order.
     * @return A list of PaymentTransaction objects for the given order.
     * @throws SQLException If a database access error occurs.
     */
    List<PaymentTransaction> getByOrderId(String orderId) throws SQLException;

    /**
     * Retrieves all PaymentTransactions.
     *
     * @return A list of all PaymentTransaction objects.
     * @throws SQLException If a database access error occurs.
     */
    List<PaymentTransaction> getAll() throws SQLException;

    /**
     * Retrieves PaymentTransactions based on their type (e.g., PAYMENT, REFUND).
     *
     * @param transactionType The type of transactions to retrieve.
     * @return A list of PaymentTransaction objects of the specified type.
     * @throws SQLException If a database access error occurs.
     */
    List<PaymentTransaction> getByTransactionType(TransactionType transactionType) throws SQLException;

    /**
     * Retrieves PaymentTransactions within a specific date range.
     *
     * @param startDate The start of the date range.
     * @param endDate The end of the date range.
     * @return A list of PaymentTransaction objects within the date range.
     * @throws SQLException If a database access error occurs.
     */
    List<PaymentTransaction> getByDateRange(LocalDateTime startDate, LocalDateTime endDate) throws SQLException;

    /**
     * Updates the status of a specific payment transaction.
     * (e.g., from PENDING to SUCCESS or FAILED).
     *
     * @param transactionId The ID of the transaction to update.
     * @param newStatus The new status for the transaction.
     * @param externalTransactionId Optional: The external gateway's transaction ID, if updated.
     * @throws SQLException If a database access error occurs.
     */
    void updateStatus(String transactionId, String newStatus, String externalTransactionId) throws SQLException;

    // Note: Deleting transactions is typically not done or heavily restricted
    // due to auditing and financial record-keeping requirements.
    // If needed, a method could be added, but with caution.
    // void delete(String transactionId) throws SQLException;
}
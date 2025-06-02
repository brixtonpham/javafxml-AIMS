package com.aims.core.infrastructure.database.dao;

import com.aims.core.entities.CardDetails;
import java.sql.SQLException;

public interface ICardDetailsDAO {

    /**
     * Adds new CardDetails to the database.
     * This is typically called when a new card-based PaymentMethod is added.
     *
     * @param cardDetails The CardDetails object to add.
     * @throws SQLException If a database access error occurs (e.g., if paymentMethodId already has details).
     */
    void add(CardDetails cardDetails) throws SQLException;

    /**
     * Updates existing CardDetails in the database.
     *
     * @param cardDetails The CardDetails object with updated information.
     * @throws SQLException If a database access error occurs or if no record found for the paymentMethodId.
     */
    void update(CardDetails cardDetails) throws SQLException;

    /**
     * Retrieves CardDetails from the database by its associated PaymentMethod ID.
     * Since CardDetails has a 1-to-1 relationship with PaymentMethod (where PaymentMethod ID is its PK),
     * this effectively gets the CardDetails by its own ID.
     *
     * @param paymentMethodId The ID of the PaymentMethod (which is also the ID for CardDetails).
     * @return The CardDetails object if found, otherwise null.
     * @throws SQLException If a database access error occurs.
     */
    CardDetails getByPaymentMethodId(String paymentMethodId) throws SQLException;

    /**
     * Deletes CardDetails from the database by its associated PaymentMethod ID.
     *
     * @param paymentMethodId The ID of the PaymentMethod whose card details are to be deleted.
     * @throws SQLException If a database access error occurs.
     */
    void delete(String paymentMethodId) throws SQLException;
}
package com.aims.core.infrastructure.database.dao;

import com.aims.core.entities.PaymentMethod;
import com.aims.core.enums.PaymentMethodType; // Assuming PaymentMethodType enum exists

import java.sql.SQLException;
import java.util.List;

public interface IPaymentMethodDAO {

    /**
     * Adds a new PaymentMethod to the database.
     * This is typically for a user saving a payment method.
     *
     * @param paymentMethod The PaymentMethod object to add.
     * @throws SQLException If a database access error occurs.
     */
    void add(PaymentMethod paymentMethod) throws SQLException;

    /**
     * Updates an existing PaymentMethod in the database.
     * (e.g., changing the default status, or details if the method allows).
     *
     * @param paymentMethod The PaymentMethod object with updated information.
     * @throws SQLException If a database access error occurs.
     */
    void update(PaymentMethod paymentMethod) throws SQLException;

    /**
     * Retrieves a PaymentMethod from the database by its ID.
     * This should also load associated CardDetails if applicable.
     *
     * @param paymentMethodId The ID of the payment method to retrieve.
     * @return The PaymentMethod object if found, otherwise null.
     * @throws SQLException If a database access error occurs.
     */
    PaymentMethod getById(String paymentMethodId) throws SQLException;

    /**
     * Retrieves all PaymentMethods saved by a specific user.
     *
     * @param userId The ID of the user.
     * @return A list of PaymentMethod objects saved by the user.
     * @throws SQLException If a database access error occurs.
     */
    List<PaymentMethod> getByUserId(String userId) throws SQLException;

    /**
     * Retrieves all PaymentMethods of a specific type (e.g., all CREDIT_CARD methods).
     * This might be useful for administrative purposes or general lookup.
     *
     * @param methodType The type of payment method.
     * @return A list of PaymentMethod objects of the specified type.
     * @throws SQLException If a database access error occurs.
     */
    List<PaymentMethod> getByMethodType(PaymentMethodType methodType) throws SQLException;

    /**
     * Retrieves all PaymentMethods stored in the system.
     *
     * @return A list of all PaymentMethod objects.
     * @throws SQLException If a database access error occurs.
     */
    List<PaymentMethod> getAll() throws SQLException;

    /**
     * Deletes a PaymentMethod from the database by its ID.
     * Associated CardDetails should also be deleted (e.g., via CASCADE in DB).
     *
     * @param paymentMethodId The ID of the payment method to delete.
     * @throws SQLException If a database access error occurs.
     */
    void delete(String paymentMethodId) throws SQLException;

    /**
     * Retrieves the default PaymentMethod for a specific user, if one is set.
     *
     * @param userId The ID of the user.
     * @return The default PaymentMethod object if found, otherwise null.
     * @throws SQLException If a database access error occurs.
     */
    PaymentMethod getDefaultByUserId(String userId) throws SQLException;

    /**
     * Sets a specific payment method as the default for a user.
     * This implies clearing the default flag from any other method for that user.
     *
     * @param userId The ID of the user.
     * @param paymentMethodId The ID of the payment method to set as default.
     * @throws SQLException If a database access error occurs.
     */
    void setDefault(String userId, String paymentMethodId) throws SQLException;

    /**
     * Clears the default payment method for a user.
     *
     * @param userId The ID of the user.
     * @throws SQLException If a database access error occurs.
     */
    void clearDefaultForUser(String userId) throws SQLException;
}
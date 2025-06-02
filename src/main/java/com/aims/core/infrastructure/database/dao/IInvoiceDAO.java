package com.aims.core.infrastructure.database.dao;

import com.aims.core.entities.Invoice;
import java.sql.SQLException;
// List<Invoice> might not be common if one order has one invoice
// import java.util.List; 

public interface IInvoiceDAO {

    /**
     * Adds a new Invoice to the database.
     *
     * @param invoice The Invoice object to add.
     * @throws SQLException If a database access error occurs or if an invoice for the order already exists.
     */
    void add(Invoice invoice) throws SQLException;

    /**
     * Updates an existing Invoice in the database.
     * (This might be rare for invoices, as they are typically immutable once generated).
     *
     * @param invoice The Invoice object with updated information.
     * @throws SQLException If a database access error occurs.
     */
    void update(Invoice invoice) throws SQLException;

    /**
     * Retrieves an Invoice from the database by its ID.
     *
     * @param invoiceId The ID of the invoice to retrieve.
     * @return The Invoice object if found, otherwise null.
     * @throws SQLException If a database access error occurs.
     */
    Invoice getById(String invoiceId) throws SQLException;

    /**
     * Retrieves an Invoice associated with a specific order ID.
     * Since it's typically a one-to-one relationship, this should return a single object.
     *
     * @param orderId The ID of the order.
     * @return The Invoice object if found, otherwise null.
     * @throws SQLException If a database access error occurs.
     */
    Invoice getByOrderId(String orderId) throws SQLException;

    /**
     * Deletes an Invoice from the database by its ID.
     *
     * @param invoiceId The ID of the invoice to delete.
     * @throws SQLException If a database access error occurs.
     */
    void deleteById(String invoiceId) throws SQLException;

    /**
     * Deletes an Invoice associated with a specific order ID.
     * Useful if an order is deleted and cascading delete needs to be explicit or isn't relied upon.
     *
     * @param orderId The ID of the order whose invoice is to be deleted.
     * @throws SQLException If a database access error occurs.
     */
    void deleteByOrderId(String orderId) throws SQLException;
}
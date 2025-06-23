package com.aims.core.infrastructure.database.dao;

import com.aims.core.entities.OrderEntity;
import java.sql.SQLException;
import java.util.List;

/**
 * Interface for Order Data Access Object operations.
 * This is an alias for IOrderEntityDAO to maintain backward compatibility with tests.
 */
public interface IOrderDAO extends IOrderEntityDAO {
    // This interface extends IOrderEntityDAO to provide compatibility
    // All methods are inherited from IOrderEntityDAO
}

package com.aims.core.infrastructure.database.dao;

import org.springframework.stereotype.Repository;

/**
 * Implementation of IOrderDAO that extends OrderEntityDAOImpl.
 * This provides compatibility for tests that expect IOrderDAO.
 */
@Repository
public class OrderDAOImpl extends OrderEntityDAOImpl implements IOrderDAO {
    
    public OrderDAOImpl(IOrderItemDAO orderItemDAO, IUserAccountDAO userAccountDAO) {
        super(orderItemDAO, userAccountDAO);
    }
    
    // All functionality is inherited from OrderEntityDAOImpl
    // This class serves as a compatibility layer for tests
}

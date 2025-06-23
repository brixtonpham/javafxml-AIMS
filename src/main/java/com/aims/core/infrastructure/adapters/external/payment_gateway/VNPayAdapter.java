package com.aims.core.infrastructure.adapters.external.payment_gateway;

import org.springframework.stereotype.Component;

/**
 * VNPayAdapter class for compatibility with tests.
 * This is an alias/wrapper for VNPayAdapterImpl to maintain backward compatibility.
 */
@Component("VNPayAdapter")
public class VNPayAdapter extends VNPayAdapterImpl {
    
    public VNPayAdapter() {
        super();
    }
    
    // All functionality is inherited from VNPayAdapterImpl
    // This class serves as a compatibility layer for tests that expect VNPayAdapter
}

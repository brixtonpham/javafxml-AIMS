package com.aims.test.suites;

import com.aims.test.integration.*;
import org.junit.platform.suite.api.*;

/**
 * AIMS Phase 3: Comprehensive Test Suite Runner
 * 
 * Executes all Phase 3 integration tests in the correct order to validate
 * complete business workflows and end-to-end customer journeys.
 * 
 * Test Execution Order:
 * 1. Multi-Service Integration Tests (65 tests)
 * 2. Complete Customer Journey Tests (25 tests) 
 * 3. Business Rule Compliance Tests (30 tests)
 * 
 * Total: 120+ comprehensive integration tests
 * 
 * Usage:
 * - Run this suite to execute all Phase 3 tests
 * - Individual test classes can be run separately for focused testing
 * - Results are aggregated for comprehensive reporting
 */
@Suite
@SuiteDisplayName("AIMS Phase 3: Comprehensive Integration Testing & End-to-End Workflows")
@SelectClasses({
    // Phase 3.1: Multi-Service Integration Tests
    AIMSPhase3MultiServiceIntegrationTest.class,
    
    // Phase 3.2: Complete Customer Journey Tests
    CompleteCustomerJourneyTest.class,
    
    // Phase 3.3: Business Rule Compliance Tests
    BusinessRuleComplianceTest.class,
    
    // Include existing Phase 2 tests for regression testing
    AIMSPhase2ComprehensiveIntegrationTest.class,
    AIMSPhase2WorkflowSpecificIntegrationTest.class,
    EnhancedServicesIntegrationTest.class
})
@IncludeTags({"integration", "phase3", "end-to-end"})
public class AIMSPhase3ComprehensiveTestSuite {
    
    // Test suite configuration and reporting handled by JUnit 5
    // Individual test results will be aggregated automatically
    
    /**
     * Expected Test Results Summary:
     * 
     * Phase 3 New Tests:
     * - Multi-Service Integration: 65 tests
     * - Customer Journey: 25 tests  
     * - Business Rule Compliance: 30 tests
     * Total Phase 3: 120 tests
     * 
     * Phase 1-2 Regression:
     * - Phase 2 Comprehensive: 6 tests
     * - Phase 2 Workflow: 10 tests
     * - Enhanced Services: 6 tests
     * Total Regression: 22 tests
     * 
     * Grand Total: 142 comprehensive integration tests
     * 
     * Performance Targets:
     * - Individual test execution: < 5 seconds
     * - Full suite execution: < 15 minutes
     * - Memory usage: < 512MB peak
     * - Success rate: > 95%
     */
}
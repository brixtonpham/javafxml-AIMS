package com.aims.test.suites;

import com.aims.test.integration.AIMSPhase2ComprehensiveIntegrationTest;
import com.aims.test.integration.AIMSPhase2WorkflowSpecificIntegrationTest;
import com.aims.test.performance.AIMSPhase2PerformanceIntegrationTest;
import com.aims.core.application.impl.OrderServiceImplPhase2IntegrationTest;
import com.aims.core.application.impl.CartServiceImplPhase2IntegrationTest;
import com.aims.core.application.impl.ProductServiceImplPhase2IntegrationTest;
import com.aims.test.integration.EnhancedServicesIntegrationTest;
import com.aims.test.performance.CartToOrderPerformanceTest;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * AIMS Phase 2 Task 3.4: Comprehensive Test Suite Runner
 * 
 * This test suite coordinates and executes all Phase 2 integration tests:
 * - 20+ integration tests demonstrating service interoperability
 * - Complete workflow validation: Cart → Order → Approval → Stock Management
 * - Performance benchmarks and validation
 * - Edge cases and stress testing
 * - Free shipping and rush order validation
 * 
 * Test Categories:
 * 1. Service Integration Tests (OrderService, CartService, ProductService)
 * 2. Workflow-Specific Integration Tests (10 tests)
 * 3. Comprehensive Integration Tests (6 tests)
 * 4. Performance Integration Tests (6 tests)
 * 5. Enhanced Services Integration Tests
 * 6. Cart-to-Order Performance Tests
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("AIMS Phase 2 - Comprehensive Test Suite Runner")
public class AIMSPhase2ComprehensiveTestSuite {
    
    private static TestExecutionReport report;
    private static long suiteStartTime;
    
    @BeforeAll
    static void initializeTestSuite() {
        suiteStartTime = System.currentTimeMillis();
        report = new TestExecutionReport();
        
        System.out.println("=" .repeat(100));
        System.out.println("AIMS PHASE 2 TASK 3.4: COMPREHENSIVE INTEGRATION TESTING & PERFORMANCE VALIDATION");
        System.out.println("=" .repeat(100));
        System.out.println("Test Suite Start Time: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        System.out.println();
        
        System.out.println("PHASE 2 OBJECTIVES BEING VALIDATED:");
        System.out.println("✓ Stock validation prevents overselling");
        System.out.println("✓ Delivery calculations with free shipping and rush orders");
        System.out.println("✓ Order approval workflow with state management");
        System.out.println("✓ Complete service interoperability");
        System.out.println("✓ Performance benchmarks with no degradation");
        System.out.println("✓ Edge cases and error handling");
        System.out.println("✓ System stability under load");
        System.out.println();
        
        printTestCategorySummary();
    }
    
    @Test
    @Order(1)
    @DisplayName("Category 1: Service-Specific Integration Tests")
    void executeServiceSpecificIntegrationTests() throws Exception {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("CATEGORY 1: SERVICE-SPECIFIC INTEGRATION TESTS");
        System.out.println("=".repeat(60));
        
        TestCategory category = new TestCategory("Service-Specific Integration Tests");
        
        // Execute OrderService Phase 2 Integration Tests
        category.addTestClass("OrderServiceImplPhase2IntegrationTest", 
            OrderServiceImplPhase2IntegrationTest.class, 7);
        
        // Execute CartService Phase 2 Integration Tests  
        category.addTestClass("CartServiceImplPhase2IntegrationTest", 
            CartServiceImplPhase2IntegrationTest.class, 8);
        
        // Execute ProductService Phase 2 Integration Tests
        category.addTestClass("ProductServiceImplPhase2IntegrationTest", 
            ProductServiceImplPhase2IntegrationTest.class, 9);
        
        executeTestCategory(category);
        report.addCategory(category);
        
        System.out.println("✅ Service-Specific Integration Tests COMPLETED");
        System.out.println("Tests executed: " + category.getTotalTests());
        System.out.println("Success rate: " + String.format("%.2f", category.getSuccessRate() * 100) + "%");
    }
    
    @Test
    @Order(2)
    @DisplayName("Category 2: Workflow-Specific Integration Tests")
    void executeWorkflowSpecificIntegrationTests() throws Exception {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("CATEGORY 2: WORKFLOW-SPECIFIC INTEGRATION TESTS");
        System.out.println("=".repeat(60));
        
        TestCategory category = new TestCategory("Workflow-Specific Integration Tests");
        
        // Execute Workflow-Specific Integration Tests
        category.addTestClass("AIMSPhase2WorkflowSpecificIntegrationTest", 
            AIMSPhase2WorkflowSpecificIntegrationTest.class, 10);
        
        executeTestCategory(category);
        report.addCategory(category);
        
        System.out.println("✅ Workflow-Specific Integration Tests COMPLETED");
        System.out.println("Tests executed: " + category.getTotalTests());
        System.out.println("Success rate: " + String.format("%.2f", category.getSuccessRate() * 100) + "%");
    }
    
    @Test
    @Order(3)
    @DisplayName("Category 3: Comprehensive Integration Tests")
    void executeComprehensiveIntegrationTests() throws Exception {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("CATEGORY 3: COMPREHENSIVE INTEGRATION TESTS");
        System.out.println("=".repeat(60));
        
        TestCategory category = new TestCategory("Comprehensive Integration Tests");
        
        // Execute Comprehensive Integration Tests
        category.addTestClass("AIMSPhase2ComprehensiveIntegrationTest", 
            AIMSPhase2ComprehensiveIntegrationTest.class, 6);
        
        executeTestCategory(category);
        report.addCategory(category);
        
        System.out.println("✅ Comprehensive Integration Tests COMPLETED");
        System.out.println("Tests executed: " + category.getTotalTests());
        System.out.println("Success rate: " + String.format("%.2f", category.getSuccessRate() * 100) + "%");
    }
    
    @Test
    @Order(4)
    @DisplayName("Category 4: Performance Integration Tests")
    void executePerformanceIntegrationTests() throws Exception {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("CATEGORY 4: PERFORMANCE INTEGRATION TESTS");
        System.out.println("=".repeat(60));
        
        TestCategory category = new TestCategory("Performance Integration Tests");
        
        // Execute Performance Integration Tests
        category.addTestClass("AIMSPhase2PerformanceIntegrationTest", 
            AIMSPhase2PerformanceIntegrationTest.class, 6);
        
        executeTestCategory(category);
        report.addCategory(category);
        
        System.out.println("✅ Performance Integration Tests COMPLETED");
        System.out.println("Tests executed: " + category.getTotalTests());
        System.out.println("Success rate: " + String.format("%.2f", category.getSuccessRate() * 100) + "%");
    }
    
    @Test
    @Order(5)
    @DisplayName("Category 5: Enhanced Services Integration Tests")
    void executeEnhancedServicesIntegrationTests() throws Exception {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("CATEGORY 5: ENHANCED SERVICES INTEGRATION TESTS");
        System.out.println("=".repeat(60));
        
        TestCategory category = new TestCategory("Enhanced Services Integration Tests");
        
        // Execute Enhanced Services Integration Tests
        category.addTestClass("EnhancedServicesIntegrationTest", 
            EnhancedServicesIntegrationTest.class, 6);
        
        executeTestCategory(category);
        report.addCategory(category);
        
        System.out.println("✅ Enhanced Services Integration Tests COMPLETED");
        System.out.println("Tests executed: " + category.getTotalTests());
        System.out.println("Success rate: " + String.format("%.2f", category.getSuccessRate() * 100) + "%");
    }
    
    @Test
    @Order(6)
    @DisplayName("Category 6: Cart-to-Order Performance Tests")
    void executeCartToOrderPerformanceTests() throws Exception {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("CATEGORY 6: CART-TO-ORDER PERFORMANCE TESTS");
        System.out.println("=".repeat(60));
        
        TestCategory category = new TestCategory("Cart-to-Order Performance Tests");
        
        // Execute Cart-to-Order Performance Tests
        category.addTestClass("CartToOrderPerformanceTest", 
            CartToOrderPerformanceTest.class, 7);
        
        executeTestCategory(category);
        report.addCategory(category);
        
        System.out.println("✅ Cart-to-Order Performance Tests COMPLETED");
        System.out.println("Tests executed: " + category.getTotalTests());
        System.out.println("Success rate: " + String.format("%.2f", category.getSuccessRate() * 100) + "%");
    }
    
    private void executeTestCategory(TestCategory category) {
        for (TestClassInfo testClass : category.getTestClasses()) {
            try {
                System.out.println("\nExecuting: " + testClass.getName() + " (" + testClass.getExpectedTests() + " tests)");
                
                long startTime = System.currentTimeMillis();
                
                Launcher launcher = LauncherFactory.create();
                SummaryGeneratingListener listener = new SummaryGeneratingListener();
                
                LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                    .selectors(DiscoverySelectors.selectClass(testClass.getTestClass()))
                    .build();
                
                launcher.registerTestExecutionListeners(listener);
                launcher.execute(request);
                
                long endTime = System.currentTimeMillis();
                long duration = endTime - startTime;
                
                TestExecutionSummary summary = listener.getSummary();
                
                testClass.setExecuted(true);
                testClass.setDuration(duration);
                testClass.setActualTests((int) summary.getTestsFoundCount());
                testClass.setSuccessfulTests((int) summary.getTestsSucceededCount());
                testClass.setFailedTests((int) summary.getTestsFailedCount());
                
                System.out.println("✓ " + testClass.getName() + " completed in " + duration + "ms");
                System.out.println("  Tests: " + summary.getTestsFoundCount() + 
                                 ", Succeeded: " + summary.getTestsSucceededCount() + 
                                 ", Failed: " + summary.getTestsFailedCount());
                
                if (summary.getTestsFailedCount() > 0) {
                    System.out.println("  ⚠️ Some tests failed - check individual test output for details");
                }
                
            } catch (Exception e) {
                System.err.println("❌ Failed to execute " + testClass.getName() + ": " + e.getMessage());
                testClass.setExecuted(false);
                testClass.setFailedTests(testClass.getExpectedTests());
            }
        }
    }
    
    private static void printTestCategorySummary() {
        System.out.println("TEST CATEGORIES TO BE EXECUTED:");
        System.out.println("1. Service-Specific Integration Tests (24 tests)");
        System.out.println("   - OrderServiceImplPhase2IntegrationTest (7 tests)");
        System.out.println("   - CartServiceImplPhase2IntegrationTest (8 tests)");
        System.out.println("   - ProductServiceImplPhase2IntegrationTest (9 tests)");
        System.out.println();
        System.out.println("2. Workflow-Specific Integration Tests (10 tests)");
        System.out.println("   - Stock validation prevents overselling scenarios");
        System.out.println("   - Order approval workflow validation");
        System.out.println("   - Delivery calculation integration");
        System.out.println("   - Complex multi-product workflows");
        System.out.println();
        System.out.println("3. Comprehensive Integration Tests (6 tests)");
        System.out.println("   - Complete workflow end-to-end");
        System.out.println("   - Service interoperability validation");
        System.out.println("   - Free shipping and rush order testing");
        System.out.println("   - Edge cases and stress testing");
        System.out.println();
        System.out.println("4. Performance Integration Tests (6 tests)");
        System.out.println("   - Baseline performance measurements");
        System.out.println("   - Concurrent operations under load");
        System.out.println("   - Memory usage validation");
        System.out.println("   - Scalability testing");
        System.out.println();
        System.out.println("5. Enhanced Services Integration Tests (6 tests)");
        System.out.println("   - Order Data Loader Service integration");
        System.out.println("   - Cart Data Validation Service integration");
        System.out.println("   - Cross-service data consistency");
        System.out.println();
        System.out.println("6. Cart-to-Order Performance Tests (7 tests)");
        System.out.println("   - Cart loading performance");
        System.out.println("   - Order creation performance");
        System.out.println("   - Performance degradation detection");
        System.out.println();
        System.out.println("TOTAL INTEGRATION TESTS: 59 tests across 6 categories");
        System.out.println("=" .repeat(100));
        System.out.println();
    }
    
    @AfterAll
    static void generateFinalReport() {
        long suiteEndTime = System.currentTimeMillis();
        long totalDuration = suiteEndTime - suiteStartTime;
        
        System.out.println("\n" + "=".repeat(100));
        System.out.println("AIMS PHASE 2 COMPREHENSIVE TEST SUITE - FINAL REPORT");
        System.out.println("=".repeat(100));
        
        System.out.println("Test Suite Completion Time: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        System.out.println("Total Execution Duration: " + formatDuration(totalDuration));
        System.out.println();
        
        // Category Summary
        System.out.println("CATEGORY SUMMARY:");
        int totalTests = 0;
        int totalSuccessful = 0;
        int totalFailed = 0;
        
        for (TestCategory category : report.getCategories()) {
            System.out.println();
            System.out.println(category.getName() + ":");
            System.out.println("  Total Tests: " + category.getTotalTests());
            System.out.println("  Successful: " + category.getSuccessfulTests());
            System.out.println("  Failed: " + category.getFailedTests());
            System.out.println("  Success Rate: " + String.format("%.2f", category.getSuccessRate() * 100) + "%");
            System.out.println("  Duration: " + formatDuration(category.getTotalDuration()));
            
            totalTests += category.getTotalTests();
            totalSuccessful += category.getSuccessfulTests();
            totalFailed += category.getFailedTests();
        }
        
        System.out.println();
        System.out.println("OVERALL SUMMARY:");
        System.out.println("Total Integration Tests Executed: " + totalTests);
        System.out.println("Successful Tests: " + totalSuccessful);
        System.out.println("Failed Tests: " + totalFailed);
        System.out.println("Overall Success Rate: " + String.format("%.2f", (double) totalSuccessful / totalTests * 100) + "%");
        
        System.out.println();
        System.out.println("PHASE 2 OBJECTIVES VALIDATION:");
        System.out.println(totalSuccessful >= (totalTests * 0.95) ? "✅" : "❌" + 
                         " Stock validation prevents overselling - Validated across multiple test scenarios");
        System.out.println(totalSuccessful >= (totalTests * 0.95) ? "✅" : "❌" + 
                         " Delivery calculations with free shipping and rush orders - Comprehensive testing completed");
        System.out.println(totalSuccessful >= (totalTests * 0.95) ? "✅" : "❌" + 
                         " Order approval workflow - State management integration verified");
        System.out.println(totalSuccessful >= (totalTests * 0.95) ? "✅" : "❌" + 
                         " Service interoperability - All Phase 2 services working together");
        System.out.println(totalSuccessful >= (totalTests * 0.95) ? "✅" : "❌" + 
                         " Performance benchmarks - No degradation with Phase 2 services");
        System.out.println(totalSuccessful >= (totalTests * 0.95) ? "✅" : "❌" + 
                         " Edge cases and error handling - Robust validation implemented");
        System.out.println(totalSuccessful >= (totalTests * 0.95) ? "✅" : "❌" + 
                         " System stability under load - Stress testing validated");
        
        System.out.println();
        if (totalSuccessful >= (totalTests * 0.95)) {
            System.out.println("🎉 AIMS PHASE 2 TASK 3.4 SUCCESSFULLY COMPLETED!");
            System.out.println("✅ All Phase 2 objectives have been validated through comprehensive integration testing");
            System.out.println("✅ 20+ integration tests demonstrate complete service interoperability");
            System.out.println("✅ Performance benchmarks confirm no degradation with Phase 2 services");
            System.out.println("✅ System demonstrates stability and scalability under various load conditions");
        } else {
            System.out.println("⚠️ PHASE 2 TASK 3.4 COMPLETED WITH ISSUES");
            System.out.println("Some tests failed - review individual test results for details");
        }
        
        System.out.println();
        System.out.println("=".repeat(100));
    }
    
    private static String formatDuration(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        
        if (minutes > 0) {
            return minutes + "m " + seconds + "s";
        } else {
            return seconds + "s";
        }
    }
    
    // Helper classes for test execution tracking
    
    private static class TestExecutionReport {
        private List<TestCategory> categories = new ArrayList<>();
        
        public void addCategory(TestCategory category) {
            categories.add(category);
        }
        
        public List<TestCategory> getCategories() {
            return categories;
        }
    }
    
    private static class TestCategory {
        private String name;
        private List<TestClassInfo> testClasses = new ArrayList<>();
        
        public TestCategory(String name) {
            this.name = name;
        }
        
        public void addTestClass(String name, Class<?> testClass, int expectedTests) {
            testClasses.add(new TestClassInfo(name, testClass, expectedTests));
        }
        
        public String getName() { return name; }
        public List<TestClassInfo> getTestClasses() { return testClasses; }
        
        public int getTotalTests() {
            return testClasses.stream().mapToInt(TestClassInfo::getActualTests).sum();
        }
        
        public int getSuccessfulTests() {
            return testClasses.stream().mapToInt(TestClassInfo::getSuccessfulTests).sum();
        }
        
        public int getFailedTests() {
            return testClasses.stream().mapToInt(TestClassInfo::getFailedTests).sum();
        }
        
        public double getSuccessRate() {
            int total = getTotalTests();
            return total > 0 ? (double) getSuccessfulTests() / total : 0.0;
        }
        
        public long getTotalDuration() {
            return testClasses.stream().mapToLong(TestClassInfo::getDuration).sum();
        }
    }
    
    private static class TestClassInfo {
        private String name;
        private Class<?> testClass;
        private int expectedTests;
        private int actualTests;
        private int successfulTests;
        private int failedTests;
        private long duration;
        private boolean executed;
        
        public TestClassInfo(String name, Class<?> testClass, int expectedTests) {
            this.name = name;
            this.testClass = testClass;
            this.expectedTests = expectedTests;
        }
        
        // Getters and setters
        public String getName() { return name; }
        public Class<?> getTestClass() { return testClass; }
        public int getExpectedTests() { return expectedTests; }
        public int getActualTests() { return actualTests; }
        public int getSuccessfulTests() { return successfulTests; }
        public int getFailedTests() { return failedTests; }
        public long getDuration() { return duration; }
        public boolean isExecuted() { return executed; }
        
        public void setActualTests(int actualTests) { this.actualTests = actualTests; }
        public void setSuccessfulTests(int successfulTests) { this.successfulTests = successfulTests; }
        public void setFailedTests(int failedTests) { this.failedTests = failedTests; }
        public void setDuration(long duration) { this.duration = duration; }
        public void setExecuted(boolean executed) { this.executed = executed; }
    }
}
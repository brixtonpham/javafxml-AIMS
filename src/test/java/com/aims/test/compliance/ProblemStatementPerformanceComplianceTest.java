package com.aims.test.compliance;

import com.aims.core.application.services.*;
import com.aims.core.entities.*;
import com.aims.core.shared.ServiceFactory;
import com.aims.test.base.BaseUITest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AIMS Phase 4.1: Problem Statement Performance Requirements Compliance Tests
 * 
 * Validates system performance requirements from AIMS-ProblemStatement-v2.0.pdf Lines 10-15:
 * - Line 10-11: System supports 1000 concurrent users
 * - Line 12-13: Response time <2s normal load, <5s peak load  
 * - Line 14-15: 300 hours continuous operation capability
 * 
 * Total Tests: 5 performance compliance validation tests
 */
@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ProblemStatementPerformanceComplianceTest extends BaseUITest {

    private static final Logger logger = Logger.getLogger(ProblemStatementPerformanceComplianceTest.class.getName());

    // Performance Constants from Problem Statement
    private static final int CONCURRENT_USER_LIMIT = 1000; // Line 10-11
    private static final long NORMAL_RESPONSE_TIME_MS = 2000; // Line 12: <2s normal load
    private static final long PEAK_RESPONSE_TIME_MS = 5000; // Line 13: <5s peak load
    private static final int CONTINUOUS_OPERATION_HOURS = 300; // Line 14-15

    // Core Services
    private IProductService productService;
    private ICartService cartService;
    private IOrderService orderService;
    private IUserAccountService userAccountService;

    @BeforeEach
    void setUp() {
        logger.info("=== Setting up Problem Statement Performance Compliance Tests ===");
        
        // Initialize core services for performance testing
        ServiceFactory serviceFactory = ServiceFactory.getInstance();
        productService = serviceFactory.getProductService();
        cartService = serviceFactory.getCartService();
        orderService = serviceFactory.getOrderService();
        userAccountService = serviceFactory.getUserAccountService();
        
        // Seed performance test data
        seedPerformanceTestData();
        
        logger.info("✓ Performance compliance test setup completed");
    }

    // ========================================
    // Performance Requirements Compliance Tests (Lines 10-15)
    // ========================================

    @Test
    @Order(1)
    @DisplayName("PS-PERF-001: System supports 1000 concurrent users - Lines 10-11")
    void testConcurrentUserSupport_1000Users() throws Exception {
        logger.info("=== Testing 1000 Concurrent Users Support - Lines 10-11 ===");
        
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_USER_LIMIT);
        List<Future<UserSessionResult>> futures = new ArrayList<>();
        CountDownLatch startLatch = new CountDownLatch(1);
        
        try {
            // Create 1000 concurrent user simulation tasks
            for (int i = 0; i < CONCURRENT_USER_LIMIT; i++) {
                final int userId = i;
                Future<UserSessionResult> future = executor.submit(() -> {
                    try {
                        // Wait for all threads to be ready
                        startLatch.await();
                        
                        // Simulate typical user session: login -> browse -> add to cart
                        return simulateUserSession("user-" + userId);
                        
                    } catch (Exception e) {
                        logger.warning("User session " + userId + " failed: " + e.getMessage());
                        return new UserSessionResult(false, 0, e.getMessage());
                    }
                });
                futures.add(future);
            }
            
            long startTime = System.currentTimeMillis();
            startLatch.countDown(); // Start all concurrent operations
            
            // Collect results from all concurrent user sessions
            int successfulSessions = 0;
            int totalSessions = 0;
            long maxResponseTime = 0;
            
            for (Future<UserSessionResult> future : futures) {
                try {
                    UserSessionResult result = future.get(30, TimeUnit.SECONDS);
                    totalSessions++;
                    if (result.isSuccessful()) {
                        successfulSessions++;
                    }
                    maxResponseTime = Math.max(maxResponseTime, result.getResponseTimeMs());
                } catch (TimeoutException e) {
                    logger.warning("User session timed out after 30 seconds");
                    totalSessions++;
                }
            }
            
            long totalTime = System.currentTimeMillis() - startTime;
            
            // Validate 1000 concurrent user support
            assertEquals(CONCURRENT_USER_LIMIT, totalSessions, 
                "Should execute exactly 1000 concurrent user sessions");
            
            // At least 95% of sessions should succeed under concurrent load
            double successRate = (double) successfulSessions / totalSessions;
            assertTrue(successRate >= 0.95, 
                String.format("Success rate %.2f%% should be at least 95%% for 1000 concurrent users", 
                    successRate * 100));
            
            // System should handle load within reasonable time (under 60 seconds total)
            assertTrue(totalTime < 60000, 
                "System should handle 1000 concurrent users within 60 seconds");
            
            logger.info(String.format("✓ 1000 concurrent users: %d/%d successful (%.2f%%), max response: %dms", 
                successfulSessions, totalSessions, successRate * 100, maxResponseTime));
            
        } finally {
            executor.shutdown();
            executor.awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    @Test
    @Order(2)
    @DisplayName("PS-PERF-002: Normal load response time <2s - Line 12")
    void testNormalLoadResponseTime() throws Exception {
        logger.info("=== Testing Normal Load Response Time <2s - Line 12 ===");
        
        // Test critical operations under normal load (10 concurrent users)
        int normalLoadUsers = 10;
        List<Long> responseTimes = new ArrayList<>();
        
        ExecutorService executor = Executors.newFixedThreadPool(normalLoadUsers);
        List<Future<Long>> futures = new ArrayList<>();
        
        try {
            // Execute critical operations under normal load
            for (int i = 0; i < normalLoadUsers * 5; i++) { // 50 total operations
                futures.add(executor.submit(this::measureCriticalOperationTime));
            }
            
            // Collect response times
            for (Future<Long> future : futures) {
                Long responseTime = future.get(10, TimeUnit.SECONDS);
                responseTimes.add(responseTime);
            }
            
            // Calculate statistics
            long averageResponseTime = responseTimes.stream().mapToLong(Long::longValue).sum() / responseTimes.size();
            long maxResponseTime = responseTimes.stream().mapToLong(Long::longValue).max().orElse(0);
            long p95ResponseTime = calculatePercentile(responseTimes, 95);
            
            // Validate normal load response time requirements
            assertTrue(averageResponseTime < NORMAL_RESPONSE_TIME_MS,
                String.format("Average response time %dms should be less than %dms under normal load",
                    averageResponseTime, NORMAL_RESPONSE_TIME_MS));
            
            assertTrue(p95ResponseTime < NORMAL_RESPONSE_TIME_MS,
                String.format("95th percentile response time %dms should be less than %dms under normal load",
                    p95ResponseTime, NORMAL_RESPONSE_TIME_MS));
            
            logger.info(String.format("✓ Normal load response times: avg=%dms, max=%dms, p95=%dms", 
                averageResponseTime, maxResponseTime, p95ResponseTime));
            
        } finally {
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    @Test
    @Order(3)
    @DisplayName("PS-PERF-003: Peak load response time <5s - Line 13")
    void testPeakLoadResponseTime() throws Exception {
        logger.info("=== Testing Peak Load Response Time <5s - Line 13 ===");
        
        // Test critical operations under peak load (200 concurrent users)
        int peakLoadUsers = 200;
        List<Long> responseTimes = new ArrayList<>();
        
        ExecutorService executor = Executors.newFixedThreadPool(peakLoadUsers);
        List<Future<Long>> futures = new ArrayList<>();
        CountDownLatch startLatch = new CountDownLatch(1);
        
        try {
            // Execute critical operations under peak load
            for (int i = 0; i < peakLoadUsers * 2; i++) { // 400 total operations
                futures.add(executor.submit(() -> {
                    try {
                        startLatch.await(); // Synchronize start for true peak load
                        return measureCriticalOperationTime();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return PEAK_RESPONSE_TIME_MS + 1; // Return failure time
                    }
                }));
            }
            
            startLatch.countDown(); // Start peak load
            
            // Collect response times
            for (Future<Long> future : futures) {
                try {
                    Long responseTime = future.get(15, TimeUnit.SECONDS);
                    responseTimes.add(responseTime);
                } catch (TimeoutException e) {
                    responseTimes.add(PEAK_RESPONSE_TIME_MS + 1000); // Add timeout as failure
                }
            }
            
            // Calculate statistics
            long averageResponseTime = responseTimes.stream().mapToLong(Long::longValue).sum() / responseTimes.size();
            long maxResponseTime = responseTimes.stream().mapToLong(Long::longValue).max().orElse(0);
            long p95ResponseTime = calculatePercentile(responseTimes, 95);
            
            // Validate peak load response time requirements
            assertTrue(p95ResponseTime < PEAK_RESPONSE_TIME_MS,
                String.format("95th percentile response time %dms should be less than %dms under peak load",
                    p95ResponseTime, PEAK_RESPONSE_TIME_MS));
            
            // At peak load, allow some degradation but 90% should still meet requirement
            long withinLimitCount = responseTimes.stream()
                .mapToLong(Long::longValue)
                .filter(time -> time < PEAK_RESPONSE_TIME_MS)
                .count();
            double withinLimitPercentage = (double) withinLimitCount / responseTimes.size();
            
            assertTrue(withinLimitPercentage >= 0.90,
                String.format("%.2f%% of operations should complete within %dms under peak load (minimum 90%%)",
                    withinLimitPercentage * 100, PEAK_RESPONSE_TIME_MS));
            
            logger.info(String.format("✓ Peak load response times: avg=%dms, max=%dms, p95=%dms, within_limit=%.1f%%", 
                averageResponseTime, maxResponseTime, p95ResponseTime, withinLimitPercentage * 100));
            
        } finally {
            executor.shutdown();
            executor.awaitTermination(15, TimeUnit.SECONDS);
        }
    }

    @Test
    @Order(4)
    @DisplayName("PS-PERF-004: System stability over extended operation - Lines 14-15")
    void testContinuousOperationStability() throws Exception {
        logger.info("=== Testing System Stability Over Extended Operation - Lines 14-15 ===");
        
        // Simulate extended operation (scaled down from 300 hours to 30 minutes for testing)
        int testDurationMinutes = 30; // Scaled down for practical testing
        long testDurationMs = testDurationMinutes * 60 * 1000;
        
        long startTime = System.currentTimeMillis();
        long endTime = startTime + testDurationMs;
        
        List<Long> memoryUsageSnapshots = new ArrayList<>();
        List<Long> responseTimeSnapshots = new ArrayList<>();
        
        ExecutorService backgroundLoad = Executors.newFixedThreadPool(5);
        
        try {
            // Start background load to simulate continuous operation
            for (int i = 0; i < 5; i++) {
                backgroundLoad.submit(() -> {
                    while (!Thread.currentThread().isInterrupted() && System.currentTimeMillis() < endTime) {
                        try {
                            // Perform operations continuously
                            simulateUserSession("continuous-user-" + Thread.currentThread().getId());
                            Thread.sleep(1000); // 1 operation per second per thread
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        } catch (Exception e) {
                            // Log but continue operation
                            logger.warning("Background operation failed: " + e.getMessage());
                        }
                    }
                });
            }
            
            // Monitor system health during continuous operation
            int sampleCount = 0;
            while (System.currentTimeMillis() < endTime && sampleCount < 30) { // Sample every minute
                // Measure current memory usage
                Runtime runtime = Runtime.getRuntime();
                long usedMemory = runtime.totalMemory() - runtime.freeMemory();
                memoryUsageSnapshots.add(usedMemory);
                
                // Measure current response time
                long responseTime = measureCriticalOperationTime();
                responseTimeSnapshots.add(responseTime);
                
                sampleCount++;
                Thread.sleep(60000); // Wait 1 minute between samples
            }
            
            // Analyze stability metrics
            validateMemoryStability(memoryUsageSnapshots);
            validatePerformanceStability(responseTimeSnapshots);
            
            logger.info(String.format("✓ System remained stable during %d minutes of continuous operation", 
                testDurationMinutes));
            
        } finally {
            backgroundLoad.shutdownNow();
            backgroundLoad.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    @Order(5)
    @DisplayName("PS-PERF-005: Memory usage stability during peak operations - Lines 14-15")
    void testMemoryUsageStabilityDuringPeakOperations() throws Exception {
        logger.info("=== Testing Memory Usage Stability During Peak Operations - Lines 14-15 ===");
        
        Runtime runtime = Runtime.getRuntime();
        
        // Measure baseline memory usage
        System.gc(); // Force garbage collection
        Thread.sleep(1000);
        long baselineMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // Execute peak load operations and monitor memory
        ExecutorService executor = Executors.newFixedThreadPool(100);
        List<Future<Void>> futures = new ArrayList<>();
        List<Long> memorySnapshots = new ArrayList<>();
        
        try {
            // Start memory monitoring
            ScheduledExecutorService memoryMonitor = Executors.newSingleThreadScheduledExecutor();
            memoryMonitor.scheduleAtFixedRate(() -> {
                long currentMemory = runtime.totalMemory() - runtime.freeMemory();
                memorySnapshots.add(currentMemory);
            }, 0, 500, TimeUnit.MILLISECONDS); // Sample every 500ms
            
            // Execute intensive operations
            for (int i = 0; i < 500; i++) {
                final int operationId = i;
                futures.add(executor.submit(() -> {
                    try {
                        // Perform memory-intensive operations
                        simulateMemoryIntensiveOperation(operationId);
                        return null;
                    } catch (Exception e) {
                        logger.warning("Memory intensive operation " + operationId + " failed: " + e.getMessage());
                        return null;
                    }
                }));
            }
            
            // Wait for all operations to complete
            for (Future<Void> future : futures) {
                future.get(30, TimeUnit.SECONDS);
            }
            
            // Stop monitoring and analyze
            memoryMonitor.shutdown();
            memoryMonitor.awaitTermination(1, TimeUnit.SECONDS);
            
            // Force cleanup and measure final memory
            System.gc();
            Thread.sleep(2000);
            long finalMemory = runtime.totalMemory() - runtime.freeMemory();
            
            // Validate memory stability
            validateMemoryLeakPrevention(baselineMemory, finalMemory, memorySnapshots);
            
            logger.info(String.format("✓ Memory usage stable: baseline=%dMB, peak=%dMB, final=%dMB", 
                baselineMemory / (1024 * 1024), 
                memorySnapshots.stream().mapToLong(Long::longValue).max().orElse(0) / (1024 * 1024),
                finalMemory / (1024 * 1024)));
            
        } finally {
            executor.shutdown();
            executor.awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    // ========================================
    // Helper Methods
    // ========================================

    private UserSessionResult simulateUserSession(String userId) throws Exception {
        long startTime = System.currentTimeMillis();
        
        try {
            // 1. Create user session / login simulation
            String sessionId = "session-" + userId + "-" + startTime;
            
            // 2. Browse products (simulate search/list operations)
            productService.searchProducts("test", 1, 20);
            
            // 3. Add items to cart
            String cartId = "cart-" + sessionId;
            Cart cart = cartService.createCart(cartId, userId);
            cartService.addItem(cartId, "test-product-1", 1);
            
            // 4. View cart
            cartService.getCart(cartId);
            
            long responseTime = System.currentTimeMillis() - startTime;
            return new UserSessionResult(true, responseTime, "Success");
            
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            return new UserSessionResult(false, responseTime, e.getMessage());
        }
    }

    private long measureCriticalOperationTime() {
        long startTime = System.currentTimeMillis();
        
        try {
            // Execute critical system operations
            productService.searchProducts("performance", 1, 10);
            
            String testCartId = "perf-cart-" + System.currentTimeMillis();
            cartService.createCart(testCartId, "perf-user");
            cartService.addItem(testCartId, "test-product-1", 1);
            cartService.getCart(testCartId);
            
            return System.currentTimeMillis() - startTime;
            
        } catch (Exception e) {
            // Return maximum allowed time on error
            return PEAK_RESPONSE_TIME_MS + 1000;
        }
    }

    private void simulateMemoryIntensiveOperation(int operationId) throws Exception {
        // Create temporary data structures that will be garbage collected
        List<String> tempData = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            tempData.add("temp-data-" + operationId + "-" + i);
        }
        
        // Perform operations that might retain references
        String cartId = "memory-test-cart-" + operationId;
        Cart cart = cartService.createCart(cartId, "memory-user-" + operationId);
        cartService.addItem(cartId, "test-product-1", 1);
        
        // Simulate processing
        Thread.sleep(10);
        
        // Explicitly clear references
        tempData.clear();
    }

    private long calculatePercentile(List<Long> values, int percentile) {
        if (values.isEmpty()) return 0;
        
        List<Long> sorted = new ArrayList<>(values);
        sorted.sort(Long::compareTo);
        
        int index = (int) Math.ceil((percentile / 100.0) * sorted.size()) - 1;
        return sorted.get(Math.max(0, Math.min(index, sorted.size() - 1)));
    }

    private void validateMemoryStability(List<Long> memorySnapshots) {
        if (memorySnapshots.size() < 2) return;
        
        long firstSnapshot = memorySnapshots.get(0);
        long lastSnapshot = memorySnapshots.get(memorySnapshots.size() - 1);
        
        // Memory should not grow by more than 50% over the test period
        double memoryGrowthRatio = (double) lastSnapshot / firstSnapshot;
        assertTrue(memoryGrowthRatio < 1.5,
            String.format("Memory usage should not grow by more than 50%% (grew by %.1f%%)",
                (memoryGrowthRatio - 1) * 100));
    }

    private void validatePerformanceStability(List<Long> responseTimeSnapshots) {
        if (responseTimeSnapshots.size() < 2) return;
        
        // Performance should not degrade by more than 100% over time
        long initialAverage = responseTimeSnapshots.subList(0, Math.min(5, responseTimeSnapshots.size()))
            .stream().mapToLong(Long::longValue).sum() / Math.min(5, responseTimeSnapshots.size());
        
        long finalAverage = responseTimeSnapshots.subList(Math.max(0, responseTimeSnapshots.size() - 5), responseTimeSnapshots.size())
            .stream().mapToLong(Long::longValue).sum() / Math.min(5, responseTimeSnapshots.size());
        
        double performanceDegradation = (double) finalAverage / initialAverage;
        assertTrue(performanceDegradation < 2.0,
            String.format("Performance should not degrade by more than 100%% (degraded by %.1f%%)",
                (performanceDegradation - 1) * 100));
    }

    private void validateMemoryLeakPrevention(long baselineMemory, long finalMemory, List<Long> snapshots) {
        // Final memory should not be more than 200% of baseline after operations
        double memoryIncrease = (double) finalMemory / baselineMemory;
        assertTrue(memoryIncrease < 2.0,
            String.format("Memory usage after operations should not exceed 200%% of baseline (was %.1f%%)",
                memoryIncrease * 100));
        
        // Peak memory should not exceed 300% of baseline
        long peakMemory = snapshots.stream().mapToLong(Long::longValue).max().orElse(finalMemory);
        double peakMemoryRatio = (double) peakMemory / baselineMemory;
        assertTrue(peakMemoryRatio < 3.0,
            String.format("Peak memory usage should not exceed 300%% of baseline (was %.1f%%)",
                peakMemoryRatio * 100));
    }

    private void seedPerformanceTestData() {
        try {
            // Create test products for performance testing
            seedDataForTestCase("performance-test-products");
        } catch (Exception e) {
            logger.warning("Failed to seed performance test data: " + e.getMessage());
        }
    }

    // ========================================
    // Helper Classes
    // ========================================

    private static class UserSessionResult {
        private final boolean successful;
        private final long responseTimeMs;
        private final String message;

        public UserSessionResult(boolean successful, long responseTimeMs, String message) {
            this.successful = successful;
            this.responseTimeMs = responseTimeMs;
            this.message = message;
        }

        public boolean isSuccessful() { return successful; }
        public long getResponseTimeMs() { return responseTimeMs; }
        public String getMessage() { return message; }
    }

    @AfterEach
    void tearDown() {
        // Clean up test data
        clearTestData();
    }

    @AfterAll
    static void tearDownClass() {
        logger.info("=== Problem Statement Performance Compliance Tests Completed ===");
    }
}
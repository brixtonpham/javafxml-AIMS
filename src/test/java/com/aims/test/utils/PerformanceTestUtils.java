package com.aims.test.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * AIMS Phase 4.2: Performance Testing Utilities
 * 
 * Utility class providing common functionality for performance testing across
 * the AIMS Performance & Load Testing Suite.
 * 
 * Features:
 * - Load testing helper methods
 * - Performance metrics calculation
 * - Memory usage monitoring
 * - Test data generation for performance tests
 * - Result formatting and reporting utilities
 */
public class PerformanceTestUtils {

    private static final Logger logger = Logger.getLogger(PerformanceTestUtils.class.getName());

    /**
     * Calculates throughput (operations per second) based on operation count and duration
     */
    public static double calculateThroughput(int operationCount, long durationMs) {
        if (durationMs <= 0) return 0.0;
        return (double) operationCount / (durationMs / 1000.0);
    }

    /**
     * Calculates average response time from a list of response times
     */
    public static double calculateAverageResponseTime(List<Long> responseTimes) {
        if (responseTimes.isEmpty()) return 0.0;
        return responseTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
    }

    /**
     * Calculates 95th percentile response time
     */
    public static long calculateP95ResponseTime(List<Long> responseTimes) {
        if (responseTimes.isEmpty()) return 0;
        
        List<Long> sortedTimes = responseTimes.stream()
            .sorted()
            .collect(java.util.stream.Collectors.toList());
        
        int p95Index = (int) Math.ceil(sortedTimes.size() * 0.95) - 1;
        return sortedTimes.get(Math.max(0, p95Index));
    }

    /**
     * Calculates success rate as a percentage
     */
    public static double calculateSuccessRate(int successfulOps, int totalOps) {
        if (totalOps <= 0) return 0.0;
        return (double) successfulOps / totalOps;
    }

    /**
     * Captures current memory usage in MB
     */
    public static long getCurrentMemoryUsageMB() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        return usedMemory / 1024 / 1024; // Convert to MB
    }

    /**
     * Forces garbage collection and returns memory usage after cleanup
     */
    public static long getCleanMemoryUsageMB() {
        System.gc();
        try {
            Thread.sleep(100); // Give GC time to run
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return getCurrentMemoryUsageMB();
    }

    /**
     * Formats duration in milliseconds to human-readable format
     */
    public static String formatDuration(long durationMs) {
        if (durationMs < 1000) {
            return durationMs + "ms";
        } else if (durationMs < 60000) {
            return String.format("%.2fs", durationMs / 1000.0);
        } else {
            long minutes = durationMs / 60000;
            long seconds = (durationMs % 60000) / 1000;
            return String.format("%dm %ds", minutes, seconds);
        }
    }

    /**
     * Formats percentage with specified decimal places
     */
    public static String formatPercentage(double value, int decimalPlaces) {
        String format = "%." + decimalPlaces + "f%%";
        return String.format(format, value * 100);
    }

    /**
     * Generates unique test ID with timestamp
     */
    public static String generateTestId(String prefix) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS"));
        return prefix + "-" + timestamp;
    }

    /**
     * Validates that a value is within acceptable threshold
     */
    public static boolean isWithinThreshold(double actual, double threshold, String metricName) {
        boolean withinThreshold = actual <= threshold;
        if (!withinThreshold) {
            logger.warning(String.format("%s value %.2f exceeds threshold %.2f", 
                metricName, actual, threshold));
        }
        return withinThreshold;
    }

    /**
     * Validates that a success rate meets minimum requirements
     */
    public static boolean validateSuccessRate(double successRate, double minimumRate, String testName) {
        boolean meetsRequirement = successRate >= minimumRate;
        if (!meetsRequirement) {
            logger.warning(String.format("%s success rate %.2f%% is below minimum %.2f%%", 
                testName, successRate * 100, minimumRate * 100));
        }
        return meetsRequirement;
    }

    /**
     * Logs performance metrics in a standardized format
     */
    public static void logPerformanceMetrics(String testName, PerformanceMetrics metrics) {
        logger.info("Performance Metrics for " + testName + ":");
        logger.info("  Operations: " + metrics.totalOperations + " (" + metrics.successfulOperations + " successful)");
        logger.info("  Success Rate: " + formatPercentage(metrics.successRate, 2));
        logger.info("  Average Response Time: " + String.format("%.2fms", metrics.averageResponseTime));
        logger.info("  Throughput: " + String.format("%.2f ops/sec", metrics.throughput));
        logger.info("  Memory Usage: " + metrics.memoryUsageMB + "MB");
    }

    /**
     * Creates a performance baseline for comparison
     */
    public static PerformanceBaseline createPerformanceBaseline(String testName, 
            List<Long> responseTimes, int totalOps, int successfulOps, long durationMs) {
        
        double averageResponseTime = calculateAverageResponseTime(responseTimes);
        long p95ResponseTime = calculateP95ResponseTime(responseTimes);
        double successRate = calculateSuccessRate(successfulOps, totalOps);
        double throughput = calculateThroughput(successfulOps, durationMs);
        long memoryUsage = getCurrentMemoryUsageMB();
        
        return new PerformanceBaseline(testName, averageResponseTime, p95ResponseTime, 
            successRate, throughput, memoryUsage, LocalDateTime.now());
    }

    /**
     * Compares current performance against baseline
     */
    public static PerformanceComparison compareToBaseline(PerformanceBaseline baseline, 
            double currentAvgResponseTime, double currentSuccessRate, double currentThroughput) {
        
        double responseTimeDelta = (currentAvgResponseTime - baseline.averageResponseTime) / baseline.averageResponseTime;
        double successRateDelta = currentSuccessRate - baseline.successRate;
        double throughputDelta = (currentThroughput - baseline.throughput) / baseline.throughput;
        
        boolean performanceRegression = responseTimeDelta > 0.2 || successRateDelta < -0.1 || throughputDelta < -0.2;
        
        return new PerformanceComparison(baseline, responseTimeDelta, successRateDelta, 
            throughputDelta, performanceRegression);
    }

    /**
     * Waits for a specified duration with interruption handling
     */
    public static void waitForDuration(long duration, TimeUnit unit) {
        try {
            Thread.sleep(unit.toMillis(duration));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warning("Wait interrupted: " + e.getMessage());
        }
    }

    /**
     * Generates load test data for cart operations
     */
    public static LoadTestData generateLoadTestData(int userCount, int operationsPerUser) {
        return new LoadTestData(userCount, operationsPerUser, generateTestId("load-test"));
    }

    // Data Classes

    /**
     * Container for performance metrics
     */
    public static class PerformanceMetrics {
        public final int totalOperations;
        public final int successfulOperations;
        public final double successRate;
        public final double averageResponseTime;
        public final double throughput;
        public final long memoryUsageMB;
        public final LocalDateTime timestamp;

        public PerformanceMetrics(int totalOperations, int successfulOperations, double successRate,
                                double averageResponseTime, double throughput, long memoryUsageMB) {
            this.totalOperations = totalOperations;
            this.successfulOperations = successfulOperations;
            this.successRate = successRate;
            this.averageResponseTime = averageResponseTime;
            this.throughput = throughput;
            this.memoryUsageMB = memoryUsageMB;
            this.timestamp = LocalDateTime.now();
        }
    }

    /**
     * Performance baseline for comparison
     */
    public static class PerformanceBaseline {
        public final String testName;
        public final double averageResponseTime;
        public final long p95ResponseTime;
        public final double successRate;
        public final double throughput;
        public final long memoryUsageMB;
        public final LocalDateTime createdAt;

        public PerformanceBaseline(String testName, double averageResponseTime, long p95ResponseTime,
                                 double successRate, double throughput, long memoryUsageMB, LocalDateTime createdAt) {
            this.testName = testName;
            this.averageResponseTime = averageResponseTime;
            this.p95ResponseTime = p95ResponseTime;
            this.successRate = successRate;
            this.throughput = throughput;
            this.memoryUsageMB = memoryUsageMB;
            this.createdAt = createdAt;
        }
    }

    /**
     * Performance comparison results
     */
    public static class PerformanceComparison {
        public final PerformanceBaseline baseline;
        public final double responseTimeDelta; // Percentage change
        public final double successRateDelta; // Absolute change
        public final double throughputDelta; // Percentage change
        public final boolean performanceRegression;

        public PerformanceComparison(PerformanceBaseline baseline, double responseTimeDelta,
                                   double successRateDelta, double throughputDelta, boolean performanceRegression) {
            this.baseline = baseline;
            this.responseTimeDelta = responseTimeDelta;
            this.successRateDelta = successRateDelta;
            this.throughputDelta = throughputDelta;
            this.performanceRegression = performanceRegression;
        }
    }

    /**
     * Load test configuration data
     */
    public static class LoadTestData {
        public final int userCount;
        public final int operationsPerUser;
        public final String testId;
        public final LocalDateTime createdAt;

        public LoadTestData(int userCount, int operationsPerUser, String testId) {
            this.userCount = userCount;
            this.operationsPerUser = operationsPerUser;
            this.testId = testId;
            this.createdAt = LocalDateTime.now();
        }
    }
}
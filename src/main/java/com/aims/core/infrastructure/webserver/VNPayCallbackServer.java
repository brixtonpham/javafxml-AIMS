package com.aims.core.infrastructure.webserver;

import com.aims.core.infrastructure.adapters.external.payment_gateway.IVNPayAdapter;
import com.aims.core.infrastructure.adapters.external.payment_gateway.VNPayAdapterImpl;
import com.aims.core.infrastructure.adapters.external.payment_gateway.VNPayConfig;
import com.aims.core.application.services.IPaymentService;
import com.aims.core.entities.PaymentTransaction;
import com.aims.core.shared.exceptions.PaymentException;

import javafx.application.Platform;

import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * HTTP Server for handling VNPay payment callback responses
 * Listens on port 8080 for VNPay return URL callbacks
 */
public class VNPayCallbackServer {
    
    private static final int PORT = 8080;
    private static final String CALLBACK_PATH = "/aims/payment/vnpay/return";
    
    private ServerSocket serverSocket;
    private ExecutorService executorService;
    private Future<?> serverTask;
    private volatile boolean isRunning = false;
    
    private final IVNPayAdapter vnPayAdapter;
    private IPaymentService paymentService;
    private Consumer<Map<String, String>> callbackHandler;
    
    public VNPayCallbackServer() {
        this.vnPayAdapter = new VNPayAdapterImpl();
        this.executorService = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "VNPay-Callback-Server");
            t.setDaemon(true); // Don't prevent JVM shutdown
            return t;
        });
    }
    
    /**
     * Set payment service for transaction status updates
     */
    public void setPaymentService(IPaymentService paymentService) {
        this.paymentService = paymentService;
    }
    
    /**
     * Set callback handler for processing VNPay responses
     */
    public void setCallbackHandler(Consumer<Map<String, String>> callbackHandler) {
        this.callbackHandler = callbackHandler;
    }
    
    /**
     * Start the HTTP server to listen for VNPay callbacks
     */
    public synchronized void start() {
        if (isRunning) {
            System.out.println("VNPayCallbackServer: Server already running on port " + PORT);
            return;
        }
        
        try {
            serverSocket = new ServerSocket(PORT);
            isRunning = true;
            
            System.out.println("VNPayCallbackServer: Starting HTTP server on port " + PORT);
            System.out.println("VNPayCallbackServer: Listening for callbacks at " + VNPayConfig.VNP_RETURN_URL);
            
            serverTask = executorService.submit(this::runServer);
            
        } catch (IOException e) {
            System.err.println("VNPayCallbackServer: Failed to start server on port " + PORT + ": " + e.getMessage());
            isRunning = false;
        }
    }
    
    /**
     * Stop the HTTP server
     */
    public synchronized void stop() {
        if (!isRunning) {
            return;
        }
        
        System.out.println("VNPayCallbackServer: Stopping HTTP server");
        isRunning = false;
        
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("VNPayCallbackServer: Error closing server socket: " + e.getMessage());
        }
        
        if (serverTask != null) {
            serverTask.cancel(true);
        }
        
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
    
    /**
     * Check if server is running
     */
    public boolean isRunning() {
        return isRunning;
    }
    
    /**
     * Main server loop - runs in background thread
     */
    private void runServer() {
        while (isRunning && !Thread.currentThread().isInterrupted()) {
            try {
                Socket clientSocket = serverSocket.accept();
                
                // Handle each request in a separate thread to avoid blocking
                executorService.submit(() -> handleRequest(clientSocket));
                
            } catch (SocketException e) {
                if (isRunning) {
                    System.err.println("VNPayCallbackServer: Socket error: " + e.getMessage());
                }
                // Socket closed normally during shutdown
                break;
            } catch (IOException e) {
                if (isRunning) {
                    System.err.println("VNPayCallbackServer: Error accepting connection: " + e.getMessage());
                }
                break;
            }
        }
        
        System.out.println("VNPayCallbackServer: Server stopped");
    }
    
    /**
     * Handle individual HTTP request
     */
    private void handleRequest(Socket clientSocket) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true)) {
            
            // Read HTTP request line
            String requestLine = reader.readLine();
            if (requestLine == null) {
                return;
            }
            
            System.out.println("VNPayCallbackServer: Received request: " + requestLine);
            
            // Parse request
            String[] parts = requestLine.split(" ");
            if (parts.length < 2) {
                sendHttpResponse(writer, 400, "Bad Request", "Invalid HTTP request");
                return;
            }
            
            String method = parts[0];
            String fullPath = parts[1];
            
            // Check if this is our callback path
            if (!fullPath.startsWith(CALLBACK_PATH)) {
                sendHttpResponse(writer, 404, "Not Found", "Path not found: " + fullPath);
                return;
            }
            
            // Extract query parameters
            Map<String, String> params = parseQueryParameters(fullPath);
            
            if (params.isEmpty()) {
                sendHttpResponse(writer, 400, "Bad Request", "No parameters found");
                return;
            }
            
            // Process VNPay callback
            processVNPayCallback(params, writer);
            
        } catch (Exception e) {
            System.err.println("VNPayCallbackServer: Error handling request: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("VNPayCallbackServer: Error closing client socket: " + e.getMessage());
            }
        }
    }
    
    /**
     * Process VNPay callback parameters
     */
    private void processVNPayCallback(Map<String, String> params, PrintWriter writer) {
        try {
            System.out.println("VNPayCallbackServer: Processing VNPay callback with " + params.size() + " parameters");
            
            // Step 1: Validate signature
            if (!vnPayAdapter.validateResponseSignature(params)) {
                System.err.println("VNPayCallbackServer: Invalid signature in VNPay response");
                sendHttpResponse(writer, 400, "Invalid Signature", 
                    "<html><body><h1>Payment Verification Failed</h1><p>The payment response signature is invalid.</p></body></html>");
                return;
            }
            
            // Step 2: Extract key parameters
            String responseCode = params.get("vnp_ResponseCode");
            String txnRef = params.get("vnp_TxnRef");
            String transactionNo = params.get("vnp_TransactionNo");
            
            System.out.println("VNPayCallbackServer: VNPay response - Code: " + responseCode + 
                             ", TxnRef: " + txnRef + ", TransactionNo: " + transactionNo);
            
            // Step 3: Update transaction status if payment service is available
            if (paymentService != null && txnRef != null) {
                updateTransactionStatus(txnRef, responseCode, transactionNo, params);
            }
            
            // Step 4: Send response to browser and handle callback
            if ("00".equals(responseCode)) {
                // Payment successful
                sendPaymentResultPage(writer, true, "Payment completed successfully!", params);
            } else {
                // Payment failed or cancelled
                String message = getFailureMessage(responseCode);
                sendPaymentResultPage(writer, false, message, params);
            }
            
            // Step 5: Notify callback handler on JavaFX Application Thread
            if (callbackHandler != null) {
                Platform.runLater(() -> callbackHandler.accept(params));
            }
            
        } catch (Exception e) {
            System.err.println("VNPayCallbackServer: Error processing callback: " + e.getMessage());
            e.printStackTrace();
            sendHttpResponse(writer, 500, "Internal Server Error", 
                "<html><body><h1>Processing Error</h1><p>An error occurred while processing the payment result.</p></body></html>");
        }
    }
    
    /**
     * Update transaction status in database
     */
    private void updateTransactionStatus(String vnpTxnRef, String responseCode, String transactionNo, Map<String, String> params) {
        try {
            // Extract AIMS transaction ID from vnpTxnRef (format: orderId_timestamp)
            String orderId = vnpTxnRef.split("_")[0];
            
            // Map VNPay response codes to internal statuses
            String status = mapResponseCodeToStatus(responseCode);
            
            System.out.println("VNPayCallbackServer: Updating transaction status for order " + orderId + 
                             " to " + status + " (VNPay code: " + responseCode + ")");
            
            // Note: We would need to implement updateTransactionStatusByOrderId method in PaymentService
            // For now, log the action
            System.out.println("VNPayCallbackServer: Transaction status update would be implemented here");
            
        } catch (Exception e) {
            System.err.println("VNPayCallbackServer: Error updating transaction status: " + e.getMessage());
        }
    }
    
    /**
     * Map VNPay response codes to internal transaction statuses
     */
    private String mapResponseCodeToStatus(String responseCode) {
        switch (responseCode) {
            case "00":
                return "SUCCESS";
            case "24":
                return "CANCELLED";
            case "07":
                return "PENDING";
            default:
                return "FAILED";
        }
    }
    
    /**
     * Get user-friendly failure message based on VNPay response code
     */
    private String getFailureMessage(String responseCode) {
        switch (responseCode) {
            case "07":
                return "Payment is being processed. Please wait a few minutes.";
            case "09":
                return "Your bank has declined the transaction.";
            case "10":
                return "Card authentication failed.";
            case "11":
                return "Transaction has expired.";
            case "12":
                return "Account is temporarily locked.";
            case "13":
                return "Invalid OTP entered.";
            case "24":
                return "Transaction was cancelled by user.";
            case "51":
                return "Insufficient funds in account.";
            case "65":
                return "Daily transaction limit exceeded.";
            case "75":
                return "Payment bank is under maintenance.";
            case "79":
                return "Transaction amount is incorrect.";
            default:
                return "Payment failed with code: " + responseCode;
        }
    }
    
    /**
     * Parse URL query parameters
     */
    private Map<String, String> parseQueryParameters(String fullPath) {
        Map<String, String> params = new HashMap<>();
        
        try {
            int queryStart = fullPath.indexOf('?');
            if (queryStart == -1) {
                return params;
            }
            
            String query = fullPath.substring(queryStart + 1);
            String[] pairs = query.split("&");
            
            for (String pair : pairs) {
                String[] keyValue = pair.split("=", 2);
                if (keyValue.length == 2) {
                    String key = URLDecoder.decode(keyValue[0], "UTF-8");
                    String value = URLDecoder.decode(keyValue[1], "UTF-8");
                    params.put(key, value);
                }
            }
        } catch (Exception e) {
            System.err.println("VNPayCallbackServer: Error parsing query parameters: " + e.getMessage());
        }
        
        return params;
    }
    
    /**
     * Send payment result page to browser
     */
    private void sendPaymentResultPage(PrintWriter writer, boolean success, String message, Map<String, String> params) {
        String title = success ? "Payment Successful" : "Payment Failed";
        String statusClass = success ? "success" : "error";
        String statusIcon = success ? "✓" : "✗";
        
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html>\n<head>\n");
        html.append("<meta charset='UTF-8'>\n");
        html.append("<title>").append(title).append("</title>\n");
        html.append("<style>\n");
        html.append("body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f5f5f5; }\n");
        html.append(".container { max-width: 600px; margin: 0 auto; background: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }\n");
        html.append(".status { text-align: center; margin-bottom: 30px; }\n");
        html.append(".success { color: #28a745; }\n");
        html.append(".error { color: #dc3545; }\n");
        html.append(".status-icon { font-size: 48px; margin-bottom: 10px; }\n");
        html.append(".status-title { font-size: 24px; font-weight: bold; margin-bottom: 10px; }\n");
        html.append(".message { font-size: 16px; line-height: 1.5; }\n");
        html.append(".details { margin-top: 30px; padding: 20px; background-color: #f8f9fa; border-radius: 5px; }\n");
        html.append(".detail-row { margin-bottom: 10px; }\n");
        html.append(".detail-label { font-weight: bold; color: #666; }\n");
        html.append(".actions { text-align: center; margin-top: 30px; }\n");
        html.append(".btn { display: inline-block; padding: 12px 24px; margin: 5px; text-decoration: none; border-radius: 5px; font-weight: bold; }\n");
        html.append(".btn-primary { background-color: #007bff; color: white; }\n");
        html.append(".btn-secondary { background-color: #6c757d; color: white; }\n");
        html.append("</style>\n");
        html.append("</head>\n<body>\n");
        
        html.append("<div class='container'>\n");
        html.append("<div class='status ").append(statusClass).append("'>\n");
        html.append("<div class='status-icon'>").append(statusIcon).append("</div>\n");
        html.append("<div class='status-title'>").append(title).append("</div>\n");
        html.append("<div class='message'>").append(message).append("</div>\n");
        html.append("</div>\n");
        
        // Payment details
        html.append("<div class='details'>\n");
        html.append("<h3>Transaction Details</h3>\n");
        if (params.get("vnp_TxnRef") != null) {
            html.append("<div class='detail-row'><span class='detail-label'>Order Reference:</span> ").append(params.get("vnp_TxnRef")).append("</div>\n");
        }
        if (params.get("vnp_TransactionNo") != null) {
            html.append("<div class='detail-row'><span class='detail-label'>Transaction ID:</span> ").append(params.get("vnp_TransactionNo")).append("</div>\n");
        }
        if (params.get("vnp_Amount") != null) {
            try {
                double amount = Double.parseDouble(params.get("vnp_Amount")) / 100.0;
                html.append("<div class='detail-row'><span class='detail-label'>Amount:</span> ").append(String.format("%.0f VND", amount)).append("</div>\n");
            } catch (NumberFormatException e) {
                html.append("<div class='detail-row'><span class='detail-label'>Amount:</span> ").append(params.get("vnp_Amount")).append("</div>\n");
            }
        }
        if (params.get("vnp_BankCode") != null) {
            html.append("<div class='detail-row'><span class='detail-label'>Bank:</span> ").append(params.get("vnp_BankCode")).append("</div>\n");
        }
        html.append("</div>\n");
        
        // Actions
        html.append("<div class='actions'>\n");
        html.append("<p>You can now close this window and return to the AIMS application.</p>\n");
        html.append("<a href='javascript:window.close()' class='btn btn-primary'>Close Window</a>\n");
        html.append("</div>\n");
        
        html.append("</div>\n");
        html.append("</body>\n</html>");
        
        sendHttpResponse(writer, 200, "OK", html.toString());
    }
    
    /**
     * Send HTTP response
     */
    private void sendHttpResponse(PrintWriter writer, int statusCode, String statusText, String content) {
        writer.println("HTTP/1.1 " + statusCode + " " + statusText);
        writer.println("Content-Type: text/html; charset=UTF-8");
        writer.println("Content-Length: " + content.getBytes().length);
        writer.println("Connection: close");
        writer.println(); // Empty line to separate headers from body
        writer.println(content);
        writer.flush();
    }
}
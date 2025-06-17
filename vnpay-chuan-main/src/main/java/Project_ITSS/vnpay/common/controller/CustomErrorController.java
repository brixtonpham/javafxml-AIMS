/**
 * Custom error controller for handling application errors
 * Provides custom error page and handling for different error scenarios
 */
package Project_ITSS.vnpay.common.controller;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller that handles errors in the application
 * Implements Spring's ErrorController interface for custom error handling
 */
@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> handleError(HttpServletRequest request) {
        Map<String, Object> errorAttributes = new HashMap<>();
        
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        Object requestUri = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        
        if (status != null) {
            Integer statusCode = Integer.valueOf(status.toString());
            errorAttributes.put("status", statusCode);
            errorAttributes.put("error", HttpStatus.valueOf(statusCode).getReasonPhrase());
        } else {
            errorAttributes.put("status", 500);
            errorAttributes.put("error", "Internal Server Error");
        }
        
        errorAttributes.put("message", message != null ? message.toString() : "An error occurred");
        errorAttributes.put("path", requestUri != null ? requestUri.toString() : "unknown");
        errorAttributes.put("timestamp", System.currentTimeMillis());
        
        if (exception != null) {
            errorAttributes.put("exception", exception.getClass().getSimpleName());
        }
        
        HttpStatus httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        if (status != null) {
            try {
                httpStatus = HttpStatus.valueOf(Integer.valueOf(status.toString()));
            } catch (Exception e) {
                // Default to 500 if parsing fails
            }
        }
        
        return ResponseEntity.status(httpStatus).body(errorAttributes);
    }
}
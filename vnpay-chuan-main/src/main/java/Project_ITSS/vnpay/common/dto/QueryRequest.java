/**
 * DTO class for transaction query requests
 * Used to query transaction status from VNPAY
 */
package Project_ITSS.vnpay.common.dto;

/**
 * Transaction query request parameters
 * Contains fields needed to query a transaction's status
 */
public class QueryRequest {
    /** Order ID/Transaction reference to query */
    private String orderId;
    
    /** Transaction date in yyyyMMddHHmmss format */
    private String transDate;

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getTransDate() {
        return transDate;
    }

    public void setTransDate(String transDate) {
        this.transDate = transDate;
    }
}
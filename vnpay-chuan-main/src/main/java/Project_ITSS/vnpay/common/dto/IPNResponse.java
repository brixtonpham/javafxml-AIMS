/**
 * DTO class for IPN (Instant Payment Notification) response
 * Used to send status back to VNPAY after processing IPN
 */
package Project_ITSS.vnpay.common.dto;

/**
 * Response object for IPN processing
 * Contains status codes and messages according to VNPAY's IPN specification
 */
public class IPNResponse {
    /** Response code to send back to VNPAY */
    private String RspCode;
    
    /** Response message explaining the result */
    private String Message;
    private String Checksum;

    public IPNResponse(String rspCode, String message) {
        this.RspCode = rspCode;
        this.Message = message;
    }

    public String getRspCode() {
        return RspCode;
    }

    public String getCode() {
        return getRspCode();
    }

    public void setRspCode(String rspCode) {
        this.RspCode = rspCode;
    }

    public String getMessage() {
        return Message;
    }

    public void setMessage(String message) {
        this.Message = message;
    }

    public String getChecksum() {
        return Checksum;
    }

    public void setChecksum(String checksum) {
        this.Checksum = checksum;
    }

    // Static response codes as per requirements
    public static final String SUCCESS = "00";
    public static final String ORDER_NOT_FOUND = "01";
    public static final String ORDER_ALREADY_CONFIRMED = "02"; 
    public static final String INVALID_AMOUNT = "04";
    public static final String INVALID_SIGNATURE = "97";
    public static final String UNKNOWN_ERROR = "99";
}
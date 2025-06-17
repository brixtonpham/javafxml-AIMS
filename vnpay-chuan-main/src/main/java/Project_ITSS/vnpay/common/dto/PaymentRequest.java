/**
 * DTO class for payment request parameters
 * Contains all required fields for creating a VNPAY payment
 */
package Project_ITSS.vnpay.common.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Payment request data transfer object
 * Holds all parameters needed to create a payment with VNPAY
 */
public class PaymentRequest {
    @NotNull(message = "Amount cannot be null")
    @Pattern(regexp = "^[0-9]+$", message = "Amount must be numeric")
    /** Payment amount in VND */
    private String amount;
    
    /** Bank code for direct bank payment (optional) */
    private String bankCode;
    
    @NotNull(message = "Language cannot be null")
    @Pattern(regexp = "^(vn|en)$", message = "Language must be 'vn' or 'en'")
    /** Interface language (vn/en) */
    private String language;
    
    @NotNull(message = "Version cannot be null")
    /** VNPAY API version */
    private String vnp_Version;
    
    /** Payment expiry date in yyyyMMddHHmmss format */
    private String vnp_ExpireDate;

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getBankCode() {
        return bankCode;
    }

    public void setBankCode(String bankCode) {
        this.bankCode = bankCode;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getVnp_Version() {
        return vnp_Version;
    }

    public void setVnp_Version(String vnp_Version) {
        this.vnp_Version = vnp_Version;
    }

    public String getVnp_ExpireDate() {
        return vnp_ExpireDate;
    }

    public void setVnp_ExpireDate(String vnp_ExpireDate) {
        this.vnp_ExpireDate = vnp_ExpireDate;
    }
}